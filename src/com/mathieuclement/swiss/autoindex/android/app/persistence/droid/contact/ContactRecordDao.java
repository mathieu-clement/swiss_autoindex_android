package com.mathieuclement.swiss.autoindex.android.app.persistence.droid.contact;

import android.database.sqlite.SQLiteDatabase;
import org.droidpersistence.dao.DroidDao;
import org.droidpersistence.dao.TableDefinition;

public class ContactRecordDao extends DroidDao<ContactRecord, Long> {
    public ContactRecordDao(TableDefinition<ContactRecord> contactRecordTableDefinition, SQLiteDatabase database) {
        super(ContactRecord.class, contactRecordTableDefinition, database);
    }
}
