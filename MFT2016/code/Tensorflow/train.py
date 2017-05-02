from model import *
from data_io import *

f_name = ["train_3000.dat", "train_6000.dat"]
data_num = [3000, 6000]
# x, y,  = load_train_data(f_name=f_name[0], data_num=data_num[0])
x, y = load_train_data_multi(f_name=f_name, data_num=data_num)
x_train, y_train, x_dev, y_dev = shuffle_split_data(x, y)
m, train_op, sess = build_model(model)

best_dev_loss = np.inf
batch_size, train_data_size = x_train.shape[0], x_train.shape[0]
start_epoch, max_epoch = 0, 100000
for epoch in range(start_epoch, max_epoch):
    for kk, start in enumerate(range(0, train_data_size, batch_size)):
        end = start + batch_size
        if end > train_data_size:break
        _, train_loss = sess.run([train_op, m.loss], feed_dict={m.x:x_train[start:end], m.label:y_train[start:end]})

    if epoch % 10 == 0:
        train_loss, train_accuracy = sess.run([m.loss, m.accuracy], feed_dict={m.x:x_train, m.label:y_train})
        dev_loss, dev_accuracy = sess.run([m.loss, m.accuracy], feed_dict={m.x:x_dev, m.label:y_dev})
        print "@epoch %s, train_loss : %s, dev_loss : %s, train_accuracy : %s, dev_accuracy : %s" % (epoch, train_loss, dev_loss, round(train_accuracy, 4), round(dev_accuracy, 4))
        if best_dev_loss > dev_loss:
            best_dev_loss = dev_loss
            print "best_dev_loss"
            w = sess.run(m.W).reshape(-1)
    if epoch % 1000 == 0:
        save_w(w)

