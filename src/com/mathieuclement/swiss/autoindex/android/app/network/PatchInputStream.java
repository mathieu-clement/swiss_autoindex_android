package com.mathieuclement.swiss.autoindex.android.app.network;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: tictac
 * Date: 26.08.12
 */
public class PatchInputStream extends FilterInputStream {

    public PatchInputStream(InputStream in) {
        super(in);
    }

    @Override
    public long skip(long count) throws IOException {
        long m = 0L;
        while (m < count) {
            long _m = in.skip(count - m);
            if (_m == 0L) {
                break;
            }
            m += _m;
        }
        return m;
    }
}
