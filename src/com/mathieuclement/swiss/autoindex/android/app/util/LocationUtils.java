package com.mathieuclement.swiss.autoindex.android.app.util;

import android.content.Context;
import com.mathieuclement.swiss.autoindex.android.app.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class LocationUtils {
    /**
     * Returns the canton abbreviation in uppercase.
     *
     * @param context Context
     * @param zip     Postal code
     * @return the canton abbreviation in uppercase, or null if not found.
     * @throws IOException if file could not be opened or close, or other I/O errors.
     */
    public static String zipToCantonAbbr(Context context, int zip) throws IOException {
        String cantonAbbr = null;

        if (zip < 1000 || zip > 9999) return null;

        // Read from file.
        // The file is PLZ Light from Swiss Post MAT[CH]
        // Generate file:
        // awk -F\\t '{print $3 "," $7}' plz_l_20140120.txt | sort -u -n | gzip > zip
        // Read file:
        // gunzip -c zip

        String zipStr = Integer.toString(zip);
        char char0 = zipStr.charAt(0);
        char char1 = zipStr.charAt(1);
        char char2 = zipStr.charAt(2);
        char char3 = zipStr.charAt(3);

        // TODO Binary search with RandomAccessFile

        InputStream inputStream = context.getResources().openRawResource(R.raw.zip);
        GZIPInputStream gis = new GZIPInputStream(inputStream);
        byte[] buf = new byte[8]; // 8 chars per line including \n
        while (gis.read(buf, 0, 4) != -1) {
            if (buf[0] < char0) {
                gis.skip(7);
            } else if (buf[1] != char1) {
                gis.skip(6);
            } else if (buf[2] != char2) {
                gis.skip(5);
            } else if (buf[3] != char3) {
                gis.skip(5);
            } else {
                // Found it
                // skip comma
                gis.skip(1);
                // next two chars is the canton abbreviation in uppercase
                gis.read(buf, 0, 2);
                cantonAbbr = new String(buf, 0, 2);
                break;
            }
        }
        gis.close();

        return cantonAbbr;
    }
}
