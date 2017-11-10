package com.mathieuclement.swiss.autoindex.android.app.util;

import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.swiss.autoindex.android.app.R;

import java.util.HashMap;
import java.util.Map;

public class PlateTypeUtils {

    private static final Map<PlateType, Integer> resourceIdentifiersMapping = new HashMap<PlateType, Integer>();

    static {
        resourceIdentifiersMapping.put(PlateType.AUTOMOBILE, R.string.plate_type_automobile);
        resourceIdentifiersMapping.put(PlateType.AUTOMOBILE_TEMPORARY, R.string.plate_type_automobile_temporary);
        resourceIdentifiersMapping.put(PlateType.AUTOMOBILE_BROWN, R.string.plate_type_automobile_brown);
        resourceIdentifiersMapping.put(PlateType.AUTOMOBILE_REPAIR_SHOP, R.string.plate_type_automobile_repair_shop);

        resourceIdentifiersMapping.put(PlateType.MOTORCYCLE, R.string.plate_type_motorcycle);
        resourceIdentifiersMapping.put(PlateType.MOTORCYCLE_TEMPORARY, R.string.plate_type_motorcycle_temporary);
        resourceIdentifiersMapping.put(PlateType.MOTORCYCLE_YELLOW, R.string.plate_type_motorcycle_yellow);
        resourceIdentifiersMapping.put(PlateType.MOTORCYCLE_BROWN, R.string.plate_type_motorcycle_brown);
        resourceIdentifiersMapping.put(PlateType.MOTORCYCLE_REPAIR_SHOP, R.string.plate_type_motorcycle_repair_shop);

        resourceIdentifiersMapping.put(PlateType.AGRICULTURAL, R.string.plate_type_agricultural);

        resourceIdentifiersMapping.put(PlateType.INDUSTRIAL, R.string.plate_type_industrial);

        resourceIdentifiersMapping.put(PlateType.MOPED, R.string.plate_type_moped);
    }

    /**
     * Returns the resource id associated with a <code>PlateType</code> or -1 if PlateType could not be found.
     *
     * @param plateType a well-known plate type
     * @return the resource id associated with a <code>PlateType</code> or -1 if PlateType could not be found.
     */
    public static int getResourceId(PlateType plateType) {
        //return resourceIdentifiersMapping.get(plateType);
        int resId = -1;
        for (Map.Entry<PlateType, Integer> entry : resourceIdentifiersMapping.entrySet()) {
            if (entry.getKey().getName().equals(plateType.getName())) {
                resId = entry.getValue();
            }
        }
        return resId;
    }

    /**
     * Reverse of {@link #getResourceId(com.mathieuclement.lib.autoindex.plate.PlateType)}.<br/>
     * Returns the <code>PlateType</code> associated with a well-known resource id.
     *
     * @param resourceId a well-known resource id
     * @return the <code>PlateType</code> associated with a well-known resource id.
     */
    public static PlateType getPlateType(int resourceId) {
        PlateType plateType = null;

        for (Map.Entry<PlateType, Integer> entry : resourceIdentifiersMapping.entrySet()) {
            if (entry.getValue() == resourceId) {
                plateType = entry.getKey();
                break;
            }
        }

        return plateType;
    }
}
