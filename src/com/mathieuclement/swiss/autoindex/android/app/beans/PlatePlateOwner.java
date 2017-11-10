package com.mathieuclement.swiss.autoindex.android.app.beans;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;

public class PlatePlateOwner {
    private Plate plate;
    private PlateOwner plateOwner;

    public PlatePlateOwner(Plate plate, PlateOwner plateOwner) {
        this.plate = plate;
        this.plateOwner = plateOwner;
    }

    public Plate getPlate() {
        return plate;
    }

    public void setPlate(Plate plate) {
        this.plate = plate;
    }

    public PlateOwner getPlateOwner() {
        return plateOwner;
    }

    public void setPlateOwner(PlateOwner plateOwner) {
        this.plateOwner = plateOwner;
    }
}
