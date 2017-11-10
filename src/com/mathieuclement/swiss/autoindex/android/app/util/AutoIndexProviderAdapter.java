package com.mathieuclement.swiss.autoindex.android.app.util;

import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerHiddenException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateOwnerNotFoundException;
import com.mathieuclement.lib.autoindex.provider.exception.ProviderException;
import com.mathieuclement.lib.autoindex.provider.exception.UnsupportedPlateException;

/**
 * @author Mathieu Clément
 * @since 22.09.2013
 */
public class AutoIndexProviderAdapter implements AutoIndexProvider {
    @Override
    public PlateOwner getPlateOwner(Plate plate, int requestId) throws ProviderException, PlateOwnerNotFoundException,
            PlateOwnerHiddenException, UnsupportedPlateException {
        return null;
    }

    @Override
    public boolean isPlateTypeSupported(PlateType plateType) {
        return false;
    }

    @Override
    public void cancel(int requestId) {
    }

    @Override
    public boolean isCancelled(int requestId) {
        return false;
    }

}
