package com.mathieuclement.swiss.autoindex.android.app.persistence.droid;

import android.app.backup.BackupManager;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import com.bugsense.trace.BugSenseHandler;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks.BookmarkRecord;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks.BookmarkRecordDao;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks.BookmarkRecordTableDefinition;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.contact.ContactRecord;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.contact.ContactRecordDao;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.contact.ContactRecordTableDefinition;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecord;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecordDao;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecordTableDefinition;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Data manager for Plate Records database
 * <p/>
 * TODO: Use an appropriate design pattern because this sucks!
 */
public class DataManager {

    public static final String DB_NAME = "PLATERECORDDATABASE";
    private Context context;
    private SQLiteDatabase database;

    private PlateRecordDao plateRecordDao;
    private BookmarkRecordDao bookmarkRecordDao;
    private ContactRecordDao contactRecordDao;


    private static final String DATABASE_PATH = Environment.getDataDirectory() + "/data/com.mathieuclement.swiss.autoindex/databases/plate_record_database.db";

    public DataManager(Context context) {
        setContext(context);
        SQLiteOpenHelper openHelper = new OpenHelper(context, DB_NAME, null, 6);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        setDatabase(db);

        this.plateRecordDao = new PlateRecordDao(new PlateRecordTableDefinition(), database);
        this.bookmarkRecordDao = new BookmarkRecordDao(new BookmarkRecordTableDefinition(), database);
        this.contactRecordDao = new ContactRecordDao(new ContactRecordTableDefinition(), database);
    }

    void openDb() {
        try {
            if (getDatabase() != null && !getDatabase().isOpen()) {
                setDatabase(SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READWRITE));
            }
        } catch (Exception e) {
            // No fail
            BugSenseHandler.sendException(e);
        }
    }

    /**
     * Close connection to database.
     * Is usually to be put in the "finally" clause of a try-catch-finally block.
     */
    public void closeDb() {
        if (getDatabase() != null && getDatabase().isOpen()) {
            getDatabase().close();
        }
    }

    /**
     * *** Plate Records *****
     */

    public PlateRecord getPlateRecord(Long id) {
        return getPlateRecordDao().get(id);
    }

    public PlateRecord getPlateRecordByPlate(Plate plate) {
        return getPlateRecordDao().getByClause(" CANTON ='" + plate.getCanton().getAbbreviation() +
                "' AND NUMBER=" + plate.getNumber() +
                " AND TYPE='" + plate.getType().getName() + "'", null);
    }

    public List<PlateRecord> getPlateRecords() {
        return getPlateRecordDao().getAll();
    }

    public boolean deletePlateRecord(Long id) {
        boolean result;
        getDatabase().beginTransaction();
        result = getPlateRecordDao().delete(id);
        getDatabase().setTransactionSuccessful();
        getDatabase().endTransaction();
        updateBackup();
        return result;
    }

    public long savePlateRecord(PlateRecord plateRecord) throws Exception {
        long result = 0;
        getDatabase().beginTransaction();
        result = getPlateRecordDao().save(plateRecord);
        getDatabase().setTransactionSuccessful();
        getDatabase().endTransaction();
        updateBackup();
        return result;
    }

    public boolean updatePlateRecord(PlateRecord plateRecord) throws Exception {
        boolean result;
        getDatabase().beginTransaction();
        getPlateRecordDao().update(plateRecord, plateRecord.getId());
        getDatabase().setTransactionSuccessful();
        result = true;
        getDatabase().endTransaction();
        updateBackup();
        return result;
    }

    public PlateRecordDao getPlateRecordDao() {
        return plateRecordDao;
    }

    public void setPlateRecordDao(PlateRecordDao plateRecordDao) {
        this.plateRecordDao = plateRecordDao;
    }

    /**
     * Returns the id of the corresponding plate record.
     *
     * @param plate Plate
     * @return the id of the corresponding plate record.
     */
    public Long getPlateRecordId(Plate plate) {
        List<PlateRecord> plateRecords = getPlateRecords();
        for (PlateRecord plateRecord : plateRecords) {
            if (plateRecord.getCanton().equals(plate.getCanton().getAbbreviation()) &&
                    plateRecord.getNumber() == plate.getNumber() &&
                    plateRecord.getType().equals(plate.getType().getName())) {
                return plateRecord.getId();
            }
        }
        return null;
    }

    /**
     * Returns true if plate is in cache.
     *
     * @param plate Plate
     * @return true if plate is in cache.
     */
