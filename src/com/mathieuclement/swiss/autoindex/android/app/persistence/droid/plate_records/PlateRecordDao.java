package com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records;

import android.database.sqlite.SQLiteDatabase;
import org.droidpersistence.dao.DroidDao;
import org.droidpersistence.dao.TableDefinition;

public class PlateRecordDao extends DroidDao<PlateRecord, Long> {
    public PlateRecordDao(TableDefinition<PlateRecord> plateRecordTableDefinition, SQLiteDatabase database) {
        super(PlateRecord.class, plateRecordTableDefinition, database);
    }
}
