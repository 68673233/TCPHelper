package com.example.tcphelper;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Created by liwb on 2017/8/15.
 */

public abstract class FTPHelper {
    private FTPClient ftpClient = null;

    //region 属性
    private String IP="192.168.1.254";
    private int Port=21;
    private String userName="root",Password="micsig";

    public void setIP(String IP) {
        this.IP = IP;
    }

    public void setPort(int port) {
        Port = port;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        Password = password;
    }

    //endregion
    public String localRootPath="/mnt/sdcard/me0919/";

    //region override

    /**
     * 当前下载，上传进度比
      * @param currPos
     */
    public abstract void currProcess(long currPos);

    /**
     * 服务器端文件不存在。
     */
    public abstract void errorServiceFileNotExist();


    //endregion

    public FTPHelper(String IP,int port ,String userName,String password) {
        ftpClient = new FTPClient();
        this.setIP(IP);
        this.setPort(port);
        this.setUserName(userName);
        this.setPassword(password);
    }

    // 连接到ftp服务器
    public synchronized boolean connect() throws Exception {
        boolean bool = false;
        if (ftpClient.isConnected()) {//判断是否已登陆
            ftpClient.disconnect();
        }
        ftpClient.setDataTimeout(20000);//设置连接超时时间
        ftpClient.setControlEncoding("utf-8");
        ftpClient.connect(IP,Port);
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            if (ftpClient.login(userName, Password)) {
                bool = true;
                System.out.println("ftp连接成功");
            }
        }
        return bool;
    }

    // 创建文件夹
    public boolean createDirectory(String path) throws Exception {
        boolean bool = false;
        String directory = path.substring(0, path.lastIndexOf("/") + 1);
        int start = 0;
        int end = 0;
        if (directory.startsWith("/")) {
            start = 1;
        }
        end = directory.indexOf("/", start);
        while (true) {
            String subDirectory = directory.substring(start, end);
            if (!ftpClient.changeWorkingDirectory(subDirectory)) {
                ftpClient.makeDirectory(subDirectory);
                ftpClient.changeWorkingDirectory(subDirectory);
                bool = true;
            }
            start = end + 1;
            end = directory.indexOf("/", start);
            if (end == -1) {
                break;
            }
        }
        return bool;
    }

    // 实现上传文件的功能
    public synchronized boolean uploadFile(String localPath, String serverPath)
            throws Exception {
        // 上传文件之前，先判断本地文件是否存在
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            System.out.println("本地文件不存在");
            return false;
        }
        System.out.println("本地文件存在，名称为：" + localFile.getName());
        createDirectory(serverPath); // 如果文件夹不存在，创建文件夹
        System.out.println("服务器文件存放路径：" + serverPath + localFile.getName());
        String fileName = localFile.getName();
        // 如果本地文件存在，服务器文件也在，上传文件，这个方法中也包括了断点上传
        long localSize = localFile.length(); // 本地文件的长度
        FTPFile[] files = ftpClient.listFiles(fileName);
        long serverSize = 0;
        if (files.length == 0) {
            System.out.println("服务器文件不存在");
            serverSize = 0;
        } else {
            serverSize = files[0].getSize(); // 服务器文件的长度
        }
        if (localSize <= serverSize) {
            if (ftpClient.deleteFile(fileName)) {
                System.out.println("服务器文件存在,删除文件,开始重新上传");
                serverSize = 0;
            }
        }
        RandomAccessFile raf = new RandomAccessFile(localFile, "r");
        // 进度
        long step = localSize / 100;
        long process = 0;
        long currentSize = 0;
        // 好了，正式开始上传文件
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        ftpClient.setRestartOffset(serverSize);
        raf.seek(serverSize);
        OutputStream output = ftpClient.appendFileStream(fileName);
        byte[] b = new byte[1024];
        int length = 0;
        while ((length = raf.read(b)) != -1) {
            output.write(b, 0, length);
            currentSize = currentSize + length;
            if (currentSize / step != process) {
                process = currentSize / step;
                if (process % 10 == 0) {
                    System.out.println("上传进度：" + process);
                    currProcess(process);
                }
            }
        }
        output.flush();
        output.close();
        raf.close();
        if (ftpClient.completePendingCommand()) {
            System.out.println("文件上传成功");
            return true;
        } else {
            System.out.println("文件上传失败");
            return false;
        }
    }

    // 实现下载文件功能，可实现断点下载
    public synchronized boolean downloadFile(String localPath, String serverPath)
            throws Exception {
        // 先判断服务器文件是否存在
        FTPFile[] files = ftpClient.listFiles(serverPath);
        if (files.length == 0) {
            System.out.println("服务器文件不存在");
            errorServiceFileNotExist();
            return false;
        }
        System.out.println("远程文件存在,名字为：" + files[0].getName());
        String[] s=files[0].getName().split("/");
        localPath = localPath + "/"+s[s.length-1];
        // 接着判断下载的文件是否能断点下载
        long serverSize = files[0].getSize(); // 获取远程文件的长度
        File localFile = new File(localPath);
        long localSize = 0;
        if (localFile.exists()) {
            localSize = localFile.length(); // 如果本地文件存在，获取本地文件的长度
            if (localSize >= serverSize) {
                System.out.println("文件已经下载完了");
                File file = new File(localPath);
                file.delete();
                System.out.println("本地文件存在，删除成功，开始重新下载");
                return false;
            }
        }
        // 进度
        long step = serverSize / 100;
        long process = 0;
        long currentSize = 0;
        // 开始准备下载文件
        ftpClient.enterLocalActiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        OutputStream out = new FileOutputStream(localFile, true);
        ftpClient.setRestartOffset(localSize);
        InputStream input = ftpClient.retrieveFileStream(serverPath);
        byte[] b = new byte[1024];
        int length = 0;
        while ((length = input.read(b)) != -1) {
            out.write(b, 0, length);
            currentSize = currentSize + length;
            if (currentSize / step != process) {
                process = currentSize / step;
                if (process % 10 == 0) {
                    System.out.println("下载进度：" + process);
                    currProcess(process);
                }
            }
        }
        out.flush();
        out.close();
        input.close();
        // 此方法是来确保流处理完毕，如果没有此方法，可能会造成现程序死掉
        if (ftpClient.completePendingCommand()) {
            System.out.println("文件下载成功");
            return true;
        } else {
            System.out.println("文件下载失败");
            return false;
        }
    }

    // 如果ftp上传打开，就关闭掉
    public void closeFTP() throws Exception {
        if (ftpClient.isConnected()) {
            ftpClient.disconnect();
        }
    }


