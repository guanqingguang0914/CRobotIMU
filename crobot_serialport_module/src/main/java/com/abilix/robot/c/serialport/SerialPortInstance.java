package com.abilix.robot.c.serialport;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 串口操作类
 * (功能：只是发送数据，接受数据RTX联系我)
 * @author wudong
 *
 */
public class SerialPortInstance {
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private static final String path = "/dev/ttyMT1";
    private static final int baudrate = 500000;
    private static SerialPortInstance serialPortInstance;


    public static SerialPortInstance getInstance() {
        if (null == serialPortInstance) {
            serialPortInstance = new SerialPortInstance();
            serialPortInstance.init();
        }
        return serialPortInstance;
    }

    /**
     * 初始化串口信息
     */
    private void init() {
        try {
            mSerialPort = new SerialPort(new File(path), baudrate,0);
            mOutputStream = mSerialPort.getOutputStream();
        } catch (Exception e) {
            Log.e("SerialPort", "init() Exception==" + e.getMessage());
        }
    }
    /**
     * @param mBuffer
     * 串口发送的byte数组
     */
    public void sendBuffer(byte[] mBuffer) {
        try {
            if (mOutputStream != null) {
                mOutputStream.write(mBuffer);
            }
        } catch (IOException e) {
            Log.e("SerialPort", "sendBuffer IOException==" + e.getMessage());
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (mSerialPort != null) {
            mSerialPort.close();
        }
    }
}