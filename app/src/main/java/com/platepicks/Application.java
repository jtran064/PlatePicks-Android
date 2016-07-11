//
// Copyright 2016 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.7
//
package com.platepicks;

import android.graphics.Bitmap;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.amazonaws.mobile.AWSMobileClient;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Application class responsible for initializing singletons and other common components.
 */
public class Application extends MultiDexApplication {
    private final static String LOG_TAG = Application.class.getSimpleName();
    private static Application singleton;

    public static final String SAVED_LIKED_FOODS = "Saved foods";
    public ReentrantLock accessList = new ReentrantLock();

    Bitmap img = null;

    public static Application getInstance() {
        return singleton;
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Application.onCreate - Initializing application...");
        super.onCreate();
        singleton = this;
        initializeApplication();
        Log.d(LOG_TAG, "Application.onCreate - Application initialized OK");
    }

    private void initializeApplication() {
        AWSMobileClient.initializeMobileClientIfNecessary(getApplicationContext());

        // ...Put any application-specific initialization logic here...
    }

    void setImage(Bitmap img){
        this.img = img;
    }

    Bitmap getImage() {
        return img;
    }
}
