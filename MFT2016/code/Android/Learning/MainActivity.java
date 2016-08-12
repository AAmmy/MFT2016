// 公開用にテストコード削除, コメント追加, FTドライバ必要
// もう1台のスマホとBleutooth接続し, それをコントローラとして操縦する
// スマホの操作をファイル名に入れて画像を保存する
// 集めた画像は別PCで学習

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends Activity {
    // camera
    private SurfaceView mySurfaceView;
    private Camera myCamera;
    private int w = 320, h = 240;
    private int size = w * h;
    private int p_num = 0; // 保存した画像枚数
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

    public String mLabel = "0"; // 現在の操作, 左:2, 直進:0, 右:5
    public String mLblHisr = "000000000000000000000000000000"; // 操作の履歴, これもファイル名に入れる

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

        main_loop();

        // Arduino
        try {
            ftdid2xx = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException ex) {
            Log.d("MyApp", "### Arduino error! ###");
        }
        mContext = this.getBaseContext();
        openUsb();
    }

    //コールバック
    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            myCamera = Camera.open();
            Camera.Parameters parameters = myCamera.getParameters();
            parameters.setPreviewSize(w, h);
            parameters.setPictureSize(w, h);
            Log.d("MyApp", "###  ###" + w + "," + h);
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
                Log.d("MyApp", "### ServerAdapter disabled! ###");
            }

        }
    };

    private View.OnClickListener onSurfaceClickListener = new View.OnClickListener(){
        @Override
        public void onClick(View view){
            if(myCamera != null)
                myCamera.autoFocus(autoFocusCallback);
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
                    // dataは-128～127であるので128を足すことで画像として表示できる → なぜか 0～255 になった
                    // NNでは負でも問題ないのでそのまま扱う
                    // dataの先頭 w x h 個には輝度値のデータが入っているのでその部分のみ使用
                    // Log.d("MyApp", "### previewCallback! ### " + mLabel);
                    if(mLabel != "7") save_byte_array_by_cropping(data, mLabel);
                }
            };

    private void savePict(byte[] data, String l){
        save_byte_array_by_cropping(data, l);
    }
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

    public void setInSocket() {
        try {
            // 接続済みソケットからI/Oストリームをそれぞれ取得
            in = receivedSocket.getInputStream();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // BlueTooth
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
                // Bluetooth
                setServer();
                setInSocket();
                read();

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
                        Log.d("MyApp", mLblHisr);
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    // 画像をSDカードに保存
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

    public void save_byte_array_by_cropping(byte[] d, String l){
        for(int i = 0 ; i < size; i++) // YUV形式で得られる画像のY(輝度値)のみ保存
            cropped_data[i] = d[i];
        save_byte_array(cropped_data, l);
    }

    // 画像の保存, byteをubyteで保存するので値が変化するがその値のまま学習する
    public void save_byte_array(byte[] d, String l){
        String saveDir = "/mnt/sdcard/external_sd/test/";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String imgPath = saveDir + sf.format(cal.getTime()) + "_" + String.valueOf(p_num) + "_" + mLblHisr + "_" + l + ".txt";
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(imgPath, true);
            fos.write(d);
            fos.close();
            p_num += 1;
            Log.d("MyApp", "saved " + p_num);
        } catch (Exception e) {
            Log.d("MyApp", "failed to saved");
        }
    }

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
}