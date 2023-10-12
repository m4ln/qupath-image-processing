
#%% function to load and prepare the palette
# simple table adaption
def loadAndPrepareColorPalette(legendPath, name_palette ="palette_list.csv"):

    #%% import-section
    import numpy as np
    import pandas as pd

    #%% load and adapt it
    lookuptable = pd.read_excel(legendPath)
    rgbvalues = np.array([lookuptable.iloc[:,3], lookuptable.iloc[:,4], lookuptable.iloc[:,5]])
    palette  = np.transpose(rgbvalues)

    #%% save it
    np.savetxt(name_palette, palette, delimiter=',')

    return palette

#%% function to create a palette on basis of an image set
# cave: does not work properly
def createpalette(legendPath, img_list, stop_iteration):

    #%%
    import numpy as np
    import random
    from numpy import savetxt
    from PIL import Image
    import pandas as pd

    #%% get the look up table
    lookuptable = pd.read_excel(legendPath)
    rgbvalues = np.array([lookuptable.iloc[:,5], lookuptable.iloc[:,4], lookuptable.iloc[:,3]])
    rgbvalues  = np.transpose(rgbvalues)

    #%%
    img_list = random.sample(img_list, len(img_list))
    if (stop_iteration != float('Inf')):
        img_list = img_list[0:stop_iteration]

    for i in range(len(img_list)):

        imgLink = Image.open(img_list[i])
        img = np.array(imgLink)

        # case for labeled image (the issue is, that qupath
        if len(img.shape) == 2:
            # get the used palette and reshape it
            t_palette =np.unique(im2.reshape(-1, im2.shape[2]), axis=0)

            palette_vector = imgLink.getpalette()
            t_palette = np.reshape(palette_vector, (-1, 3))


            idx = np.unique(img)
            t_palette = t_palette[idx]

        elif len(img.shape) == 3:
            img = np.reshape(img, (-1,3))
            t_palette = np.unique(img, axis = 0)

        #print(t_palette)
        if i == 0:
            palette = t_palette
        else:
            palette = np.concatenate((palette, t_palette), axis=0)

        print('label #', i + 1, ' added')

    # Perform lex sort and get sorted data
    sorted_idx = np.lexsort(palette.T)
    sorted_data = palette[sorted_idx, :]

    # Get unique row mask
    row_mask = np.append([True], np.any(np.diff(sorted_data, axis=0), 1))

    # Get unique rows
    palette = sorted_data[row_mask]
    palette = palette[palette[:, 0].argsort()]

    #%%
    savetxt('palette.csv', palette, delimiter=',')

    return palette

#%% function to convert the rgb-images on basis of a palette to a label-image
# simple loop that compares rgb-values to a table

def rgb2index(label_path, palette, vis=0):

    #%% import functions
    import numpy as np
    from PIL import Image, ImagePalette
    import matplotlib.pyplot as plt

    #%% load the label image
    label_img = Image.open(label_path)

    #%% load the palette
    if type(palette) == str:
        palette = np.loadtxt('palette.csv', delimiter=',')

    #%% iterate over the palette
    label_rgb = np.asarray(label_img.convert('RGB'))
    x = int(label_rgb.shape[0]/1)
    y = int(label_rgb.shape[1]/1)

    label_mask = np.uint8(np.zeros((x,y)))
    output = np.uint8(np.zeros((x,y)))

    for i in range(palette.shape[0]):

        label = palette[i]
        BooleanArr = np.all(label_rgb == label, axis=2, out = output)
        label_mask[BooleanArr==1] = i+1

    if vis:
        from ModelEvaluationTools import vislabel

        plt.figure(1)
        plt.subplot(121)
        plt.imshow(label_rgb)
        plt.title('input image')
        plt.subplot(122)
        vislabel(label_mask, n_label=20)
        plt.title('output image')
        plt.show()
        plt.pause(0.5)

    #%% output-section
    return(label_mask)

#%% test
if __name__ == '__main__':
    import numpy as np
    from numpy import genfromtxt
    from PIL import Image
    import os
    import glob

    input_path = 'Z:/Marlen/datasets/segmentation/urothel/'
    input_file = input_path + 'colorPalette_urothel.xls'
    # os.path.basename(input_path)

    output_file = input_path + 'colorPalette_list.csv'

    # if palette file is not already prepared
    # palette = loadAndPrepareColorPalette(input_file, output_file)

    palette = genfromtxt(output_file, delimiter=',')

    label_dir = 'Z:/Marlen/datasets/segmentation/urothel/classes_12/test/*.png'

    for label_file in glob.glob(label_dir):
        label_mask = rgb2index(label_file, palette, 0)
        # np.save(label_file.replace('.png', 'converted.png'), label_mask)
        label_mask2 = Image.fromarray(label_mask, mode='P')
        label_mask2.putpalette(palette)
        label_mask2.save(label_file.replace('.png', 'converted.png'))

