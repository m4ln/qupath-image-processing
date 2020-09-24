# qupath scripting with Groovy for histological image processing

**_cropper:_**  
Export cropped areas from whole slide image (WSI) as tiff files.
Based on the code by Sara McArdle [1]

**_exportAnnotatedTiles:_**  
Script to export pixels & annotations for WSI.
 
The image can optionally be tiled during export, so that even large images can be exported at high resolution.
(Note: In this case 'tiled' means as separate, non-overlapping images... not a single, tiled pyramidal image.)
 
The downsample value and coordinates are encoded in each image file name.

The annotations are exported as 8-bit labelled images.
These labels depend upon annotation classifications; a text file giving the key is written for reference.

The labelled image can also optionally use indexed colors to depict the colors of the
original classifications within QuPath for easier visualization & comparison.  
 
Based on the code by Pete Bankhead [2]  

**_exportTiles:_**  
Export image tiles from WSI [3]  

## References
[[1]](https://github.com/saramcardle/Image-Analysis-Scripts/blob/master/QuPath%20Groovy%20Scripts/QuPath%200.1.2/Cropper.groovy)

[[2]](https://petebankhead.github.io/qupath/scripting/2018/03/14/script-export-labelled-images.html)

[[3]](https://qupath.readthedocs.io/en/latest/docs/advanced/exporting_images.html)
