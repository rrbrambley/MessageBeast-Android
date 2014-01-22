package com.alwaysallthetime.messagebeast;

import android.app.Application;
import android.content.Context;

public class ADNApplication extends Application {
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = getApplicationContext();
    }
}
