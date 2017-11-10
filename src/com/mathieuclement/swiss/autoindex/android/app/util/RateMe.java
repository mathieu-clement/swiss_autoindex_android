package com.mathieuclement.swiss.autoindex.android.app.util;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.widget.Toast;
import com.mathieuclement.swiss.autoindex.android.app.R;

import java.util.Date;

/**
 * @author Mathieu Clément
 * @since 09.11.2013
 */
public class RateMe {
    private static final String PREF_ALREADY_RATED = "already_rated";       // boolean. true = never show dialog again
    private static final String PREF_LAST_CHECK_DATE = "last_check_date";   // long. seconds since epoch

    private final SharedPreferences preferences;
    private Context context;

    public RateMe(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences("advert_rating", Context.MODE_PRIVATE);
    }

    public void reset() {
        preferences.edit().clear().commit();
    }


    /**
     * Show dialog if necessary
     *
     * @return true if dialog has been shown this time
     */
    public boolean advertRatingIfNeeded() {
        /*
         Pseudo-code

         if already_rated exists and is true (default value = false):
            return false

         if date property absent:
            create date property with current time
            return false

         if 1 week passed:
             set date property to current date

             show dialog

             if "rate now" selected:
                open google play
             else if "already rated" selected:
                set "already_rated" = true
             else if "rate later" or back button pressed":
                pass

             close dialog

             return true
         */

        if (preferences.contains(PREF_ALREADY_RATED) && preferences.getBoolean(PREF_ALREADY_RATED, false)) {
            return false;
        }

        if (!preferences.contains(PREF_LAST_CHECK_DATE)) {
            preferences.edit().putLong(PREF_LAST_CHECK_DATE, currentDateAsLong()).commit();
            return false;
        }

        if (daysPassed(7, preferences.getLong(PREF_LAST_CHECK_DATE, currentDateAsLong()), currentDateAsLong())) {
            preferences.edit().putLong(PREF_LAST_CHECK_DATE, currentDateAsLong()).commit();

            makeDialog().show();
            return true;
        }

        return false;
    }

    private AlertDialog makeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        AlertDialog dialog = builder.create();
        dialog.setTitle(context.getString(R.string.rate_this_app_title));
        dialog.setMessage(context.getString(R.string.rate_this_app_text));
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE,
                context.getString(R.string.rate_this_app_rate_now),
                new RateNowListener());
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.rate_this_app_already_rated),
                new AlreadyRatedListener());
        CancelListener cancelListener = new CancelListener();
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL,
                context.getString(R.string.rate_this_app_rate_later),
                cancelListener);
        dialog.setOnCancelListener(cancelListener);
        return dialog;
    }

    protected long currentDateAsLong() {
        return new Date().getTime();
    }

    // Returns true if (newDate - oldDate) >= days
    private boolean daysPassed(int days, long oldDateInMillis, long newDateInMillis) {
        return (newDateInMillis - oldDateInMillis) >= days * 86400000;
    }

    private class RateNowListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            rateThisApp(context);
            dialog.dismiss();
        }
    }

    private class AlreadyRatedListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            preferences.edit().putBoolean(PREF_ALREADY_RATED, true).commit();
            dialog.dismiss();
        }
    }


    private class CancelListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener  {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            preferences.edit().putLong(PREF_LAST_CHECK_DATE, currentDateAsLong()).commit();
        }
    }

    public static void rateThisApp(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=com.mathieuclement.swiss.autoindex.android.app"));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
            // This is not supposed to happen if the application was acquired legally on Google Play, but we never know...
            // Also this exception will be thrown when running the application in the emulator,
            Toast.makeText(context, "Cannot find Google Play Store application on this device.", Toast.LENGTH_LONG).show();
        }
    }
}

