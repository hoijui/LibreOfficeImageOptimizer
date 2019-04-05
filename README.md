# Libre- and Open-Office image optimizer

This is an utility that reduces the file-size of images
contained within a Libre-/Open-Office document,
in order to cut down on the overall file-size,
and to save memory and speed up working on the document.

## Building

```bash
mvn package
```

The resulting "binary" will be at
_target/libreofficeimageoptimizer-*-jar-with-dependencies.jar_.

## Usage

### Examples

```bash
java -cp libreofficeimageoptimizer-*-jar-with-dependencies.jar \
        net.hoijui.libreofficeimageoptimizer.Optimizer \
        my-oo-file-with-huge-images.ods \
        optimized-output.ods
```

## How it works

It simply:

 1. unzips the Libre-/Open-Office document to a temporary directory
 2. resizes the large images, making them smaller
 3. re-zips the archive into an optimized quasi copy
    of the original document
