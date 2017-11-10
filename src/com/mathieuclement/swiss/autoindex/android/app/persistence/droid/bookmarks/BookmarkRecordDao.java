package com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks;

import android.database.sqlite.SQLiteDatabase;
import org.droidpersistence.dao.DroidDao;
import org.droidpersistence.dao.TableDefinition;

public class BookmarkRecordDao extends DroidDao<BookmarkRecord, Long> {
    public BookmarkRecordDao(TableDefinition<BookmarkRecord> bookmarkRecordTableDefinition, SQLiteDatabase database) {
        super(BookmarkRecord.class, bookmarkRecordTableDefinition, database);
    }
}
