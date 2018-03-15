package com.abilix.robot.c.imu;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.abilix.robot.c.imu.event.YawEvent;
import com.abilix.robot.c.serialport.SerialPortInstance;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    @BindView(R.id.yaw)
    public TextView yaw;
    @BindView(R.id.delta_id)
    public EditText delta_id;
    @BindView(R.id.sudu_value)
    public EditText sudu_value;
    @BindView(R.id.angle_value)
    public EditText angle_value;
    private int mCurrentID1 = 2;
    private int mCurrentID2 = 4;
    private float Angle_yaw, currentYaw;
    private double p = 2.5;
    private int leftSpeed, rightSpeed;
    private boolean flag = true;
    private int speed0 = 512;
    private boolean isTurnLeft,isTurnRight,isWalkLine;
    private ExecutorService executorService;
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 1:
                    Angle_yaw = currentYaw;
                    isTurnLeft = false;
                    isTurnRight = false;
                    isWalkLine = true;
                    flag = false;
                    break;
                case 2:
                    Angle_yaw = currentYaw + 360;
                    isTurnLeft = true;
                    isTurnRight = false;
                    isWalkLine = false;
                    flag = false;
                    break;
                case 3:
                    Angle_yaw = currentYaw - 360;
                    isTurnLeft = false;
                    isTurnRight = true;
                    isWalkLine = false;
                    flag = false;
                    break;
                case 4:
                    Angle_yaw = currentYaw;
                    isTurnLeft = false;
                    isTurnRight = false;
                    isWalkLine = true;
                    flag = false;
                    break;
                case 5:
                    try {
                        flag = true;
                        Thread.sleep(50);
//                stopService(new Intent(MainActivity.this, SensorImuService.class));
                        byte[] w1 = {(byte)0x03,(byte)mCurrentID1,(byte)0x00,(byte) 0x00,(byte)0x00};
                        byte[] w2 = {(byte)0x03,(byte)mCurrentID2,(byte)0x00,(byte) 0x00,(byte)0x00};
                        SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w1));
                        Thread.sleep(10);
                        SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w2));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case 6:
                    flag = true;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            Log.d("SensorYaw","write0");
            SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x09, new byte[]{(byte)0x02,(byte)mCurrentID1,(byte)0x01}));
            Log.d("SensorYaw","write");
            Thread.sleep(10);
            SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x09, new byte[]{(byte)0x02,(byte)mCurrentID2,(byte)0x01}));
//                    byte[] w3 = {(byte)0x03,(byte)0x02,(byte)0x01,(byte) (512 & 0xFF),(byte) ((512 >> 8) & 0xFF)};
//                    byte[] w4 = {(byte)0x03,(byte)0x04,(byte)0x02,(byte) (512 & 0xFF),(byte) ((512 >> 8) & 0xFF)};
//                    write(sendProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xAA, w3));
//                    Thread.sleep(5);
//                    write(sendProtocol((byte) 0x03, (byte) 0xA3, (byte) 0xAA, w4));
        } catch (Exception e) {
            e.printStackTrace();
        }

        startService(new Intent(MainActivity.this, SensorImuService.class));
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        executorService =  Executors.newCachedThreadPool();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveEvent(YawEvent event) {
        currentYaw = event.getYaw();
        yaw.setText(""+currentYaw);
        if (!flag) {
            try {
            float delta = currentYaw - Angle_yaw;
            if(isWalkLine){
//                float delta = currentYaw - Angle_yaw;
                    leftSpeed = (speed0 + (int) (p * delta));
                    rightSpeed = (speed0 - (int) (p * delta));
                    Log.e("SensorYaw", "Yaw====>" + event.yaw + ",delta===>" + delta + ",w3[0]===>" + leftSpeed + ",w3[1]====>" + rightSpeed);
            }else {
//                float delta = (isTurnLeft ? currentYaw - Angle_yaw : Angle_yaw - currentYaw);
                Log.e("SensorYaw", "delta===>" + delta);
                if (delta > -2 && delta < 2){
                    leftSpeed = 1;
                    rightSpeed = 1;
                    flag = true;
                }else {
                    leftSpeed = speed0;
                    rightSpeed = speed0;
                }
            }
            if (rightSpeed < 0) {
                rightSpeed = 1;
            }
                if (leftSpeed < 0) {
                    leftSpeed = 1;
                }
            if (leftSpeed > 1023) {
                leftSpeed = 1023;
            }
                if (rightSpeed > 1023) {
                    rightSpeed = 1023;
                }
            Log.i("SensorYaw", "leftSpeed = "+leftSpeed+" rightSpeed = "+rightSpeed);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        byte[] w1;
                        byte[] w2;
                        if(isWalkLine){
                            w1 = new byte[]{(byte)0x03,(byte)mCurrentID2,(byte)0x01,(byte) ((leftSpeed >> 8) & 0xFF),(byte) (leftSpeed & 0xFF)};//4
                            w2 = new byte[]{(byte)0x03,(byte)mCurrentID1,(byte)0x02,(byte) ((rightSpeed >> 8) & 0xFF),(byte) (rightSpeed & 0xFF)};//2
                        }else {
                            w1 = new byte[]{(byte)0x03,(byte)mCurrentID2,(byte)(isTurnLeft ? 1 : 2),(byte) ((leftSpeed >> 8) & 0xFF),(byte) (leftSpeed & 0xFF)};//4
                            w2 = new byte[]{(byte)0x03,(byte)mCurrentID1,(byte)(isTurnLeft ? 1 : 2),(byte) ((rightSpeed >> 8) & 0xFF),(byte) (rightSpeed & 0xFF)};//2
                        }
                        try {
                            SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w1));
                            Thread.sleep(5);
                            SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w2));
