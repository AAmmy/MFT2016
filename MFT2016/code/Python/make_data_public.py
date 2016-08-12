import numpy, glob
import read_byte_data as rbd
nw, nh = rbd.nw, rbd.nh
n_in = rbd.nw * rbd.nh
n_out = 3

def add_h(x, h):
    for i in range(len(x)):
        add_h_(x[i], h[i])
    return x
    
def add_h_(x, h):
    for j, s in enumerate(h):
        for k in range(1, 3):
            x[(10 + j) * nw + nw - k] = int(s) * 10 + 300.
    return x

def reg(x):
    res = []
    for xx in x:
        x_max = xx.max()
        x_min = xx.min()
        tmp = (xx - x_min) * 255 / (x_max - x_min)
        res.append(tmp.astype('int'))
    return numpy.array(res).astype('float32')

def split_data(x, y, rtn=0.8, rvd=0.1, rtt=0.1):
    tnn, vdn, ttn = [int(len(y) * rr) for rr in [rtn, rvd, rtt]]
    train_set, valid_set, test_set = [x[0:tnn], y[0:tnn]], [x[tnn:tnn+vdn], y[tnn:tnn+vdn]], [x[tnn+vdn:tnn+vdn+ttn], y[tnn+vdn:tnn+vdn+ttn]]
    return train_set, valid_set, test_set

def make_data():
    folder, sufix = 'bw_o_obj/', ''
    x, y, hist = load_convert(folder, sufix)
    
    folder, sufix = 'bw_o_obj/', ''
    x, y, hist = numpy.load(folder + 'xyh.npy')
    x = reg(x)
    x = add_h(x, hist)
    train_set, valid_set, test_set = split_data(x, y)
    numpy.save(folder + 'dataset' + sufix, [train_set, valid_set, test_set])
    
def load_convert(folder, sufix='', c=''):
    flist = glob.glob(folder + '*' + c + '.txt')
    numpy.random.shuffle(flist)
    load_convert_(flist, folder, sufix=sufix)

def load_convert_(flist, folder, sufix=''):
    x, y, h = [], [], []
    for n in flist:
        image = rbd.read_file(n)
        x.append(rbd.crop_resize(image).reshape(n_in))
        y.append(int(n[n.rfind('_') + 1]))
        h.append([int(k) for k in n[n.rfind('_', 0, -7) + 1:n.rfind('_')]])
    y = numpy.array(y)
    numpy.save(folder + 'xyh' + sufix, [x, y, h])

