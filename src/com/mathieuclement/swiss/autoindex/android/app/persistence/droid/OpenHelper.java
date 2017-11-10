package com.mathieuclement.swiss.autoindex.android.app.persistence.droid;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks.BookmarkRecordTableDefinition;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.contact.ContactRecordTableDefinition;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecordTableDefinition;

import java.util.LinkedHashSet;
import java.util.Set;

@SuppressWarnings("AccessStaticViaInstance")
class OpenHelper extends SQLiteOpenHelper {


    public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,
                      int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Enable foreign keys
        /*
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
        */
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(getClass().getName(), "ON CREATE");

        try {
            // Not doing this by calling the constructor would result in a bug because there is some initialization
            // done in the constructor.
            //noinspection AccessStaticViaInstance
            new PlateRecordTableDefinition().onCreate(db);
            Log.d(getClass().getName(), "created platerecords");
            new BookmarkRecordTableDefinition().onCreate(db);
            Log.d(getClass().getName(), "created bookmarkrecords");
            new ContactRecordTableDefinition().onCreate(db);
            Log.d(getClass().getName(), "created contactrecords");
        } catch (Exception e) {
            Log.e(getClass().getName(), e.getMessage(), e);
            BugSenseHandler.sendException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // A new column "ADDRESSCOMPLEMENT" was added in version program version 11, which now has database version 3
        if (oldVersion < 3) {
            Log.i(getClass().getSimpleName(), "Old database version detected. Adding new column \"ADDRESSCOMPLEMENT\".");
            // Add new address complement column
            db.execSQL("ALTER TABLE " + "PLATERECORDS " +
                    "ADD COLUMN " + "ADDRESSCOMPLEMENT" + " TEXT;");
            // PLATERECORDS is the table name, which should match CacheRecord @Table annotation parameter "name".
        }

        if (oldVersion < 4) { // Program version name: 1.6.0

            Log.i(getClass().getName(), "Perform database upgrade for 1.6.0 and later");

            try {
                // Create new tables of contacts and bookmarks
                new BookmarkRecordTableDefinition().onCreate(db);
                new ContactRecordTableDefinition().onCreate(db);

                // Copy ids to bookmarks table
                Cursor cursor = db.rawQuery("SELECT ID FROM PLATERECORDS", null);
                while (cursor.moveToNext()) {
                    long plateRecordId = cursor.getLong(cursor.getColumnIndex("ID"));
                    db.execSQL("INSERT INTO BOOKMARKRECORDS (platerecord_id) VALUES (" + plateRecordId + ")");
                }
                cursor.close();
            } catch (Exception e) {
                Log.e(getClass().getName(), e.getMessage(), e);
                BugSenseHandler.sendException(e);
            }
        }

        if (oldVersion < 5) { // Program version name: 1.6.1.3
            // Boats cannot be searched anymore in FR and VS
            // Remove all records related to boats from DB

            Log.i(getClass().getName(), "Perform database upgrade for 1.6.1.3 and later");

            try {
                // Find platerecords for boats
                Cursor cursor = db.rawQuery("SELECT id FROM platerecords WHERE type = 'boat'", new String[]{});
                // Ids of records matching
                Set<Long> ids = new LinkedHashSet<Long>();
                while (cursor.moveToNext()) {
                    ids.add(cursor.getLong(0));
                }
                Log.d(getClass().getName(), "Found " + ids.size() + " boats.");
                cursor.close();

                // Remove bookmarks, contact records and plate records matching these IDs
                for (Long id : ids) {
                    db.execSQL("DELETE FROM bookmarkrecords WHERE platerecord_id = " + id);
                    db.execSQL("DELETE FROM contactrecords  WHERE platerecord_id = " + id);
                    db.execSQL("DELETE FROM platerecords    WHERE id = " + id);
                }
            } catch (Exception e) {
                Log.e(getClass().getName(), e.getMessage(), e);
                BugSenseHandler.sendException(e);
            }
        }

        if (oldVersion < 6) { // Program version name 1.9
            // Add a "Remarks" field for bookmarks
            Log.i(getClass().getName(), "Perform database upgrade for 1.9 and later");

            try {
                db.execSQL("ALTER TABLE bookmarkrecords " +
                        "ADD COLUMN remarks TEXT");
            } catch (SQLiteException e) {
                if (e.getMessage().contains("duplicate")) {
                    // ignore if exception thrown because the column already exists
                } else {
                    throw e;
                }
            }
        }

        /*
        try {
            PlateRecordTableDefinition plateRecordTableDefinition = new PlateRecordTableDefinition();
            plateRecordTableDefinition.onUpgrade(db, oldVersion, newVersion);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Exception while upgrading database", e);
            e.printStackTrace();
        }
        */

    }

}
