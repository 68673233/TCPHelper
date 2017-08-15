package com.example.tcphelper;


import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Created by liwb on 2017/7/10.
 */

public abstract class TcpHelper {
    private static final int MaxRecieveTimeOut=20000;

    private String IP;
    private int port;
    private Socket socket = null;
    //发送数据
    private PrintWriter out;
    private DataOutputStream output;
    //接收数据
    private DataInputStream in;
    //接收缓冲区
    private byte[] recieveChar = new byte[1024];
    //接收线程
    private RecieveData recieveData = null;
    //
    private Object lockTcpSwitch = new Object();

    //发送数据缓冲区
    private List<byte[]> list=new ArrayList<>();
    //发送线程
    private SendData sendData=new SendData();
    //超时接收是否开启 即：true 为service  false:client
    private boolean timeOutEnable=true;

    //region  虚有函数
    public abstract void ReceiveChar(byte[] recieve);
    public abstract void connectSuccess();
    public abstract  void connectTimeOut();
    public abstract void receiveTimeOut();
    //endregion
    public TcpHelper(String IP, int port,boolean RecieveTimeOutEnable) {
        this.IP = IP;
        this.port = port;
        this.timeOutEnable=RecieveTimeOutEnable;

        recieveData = new RecieveData();
        recieveData.start();

        if (sendData==null) sendData=new SendData();
        new Thread(sendData).start();
    }

    /**
     * 创建连接
     * @param IP
     * @param port
     */
    public void tcpConnect(String IP, int port,boolean RecieveTimeOutEnable){
        this.IP = IP;
        this.port = port;
        this.timeOutEnable=RecieveTimeOutEnable;

     if (recieveData==null && sendData==null) {
         recieveData = new RecieveData();
         recieveData.start();

         if (sendData == null) sendData = new SendData();
         new Thread(sendData).start();
     }
    }

    /***
     * 打开socket连接
     */
    private boolean openConnect() {
        closeConnect();
        synchronized (lockTcpSwitch) {
            socket = new Socket();
            try {
                SocketAddress socAddress = new InetSocketAddress(this.IP, this.port);
                socket.connect(socAddress, 3000);
                if (timeOutEnable==true)  socket.setSoTimeout(MaxRecieveTimeOut); //20秒
                createInStream();
                createOutStream();
                if (timeOutEnable==false) {
                     connectSuccess();
                }
            } catch (SocketException se) {
                //中断
                //se.printStackTrace();
                  connectTimeOut();
                return false;
            } catch (SocketTimeoutException se) {
                //超时
                connectTimeOut();
                return false;
            } catch (IOException e) {
                //IO错误
                return false;
            }
            return true;
        }
    }

    //关闭socket连接
    public void closeConnect() {
        synchronized (lockTcpSwitch) {
            if (socket != null) {
                try {
                    if (recieveData != null) {
                        recieveData.interrupt();
                        recieveData = null;
                    }
                    if (sendData!=null){
                        sendData.interrupt();
                        sendData=null;
                    }
                    if (in != null) in.close();
                    in = null;
                    if (output != null) out.close();
                    output = null;
                    socket.close();
                    socket = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void createOutStream() {
        try {
            out = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(
                            socket.getOutputStream())), true);
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createInStream() {
        try {
            in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //region 发送数据
    //发送数据
    public void sendMessage(byte[] data) {
         if (sendData!=null)  sendData.setSendData(data);
    }

    class SendData extends Thread {
        public void setSendData(byte[] data) {
            synchronized (lockTcpSwitch) {
                list.add(data);
            }
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    synchronized (lockTcpSwitch) {
                        if (output != null && list.size() > 0) {
                            output.write(list.get(0));
                            list.remove(0);
                        } else {
                            Thread.yield();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }
    }

    //endregion


    class RecieveData extends Thread {
        @Override
        public void run() {
            openConnect();
            createOutStream();
            createInStream();
            while (!this.isInterrupted()) {
                if (socket!=null)
                if (!socket.isInputShutdown() && in != null) {
                    try {
                        int len = in.read(recieveChar);
                        if (len > 0) {
                            ReceiveChar(Arrays.copyOf(recieveChar, len));
                        }
                    }catch (SocketTimeoutException e){
                        e.printStackTrace();
                        receiveTimeOut();
                    }
                    catch (InterruptedIOException e){
                        e.printStackTrace();
                        receiveTimeOut();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            super.run();
        }
    }

}
