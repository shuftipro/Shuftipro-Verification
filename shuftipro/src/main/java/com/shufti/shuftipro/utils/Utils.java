package com.shufti.shuftipro.utils;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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


    public static String getDeviceInformation() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        }
        return capitalize(manufacturer) + " " + model;
    }

    private static String capitalize(String str) {
        if (TextUtils.isEmpty(str)) {
            return str;
        }
        char[] arr = str.toCharArray();
        boolean capitalizeNext = true;

        StringBuilder phrase = new StringBuilder();
        for (char c : arr) {
            if (capitalizeNext && Character.isLetter(c)) {
                phrase.append(Character.toUpperCase(c));
                capitalizeNext = false;
                continue;
            } else if (Character.isWhitespace(c)) {
                capitalizeNext = true;
            }
            phrase.append(c);
        }
        return phrase.toString();
    }

    public static String getSDKVersion(){
        return "RestFul SDK 1.2.10";
    }

    public static String getCurrentTimeStamp(){
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return formatter.format(new Date(timeStamp));
    }

    public static String sdkVersion(){
        return  "1.2.10";
    }

    public static String sdkType(){
        return  "android_web";
    }
}

