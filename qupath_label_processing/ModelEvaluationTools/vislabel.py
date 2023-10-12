
def vislabel(map, n_label = 9):

    import matplotlib
    import numpy as np
    import matplotlib.pyplot as plt
    from mpl_toolkits.axes_grid1 import make_axes_locatable

    #ax = plt.gca()
    #cmap = plt.cm.rainbow
    #norm = matplotlib.colors.BoundaryNorm(np.arange(0, n_label, 1), cmap.N)
    #im = ax.imshow(map, cmap=cmap, norm=norm)

    # create an axes on the right side of ax. The width of cax will be 5%
    # of ax and the padding between cax and ax will be fixed at 0.05 inch.
    #divider = make_axes_locatable(ax)
    #cax = divider.append_axes("right", size="5%", pad=0.05)

    #plt.colorbar(ticks=np.linspace(0, n_label,5).astype("int64"), cax = cax )

    #%%
    fig = plt.figure(1, figsize=(5, 3))

    ax = plt.gca()
    cmap = plt.cm.rainbow
    norm = matplotlib.colors.BoundaryNorm(np.arange(0, n_label, 1), cmap.N)
    im = ax.imshow(map,  cmap=cmap, norm=norm)

    divider = make_axes_locatable(ax)
    cax = divider.append_axes("right", size="5%", pad=0.1)

    plt.colorbar(im, cax=cax)
    plt.tight_layout()
