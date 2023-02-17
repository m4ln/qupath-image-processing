/**
 * Script to export pixels & annotations for whole slide images.
 *
 * The image can optionally be tiled during export, so that even large images can be exported at high resolution.
 * (Note: In this case 'tiled' means as separate, non-overlapping images... not a single, tiled pyramidal image.)
 *
 * The downsample value and coordinates are encoded in each image file name.
 *
 * The annotations are exported as 8-bit labelled images.
 * These labels depend upon annotation classifications; a text file giving the key is written for reference.
 *
 * The labelled image can also optionally use indexed colors to depict the colors of the
 * original classifications within QuPath for easier visualization & comparison.
 *
 * @author Pete Bankhead
 */


// Imports
import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.DataBufferByte
import java.awt.image.IndexColorModel

// ================================ INITIALIZE PARAMETERS HERE =========================================================
// Output directory for storing the tiles
def saveDir = '/Z:/marlen/histoSeg/urothel/qupath/extracted_patches_group1'

// set a value to limit the number of tiles to export
// Todo bugfix: works correctly but doesn't keep in mind background images, so actually number extracted might be much lower
int maxTiles = 30000

// downsample factor (changes the resolution: < 1 higher resolution; 1 full resolution; > 1 lower resolution)
double downsample = 3
// size of each tile, in pixels
int tileSize = 512

// overlap, in pixel units at the export resolution, (0 = no overlap)
int overlapSize = tileSize/1.2

// remove background images, i.e when the labeled pixels inside the mask covers less than backgroundThreshold [%] of the total area of the mask
boolean removeBackgroundImages = true
double backgroundThreshold = 0.3

// Ignore annotations that don't have a classification set
boolean skipUnclassifiedAnnotations = true

// Skip tiles without annotations (WARNING: all image tiles will be written, this may take a lot of time and memory)
boolean skipUnannotatedTiles = true

// Create an 8-bit indexed label image
// This is very useful for display/previewing - although need to be careful when importing into other software,
// which can prefer to replaces labels with the RGB colors they refer to
boolean createIndexedImageLabels = true

// Export the original pixels (assumed to be RGB) for each tile
boolean exportOriginalPixels = true
// NOTE: The following parameter only matters if exportOriginalPixels == true
// Define image export type; valid values are JPG, PNG or null (if no image region should be exported with the mask)
// Note: masks will always be exported as PNG
def imageFormat = 'PNG'

// export tiles from whole project (instead of single file selected)
boolean exportProject = true

// in case exportProject is false, define the number of the image to export (starting from 0) 
int imageNumber = 2

// set true to save tiles for each WSI in seperate folder
boolean storeTilesSeperately = false

// =====================================================================================================================

// ================================ DO NOT CHANGE ANYTHING BELOW THIS LINE ==============================================

// Checking and writing inputs
if(downsample < 1){
    print('WARNING: downsample factor < 1, will be set to 1')
    downsample = 1
    }

// get the list of images to export (either from project or from single file)
def imageList

if (exportProject){
    imageList = project.getImageList()
}
else {
    //imageList = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName()) 
    imageList = project.getImageList()[imageNumber]
}

println('WSI files to export: ' + imageList)

