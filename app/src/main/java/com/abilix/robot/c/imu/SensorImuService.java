package com.abilix.robot.c.imu;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;

import com.abilix.robot.c.imu.event.YawEvent;

import org.greenrobot.eventbus.EventBus;

public class SensorImuService extends Service implements SensorEventListener {

    //Sensor
    private SensorManager sensorManager;
    private Sensor gyroSsensor;
    private Sensor accSensor;
    private Sensor mZensor;
    //=================================Yaw=========================================
    public static float Angle_yaw;
    private static boolean isAngleYawAvaiable = false;
    private float timestamp = 0;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private float angle[] = new float[3];
    private static float zeroDrift = 0.011f;

    private enum INIT_STATUS {
        NOT_INIT,
        ON_INITING,
        ON_INITING2,
        INITED,
    }

    private static INIT_STATUS initStatus = INIT_STATUS.NOT_INIT;
    private float zeroDriftSum = 0.0f;
    private int zeroDriftCount = 0;

    private boolean beginYaw = false;
    private float beginYawTime = 0;
    private float beginYawValue = 0.0f;


    private static int ZERO_DRIFT_MAX_COUNT = 1000;   //计算零漂第一阶测试次数  默认值1000,大约耗时6秒钟 500,大约耗时3秒钟
    private int TestYawTime_s = 5;                   //计算零漂第二阶测试时长，单位：秒 默认测试值为15
    private float yawDiffPreMinute = 1.0f;            //除零漂后计算的yaw值，每分钟最大偏yawDiffPreMinute度 默认值：0.1
    private float preYawMaxDiff = TestYawTime_s * yawDiffPreMinute / 60.0f;
    private int ignoreCountWhenCalcuZeroDrift = 1000;   //不考虑第一次放下机器人时，陀螺仪不准确。 默认值1000,大约耗时6秒钟
    //==============================================================================================================================


    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Angle_yaw","onCreate()");
        onInit();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        gyroSsensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyroSsensor, SensorManager.SENSOR_DELAY_FASTEST);
        accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        mZensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(this, mZensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            if (timestamp != 0) {
                Log.d("Angle_yaw","Angle_yaw======>1");
                if (initStatus == INIT_STATUS.ON_INITING) {
//                    if (--ignoreCountWhenCalcuZeroDrift > 0) {
//                        return;
//                    }
                    zeroDriftSum += event.values[2];
                    ++zeroDriftCount;
                    if (zeroDriftCount >= ZERO_DRIFT_MAX_COUNT) {
                        zeroDrift = zeroDriftSum / zeroDriftCount;
                        initStatus = INIT_STATUS.ON_INITING2;
                        beginYaw = false;
                    }
                } else {
                    Log.d("Angle_yaw","Angle_yaw======>2");
                    final float dT = (event.timestamp - timestamp) * NS2S;
                    angle[0] += (event.values[2] - zeroDrift) * dT;
                    Log.d("Angle_yaw","Angle_yaw======>2 + angle[0]" + angle[0]);
                    Angle_yaw = (float) Math.toDegrees(angle[0]);
//                    if (Angle_yaw > 180) Angle_yaw -= 360;
//                    if (Angle_yaw < 0) Angle_yaw += 360;
                    if (initStatus == INIT_STATUS.ON_INITING2) {
                        if (!beginYaw) {
                            beginYawTime = event.timestamp;
                            beginYawValue = Angle_yaw;
                            beginYaw = true;
                        } else {
                            Log.d("Angle_yaw","Angle_yaw======>3");
                            float t_time = event.timestamp - beginYawTime;
                            if (t_time > 1000000000 * TestYawTime_s) {

                                float diff = Math.abs(Math.abs(Angle_yaw) - Math.abs(beginYawValue));
                                Log.d("Angle_yaw","Angle_yaw======>4 +diff " + diff);
                                if (preYawMaxDiff >= diff) {
                                    initStatus = INIT_STATUS.INITED;
                                    isAngleYawAvaiable = true;
                                } else {
                                    Log.d("onInit","onInit");
                                    onInit();
                                }
                            }
                        }
                    }
                }
            }
            timestamp = event.timestamp;
            if (isAngleYawAvaiable){
//                if (Angle_yaw > 180) Angle_yaw -= 360;
//            if (Angle_yaw < 0) Angle_yaw += 360;
                Log.e("Angle_yaw","Angle_yaw======>"+Angle_yaw);
                EventBus.getDefault().post(new YawEvent(Angle_yaw));
            }
        }
//        else if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){
//            if (initStatus == INIT_STATUS.ON_INITING) {
////                    if (--ignoreCountWhenCalcuZeroDrift > 0) {
////                        return;
////                    }
//                zeroDriftSum += event.values[0];
//                ++zeroDriftCount;
//                if (zeroDriftCount >= ZERO_DRIFT_MAX_COUNT) {
//                    zeroDrift = zeroDriftSum / zeroDriftCount;
//                    initStatus = INIT_STATUS.ON_INITING2;
//                    beginYaw = false;
//                    EventBus.getDefault().post(new YawEvent(zeroDrift));
//                }
//            }else {
//                Log.d("Angle_yaw","Angle_yaw======>2");
////                final float dT = (event.timestamp - timestamp) * NS2S;
////                angle[0] += (event.values[2] - zeroDrift) * dT;
////                Log.d("Angle_yaw","Angle_yaw======>2 + angle[0]" + angle[0]);
//                Angle_yaw = event.values[0];
//                if (Angle_yaw > 180) Angle_yaw -= 360;
////                    if (Angle_yaw < 0) Angle_yaw += 360;
//                if (initStatus == INIT_STATUS.ON_INITING2) {
//                    if (!beginYaw) {
//                        beginYawTime = event.timestamp;
//                        beginYawValue = Angle_yaw;
//                        beginYaw = true;
//                    } else {
//                        Log.d("Angle_yaw","Angle_yaw======>3");
//                        float t_time = event.timestamp - beginYawTime;
//                        if (t_time > 1000000000 * TestYawTime_s) {
//
//                            float diff = Math.abs(Math.abs(Angle_yaw) - Math.abs(beginYawValue));
//                            Log.d("Angle_yaw","Angle_yaw======>4 +diff " + diff);
//                            if (preYawMaxDiff >= diff) {
//                                initStatus = INIT_STATUS.INITED;
//                                isAngleYawAvaiable = true;
//                            } else {
//                                onInit();
//                            }
//                        }
//                    }
//                }
//            }
//        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onInit() {
        Log.d("Angle_yaw","Angle_yaw======>5");
        zeroDriftSum = 0.0f;
        zeroDriftCount = 0;
        ignoreCountWhenCalcuZeroDrift = 0;
        initStatus = INIT_STATUS.ON_INITING;
        isAngleYawAvaiable = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }
}
