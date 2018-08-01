package com.yihengke.robotspeech.utils;

import android.os.IBinder;
import android.os.ISteeringService;
import android.os.RemoteException;

public class SteeringUtil {

    private static ISteeringService iSteeringService;

    private SteeringUtil() {
        iSteeringService = ISteeringService.Stub.asInterface((IBinder) Util.getServerService());
        if (iSteeringService != null) {
            try {
                iSteeringService.openDev();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static ISteeringService getInstance() {
        if (iSteeringService == null) {
            new SteeringUtil();
        }
        return iSteeringService;
    }
}
