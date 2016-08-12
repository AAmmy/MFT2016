import numpy
import glob

w, h = 320, 240
of_wl, of_wr = 130, 30
of_ht, of_hb = 25, 25
w_o = w - of_wl - of_wr
h_o = h - of_ht - of_hb
nw, nh = 50, 50

def read_file(f_name):
    f = open(f_name, 'rb')
    image = f.read()[0:w * h] # read image vector, byte array
    image = numpy.array([ord(d) for d in image]).reshape(h, w) # convert to byte and reshape
    return image

def crop_resize(image):
    image = crop(image)
    image = resize(image)
    return image
    
def crop(image, a=of_wl, b=of_wr, c=of_ht, d=of_hb, e_=0, f_=0, g_=0, h_=0):
    return numpy.array([img[a + e_:w - b + f_] for img in image[c + g_:h - d + h_]]).astype('float32')

def resize(image, nx=nw, ny=nh):
    y, x = image.shape
    ry, rx = y / float(ny), x / float(nx)
    res = numpy.zeros((ny, nx)).astype('float32')
    for j in range(ny):
        for i in range(nx):
            res[j][i] = image[int(j * ry)][int(i * rx)]
    return res
    
def test():
    image = read_file('data_car_4/20160618_145708_0_.txt')
    image = read_file('data_car_5/20160618_155102_0_.txt')
    image = read_file('data_car_7/20160618_163859_4_.txt')
    image = read_file('data_car_9/20160618_171514_0_.txt')
    flist = glob.glob('data_car_12/*_.txt')
    image = read_file(flist[0])
    show_file(image)
    show_file(crop_resize(image), f='test_c.jpg')
    show_file(255 - image + 1) # true color, prevent being black by removing 0
    show_file(255 - crop_resize(image) + 1, f='test_c.jpg') # true color, prevent being black by removing 0
    show_file(image + 1) # prevent being black by removing 0
    show_file(crop_resize(image) + 1, f='test_c.jpg') # prevent being black by removing 0

    i = 10
    image = read_file(flist[i])
    show_file(255 - image + 1) # true color, prevent being black by removing 0
    show_file(255 - crop_resize(image) + 1, f='test_c.jpg') # true color, prevent being black by removing 0
    flist[i]
    
    












