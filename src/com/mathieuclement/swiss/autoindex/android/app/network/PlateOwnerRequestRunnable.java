package com.mathieuclement.swiss.autoindex.android.app.network;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;
import com.mathieuclement.swiss.autoindex.android.app.R;
import org.apache.http.impl.client.DefaultHttpClient;

class PlateOwnerRequestRunnable implements Runnable {

    private Activity activity;
    private AsyncAutoIndexProvider provider;
    private Plate plate;

    PlateOwnerRequestRunnable(Activity activity, AsyncAutoIndexProvider provider, Plate plate) {
        this.activity = activity;
        this.provider = provider;
        this.plate = plate;
    }

    @Override
    public void run() {
        try {
            this.provider.requestPlateOwner(plate, new DefaultHttpClient());
        } catch (final Exception e) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.e(getClass().getSimpleName(), "Exception on plate request: " + plate, e);
                    Toast.makeText(activity.getApplicationContext(), R.string.request_error_try_again, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