def samples():
    d = {
     0:['nc_C/',     '',   5,  2, 1],
     1:['nc_C_2/',   '',   5,  2, 1],
     2:['nc_C_3/',   '',   5,  2, 1],
    15:['nc_C_4/',   '',   5,  2, 1],
    23:['nc_C/',     '_0', 5,  2, 1],
    24:['nc_C_2/',   '_0', 5,  2, 1],
    25:['nc_C_3/',   '_0', 5,  2, 1],
    26:['nc_C_4/',   '_0', 5,  2, 1],
    27:['nc_C_2/',   '_2', 5,  2, 1],
    28:['nc_C/',     '_2', 5,  2, 1],
    29:['nc_C_3/',   '_2', 5,  2, 1],
    40:['nc_C_4/',   '_2', 5,  2, 1],
    31:['nc_C/',     '_5', 5,  2, 1],
    32:['nc_C_2/',   '_5', 5,  2, 1],
    33:['nc_C_3/',   '_5', 5,  2, 1],
    34:['nc_C_4/',   '_5', 5,  2, 1],
    22:['nc_Ci/',    '',   5,  2, 1],
    35:['nc_Ci/',    '_0', 5,  2, 1],
    36:['nc_Ci/',    '_2', 5,  2, 1],
    37:['nc_Ci/',    '_5', 5,  2, 1],
     3:['nc_I/',     '',   5, -1, 1],
     4:['nc_I_2/',   '',   5, -1, 1],
     5:['nc_Ii/',    '',   5,  5, 1],
     6:['nc_O/',     '',  -1,  2, 0],
     7:['nc_O_2/',   '',  -1,  2, 0],
     8:['nc_O_3/',   '',  -1,  2, 0],
    16:['nc_O_4/',   '',  -1,  2, 0],
    21:['nc_O_5/',   '',  -1,  2, 0],
     9:['nc_OC_05/', '_0',-1,  2, 0],
    10:['nc_OC_05/', '_5',-1,  2, 0],
    11:['nc_Oo/',    '',   2,  2, 1],
    12:['nc_Oo_2/',  '',   2,  2, 1],
    17:['nc_Oo_3/',  '',   2,  2, 1],
    13:['nc_Ob/',    '',  -1,  2, 0],
    14:['nc_Ob_2/',  '',  -1,  2, 0],
    38:['nc_Ob_3/',  '',  -1,  2, 0],
    18:['nc_O_d/',   '',  -1,  2, 0],
    19:['nc_O_d_2/', '',  -1,  2, 0],
    20:['nc_O_d_3/', '',  -1,  2, 0],
    21:['r_O/',         '',  -1,  2, 0],
    }

    alldata = []
    mode = 2;
    t_ = 39
    type_ = {
    0:[0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 13],
    1:[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
    2:[0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12],
    3:[0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 14],
    4:[0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 15, 16, 17],
    5:[0, 1, 2, 3, 4, 5, 6, 7, 8, 11, 12, 15, 16, 17, 18, 19, 20],
    6:[6, 7, 8, 18, 19, 20],
    7:[6, 7, 8],
    8:[0, 1, 2, 15, 6, 7, 8, 16], # CO
    9:[6, 7, 8, 16, 11, 12, 17], # OOo
    10:[0, 1, 2, 15, 6, 7, 8, 16, 11, 12, 17], # COOo
    11:[0, 1, 2, 15, 3, 4, 5, 6, 7, 8, 16, 11, 12, 17,], # CIOOo
    12:[6, 7, 8, 16, 21], # O
    13:[15, 21], # O
    14:[0, 1, 2, 15, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CIOOo
    15:[6, 7, 8, 16, 21, 11, 12, 17], # OOo
    16:[3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # IOOo
    17:[5, 6, 7, 8, 16, 21, 11, 12, 17,], # IOOo
    18:[4, 6, 7, 8, 16, 21, 11, 12, 17,], # IOOo
    19:[16, 21], # OOo
    20:[16, 21, 11], # OOo
    21:[16, 21, 12], # OOo
    22:[16, 21, 17], # OOo
    23:[15, 16, 21, 17], # COOo
    24:[4, 16, 21, 17], # IOOo
    25:[3, 4, 16, 21, 17], # IOOo
    26:[15, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CIOOo
    27:[22, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CiCIOOo
    28:[26, 34, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CiCIOOo
    29:[34, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CiCIOOo
    30:[26, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CiCIOOo
    31:[32, 33, 34, 37, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CiCIOOo
    32:[34, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 20], # CiCIOOo
    33:[34, 38, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 20],
    37:[31, 32, 33, 34, 38, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 20],
    34:[34, 18, 19, 20, 38, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 20],
    35:[34, 13, 14, 38, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 20],
    36:[31, 32, 33, 34, 13, 14, 38, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 20],
    38:[31, 32, 33, 34, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17,], # CiCIOOo
    39:[34, 3, 4, 5, 6, 7, 8, 16, 21, 11, 12, 17, 21], # CiCIOOo
    }
    for i in type_[t_]:
        x, y, hist = numpy.load(d[i][0] + 'xyh' + d[i][1] + '.npy')
        if not d[i][mode] == -1: y = numpy.ones(len(y)).astype('int32') * d[i][mode]
        # x, y, hist = reduce_0(x, y, hist)
        x = reg(x)
        x = add_h(x, hist).astype('int32')
        alldata += list(zip(x,y))
    alldata = numpy.array(alldata)
    numpy.random.shuffle(alldata)
    alldata = alldata.transpose()
    train_set, valid_set, test_set = split_data(alldata[0], alldata[1])
    numpy.save('nc_O/' + 'dataset_' + str(mode) + '_' + str(t_), [train_set, valid_set, test_set])

    alldata = []
    for i in type_[t_]:
        x, y, hist = numpy.load(d[i][0] + 'xyh' + d[i][1] + '.npy')
        if not d[i][mode] == -1: y = numpy.ones(len(y)).astype('int32') * d[i][mode]
        # x, y, hist = reduce_0(x, y, hist)
        x = reg(x)
        x = add_h(x, hist)
        alldata += list(zip(x,y))
        if not d[i][mode] == -1 and not i in [0, 1, 2, 15]:
            for k in range(30):
                x, y, hist = numpy.load(d[i][0] + 'xyh' + d[i][1] + '.npy')
                if not d[i][mode] == -1: y = numpy.ones(len(y)).astype('int32') * d[i][mode]
                x = reg(x)
                hhhh = numpy.random.choice([0,0,0,0,0,2,2,2,5], (len(hist), 30))
                x = add_h(x, hhhh)
                alldata += list(zip(x,y))
        if d[i][mode] == -1:
            for k in range(0):
                x, y, hist = numpy.load(d[i][0] + 'xyh' + d[i][1] + '.npy')
                if not d[i][mode] == -1: y = numpy.ones(len(y)).astype('int32') * d[i][mode]
                x = reg(x)
                hhhh = numpy.random.choice([0,0,0,0,0,2,2,2,5], (len(hist), 30))
                x = add_h(x, hhhh)
                alldata += list(zip(x,y))
        if i in [13, 14, 38]:
            for k in range(20):
                x, y, hist = numpy.load(d[i][0] + 'xyh' + d[i][1] + '.npy')
                if not d[i][mode] == -1: y = numpy.ones(len(y)).astype('int32') * d[i][mode]
                x = reg(x)
                hhhh = numpy.random.choice([0,0,0,0,0,2,2,2,5], (len(hist), 30))
                x = add_h(x, hhhh)
                alldata += list(zip(x,y))
        
    alldata = numpy.array(alldata)
    numpy.random.shuffle(alldata)
    alldata = alldata.transpose()
    train_set, valid_set, test_set = split_data(alldata[0], alldata[1])
    numpy.save('nc_O/' + 'dataseth30p0d_' + str(mode) + '_' + str(t_), [train_set, valid_set, test_set])

    folders = [d[i][0] for i in range(len(d))]
    for fd in folders: load_convert(fd, "v2")
    load_convert('nc_Ci/', "_2", c = '2')

    2_8 < 2_6 < 2_5 < 2_7 < 2_9 < 2_11 < 2_10

    bin_sgd_wh20p20_2_14 # stable3
    bin_sgd_wh20p20_2_17 # stable4
    bin_sgd_wh20p5_2_17 # stable5
    bin_sgd_wh20p5_2_26 # stable6
    bin_sgd_wh20p5_2_29 # stable7, best1
    bin_sgd_bh20p5d_2_33 # dstable1
    bin_sgd_bh20p5d_2_35 # dstable1, best1




