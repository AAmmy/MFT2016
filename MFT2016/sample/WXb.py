import numpy

def load_params():
    w = numpy.fromfile('sgd_w', dtype='float32').reshape((784, 10))
    b = numpy.fromfile('sgd_b', dtype='float32')
    return w, b

def load_data():
    x = numpy.fromfile('test_x', dtype='float32').reshape(10000, 784)
    y = numpy.fromfile('test_y', dtype='int64')
    return x, y

def pred(x, w, b):
    o = numpy.dot(x, w) + b
    res = numpy.argmax(o)
    return res

if __name__ == '__main__':
    w, b = load_params()
    test_x, test_y = load_data()
    
    res = [pred(x, w, b) for x in test_x]

    t = 0
    for pred, gt in zip(res, test_y):
        if pred == gt: t += 1
    print t / 100., '%'

