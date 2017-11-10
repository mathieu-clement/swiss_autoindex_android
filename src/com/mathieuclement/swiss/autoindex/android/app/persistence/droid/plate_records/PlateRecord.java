package com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records;

import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.swiss.autoindex.android.app.util.AutoIndexProviderAdapter;
import org.droidpersistence.annotation.Column;
import org.droidpersistence.annotation.PrimaryKey;
import org.droidpersistence.annotation.Table;

@SuppressWarnings("ALL")
@Table(name = "PLATERECORDS")
public class PlateRecord {

    @PrimaryKey(autoIncrement = true)
    @Column(name = "ID")
    private long id;

    @Column(name = "CANTON")
    private String canton = "";

    @Column(name = "NUMBER")
    private int number;

    @Column(name = "TYPE")
    private String type = "";

    @Column(name = "NAME")
    private String name = "";

    @Column(name = "ADDRESS")
    private String address = "";

    @Column(name = "ADDRESSCOMPLEMENT")
    private String addressComplement = "";

    @Column(name = "ZIP")
    private int zip;

    @Column(name = "TOWN")
    private String town = "";

    public PlateRecord(Plate plate, PlateOwner plateOwner) {
        this.canton = plate.getCanton().getAbbreviation();
        this.number = plate.getNumber();
        this.type = plate.getType().getName();

        this.name = plateOwner.getName();
        this.address = plateOwner.getAddress();
        this.addressComplement = plateOwner.getAddressComplement();
        this.zip = plateOwner.getZip();
        this.town = plateOwner.getTown();
    }

    public Plate toPlate() {
        return new Plate(number, new PlateType(type), new Canton(canton, false, new AutoIndexProviderAdapter()));
    }

    public PlateOwner toPlateOwner() {
        return new PlateOwner(name, address, addressComplement, zip, town);
    }

    public PlateRecord() {
    }

    public boolean autoIncrement() {
        return true;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCanton() {
        return canton == null ? "" : canton;
    }

    public void setCanton(String canton) {
        this.canton = canton;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getType() {
        return type == null ? "" : type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address == null ? "" : address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressComplement() {
        return addressComplement == null ? "" : addressComplement;
    }

    public void setAddressComplement(String addressComplement) {
        this.addressComplement = addressComplement;
    }

    public int getZip() {
        return zip;
    }

    public void setZip(int zip) {
        this.zip = zip;
    }

    public String getTown() {
        return town == null ? "" : town;
    }

    public void setTown(String town) {
        this.town = town;
    }
}
