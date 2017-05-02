import tensorflow as tf

def build_model(model, learning_rate=0.001, gpu_id="0"):
    m = model()
    m.build_model()
    train_op = tf.train.AdamOptimizer(learning_rate).minimize(m.loss)
    config = tf.ConfigProto()
    config.gpu_options.allow_growth = True
    config.gpu_options.visible_device_list = gpu_id
    sess = tf.Session(config=config)
    sess.run(tf.global_variables_initializer())
    return m, train_op, sess

class model():
    def __init__(self, input_num=901, class_num=101):
        self.input_num, self.class_num = input_num, class_num
        self.W = tf.Variable(tf.random_uniform([input_num, class_num], -1.0, 1.0), name='W')

    def build_model(self):
        x = tf.placeholder(tf.float32, [None, self.input_num])
        label = tf.placeholder(tf.int64, [None])

        h = tf.matmul(x, self.W)
        pred = tf.argmax(h, 1)
        cross_entropy = tf.nn.sparse_softmax_cross_entropy_with_logits(h, label)
        loss = tf.reduce_sum(cross_entropy) + tf.nn.l2_loss(self.W)
        self.accuracy = tf.contrib.metrics.accuracy(pred, label)

        self.x, self.label = x, label
        self.h, self.cross_entropy, self.pred, self.loss = h, cross_entropy, pred, loss

