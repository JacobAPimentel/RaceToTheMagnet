package com.example.raceapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;

//A static class that makes transferring certain data easier.
public class Settings
{
    //race (I previously used intents, but I felt like this method made switching orientation a lot more easier)
    public static Race race = null;


    //keys
    public static SharedPreferences preferences; //no need to keep on declaring preferences, just call the one in here.
    public static String SOUND_KEY = "soundURI";
    public static String MAG_THRESHOLD_KEY = "defaultMagThreshold";
    public static String DELAY_TIME_KEY = "delayTime";
    public static String POCKET_KEY = "pocketAmbient";
    public static String LIGHT_KEY = "defaultLight";

    //sensors
    public static SensorManager sensorManager; // this will never change throughout the app. No need to check for magnetic sensor for every activity.
    public static Sensor magnetic = null;
    public static Sensor light = null;

    //Main Activity Settings
    public static boolean isAutomatic = true;
    public static double magnetThreshold; //set in main activity

    //Settings
    public static Uri defaultSound = Uri.parse("android.resource://com.example.raceapp/raw/"+R.raw.notify);
    public static Uri notifySound = defaultSound;
    public static int defaultMagnetThreshold = 75;
    public static int delayTime = 5;
    public static int pocketAmbientThreshold = 0;
    public static boolean defaultLight = true;

    //methods that multiple activities calls
    public static float GetMagneticStrength(float[] axisStrengths)
    {
        float x = axisStrengths[0];
        float y = axisStrengths[1];
        float z = axisStrengths [2];

        return (float) Math.sqrt(x*x + y*y + z*z);
    }//GetMagneticStrength

    public static String GetAccuracyString(Context context, int accuracy)
    {
        switch(accuracy)
        {
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                return context.getString(R.string.magnet_status, "LOW ACCURACY");
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                return context.getString(R.string.magnet_status, "MEDIUM ACCURACY");
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                return context.getString(R.string.magnet_status, "HIGH ACCURACY");
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                return context.getString(R.string.magnet_status, "UNRELIABLE.\n[RECALIBRATE BY DOING FIGURE-8 MOTION]");
            default:
                return context.getString(R.string.magnet_status, "NOT FOUND"); //doubt it will be here as it already checks for magnet starting out.
        }
    }
}//Settings
