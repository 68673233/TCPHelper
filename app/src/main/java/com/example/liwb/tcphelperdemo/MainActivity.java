package com.example.liwb.tcphelperdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.tcphelper.FTPHelper;

public class MainActivity extends AppCompatActivity {

    private TcpClient tcpClient;
    private Button btn2,btn4;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//       tcpClient=new TcpClient("192.168.0.3",5000,true);
//       if (tcpClient.isConnectState()){
//           tcpClient.sendMessage(new byte[]{(byte)0xAA,0x01,0x02});
//       }

        btn2=findViewById(R.id.button2);
        btn2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                  ftpUpload();
            }
        });

        btn4=findViewById(R.id.button4);
        btn4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                ftpDownload();
            }
        });
    }


    private void ftpUpload() {
        new Thread() {
            public void run() {
                try {
                    System.out.println("正在连接ftp服务器....");
                    FTPClient ftpHelper = new FTPClient();
                    if (ftpHelper.connect()) {
                        if (ftpHelper.uploadFile(ftpHelper.localRootPath+"me0919.bin", "/mnt/sdcard/" )) {
                            ftpHelper.closeFTP();
                            System.out.println("上传文件成功！");
                        }
                    }
                } catch (Exception e) {
                    // TODO: handle exception

                     System.out.println(e.getMessage());
                }
            }
        }.start();
    }

    private void ftpDownload() {
        new Thread() {
            public void run() {
                try {
                    System.out.println("正在连接ftp服务器....");
                    FTPClient ftpHelper = new FTPClient();
                    if (ftpHelper.connect()) {
                        if (ftpHelper.downloadFile("/mnt/sdcard/me0919s/", "/mnt/sdcard/me0919.bin")) {
                            ftpHelper.closeFTP();
                        }
                    }
                } catch (Exception e) {
                    // TODO: handle exception
                     System.out.println(e.getMessage());
                }
            }
        }.start();
    }
}
