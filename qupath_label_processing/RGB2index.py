import numpy as np
from PIL import Image
import glob
import os


datapath = '/home/dr1/sds_hd/sd18a006/DataBaseCRCProjekt/Daniel/GroundTruth/FinalData'
files = glob.glob(os.path.join(datapath, '*.png'))
classes=[0,1,2,3,4,5,6,7,8,9,10,11,12] #what classes we expect to have in the data, here we have only 2 classes but we could add additional classes and/or specify an index from which we would like to ignore
palette = []
with open("Qu022-Palette.txt", "r") as f:
   for line in f:
       palette.append(int(line.strip()))
paletteArr = np.reshape(palette, (-1,3))

for file in files:


    img = Image.open(file)
    RGB = np.asarray(img.convert('RGB'))
    idx = np.uint8(np.zeros((img.height, img.width)))
    output = np.uint8(np.zeros((img.height, img.width)))
    for i in classes:  # converting indexes so they are equal for one class for the whole dataset
        label = paletteArr[i]
        idx = idx + i * np.all(RGB == label, axis=2, out=output)
    idx = np.uint8(idx)
    # io = np.repeat(idx[:, :, np.newaxis], 3, axis=2)  # due to later transformations we need 3 layers
    # interp_method = PIL.Image.NEAREST  # want to use nearest! otherwise resizing may cause non-existing classes to be produced via interpolation (e.g., ".25")
    # give visual output to controll function
    # img.show()
    aimMask = Image.fromarray(idx, mode='P')
    aimMask.putpalette(palette)
    aimMask.save(file.replace('.png', 'IDX13classes.png'))