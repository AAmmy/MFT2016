import array
from struct import *
import numpy as np

def load_train_data(f_name="train_3000.dat", data_num=3000, nx=900, ny=1):
    x = np.empty(0, dtype=np.int32)
    y = np.empty(0, dtype=np.uint32)
    with open(f_name, "rb") as f:
        for i in range(data_num):
            _x = array.array('B')
            _y = array.array('i')
            _y.fromfile(f, ny)
            _x.fromfile(f, nx)
            x = np.append(x, _x)
            y = np.append(y, _y)
    x = x.reshape((data_num, nx))
    _o = np.ones(data_num)
    x = np.c_[_o, x]
    y /= 2
    x = np.cast[np.int32](x)
    y = np.cast[np.uint32](y)
    return x, y

def load_train_data_multi(f_name, data_num):
    for i, (fn, dn) in enumerate(zip(f_name, data_num)):
        _x, _y,  = load_train_data(f_name=fn, data_num=dn)
        if i == 0:
            x, y = _x, _y
        else:
            x = np.r_[x, _x]
            y = np.r_[y, _y]
    return x, y

def shuffle_split_data(x, y, r=0.9):
    data_size = x.shape[0]
    np.random.seed(1234)
    idx = np.random.choice(range(data_size), data_size, replace=False).astype('int64')
    n_train = int(data_size * r)
    n_dev = data_size - n_train
    x_train, y_train = x[idx[:n_train]], y[idx[:n_train]]
    x_dev, y_dev = x[idx[n_train:n_train + n_dev]], y[idx[n_train:n_train + n_dev]]
    return x_train, y_train, x_dev, y_dev

def make_y_mat(y, prob):
    class_num = 101
    y_mat = np.zeros((y.shape[0], class_num))
    prob = np.array(prob)
    n = prob.shape[0]
    for i in range(y_mat.shape[0]):
        idx = y[i]
        s = max([0, idx - n / 2])
        e = min([idx - n / 2 + n, class_num])
        _n = e - s
        if _n != n:
            if s == 0:
                _s, _e = n - _n, n
            else:
                _s, _e = 0, _n
        else:
            _s, _e = 0, n
        y_mat[i][s:e] = prob[_s:_e]

    for i in range(100):
        if y_mat[i][y[i]] != 1:
            print 'making y_mat was failed'
    return y_mat

def save_w(w, f_name="all_theta.dat"):
    with open(f_name, "wb") as f:
        for _w in w:
            f.write(pack('>d',_w))

    __w = array.array('d')
    with open(f_name, "rb") as f: __w.fromfile(f, w.shape[0])


    with open(f_name, "wb") as f:
        for _w in __w:
            f.write(pack('>d',_w))

    __w = array.array('d')
    with open(f_name, "rb") as f: __w.fromfile(f, w.shape[0])

