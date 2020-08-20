/**
 * Script to export image tiles (can be customized in various ways).
 */

def saveDir = 'Z:\Marlen\datasets'
def projectName = '\dieSchöneUndDasBiest_HE'
def project = getProject()
for (entry in project.getImageList()) {

// Get the current image (supports 'Run for project')
def imageData = entry.readImageData()
//def imageData = getCurrentImageData()

// Define output path (here, relative to project)
def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
def pathOutput = buildFilePath(saveDir, projectName, name, 'qupathTiles')
mkdirs(pathOutput)

// Convert output resolution to a downsample factor
double pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSize()
print('input pixelSize: ' + pixelSize)

// Define output resolution in calibrated units (e.g. µm if available)
double requestedPixelSize = 0
print('output pixelSize: ' + requestedPixelSize)

// downsample factor (1 = no downsampling)
double downsample = 1
if (requestedPixelSize > 0)
    downsample = requestedPixelSize / pixelSize
print('downsampling: ' + downsample)

// Define size of each tile, in pixels
int tileSize = 1024
print('tileSize: ' + tileSize)

// Define overlap, in pixel units at the export resolution, (0 = no overlap)
int overlapSize = 0
print('overlapSize: ' + overlapSize)

// Create an exporter that requests corresponding tiles from the original & labelled image servers
new TileExporter(imageData)
    .downsample(downsample)   // Define export resolution
    .imageExtension('.tif')   // Define file extension for original pixels (often .tif, .jpg, '.png' or '.ome.tif')
    .tileSize(tileSize)            // Define size of each tile, in pixels
    .annotatedTilesOnly(false) // If true, only export tiles if there is a (classified) annotation present
    .overlap(overlapSize)              // Define overlap, in pixel units at the export resolution
    .writeTiles(pathOutput)   // Write tiles to the specified directory

}
print 'Done!'