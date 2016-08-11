import numpy
def load_params():
    wh = numpy.fromfile('mlp_wh', dtype='float32').reshape((784, 500))
    bh = numpy.fromfile('mlp_bh', dtype='float32')
    wl = numpy.fromfile('mlp_wl', dtype='float32').reshape((500, 10))
    bl = numpy.fromfile('mlp_bl', dtype='float32')
    return wh, bh, wl, bl

def load_data():
    x = numpy.fromfile('test_x', dtype='float32').reshape(10000, 784)
    y = numpy.fromfile('test_y', dtype='int64')
    return x, y

def pred(x, wh, bh, wl, bl):
    oh = numpy.tanh(numpy.dot(x, wh) + bh)
    o = numpy.dot(oh, wl) + bl
    res = numpy.argmax(o)
    return res
    
if __name__ == '__main__':
    wh, bh, wl, bl = load_params()
    test_x, test_y = load_data()
    
    res = [pred(x, wh, bh, wl, bl) for x in test_x]

    t = 0
    for pred, gt in zip(res, test_y):
        if pred == gt: t += 1
    print t / 100., '%'

