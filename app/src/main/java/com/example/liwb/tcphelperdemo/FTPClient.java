package com.example.liwb.tcphelperdemo;

import com.example.tcphelper.FTPHelper;

/**
 * Created by liwb on 2017/8/16.
 */

public class FTPClient extends FTPHelper {

    public FTPClient(){

        super("192.168.1.254", 21, "root", "micsig");
    }
    public FTPClient(String IP, int port, String userName, String password) {
        super(IP, port, userName, password);
    }
    @Override
    public void currProcess(long currPos) {
        //to-do 在线程中进行，需要发送message 到主线程更新界面
    }

    @Override
    public void errorServiceFileNotExist() {

    }


}
