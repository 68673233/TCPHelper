package com.example.liwb.tcphelperdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    private TcpClient tcpClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       tcpClient=new TcpClient("192.168.0.3",5000,true);
       if (tcpClient.isConnectState()){
           tcpClient.sendMessage(new byte[]{(byte)0xAA,0x01,0x02});
       }
    }


}
