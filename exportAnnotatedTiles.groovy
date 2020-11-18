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
import qupath.lib.images.servers.ImageServer
import qupath.lib.objects.PathObject

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.DataBufferByte
import java.awt.image.IndexColorModel

// ================================ INITIALIZE PARAMETERS HERE =========================================================
// Output directory for storing the tiles
def saveDir = '/Z:/Marlen/datasets/beauty-and-beast/'

// set downsample = 1 to use the full resolution and downsample > 1 for lower resolution
double downsample = 1

// Maximum size of an image tile when exporting
int maxTileSize = 256

// Ignore annotations that don't have a classification set
boolean skipUnclassifiedAnnotations = false

// Skip tiles without annotations (WARNING: all image tiles will be written, this may take a lot of time and memory)
boolean skipUnannotatedTiles = true

// Create an 8-bit indexed label image
// This is very useful for display/previewing - although need to be careful when importing into other software,
// which can prefer to replaces labels with the RGB colors they refer to
boolean createIndexedImageLabels = false

// Export the original pixels (assumed to be RGB) for each tile
boolean exportOriginalPixels = true
// NOTE: The following parameter only matters if exportOriginalPixels == true
// Define image export type; valid values are JPG, PNG or null (if no image region should be exported with the mask)
// Note: masks will always be exported as PNG
def imageFormat = 'JPG'

// =====================================================================================================================

// Checking and writing inputs
if(downsample < 1){
    downsample = 1
}

// prepare stringBuilder to write parameters into text-file
def parameterInfo = new StringBuilder()
parameterInfo   << 'downsample' << '\t' << downsample << System.lineSeparator()
                << 'maxTileSize' << '\t' << maxTileSize << System.lineSeparator()
                << 'skipUnclassifiedAnnotations' << '\t' << skipUnclassifiedAnnotations << System.lineSeparator()
                << 'skipUnannotatedTiles' << '\t' << skipUnannotatedTiles << System.lineSeparator()
                << 'createIndexedImageLabels' << '\t' << createIndexedImageLabels << System.lineSeparator()
                << 'exportOriginalPixels' << '\t' << exportOriginalPixels << System.lineSeparator()
if(exportOriginalPixels)
    parameterInfo   << 'imageFormat' << '\t' << imageFormat << System.lineSeparator()

println parameterInfo

// Get the main QuPath data structures
def imageData = getCurrentImageData()
def hierarchy = imageData.getHierarchy()
def server = imageData.getServer()

// create output directory
def name_tmp = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(saveDir, name_tmp, maxTileSize.toString())
QPEx.mkdirs(pathOutput)

// Get the annotations that have ROIs & are have classifications (if required)
def annotations = hierarchy.getFlattenedObjectList(null).findAll {
    it.isAnnotation() && it.hasROI() && (!skipUnclassifiedAnnotations || it.getPathClass() != null) }

// Get all the represented classifications
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
int n = pathClasses.size() + 1
def r = ([(byte)0] * n) as byte[]
def g = r.clone()
def b = r.clone()
def a = r.clone()
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

// Calculate the tile spacing in full resolution pixels
int spacing = (int)(maxTileSize * downsample)

// Create the RegionRequests
def requests = new ArrayList<RegionRequest>()
for (int y = 0; y < server.getHeight(); y += spacing) {
    int h = spacing
    if (y + h > server.getHeight())
        h = server.getHeight() - y
    for (int x = 0; x < server.getWidth(); x += spacing) {
        int w = spacing
        if (x + w > server.getWidth())
            w = server.getWidth() - x
        requests << RegionRequest.createInstance(server.getPath(), downsample, x, y, w, h)
    }
}

// Write the label 'key'
println labelKey
String imgName = server.getMetadata().getName()
def keyName = String.format('%s_(downsample=%.3f,tiles=%d)-key.txt', imgName, downsample, maxTileSize)
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
    for (annotation in annotations) {
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
            // Write the mask
            def fileOutput = new File(pathOutput, name + '-labels.png')
            ImageIO.write(imgMask, 'PNG', fileOutput)
        }
    	/*
    	// Write the mask
    	def fileOutput = new File(pathOutput, name + '-labels.png')
    	ImageIO.write(imgMask, 'PNG', fileOutput)
	*/
        if (exportOriginalPixels) {
            def img = server.readBufferedImage(request)
            width = img.getWidth()
            height = img.getHeight()
            fileOutput = new File(pathOutput, name + '.' + imageFormat.toLowerCase())
            ImageIO.write(img, imageFormat, fileOutput)
        }
    }
}

print 'Done!'
