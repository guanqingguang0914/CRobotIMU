package com.abilix.robot.c.imu;

import android.app.Application;
import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;


public class SApplication extends Application {
	public static SApplication instance;
	
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		SPAPI.init(this);

	}

}
