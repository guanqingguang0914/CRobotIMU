package com.abilix.robot.c.imu.event;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by tony on 17-11-23.
 */

public class YawEvent {
    public final float yaw;
    private YawEvent y;
    public YawEvent(float yaw) {
        this.yaw = yaw;
    }
    public float getYaw() {
        return yaw;
    }

}
