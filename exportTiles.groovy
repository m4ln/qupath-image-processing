/**
 * Script to export image tiles (can be customized in various ways).
 */
 

// ================================ INITIALIZE PARAMETERS HERE =========================================================
// path to save data
def saveDir = '/Z:/Marlen/datasets/tatar/'

// size of each tile, in pixels
int tileSize = 2048/4

// downsample factor (changes the resolution: < 1 higher resolution; 1 full resolution; > 1 lower resolution)
double downsample = 2

// output resolution in calibrated units (e.g. µm if available)
// use this instead of downsample to change the resolution
double requestedPixelSize = 0

// overlap, in pixel units at the export resolution, (0 = no overlap)
int overlapSize = tileSize/2

// Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
def imageFormat = '.png'

// If true, only export tiles if there is a (classified) annotation present
boolean annotatedTilesOnly = false

// export tiles from whole project (instead of single file selected)
boolean exportProject = false

// in case exportProject is false, define the number of the image to export (starting from 0) 
int imageNumber = 0

// set true to save tiles for each WSI in seperate folder
boolean storeTilesSeperately = true
// =====================================================================================================================

def imageList

if (exportProject){
    imageList = project.getImageList()
}
else {
    //imageList = GeneralTools.getNameWithoutExtension(getCurrentImageData().getServer().getMetadata().getName()) 
    imageList = project.getImageList()[imageNumber]
}

print imageList

for (image in imageList) {
    print  'exporting: ' + image  + '\n'
    
    // Get the current image
    def imageData = image.readImageData()
    
    // create output directory
    def wsi_name
    def pathOutput
    if (storeTilesSeperately) {
        wsi_name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
        folder_name =  "sz" + tileSize.toString() + "_ds" + downsample.toString() + "_os" + overlapSize.toString() 
        pathOutput = buildFilePath(saveDir, wsi_name, folder_name )
    }
    else {
        pathOutput = buildFilePath(saveDir, tileSize.toString())
    }

    mkdirs(pathOutput)
    
    // Convert output resolution to a downsample factor
    double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSize()
    
    // recompute downsampling if output resolution (requestedPixelSize) is set
    if (requestedPixelSize > 0)
        downsample = requestedPixelSize / pixelSize
    
    // prepare stringBuilder to write parameters into text-file
    def parameterInfo = new StringBuilder()
    parameterInfo   << '\ninput pixelSize' << '\t' << pixelSize << System.lineSeparator()
                    << 'output pixelSize' << '\t' << requestedPixelSize << System.lineSeparator()
                    << 'downsampling' << '\t' << downsample << System.lineSeparator()
                    << 'tileSize' << '\t' << tileSize << System.lineSeparator()
                    << 'overlapSize' << '\t' << overlapSize << System.lineSeparator()
                    << 'imageFormat' << '\t' << imageFormat << System.lineSeparator()
    
    println parameterInfo
    

    
    // Create an exporter that requests corresponding tiles from the original & labelled image servers
    new TileExporter(imageData)
        .downsample(downsample)   // Define export resolution
        .imageExtension(imageFormat)   // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
        .tileSize(tileSize)            // Define size of each tile, in pixels
        .annotatedTilesOnly(annotatedTilesOnly) // If true, only export tiles if there is a (classified) annotation present
        .overlap(overlapSize)              // Define overlap, in pixel units at the export resolution
        .writeTiles(pathOutput)   // Write tiles to the specified directory
    
}

print 'Done!'