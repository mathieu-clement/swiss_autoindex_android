package com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks;

import org.droidpersistence.annotation.Column;
import org.droidpersistence.annotation.ForeignKey;
import org.droidpersistence.annotation.PrimaryKey;
import org.droidpersistence.annotation.Table;

@SuppressWarnings("ALL")
@Table(name = "BOOKMARKRECORDS")
public class BookmarkRecord {

    @PrimaryKey(autoIncrement = false)
    @Column(name = "platerecord_id")
    @ForeignKey(tableReference = "PLATERECORDS", columnReference = "ID")
    private long plateRecordId;

    @Column(name = "remarks")
    private String remarks;

    public BookmarkRecord(long plateRecordId, String remarks) {
        this.plateRecordId = plateRecordId;
        this.remarks = remarks;
    }

    public BookmarkRecord() {
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