//    public boolean isCached(Plate plate) {
//        PlateRecord plateRecord = getPlateRecordByPlate(plate);
//        if (plateRecord != null && getCacheRecordDao().get(plateRecord.getId()) != null) {
//            return true;
//        }
//
//        return false;
//    }

    /**
     * Returns the bookmarked plates as plate records.
     *
     * @return the bookmarked plates as plate records.
     */
    public List<PlateRecord> getBookmarks() {
        List<PlateRecord> plateRecords = getPlateRecordDao().getAll();
        List<PlateRecord> plateRecordsCopy = new LinkedList<PlateRecord>(plateRecords);
        List<BookmarkRecord> bookmarkRecords = getBookmarkRecordDao().getAll();

        Set<Long> bookmarkedIds = new HashSet<Long>();
        for (BookmarkRecord bookmarkRecord : bookmarkRecords) {
            bookmarkedIds.add(bookmarkRecord.getPlateRecordId());
        }

        for (PlateRecord plateRecord : plateRecordsCopy) {
            if (!bookmarkedIds.contains(plateRecord.getId())) {
                plateRecords.remove(plateRecord);
            }
        }

        return plateRecords;
    }

    /**
     * Returns true if the plate is bookmarked.
     *
     * @return true if the plate is bookmarked.
     */
    public boolean isBookmarked(Plate plate) {
        List<PlateRecord> plateRecords = getPlateRecords();
        for (PlateRecord plateRecord : plateRecords) {
            if (plateRecord.getCanton().equals(plate.getCanton().getAbbreviation()) &&
                    plateRecord.getNumber() == plate.getNumber() &&
                    plateRecord.getType().equals(plate.getType().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove bookmark.
     *
     * @param id Plate record ID
     */
    public void removeBookmark(Long id) {
        removeBookmarkRecord(id);
//        if (!isCached(plate)) {
        if (!isContact(id)) {
            deletePlateRecord(id);
        }
//        }
    }

    private boolean isContact(Long id) {
        return getContactRecordDao().get(id) != null;
    }

    /**
     * Add a new contact to database
     *
     * @param plate      Plate
     * @param plateOwner Plate owner
     * @param lookupKey  Lookup key (Contacts API 2.0)
     * @return ID of plate record in table.
     * @throws Exception
     */
    public Long addContact(Plate plate, PlateOwner plateOwner, String lookupKey) throws Exception {
        PlateRecord record = getPlateRecordByPlate(plate);
        long plateRecordId;
        if (record == null) {
            plateRecordId = savePlateRecord(new PlateRecord(plate, plateOwner));
        } else {
            plateRecordId = record.getId();
        }
        getContactRecordDao().save(new ContactRecord(plateRecordId, lookupKey));
        return plateRecordId;
    }

    public void removeContact(Plate plate) {
        removeContact(plate, getPlateRecordId(plate));
    }

    public void removeContact(Plate plate, Long id) {
        getContactRecordDao().delete(id);
        if (!isBookmarked(plate)) {
            deletePlateRecord(id);
        }
    }

    public String getContactLookupKey(Plate plate) {
        Long plateRecordId = getPlateRecordId(plate);
        ContactRecord record = getContactRecordDao().getByClause(" platerecord_id = ?",
                new String[]{Long.toString(plateRecordId)});
        if (record == null) return null;
        return record.getLookupKey();
    }

    public String getRemarks(Plate plate) {
        Long plateRecordId = getPlateRecordId(plate);
        BookmarkRecord record = getBookmarkRecordDao().getByClause(" platerecord_id = ?",
                new String[]{Long.toString(plateRecordId)});
        if (record == null) return null;
        return record.getRemarks();
    }

    /**
     * Removes a bookmark record (NO CHECK!)
     *
     * @param id ID of plate record
     * @return true on success
     */
    protected boolean removeBookmarkRecord(Long id) {
        boolean result;
        getDatabase().beginTransaction();
        result = getBookmarkRecordDao().delete(id);
        getDatabase().setTransactionSuccessful();
        getDatabase().endTransaction();
        updateBackup();
        return result;
    }

    public long addBookmark(Plate plate, PlateOwner owner) throws Exception {
        Long id = getPlateRecordId(plate);
        if (id == null) {
            // Create whole entry
            id = savePlateRecord(new PlateRecord(plate, owner));
        }

        // In all cases, put ID in bookmarks table
        try {
            addBookmarkRecord(new BookmarkRecord(id, ""));
        } catch (Throwable t) {
            // Remove record from plate records if anything goes wrong.
            removeBookmark(id);
        }

        return id;
    }

//    public long addToCache(Plate plate, PlateOwner owner) throws Exception {
//        Long id = getPlateRecordId(plate);
//        if (id == null) {
//            // Create whole entry
//            id = savePlateRecord(new PlateRecord(plate, owner));
//        }
//
//        // In all cases, put ID in cache table
//        addCacheRecord(new CacheRecord(id));
//
//        return id;
//    }

    /**
     * Un-cache plate
     *
     * @param plate Plate
     * @param cacheRecordId    Plate record ID
     */
//    protected void removeFromCache(Plate plate, Long cacheRecordId, Long plateRecordId) {
//        removeCacheRecord(cacheRecordId);
//        if (!isBookmarked(plate)) {
//            deletePlateRecord(cacheRecordId);
//        }
//    }

//    protected Long getCacheRecordId(Plate plate) {
//        Long plateRecordId = getPlateRecordId(plate);
//        CacheRecord cacheRecord = getCacheRecordDao().getByClause(
//                " platerecord_id = ?", new String[]{Long.toString(plateRecordId)});
//        if(cacheRecord == null) {
//            return null;
//        }
//        return cacheRecord.getCacheId();
//    }

//    public void removeFromCache(Plate plate) {
//        removeFromCache(plate, getCacheRecordId(plate), getPlateRecordId(plate));
//    }

//    public void removeAllFromCache() {
//        List<PlateRecord> cachedPlates = getCachedPlates();
//        for (PlateRecord plateRecord : cachedPlates) {
//            Plate plate = plateRecord.toPlate();
//            removeFromCache(plate, getCacheRecordId(plate), plateRecord.getId());
//        }
//    }

    /**
     * Returns cached plates as plate records.
     *
     * @return cached plates as plate records.
     */
//    public List<PlateRecord> getCachedPlates() {
//        List<PlateRecord> plateRecords = getPlateRecordDao().getAll();
//        List<PlateRecord> plateRecordsCopy = new LinkedList<PlateRecord>(plateRecords);
//        List<CacheRecord> cacheRecords = getCacheRecordDao().getAll();
//
//        Set<Long> cachedIds = new HashSet<Long>();
//        for (CacheRecord cacheRecord : cacheRecords) {
//            cachedIds.add(cacheRecord.getPlateRecordId());
//        }
//
//        for (PlateRecord plateRecord : plateRecordsCopy) {
//            if (!cachedIds.contains(plateRecord.getId())) {
//                plateRecords.remove(plateRecord);
//            }
//        }
//
//        return plateRecords;
//    }

    /**
     * Removes a bookmark record (NO CHECK!)
     *
     * @param id ID of plate record
     * @return true on success
     */
//    protected boolean removeCacheRecord(Long id) {
//        boolean result;
//        getDatabase().beginTransaction();
//        result = getCacheRecordDao().delete(id);
//        getDatabase().setTransactionSuccessful();
//        getDatabase().endTransaction();
//        updateBackup();
//        return result;
//    }

//    protected long addCacheRecord(CacheRecord cacheRecord) throws Exception {
//        long result;
//        getDatabase().beginTransaction();
//        result = getCacheRecordDao().save(cacheRecord);
//        getDatabase().setTransactionSuccessful();
//        getDatabase().endTransaction();
//        updateBackup();
//        return result;
//    }

    /**
     * Add a bookmark record
     *
     * @param bookmarkRecord
     * @return id of new record
     * @throws Exception
     */
    protected long addBookmarkRecord(BookmarkRecord bookmarkRecord) throws Exception {
        long result;
        getDatabase().beginTransaction();
        result = getBookmarkRecordDao().save(bookmarkRecord);
        getDatabase().setTransactionSuccessful();
        getDatabase().endTransaction();
        updateBackup();
        return result;
    }

    public BookmarkRecordDao getBookmarkRecordDao() {
        return bookmarkRecordDao;
    }

    public void setBookmarkRecordDao(BookmarkRecordDao bookmarkRecordDao) {
        this.bookmarkRecordDao = bookmarkRecordDao;
    }

//    public CacheRecordDao getCacheRecordDao() {
//        return cacheRecordDao;
//    }

    public ContactRecordDao getContactRecordDao() {
        return contactRecordDao;
    }

    public Context getContext() {
        return context;
    }

    void setContext(Context context) {
        this.context = context;
    }

    SQLiteDatabase getDatabase() {
        return database;
    }

    void setDatabase(SQLiteDatabase database) {
        this.database = database;
    }


    private BackupManager backupManager;

    private void updateBackup() {
        if (backupManager == null) {
            backupManager = new BackupManager(context);
        }
        backupManager.dataChanged();
    }
}
