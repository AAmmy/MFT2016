/*
* 0で通信要求
* コントローラ
* */

package com.bt_client.abc.btclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public BluetoothSocket receivedSocket = null;
    public OutputStream out = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button[] button = new Button[8];
        int[] btn_id = {R.id.button_0, R.id.button_1, R.id.button_2, R.id.button_3, R.id.button_4, R.id.button_5, R.id.button_6, R.id.button_7};
        for (int i = 0; i < btn_id.length; i++)
            button[i] = (Button) findViewById(btn_id[i]);

        button[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(setClient(1) == null)
                    setClient(2);
                setOutSocket();
            }
        });
        button[7].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                write("7");
            }
        });

        for (int i = 1; i < btn_id.length - 1; i++){
            final String l = String.valueOf(i);
            button[i].setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return sendButtonAction(event.getAction(), l);
                }
            });
        }
    }

    public boolean sendButtonAction(int motion, String label){
        if(motion == MotionEvent.ACTION_UP) write("0");
        else if(motion == MotionEvent.ACTION_DOWN) write(label);
        return true;
    }

    public BluetoothSocket setClient(int nc){
        BluetoothAdapter myClientAdapter = BluetoothAdapter.getDefaultAdapter();
        Set pairedDevices = myClientAdapter.getBondedDevices();
        Iterator pdi = pairedDevices.iterator();
        BluetoothSocket tmpSock = null;
        BluetoothDevice mDevice = (BluetoothDevice)pdi.next();
        for(int i = 1; i < nc; i++)
            mDevice = (BluetoothDevice)pdi.next();

        //UUIDの生成
        UUID TECHBOOSTER_BTSAMPLE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try{
            // 自デバイスのBluetoothクライアントソケットの取得
            tmpSock = mDevice.createRfcommSocketToServiceRecord(TECHBOOSTER_BTSAMPLE_UUID);
            Log.d("MyApp", "Client ソケット取得完了:" + tmpSock.toString());
        }catch(IOException e){
            e.printStackTrace();
        }
        receivedSocket = tmpSock;

        // 接続要求を出す前に、検索処理を中断する。
        if(myClientAdapter.isDiscovering())
            myClientAdapter.cancelDiscovery();

        try{
            // サーバー側に接続要求
            receivedSocket.connect();
        }catch(IOException e){
            Log.d("MyApp", "Client 接続エラー");
            try {
                receivedSocket.close();
            } catch (IOException closeException) {
                e.printStackTrace();
            }
            return null;
        }
        return receivedSocket;
    }

    public void setOutSocket(){
        // 接続済みソケットからI/Oストリームをそれぞれ取得
        try {
            out = receivedSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String str){
        try {
            write_(str.getBytes("UTF-8"), out);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }

    public void write_(byte[] buf, OutputStream out){
        // Outputストリームへのデータ書き込み
        try {
            out.write(buf);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }
    }
}
