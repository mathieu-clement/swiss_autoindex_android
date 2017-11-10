package com.mathieuclement.swiss.autoindex.android.app.persistence.droid.contact;

import org.droidpersistence.annotation.Column;
import org.droidpersistence.annotation.ForeignKey;
import org.droidpersistence.annotation.PrimaryKey;
import org.droidpersistence.annotation.Table;

@SuppressWarnings("ALL")
@Table(name = "CONTACTRECORDS")
public class ContactRecord {

    @PrimaryKey(autoIncrement = false)
    @Column(name = "platerecord_id")
    @ForeignKey(tableReference = "PLATERECORDS", columnReference = "ID")
    private long plateRecordId;

    @Column(name = "lookup_key")
    private String lookupKey;

    public ContactRecord(long plateRecordId, String lookupKey) {
        this.plateRecordId = plateRecordId;
        this.lookupKey = lookupKey;
    }

    public ContactRecord() {
    }

    public boolean autoIncrement() {
        return false;
    }

    public long getPlateRecordId() {
        return plateRecordId;
    }

    public void setPlateRecordId(long plateRecordId) {
        this.plateRecordId = plateRecordId;
    }

    public String getLookupKey() {
        return lookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.lookupKey = lookupKey;
    }
}