//                            Thread.sleep(5);
//                            SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w1));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    public byte[] intToBytes(int value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    @OnClick({R.id.start, R.id.stop, R.id.exit, R.id.delta_ok, R.id.sudu_ok, R.id.angle_left, R.id.angle_right, R.id.testmodel})
    public void execButtonClick(View v) {
        switch (v.getId()) {
            case R.id.testmodel:
                mHandler.sendEmptyMessageDelayed(1,1000);
//                mHandler.sendEmptyMessageDelayed(6,5500);
                mHandler.sendEmptyMessageDelayed(2,6000);
//                mHandler.sendEmptyMessageDelayed(6,9500);
                mHandler.sendEmptyMessageDelayed(3,15000);
//                mHandler.sendEmptyMessageDelayed(6,5500);
                mHandler.sendEmptyMessageDelayed(4,25000);
//                mHandler.sendEmptyMessageDelayed(6,5500);
                mHandler.sendEmptyMessageDelayed(5,30000);
                break;
            case R.id.angle_left:
                String left = angle_value.getText().toString().trim();
                Angle_yaw = currentYaw + Integer.valueOf(left);
                isTurnLeft = true;
                isTurnRight = false;
                isWalkLine = false;
                flag = false;
                break;
            case R.id.angle_right:
                String right = angle_value.getText().toString().trim();
                Angle_yaw = currentYaw - Integer.valueOf(right);
                isTurnLeft = false;
                isTurnRight = true;
                isWalkLine = false;
                flag = false;
                break;
            case R.id.start:
                Angle_yaw = currentYaw;
                isTurnLeft = false;
                isTurnRight = false;
                isWalkLine = true;
                flag = false;
                break;
            case R.id.stop:
                try {
                    flag = true;
                    Thread.sleep(50);
//                stopService(new Intent(MainActivity.this, SensorImuService.class));
                    byte[] w1 = {(byte)0x03,(byte)mCurrentID1,(byte)0x00,(byte) 0x00,(byte)0x00};
                    byte[] w2 = {(byte)0x03,(byte)mCurrentID2,(byte)0x00,(byte) 0x00,(byte)0x00};
                    SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w1));
                    Thread.sleep(10);
                    SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0xAA, w2));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.exit:
                SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x09, new byte[]{(byte)0x02,(byte)mCurrentID1,(byte)0x00}));
                SerialPortInstance.getInstance().sendBuffer(sendProtocol((byte) 0x05, (byte) 0xA3, (byte) 0x09, new byte[]{(byte)0x02,(byte)mCurrentID2,(byte)0x00}));
//                SPAPI.unbindControlService();
                finishAll();
                break;
            case R.id.delta_ok:
                String value = delta_id.getText().toString().trim();
                p = Integer.valueOf(value);
                break;
            case R.id.sudu_ok:
                String suduvalue = sudu_value.getText().toString().trim();
                speed0 = Integer.valueOf(suduvalue);
                break;
        }
    }


    private void finishAll() {
        EventBus.getDefault().unregister(this);
        stopService(new Intent(MainActivity.this, SensorImuService.class));
        finish();
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        stopService(new Intent(MainActivity.this, SensorImuService.class));
        super.onStop();
    }

    public byte[] sendProtocol(byte type, byte cmd1, byte cmd2, byte[] data) {
        byte[] sendbuff;
        if (data == null) {
            byte[] buf = new byte[8];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            sendbuff = addProtocol(buf);
        } else {
            byte[] buf = new byte[8 + data.length];
            buf[0] = type;
            buf[1] = cmd1;
            buf[2] = cmd2;
            System.arraycopy(data, 0, buf, 7, data.length);
            sendbuff = addProtocol(buf);
        }
        return sendbuff;
    }

    // 协议封装： AA 55 len1 len2 type cmd1 cmd2 00 00 00 00 (data) check
    public byte[] addProtocol(byte[] buff) {

        short len = (short) (buff.length);
        byte[] sendbuff = new byte[len + 4];
        sendbuff[0] = (byte) 0xAA; // 头
        sendbuff[1] = (byte) 0x55;
        sendbuff[3] = (byte) (len & 0x00FF); // 长度: 从type到check
        sendbuff[2] = (byte) ((len >> 8) & 0x00FF);
        System.arraycopy(buff, 0, sendbuff, 4, buff.length); // type - data
        byte check = 0x00; // 校验位
        for (int n = 0; n <= len + 2; n++) {
            check += sendbuff[n];
        }
        sendbuff[len + 3] = (byte) (check & 0x00FF);
        return sendbuff;
    }




    private synchronized void  write(byte[] bs) {

        if (SPAPI.getIControl() == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            write(bs);
        } else {
            try {
                SPAPI.getIControl().write(bs);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized byte[] request(byte[] bs) {
        byte[] bs2 = new byte[30];
        if (SPAPI.getIControl() == null) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            request(bs);
        } else {
            try {
                bs2 = SPAPI.getIControl().request(bs);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return bs2;
    }

}
