package com.abilix.robot.c.imu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.abilix.control.aidl.IControl;


/**
 * 
 * 类说明
 */
public class SPAPI {
	private static SApplication instance;

    public static void init(SApplication application) {
        instance = application;
        bindControlService();
    }

    private static IControl mIControl = null;
    private static ServiceConnection mIControlConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mIControl = IControl.Stub.asInterface(service);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mIControl = null;
        }
    };

    public static IControl getIControl() {
        return mIControl;
    }
    
    /**
     * 绑定Control
     */
     public static void bindControlService() {
        Intent mIntent = new Intent();
        mIntent.setAction("com.abilix.control.aidl.IControl");
        mIntent.setPackage("com.abilix.control");
         if (instance != null) {
             instance.getApplicationContext().bindService(mIntent, mIControlConnection, Context.BIND_AUTO_CREATE);
         }
    }
     /**
      * 解绑定Control
      */
      public static void unbindControlService() {
         Intent mIntent = new Intent();
         mIntent.setAction("com.abilix.control.aidl.IControl");
         mIntent.setPackage("com.abilix.control");
          if (instance != null) {
              instance.getApplicationContext().unbindService(mIControlConnection);
          }
     }
     
}
