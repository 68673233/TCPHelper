package com.example.liwb.tcphelperdemo;

import com.example.tcphelper.TcpHelper;

/**
 * Created by liwb on 2017/8/15.
 */

public class TcpClient extends TcpHelper {

    private boolean connectState=false;
    public boolean isConnectState() {
        return connectState;
    }

    public TcpClient(String IP, int port, boolean RecieveTimeOutEnable) {
        super(IP, port, RecieveTimeOutEnable);
    }

    @Override
    public void tcpConnect(String IP, int port, boolean RecieveTimeOutEnable) {
        super.tcpConnect(IP, port, RecieveTimeOutEnable);
    }


    @Override
    public void ReceiveChar(byte[] recieve) {

    }

    @Override
    public void connectSuccess() {
        connectState=true;
    }

    @Override
    public void connectTimeOut() {
        connectState=false;
    }

    @Override
    public void receiveTimeOut() {

    }

    @Override
    public void closeConnect() {
        super.closeConnect();
    }

    @Override
    public void sendMessage(byte[] data) {
        super.sendMessage(data);
    }
}
