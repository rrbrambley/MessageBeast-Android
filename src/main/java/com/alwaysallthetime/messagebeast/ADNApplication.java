package com.alwaysallthetime.messagebeast;

import android.app.Application;
import android.content.Context;

/**
 * By using ADNApplication, you will enable MessageBeast to access an Application Context
 * easily and globally. To do so, just modify your AndroidManifest.xml's application tag
 * to include "com.alwaysallthetime.messagebeast.ADNApplication" as the value for the name property.
 *
 * If you'd rather not use ADNApplication as your Application type, call setApplicationContext(getApplicationContext())
 * when your app launches to ensure that there is a global Context available.
 */
public class ADNApplication extends Application {
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    /**
     * For apps that would rather not use ADNApplication as their Application type,
     * call this method when your app starts to allow MessageBeast features to access
     * a global Application Context easily.
     *
     * @param context the Application Context
     */
    public static void setApplicationContext(Context context) {
        sContext = context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }
}
