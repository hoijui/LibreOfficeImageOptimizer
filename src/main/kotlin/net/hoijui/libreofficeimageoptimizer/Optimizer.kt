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

class Optimizer {

    fun optimizeFile(inFile: File, outFile: File) {

        // create a temporary directory for extracting the archive
        val tmpArchiveRootDir = Files.createTempDirectory(javaClass.canonicalName + "-")
        tmpArchiveRootDir.toFile().deleteOnExit()

        // extract the archive
        try {
            val input = ArchiveStreamFactory().createArchiveInputStream(inFile.inputStream())
            extractArchive(input, tmpArchiveRootDir)
            input.close()
        } catch (ex: Exception) {
            LOG.error("Failed to read archive ${inFile.absolutePath}")
        }

        // optimize
        optimize(tmpArchiveRootDir)

        // re-pack the archive
        try {
            val output = ArchiveStreamFactory().createArchiveOutputStream(ArchiveStreamFactory.ZIP,
                    outFile.outputStream())
            packArchive(tmpArchiveRootDir, output)

            tmpArchiveRootDir.toFile().deleteRecursively()
        } catch (ex: Exception) {
            LOG.error("Failed to read archive ${inFile.absolutePath}")
        }
    }

    fun optimize(path: Path) {
        TODO()
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

        private val LOG= LoggerFactory.getLogger(this::class.java.canonicalName)!!

        /**
         * TODO
         */
        @JvmStatic
        fun main(args : Array<String>) {

            val optimizer = Optimizer()
            if (args.isEmpty()) {
                optimizer.printHelp()
            } else {
                val inFile = File(args[0])
                val outFile = if (args.size > 1) {
                    File(args[1])
                } else {
                    val filePath = inFile.toString()
                    File(filePath.substringBeforeLast('.')
                            + "_optimized."
                            + filePath.substringAfterLast('.'))
                }
                optimizer.optimizeFile(inFile, outFile)
            }
        }

        fun extractArchive(inStream: ArchiveInputStream, outDir : Path) {

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

        fun packArchive(inDir : Path, outStream: ArchiveOutputStream) {

            inDir.forEach {
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
