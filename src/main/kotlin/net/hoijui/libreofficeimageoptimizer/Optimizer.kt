/*
 * Copyright (C) 2019, The authors of the LibreOfficeImageOptimizer project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.hoijui.libreofficeimageoptimizer

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import java.io.File
import java.nio.file.Files
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.io.IOException
import java.awt.AlphaComposite
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class Optimizer {

    fun optimizeFile(inFile: File, outFile: File) {

        // create a temporary directory for extracting the archive
        val tmpArchiveRootDir = Files.createTempDirectory(javaClass.canonicalName + "-")
        tmpArchiveRootDir.toFile().deleteOnExit()

        // extract the archive
        val input = ArchiveStreamFactory().createArchiveInputStream(inFile.inputStream().buffered())
        extractArchive(input, tmpArchiveRootDir)
        input.close()

        // optimize
        optimizeArchive(tmpArchiveRootDir)

        // re-pack the archive
        val output = ArchiveStreamFactory().createArchiveOutputStream(
            ArchiveStreamFactory.ZIP,
            outFile.outputStream()
        )
        packArchive(tmpArchiveRootDir, output)
    }

    private fun optimizeArchive(archiveRootDir: Path) {

        Files.list(archiveRootDir.resolve("Pictures")).forEach {
            optimizeImage(it)
        }
    }

    private fun optimizeImage(path: Path) {

        LOG.info("Optimizing '{}' ...", path.toAbsolutePath())
        val origImage = readImage(path)
        if ((origImage.width > 800) or (origImage.height > 800)) {
            val imageType = extractImageType(path)
            val newWidth: Int
            val newHeight: Int
            if (origImage.width > origImage.height) {
                newWidth = 800
                newHeight = origImage.height * newWidth / origImage.width
            } else {
                newHeight = 800
                newWidth = origImage.width * newHeight / origImage.height
            }
            val smallerImage = createResizedCopy(origImage, newWidth, newHeight, true)
            writeImage(smallerImage, imageType, path)
        }
    }

    fun printHelp() {

        LOG.info("This is an utility that reduces the file size of images")
        LOG.info("contained withing a Libre- or Open-Office file.")
        LOG.info("")
        LOG.info("Usage:")
        LOG.info("\tOptimizer [in-file] <out-file>")
        LOG.info("")
        LOG.info("examples:")
        LOG.info("\tOptimizer myOOFile.opm")
        LOG.info("\tOptimizer myOOFile.opm myOOFile_small.opm")
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(this::class.java.canonicalName)!!

        /**
         * TODO
         */
        @JvmStatic
        fun main(args: Array<String>) {

            val optimizer = Optimizer()
            if (args.isEmpty()) {
                optimizer.printHelp()
            } else {
                val inFile = File(args[0])
                val outFile = if (args.size > 1) {
                    File(args[1])
                } else {
                    val filePath = inFile.toString()
                    File(
                        filePath.substringBeforeLast('.')
                                + "_optimized."
                                + filePath.substringAfterLast('.')
                    )
                }
                optimizer.optimizeFile(inFile, outFile)
            }
        }

        private fun readImage(imgFilePath: Path): BufferedImage {
            return ImageIO.read(imgFilePath.toFile())
        }

        private fun extractImageType(imgFilePath: Path): String {
            return imgFilePath.toString().substringAfterLast('.')
        }

        private fun writeImage(image: BufferedImage, imageType: String, imgFilePath: Path) {
            ImageIO.write(image, imageType, imgFilePath.toFile())
        }

        fun createResizedCopy(
            originalImage: Image,
            scaledWidth: Int, scaledHeight: Int,
            preserveAlpha: Boolean
        ): BufferedImage {

            val imageType = if (preserveAlpha) BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
            val scaledBI = BufferedImage(scaledWidth, scaledHeight, imageType)
            val g = scaledBI.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            if (preserveAlpha) {
                g.composite = AlphaComposite.Src
            }
            g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null)
            g.dispose()
            return scaledBI
        }

        fun extractArchive(inStream: ArchiveInputStream, outDir: Path) {

            var entry: ArchiveEntry? = inStream.nextEntry
            while (entry != null) {
                if (!inStream.canReadEntryData(entry)) {
                    // log something?
                    entry = inStream.nextEntry
                    continue
                }
                val pathEntryOutput = outDir.resolve(entry.name)

                val f = pathEntryOutput.toFile()
                if (entry.isDirectory) {
                    if (!f.isDirectory && !f.mkdirs()) {
                        throw IOException("failed to create directory $f")
                    }
                } else {
                    val parent = f.parentFile
                    if (!parent.isDirectory && !parent.mkdirs()) {
                        throw IOException("failed to create directory $parent")
                    }
                    Files.newOutputStream(f.toPath()).use { o -> IOUtils.copy(inStream, o) }
                }
                entry = inStream.nextEntry
            }
        }

        fun packArchive(inDir: Path, outStream: ArchiveOutputStream) {

            Files.walk(inDir).forEach {
                val file = it.toFile()
                // maybe skip directories for formats like AR that don't store directories
                val entry = outStream.createArchiveEntry(file, inDir.relativize(it).toString())
                // potentially add more flags to entry
                outStream.putArchiveEntry(entry)
                if (file.isFile) {
                    Files.newInputStream(file.toPath()).use { i -> IOUtils.copy(i, outStream) }
                }
                outStream.closeArchiveEntry()
            }
            outStream.finish()
        }
    }
}
