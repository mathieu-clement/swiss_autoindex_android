package com.mathieuclement.swiss.autoindex.android.app.backup;

import android.app.backup.BackupAgentHelper;
import android.app.backup.BackupHelper;
import android.app.backup.FileBackupHelper;
import android.app.backup.SharedPreferencesBackupHelper;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.DataManager;

public class MyBackupAgent extends BackupAgentHelper {
    @Override
    public void onCreate() {
        super.onCreate();

        // The filename is not the ".db" file but the file that can be seen in /data/data/com.mathieuclement.../databases/ directory
        // FileBackupHelper is not meant for "big" files but we don't care
        BackupHelper favoritesDatabaseBackupHelper = new FileBackupHelper(getApplicationContext(), "../databases/" + DataManager.DB_NAME);
        addHelper("FAVORITES_DATABASE", favoritesDatabaseBackupHelper);

        // Default preferences (from preferences.xml)
        BackupHelper defaultPrefBackupHelper = new SharedPreferencesBackupHelper(getApplicationContext(), getPackageName() + "_preferences");
        addHelper("DEFAULT_SHARED_PREFERENCES", defaultPrefBackupHelper);

        // TODO Other preferences
    }
}
