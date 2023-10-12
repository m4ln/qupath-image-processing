
def crop(infile,height,width):

    #%% load background
    from PIL import Image
    import numpy as np

    #%% decide if it is an image
    if isinstance(infile, np.ndarray):
        im = Image.fromarray(infile)
    else:
        im = Image.open(infile)

    #%% get image information and iterate over it
    imgwidth, imgheight = im.size
    #k = 1
    for i in range(imgheight//height):
        for j in range(imgwidth//width):
            box = (j*width, i*height, (j+1)*width, (i+1)*height)
            tile = im.crop(box)
            tile = np.array(tile)
            #plt.imshow(tile)
            #plt.show()
            #k += 1
            #print(k)
            yield tile


#%% function to split the images into n-tiles
# nice if an image is much too big for the training

def split_images_and_labels(list_images, list_labels, tiles_size, path_data):

    #%% load the libraries
    from DataBasePreparationTools.im_preparation import crop
    import os
    from DataBasePreparationTools.label_preparation import rgb2index
    from numpy import loadtxt
    from PIL import Image
    import shutil

    #%% create the folders
    path_input = path_data + '/input'
    if os.path.exists(path_input):
        shutil.rmtree(path_input)
    os.makedirs(path_input)
    path_output = path_data + '/output'
    if os.path.exists(path_output):
        shutil.rmtree(path_output)
    os.makedirs(path_output)

    #%% load the palette
    palette = loadtxt('palette.csv', delimiter=',')
    palette = palette.astype('int32')
    n_label = list(range(0, palette.shape[0]))

    #%% iterate over all images
    n_tiles = (Image.open(list_images[0]).width // tiles_size) * \
              (Image.open(list_images[0]).height // tiles_size)

    i_image = 0
    n_iterations = n_tiles * len(list_images) - 1
    for idx in range(0, len(list_images)):

        #%% load the image and the label
        lab =  rgb2index(list_labels[idx], palette, False)

        #%% mount the generator
        im_tiles = crop(list_images[idx], tiles_size, tiles_size)
        label_tiles = crop(lab, tiles_size, tiles_size)

        #%% generate tiles
        for i in range(0,n_tiles):
            im_tile = Image.fromarray(next(im_tiles))
            im_tile.save(path_input + '/tile#'+  str(i_image) + '.jpg')
            label_tile = Image.fromarray(next(label_tiles), mode = 'L')
            label_tile.save(path_output + '/label#' + str(i_image) + '.jpg', mode = 'L')

            print('iteration #'+ str(i_image) + ' from n=' + str(n_iterations))
            i_image +=1

    return

#%% old function -> ignore it
def only_rgb2index(list_images, list_labels, path_data):

    #%% load the libraries
    import numpy as np
    import os
    from DataBasePreparationTools.label_preparation import rgb2index
    from numpy import loadtxt
    from PIL import Image
    import shutil
    import cv2

    #%% create the folders
    path_input = path_data + '/input'
    if os.path.exists(path_input):
        shutil.rmtree(path_input)
    os.makedirs(path_input)
    path_output = path_data + '/output'
    if os.path.exists(path_output):
        shutil.rmtree(path_output)
    os.makedirs(path_output)

    #%% load the palette
    palette = loadtxt('palette.csv', delimiter=',')
    palette = palette.astype('int32')
    n_label = list(range(0, palette.shape[0]))

    #%% iterate over all images
    i_image = 0
    n_iterations =len(list_images) - 1

    for idx in range(0, len(list_images)):

        #%% load and save the image and the label
        lab =  rgb2index(list_labels[idx], palette, True)

        if idx == 0:
            label_values = np.unique(lab)
        else:
            label_values = np.concatenate((label_values, np.unique(lab)), 0)

        #lab = Image.fromarray(lab, mode='L')
        #lab.save(path_output + '/label#' + str(i_image) + '.jpg', mode='L')
        cv2.imwrite(path_output + '/label#' + str(i_image) + '.png', lab)
        im = Image.open(list_images[idx])
        im.save(path_input + '/tile#' + str(i_image) + '.jpg')

        print('iteration #'+ str(i_image) + ' from n=' + str(n_iterations))
        i_image +=1


    print("These labels where created: ")
    print(np.unique(label_values))

    return