//    // 上传例子
//    private void ftpUpload() {
//        new Thread() {
//            public void run() {
//                try {
//                    System.out.println("正在连接ftp服务器....");
//                    FTPHelper ftpHelper = new FTPHelper();
//                    if (ftpHelper.connect()) {
//                        if (ftpHelper.uploadFile(ftpHelper.localRootPath+"softwareParam.txt", "/mnt/sdcard/" )) {
//                            ftpHelper.closeFTP();
//                            System.out.println("上传文件成功！");
//                        }
//                    }
//                } catch (Exception e) {
//                    // TODO: handle exception
//                    // System.out.println(e.getMessage());
//                }
//            }
//        }.start();
//    }

//    // 下载例子
//    private void ftpDownload() {
//        new Thread() {
//            public void run() {
//                try {
//                    System.out.println("正在连接ftp服务器....");
//                    FTPHelper ftpHelper = new FTPHelper();
//                    if (ftpHelper.connect()) {
//                        if (ftpHelper.downloadFile(ftpHelper.localRootPath, "20120723_XFQ07_XZMarketPlatform.db")) {
//                            ftpHelper.closeFTP();
//                        }
//                    }
//                } catch (Exception e) {
//                    // TODO: handle exception
//                    // System.out.println(e.getMessage());
//                }
//            }
//        }.start();
//    }


    /**
     * Description: 向FTP服务器上传文件
     *
     * @param url
     *            FTP服务器hostname
     * @param port
     *            FTP服务器端口
     * @param username
     *            FTP登录账号
     * @param password
     *            FTP登录密码
     * @param path
     *            FTP服务器保存目录，是linux下的目录形式,如/photo/
     * @param filename
     *            上传到FTP服务器上的文件名,是自己定义的名字，
     * @param input
     *            输入流
     * @return 成功返回true，否则返回false
     */
    public static boolean uploadFile(String url, int port, String username,
                                     String password, String path, String filename, InputStream input) {
        boolean success = false;
        FTPClient ftp = new FTPClient();


        try {
            int reply;
            ftp.connect(url, port);// 连接FTP服务器
            // 如果采用默认端口，可以使用ftp.connect(url)的方式直接连接FTP服务器
            ftp.login(username, password);//登录
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return success;
            }
            ftp.changeWorkingDirectory(path);
            ftp.storeFile(filename, input);

            input.close();
            ftp.logout();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return success;
    }
}
