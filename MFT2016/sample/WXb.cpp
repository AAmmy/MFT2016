#include <cstring>
#include <dirent.h>
#include <iostream>
#include <fstream>
#include <cstdlib>
using namespace std;

int i_n = 784, o_n = 10, d_n = 10000;
float b[10], w_[784 * 10], w[784][10], wxb[10];
float test_x_[10000 * 784], test_x[10000][784];
int test_y[10000];
fstream fs;

void get_test_x(){ // 手書き数字テストデータ読み込み (28x28の画像1万枚)
  fs.open("test_x", ios::in | ios::binary);
  for(int i = 0; i < d_n * i_n; i++)
    fs.read((char*)&test_x_[i], sizeof test_x_);
  fs.close();
  for(int j = 0; j < d_n; j++)
    for(int i = 0; i < i_n; i++)
      test_x[j][i] = test_x_[j * i_n + i];
}

void get_test_y(){ // ラベルデータ読み込み (1万枚の画像がそれぞれ何の数字か)
  fs.open("test_y", ios::in | ios::binary);
  for(int i = 0; i < d_n; i++)
    fs.read((char*)&test_y[i], sizeof long());
  fs.close();
}

void get_b(){ // 学習済みバイアス読み込み
  fs.open("sgd_b", ios::in | ios::binary);
  for(int i = 0; i < o_n; i++)
    fs.read((char*)&b[i], sizeof b);
  fs.close();
}

void get_w(){ // 学習済み重み読み込み
  fs.open("sgd_w", ios::in | ios::binary);
  for(int i = 0; i < i_n * o_n; i++)
    fs.read((char*)&w_[i], sizeof w_);
  fs.close();
  for(int j = 0; j < i_n; j++)
    for(int i = 0; i < o_n; i++)
      w[j][i] = w_[j * o_n + i];
}

void dotb(int k){ // WX + b を計算
  for(int i = 0; i < o_n; i++) // 最初にbで初期化する
    wxb[i] = b[i];
  for(int i = 0; i < o_n; i++) // 内積の計算
    for(int j = 0; j < i_n; j++)
      wxb[i] += test_x[k][j] * w[j][i];
}

int argmax(float *x_){ // 最大の要素を含むインデックスを求める
  int max = x_[0], argmax = 0;
  for(int i = 0; i < o_n; i++){
    if(max < x_[i]){
      max = x_[i];
      argmax = i;
    }
  }
  return argmax;
}

void load_data(){
  get_b();
  get_w();
  get_test_x();
  get_test_y();
}

int main() {
  load_data();

  int res[d_n];
  for(int i = 0; i < d_n; i++){ // 1万枚の画像を全て識別する
    dotb(i);
    res[i] = argmax(wxb);
  }
  
  // 正答率の計算
  int t = 0;
  for(int i = 0; i < d_n; i++){
    if(res[i] == test_y[i])
      t++;
  }
  cout << t / (float) 100 << "%" << endl;
  return 0;
}

