package com.shufti.shuftipro.utils;

import android.content.Context;
import android.provider.Settings;

public class Utils {

    private static final String PREF_UNIQUE_ID = "ANDROID_SHUFTIPRO";
    private static String uniqueID = null;

    public synchronized static String getUniqueReference(Context context) {
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            long milliseconds = System.currentTimeMillis();
            String timeStamp = Long.toString(milliseconds);
            uniqueID = PREF_UNIQUE_ID + timeStamp + androidId;
        return uniqueID;
    }
}

