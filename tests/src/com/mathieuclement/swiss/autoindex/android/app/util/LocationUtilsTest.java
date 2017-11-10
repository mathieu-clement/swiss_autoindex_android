package com.mathieuclement.swiss.autoindex.android.app.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.SearchActivity;

import static com.mathieuclement.swiss.autoindex.android.app.util.LocationUtils.zipToCantonAbbr;

public class LocationUtilsTest extends ActivityInstrumentationTestCase2<SearchActivity> {


    @TargetApi(Build.VERSION_CODES.FROYO)
    public LocationUtilsTest(Class<SearchActivity> activityClass) {
        super(activityClass);
    }

    public LocationUtilsTest() {
        this(SearchActivity.class);
    }

    @SmallTest
    public void testZipToCantonAbbr() throws Exception {
        testZip("VD", 1000); // first line
        testZip("VD", 1052);
        testZip("VD", 1197);
        testZip("GE", 1200);
        testZip("FR", 1731);
        testZip("SG", 9658); // last line
        testZip(null, 9876); // not found

        testZip(null, -10);
        testZip(null, Integer.MAX_VALUE);
        testZip(null, Integer.MIN_VALUE);
        testZip(null, 858);
        testZip(null, 10000);

    }

    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private void testZip(String expectedCanton, int zip) throws Exception {
        assertEquals(expectedCanton, zipToCantonAbbr(this.getActivity(), zip));
    }
}