// loop over all image files
for (image in imageList) {
    print('\nexporting file: ' + image)
    
    // prepare stringBuilder to write parameters into text-file
    def parameterInfo = new StringBuilder()
    parameterInfo   << '\n\ndownsample' << '\t' << downsample << System.lineSeparator()
                    << 'tileSize' << '\t' << tileSize << System.lineSeparator()
                    << 'skipUnclassifiedAnnotations' << '\t' << skipUnclassifiedAnnotations << System.lineSeparator()
                    << 'skipUnannotatedTiles' << '\t' << skipUnannotatedTiles << System.lineSeparator()
                    << 'createIndexedImageLabels' << '\t' << createIndexedImageLabels << System.lineSeparator()
                    << 'exportOriginalPixels' << '\t' << exportOriginalPixels << System.lineSeparator()
    if(exportOriginalPixels)
        parameterInfo   << 'imageFormat' << '\t' << imageFormat << System.lineSeparator()
    
    println parameterInfo
    
    // Get the main QuPath data structures
    def imageData = image.readImageData()
    def hierarchy = imageData.getHierarchy()
    def server = imageData.getServer()
    
    // create output directory
    def wsi_name
    def pathOutput
    folder_name =  "sz" + tileSize.toString() + "_ds" + downsample.toString() + "_os" + overlapSize.toString() + "_bg" + backgroundThreshold.toString() 
    if (storeTilesSeperately) {
        wsi_name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
        pathOutput = buildFilePath(saveDir, wsi_name, folder_name)
    }
    else {
        pathOutput = buildFilePath(saveDir, folder_name)
    }

    mkdirs(pathOutput)
    
    // Get the annotations that have ROIs & classifications (if required)
    def annotations = hierarchy.getFlattenedObjectList(null).findAll {
        it.isAnnotation() && it.hasROI() && (!skipUnclassifiedAnnotations || it.getPathClass() != null) }
    
    // Get all the represented classes
    def pathClasses = annotations.collect({it.getPathClass()}) as Set
    
    // We can't handle more than 255 classes (because of 8-bit representation)
    if (pathClasses.size() > 255) {
        print 'Sorry! Cannot handle > 255 classications - number here is ' + pathClasses.size()
        return
    }
    
    // For each classification, create an integer label >= 1 & cache a color for drawing it
    // Also, create a LUT for visualizing more easily
    def labelKey = new StringBuilder()
    def pathClassColors = [:]
    // Used to store the RGB values for an IndexColorModel
    int n = pathClasses.size() + 1
    def r = ([(byte)0] * n) as byte[]
    def g = r.clone()
    def b = r.clone()
    def a = r.clone()
    // print variables
    println 'Number of classes: ' + pathClasses.size()
    println 'Number of annotations: ' + annotations.size()

    pathClasses.eachWithIndex{ PathClass entry, int i ->
        // Record integer label for key
        int label = i+1
        String name = entry == null ? 'None' : entry.toString()
        labelKey << name << '\t' << label << System.lineSeparator()
        // Store Color based on label - needed for painting with Graphics2D
        pathClassColors.put(entry, new Color(label, label, label))
    
        // Update RGB values for IndexColorModel
        // Use gray as the default color indicating no classification
        int rgb = entry == null ? ColorTools.makeRGB(127, 127, 127) : entry.getColor()
    
        r[label] = ColorTools.red(rgb)
        g[label] = ColorTools.green(rgb)
        b[label] = ColorTools.blue(rgb)
        a[label] = 255
    }

    int patchSize = (int)(tileSize * downsample)
    int stepSize = (int)(tileSize * downsample) - overlapSize
    println '\npatchSize: ' + patchSize
    println 'stepSize: ' + stepSize

    // Create the RegionRequests, i.e the image patches from the whoe image
    int tiles_cnt = 0
    def requests = new ArrayList<RegionRequest>()
    for (int y = 0; y < server.getHeight(); y += stepSize) {
        int h = patchSize
        if (y + h > server.getHeight())
            // skip last patch if it is smaller than patchSize (if image size is not a multiple of patchSize)
            continue
            // last patch will be smaller than patchSize if image size is not a multiple of patchSize 
            // h = server.getHeight() - y
        for (int x = 0; x < server.getWidth(); x += stepSize) {
            int w = patchSize
            if (x + w > server.getWidth())
                // skip last patch if it is smaller than patchSize (if image size is not a multiple of patchSize)
                continue
                // last patch will be smaller than patchSize if image size is not a multiple of patchSize 
                // w = server.getWidth() - x

            requests << RegionRequest.createInstance(server.getPath(), downsample, x, y, w, h)

            tiles_cnt++
            if (maxTiles > 0 && tiles_cnt >= maxTiles)
                break
        }
    }
    println('\nnumber of tiles to extract: ' + tiles_cnt)
    
    // Write the label 'keys'
    println '\nlabelKeys:\n' + labelKey
    String imgName = server.getMetadata().getName()
    def keyName = String.format('%s_(tileSize=%d,downsample=%.3f,overlap=%d).txt', imgName, tileSize, downsample, overlapSize)
    def fileLabels = new File(pathOutput, keyName)
    fileLabels.text = parameterInfo.toString() + labelKey.toString()
    
    // Handle the requests in parallel
    requests.parallelStream().forEach { request ->
        // Create a suitable base image name
        String name = String.format('%s_(%.2f,%d,%d,%d,%d)',
                imgName,
                request.getDownsample(),
                request.getX(),
                request.getY(),
                request.getWidth(),
                request.getHeight()
        )

        // Export the raw image pixels if necessary
        // If we do this, store the width & height - to make sure we have an exact match
        int width = -1
        int height = -1
    
        // Export the labelled tiles if necessary
        // Calculate dimensions if we don't know them already
        if (width < 0 || height < 0) {
            width = Math.round(request.getWidth() / downsample)
            height = Math.round(request.getHeight() / downsample)
        }
        // Fill the annotations with the appropriate label
        def imgMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY)
        def g2d = imgMask.createGraphics()
        g2d.setClip(0, 0, width, height)
        g2d.scale(1.0/downsample, 1.0/downsample)
        g2d.translate(-request.getX(), -request.getY())
        int count = 0
        // iterate through all annotations
        for (annotation in annotations) {
            // // if annotation does not contain pathClass bladder wall, skip it
            // if (!annotation.getPathClass().toString().equals('perivesicular fat'))
            //     continue
            def roi = annotation.getROI()
            if (!request.intersects(roi.getBoundsX(), roi.getBoundsY(), roi.getBoundsWidth(), roi.getBoundsHeight()))
                continue
            def shape = RoiTools.getShape(roi)
            def color = pathClassColors.get(annotation.getPathClass())
            g2d.setColor(color)
            g2d.fill(shape)
            count++
        }
        g2d.dispose()
        if (count > 0 || !skipUnannotatedTiles) {
            // Extract the bytes from the image
            def buf = imgMask.getRaster().getDataBuffer() as DataBufferByte
            def bytes = buf.getData()
            // Check if we actually have any non-zero pixels, if necessary -
            // we might not if the annotation bounding box intersected the region, but the annotation itself does not
            if (skipUnannotatedTiles && !bytes.any { it != (byte)0 })
                return
            // If we want an indexed color image, create one using the data buffer & LUT
            if (createIndexedImageLabels) {
                def colorModel = new IndexColorModel(8, n, r, g, b, a)
                def imgMaskColor = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel)
                System.arraycopy(bytes, 0, imgMaskColor.getRaster().getDataBuffer().getData(), 0, width*height)
                imgMask = imgMaskColor
                
                // don't write images which have a lot of background if true
                if (removeBackgroundImages) {
                    int pixel_count = 0
                    for (int i = 0; i < bytes.length; i++) {
                        if (bytes[i] != 0)
                            pixel_count++
                    }
                    if (pixel_count < width * height * backgroundThreshold)
                        return
                }

                // Write the mask
                def fileOutput = new File(pathOutput, name + '-labels.png')
                ImageIO.write(imgMask, 'PNG', fileOutput)
            }

            if (exportOriginalPixels) {
                def img = server.readBufferedImage(request)
                width = img.getWidth()
                height = img.getHeight()
                fileOutput = new File(pathOutput, name + '.' + imageFormat.toLowerCase())
                ImageIO.write(img, imageFormat, fileOutput)
            }
        }
    }
}  
print 'Done all!\n'
