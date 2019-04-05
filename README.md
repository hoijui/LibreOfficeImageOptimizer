# Libre- and Open-Office image optimizer

This is an utility that reduces the file-size of images
contained within a Libre-/Open-Office document,
in order to cut down on the overall file-size,
and to save memory and speed up working on the document.

## How to use:

```bash
java -cp libreofficeimageoptimizer-*.jar \
        net.hoijui.libreofficeimageoptimizer.Optimizer \
        my-oo-file-with-huge-images.opm \
        optimized-output.opm
```
