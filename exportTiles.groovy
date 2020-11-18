/**
 * Script to export image tiles (can be customized in various ways).
 */
 

//=============== SET VARIABLES ======================
//==================================================== 
// path to save data
def saveDir = '/Z:/Marlen/datasets/beauty-and-beast/train_test_sets/test_FID/'
// size of each tile, in pixels
int tileSize = 512
// downsample factor (1 = no downsampling)
double downsample = 1
// output resolution in calibrated units (e.g. µm if available)
double requestedPixelSize = 0
// overlap, in pixel units at the export resolution, (0 = no overlap)
int overlapSize = tileSize/2
//====================================================
//====================================================

def project = getProject()
//for (entry in project.getImageList()) {

// Get the current image (supports 'Run for project')
//def imageData = entry.readImageData()
def imageData = getCurrentImageData()
// Define output path (here, relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(saveDir, name, ('size' + tileSize.toString() + '_overlap' + overlapSize.toString()))
mkdirs(pathOutput)


// Convert output resolution to a downsample factor
double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSize()

// recompute downsampling if output resolution (requestedPixelSize) is set
if (requestedPixelSize > 0)
    downsample = requestedPixelSize / pixelSize

// print info
print('input pixelSize: ' + pixelSize)
print('output pixelSize: ' + requestedPixelSize)
print('downsampling: ' + downsample)
print('tileSize: ' + tileSize)
print('overlapSize: ' + overlapSize)

// Create an exporter that requests corresponding tiles from the original & labelled image servers
new TileExporter(imageData)
    .downsample(downsample)   // Define export resolution
    .imageExtension('.tif')   // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(tileSize)            // Define size of each tile, in pixels
    .annotatedTilesOnly(false) // If true, only export tiles if there is a (classified) annotation present
    .overlap(overlapSize)              // Define overlap, in pixel units at the export resolution
    .writeTiles(pathOutput)   // Write tiles to the specified directory

//}

print 'Done!'