#!/usr/bin/env python
# -*- coding: utf-8 -*-
import numpy

def load_params(): # 重みを読み込み
    w = numpy.fromfile('sgd_w', dtype='float32').reshape((784, 10))
    b = numpy.fromfile('sgd_b', dtype='float32')
    return w, b

def load_data(): # 手書き数字テストデータ読み込み
    x = numpy.fromfile('test_x', dtype='float32').reshape(10000, 784) # 28x28の画像1万枚
    y = numpy.fromfile('test_y', dtype='int64') # 1万枚の画像がそれぞれ何の数字か
    return x, y

def pred(x, w, b): # ネットワークの実行, 画像と重みの行列を計算しているだけ
    o = numpy.dot(x, w) + b # WX + b, 出力は10個
    res = numpy.argmax(o) # 10個のうち最も大きいものが正解の数字と判断
    return res

if __name__ == '__main__':
    w, b = load_params()
    test_x, test_y = load_data()
    
    res = [pred(x, w, b) for x in test_x] # 1万枚の画像を全て識別する

    # 正答率の計算
    t = [pred == gt for pred, gt in zip(res, test_y)].count(True)
    print t / 100., '%'

