package com.mathieuclement.swiss.autoindex.android.app;

import android.app.Application;
import android.content.Context;
import com.bugsense.trace.BugSenseHandler;

public class SwissAutoIndexApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        SwissAutoIndexApplication.context = getApplicationContext();
        BugSenseHandler.initAndStartSession(getApplicationContext(), "f25bcbb6");
    }

    public static Context getAppContext() {
        return SwissAutoIndexApplication.context;
    }
}
