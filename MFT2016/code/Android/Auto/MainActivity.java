// 公開用にテストコード削除, コメント追加, FTドライバ必要
// 50msecに一度ネットワークを実行し自動運転する
// 一番単純なネットワークのみ公開(後で追加予定)
package com.camera.abc.camera;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {
    // camera
    private SurfaceView mySurfaceView;
    private Camera myCamera;
    private int w = 320, h = 240;
    private int size = w * h;
    private int p_num = 0;
    private byte[] cropped_data = new byte[size];

    // Bluetooth
    public BluetoothSocket receivedSocket = null;
    public BluetoothAdapter myServerAdapter = BluetoothAdapter.getDefaultAdapter();
    public InputStream in = null;
    public boolean BluetoothReading = true;

    // Arduino
    private static final int readLength = 512;
    private static Context mContext;
    private byte[] readData;
    // private char[] readDataToText;
    private FT_Device ftDev = null;
    private D2xxManager ftdid2xx;
    private boolean isReading = false;
    private ReadThread mReadThread;
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                // never come here(when attached, go to onNewIntent)
                openUsb();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                if(ftDev != null) {
                    ftDev.close();
                    isReading = false;
                }
            }
        }
    };
    private int iavailable = 0;
    private TextView mInputValue;
    // 描画処理はHandlerでおこなう
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String mData = (String)msg.obj;
            mInputValue.setText(mData);
        }
    };

    public String mLabel = "0";
    public String mLblHisr = "000000000000000000000000000000";

    public int n_in = 2500, n_out = 3, n_x = 50;
    public String dataDir = "/mnt/sdcard/external_sd/";
    public float[] sgd_b = read_data(3, dataDir + "bin_sgd_b");
    public float[] sgd_w_ = read_data(2500 * 3, dataDir + "bin_sgd_w");
    public float[][] sgd_w = reshape (sgd_w_, 2500, 3);
    public float[] sgd_b_d = read_data(3, dataDir + "bin_sgd_b_d");
    public float[] sgd_w_d_ = read_data(2500 * 3, dataDir + "bin_sgd_w_d");
    public float[][] sgd_w_d = reshape (sgd_w_d_, 2500, 3);
    public boolean mode_d = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // camera
        mySurfaceView = (SurfaceView)findViewById(R.id.mySurfaceVIew);
        mySurfaceView.setOnClickListener(onSurfaceClickListener);
        // SurfaceHolder(SVの制御に使うInterface）
        SurfaceHolder holder = mySurfaceView.getHolder();
        // コールバックを設定
        holder.addCallback(callback);

        // Arduino
        try {
            ftdid2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.d("MyApp", "### Arduino error! ###");
        }
        mContext = this.getBaseContext();
        openUsb();

        main_loop();
    }

    //コールバック
    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            myCamera = Camera.open();
            Camera.Parameters parameters = myCamera.getParameters();
            parameters.setPreviewSize(w, h);
            parameters.setPictureSize(w, h);
            myCamera.setParameters(parameters);

            //出力をSurfaceViewに設定
            try{
                myCamera.setPreviewDisplay(surfaceHolder);
            }catch(Exception e){
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            myCamera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            myCamera.release();
            myCamera = null;

            if (myServerAdapter.isEnabled()) {
                // myServerAdapter.disable();
                receivedSocket = null;
                BluetoothReading = false;
            }

        }
    };

    private View.OnClickListener onSurfaceClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View view){
            if(myCamera != null){
                myCamera.autoFocus(autoFocusCallback);
                mode_d = !mode_d;
            }

        }
    };

    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback(){
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            Log.d("MyApp", "### Auto Focused! ###");
        }
    };

    private final Camera.PreviewCallback previewCallback =
            new Camera.PreviewCallback() {
                public void onPreviewFrame(byte[] data, Camera camera) {
                    save_byte_array_by_cropping(data);
                }
            };

    // Bluetooth
    public BluetoothSocket setServer(){
        // UUID：Bluetoothプロファイル毎に決められた値
        BluetoothServerSocket servSock, tmpServSock = null;
        // UUIDの生成
        UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try{
            // 自デバイスのBluetoothサーバーソケットの取得
            tmpServSock = myServerAdapter.listenUsingRfcommWithServiceRecord("BlueToothSample03", TECHBOOSTER_BTSAMPLE_UUID);
            Log.d("MyApp", "Server ソケット取得完了" + tmpServSock.toString());
        }catch(IOException e){
            e.printStackTrace();
        }
        servSock = tmpServSock;

        while(true){
            try{
                // クライアント側からの接続要求待ち, ソケットが返される
                receivedSocket = servSock.accept();
            }catch(IOException e){
                Log.d("MyApp", "Server 接続エラー");
                break;
            }

            if(receivedSocket != null){
                // ソケットを受け取れていた(接続完了時)の処理
                try {
                    // 処理が完了したソケットは閉じる
                    servSock.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        Log.d("MyApp", "Server end");
        return receivedSocket;
    }

    // Bluetooth
    public void setInSocket() {
        try {
            // 接続済みソケットからI/Oストリームをそれぞれ取得
            in = receivedSocket.getInputStream();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // Bluetooth
    public void read() {
        new Thread(new Runnable() {
            byte[] buf = new byte[1];
            String rcvNum = null;
            int tmpBuf = 0;
            @Override
            public void run() {
                while(BluetoothReading){
                    try {
                        tmpBuf = in.read(buf);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if(tmpBuf!=0){
                        try {
                            rcvNum = new String(buf, "UTF-8").substring(0, 1);
                            Log.d("MyApp", "got : " + rcvNum);
                            if(rcvNum.equals("0") || rcvNum.equals("1") || rcvNum.equals("2") || rcvNum.equals("3") || rcvNum.equals("4") || rcvNum.equals("5") || rcvNum.equals("6") || rcvNum.equals("7")){
                                SendMessage(rcvNum);
                            }
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    public void main_loop() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                saveThread();
                lhist();
            }
        }).start();
    }

    // 過去の操作を記録
    public void lhist() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(BluetoothReading){
                    try {
                        mLblHisr = mLblHisr.substring(1, mLblHisr.length());
                        mLblHisr += mLabel;
                        Thread.sleep(50); // 50msecごとに
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // ネットワークの実行
    public void saveThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000); // 起動3秒後から
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(BluetoothReading){
                    try {
                        myCamera.setOneShotPreviewCallback(previewCallback);
                        Thread.sleep(50); // 50msecごとに
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // Arduino
    public void SendMessage(String msg) {
        mLabel = msg;
        if(ftDev == null){
            Log.d("MyApp", "### ftDev null! ###" + mLabel);
            return;
        }
        synchronized (ftDev) {
            if (ftDev.isOpen() == false) {
                Log.e("j2xx", "SendMessage: device not open");
                return;
            }
            ftDev.setLatencyTimer((byte) 16);
            if (msg != null) {
                byte[] OutData = msg.getBytes();
                ftDev.write(OutData, msg.length());
            }
        }
    }

    // Arduino
    private class ReadThread  extends Thread
    {
        Handler mHandler;
        ReadThread(Handler h){
            mHandler = h;
            this.setPriority(Thread.MIN_PRIORITY);
        }
        @Override
        public void run()
        {
            int i;
            while(true == isReading)
            {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
                synchronized(ftDev)
                {
                    iavailable = ftDev.getQueueStatus();
                    if (iavailable > 0) {
                        if(iavailable > readLength){
                            iavailable = readLength;
                        }
                        ftDev.read(readData, iavailable);
                        String mData = new String(readData);
                        Message msg = mHandler.obtainMessage();
                        msg.obj = mData;
                        mHandler.sendMessage(msg);
                    }
                }
            }
        }
    }

    // Arduino
    public void openUsb(){
        int devCount = 0;
        devCount = ftdid2xx.createDeviceInfoList(this);
        if (devCount <= 0)
        {
            Log.d("MyApp", "### arduino not found! ###");
            return;
        }
        else{
            Log.d("MyApp", "### arduino found! ### " + devCount);
        }
        if(null == ftDev)
        {
            ftDev = ftdid2xx.openByIndex(mContext, 0);
        }
        else
        {
            synchronized(ftDev)
            {
                ftDev = ftdid2xx.openByIndex(mContext, 0);
            }
        }
        //ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
        ftDev.setBaudRate(9600);
        ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8, D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
        ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x0b, (byte) 0x0d);
        ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
        ftDev.restartInTask();
        readData = new byte[readLength];
        // readDataToText = new char[readLength];
        mReadThread = new ReadThread(mHandler);
        mReadThread.start();
        isReading = true;
    }

    public void test_pict(byte[] image){
        byte[] x0 = crop(image, 320, 240);
        byte[] x1 = resize(x0, 160, 190);
        int[] x2 = b_to_i(x1);
        int[] x3 = adjust(x2);
        int[] x4 = reg(x3);
        int[] x = add_hist(x4);

        // ネットワークの実行, モードによって使用する重みを変更
        float[] o;
        if(mode_d)
            o = wxb(x4, sgd_w_d, sgd_b_d);
        else
            o = wxb(x4, sgd_w, sgd_b);

        int pred = argmax(o);
        if(pred == 1)
            pred = 2;
        else if(pred == 2)
            pred = 5;

        SendMessage(String.valueOf(pred));
    }

    public void save_byte_array_by_cropping(byte[] d){
        for(int i = 0 ; i < size; i++)
            cropped_data[i] = d[i];
        test_pict(cropped_data);
    }

    public byte[][] crop(byte[][] image, int ww, int hh) {
        int of_wl = 130, of_wr = 30, of_ht = 25, of_hb = 25;
        int w_o = ww - of_wl - of_wr;
        int h_o = hh - of_ht - of_hb;
        byte[][] res = new byte[h_o][w_o];
        for (int j = of_ht; j < hh - of_hb; j++)
            for (int i =of_wl; i < ww - of_wr; i++)
                res[j - of_ht][i - of_wl] = image[j][i];
        return res;
    }
    public byte[] crop(byte[] image, int ww, int hh) {
        int of_wl = 130, of_wr = 30, of_ht = 25, of_hb = 25;
        int w_o = ww - of_wl - of_wr;
        int h_o = hh - of_ht - of_hb;
        byte[] res = new byte[w_o * h_o];
        for (int j = of_ht; j < hh - of_hb; j++)
            for (int i =of_wl; i < ww - of_wr; i++)
                res[(j - of_ht) * w_o + i - of_wl] = image[j * ww + i];
        return res;
    }
    public byte[][] resize(byte[][] image, int ww, int hh){
        int nw = n_x, nh = n_x;
        float ry = hh / (float)nh, rx = ww / (float)nw;
        byte[][] res = new byte[nh][nw];
        for (int j = 0; j < nh; j++)
            for (int i = 0; i < nw; i++)
                res[j][i] = image[(int)(j * ry)][(int)(i * rx)];
        return res;
    }
    public byte[] resize(byte[] image, int ww, int hh){
        int nw = n_x, nh = n_x;
        float ry = hh / (float)nh, rx = ww / (float)nw;
        byte[] res = new byte[nw * nh];
        for (int j = 0; j < nh; j++)
            for (int i = 0; i < nw; i++)
                 res[j * nw + i] = image[(int)(j * ry) * ww + (int)(i * rx)];
        return res;
    }
    public byte[] i_to_b(int[] image){
        byte[] res = new byte[n_in];
        for (int i = 0; i < n_in; i++)
            res[i] = (byte)Math.min(image[i], 255);
        return res;

    }
    public int[] b_to_i(byte[] image){
        int[] res = new int[n_in];
        for (int i = 0; i < n_in; i++)
            res[i] = image[i];
        return res;

    }
    public int[] add_hist(int[] image){ // 画像に過去の操作を埋め込む
        int nw = n_x, nh = n_x;
        String nhist = mLblHisr;
        for (int j = 0; j < nhist.length(); j++)
            for (int i = 1; i < n_out; i++)
                image[(10 + j) * nw + n_x - i] = Integer.parseInt(nhist.substring(j, j + 1)) * 10 + 300;

        return image;
    }
    public int[] adjust(int[] image){
        int[] res = new int[n_in];
        for (int i = 0; i < n_in; i++){
            res[i] = image[i];
            if(res[i] < 0)
                res[i] += 256;
        }
        return res;
    }
    public int[] reg(int[] x){ // 単純に正規化
        int min = 1000, max = 0;
        for (int i = 0; i < n_in; i++){
            if(x[i] > max)
                max = x[i];
            if(x[i] < min)
                min = x[i];
        }
        int diff = max - min;
        if(diff == 0) diff = 1;
        int[] res = new int[n_in];
        for (int i = 0; i < n_in; i++)
            res[i] = (x[i] - min) * 255 / diff;

        return res;
    }
    public byte[] flatten(byte[][] arr, int xx, int yy){
        byte[] res = new byte[xx * yy];
        for (int j = 0; j < yy; j++){
            for (int i = 0; i < xx; i++){
                res[j * xx + i] = arr[j][i];
            }
        }
        return res;
    }
    public byte[][] reshape(byte[] f, int y, int x){
        byte[][] res = new byte[y][x];
        for(int j = 0; j < y; j++)
            for(int i = 0; i < x; i++)
                res[j][i] = f[j * x + i];
        return res;
    }
    public float[][] reshape(float[] f, int y, int x){
        float[][] res = new float[y][x];
        for(int j = 0; j < y; j++)
            for(int i = 0; i < x; i++)
                res[j][i] = f[j * x + i];
        return res;
    }
    public float[] read_data(int n, String fileName){
        float[] res = new float[n];
        try {
            DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(fileName)));
            byte[] buffer = new byte[n * 4];
            int bytesRead = in.read(buffer);
            byte[] swbuffer = switch_buffer(buffer);
            in = new DataInputStream(new ByteArrayInputStream(swbuffer));
            try {
                int i = 0;
                while (true) {
                    res[i] = in.readFloat();
                    i++;
                }
            } catch (Exception e) {
                in.close();
            }
        } catch (Exception e) {
        }
        return res;
    }
    public static byte[] switch_buffer(byte[] b){ // リトルエンディアン, ビッグエンディアン
        byte[] res = new byte[b.length];
        for(int i = 0; i < b.length; i+=4) {
            res[i+0] = b[i+3];
            res[i+1] = b[i+2];
            res[i+2] = b[i+1];
            res[i+3] = b[i+0];
        }
        return res;
    }
    public int argmax(float[] x){
        int max_i = 0;
        float max_v = x[0];
        for(int i = 0; i < x.length; i++) {
            if(max_v < x[i]){
                max_v = x[i];
                max_i = i;
            }
        }
        return max_i;
    }
    public float[] wxb(byte[] x, float[][] w, float[] b){ // WX + b
        float[] res = new float[w[0].length];
        float sum = 0;
        for(int j = 0; j < res.length; j++) {
            sum = 0;
            for(int i = 0; i < x.length; i++)
                sum += x[i] * w[i][j];
            res[j] = sum + b[j];
        }
        return res;
    }
    public float[] wxb(int[] x, float[][] w, float[] b){ // WX + b
        float[] res = new float[w[0].length];
        float sum = 0;
        for(int j = 0; j < res.length; j++) {
            sum = 0;
            for(int i = 0; i < x.length; i++)
                sum += x[i] * w[i][j];
            res[j] = sum + b[j];
        }
        return res;
    }
}
