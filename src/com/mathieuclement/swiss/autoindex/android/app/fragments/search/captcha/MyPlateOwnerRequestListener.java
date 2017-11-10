package com.mathieuclement.swiss.autoindex.android.app.fragments.search.captcha;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.PlateRequestListener;
import com.mathieuclement.lib.autoindex.provider.exception.*;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.activity.MainDrawerActivity;
import com.mathieuclement.swiss.autoindex.android.app.fragments.ShowPlateOwnerFragment;
import com.mathieuclement.swiss.autoindex.android.app.speech.SpeechRecognitionWrapper;

public class MyPlateOwnerRequestListener implements PlateRequestListener {

    protected Activity activity;
    private ProgressDialog progressDialog; // In Viacar, progress dialog must show 1/3, 2/3 and 3/3 as in the tests

    public MyPlateOwnerRequestListener(Activity activity, ProgressDialog progressDialog) {
        this.activity = activity;
        this.progressDialog = progressDialog;
    }

    @Override
    public void onPlateOwnerFound(Plate plate, PlateOwner plateOwner) {
        Log.d(getClass().getName(), "Dismiss progress dialog.");
        progressDialog.dismiss();
        doOnPlateOwnerFoundNotGui(plate, plateOwner);
    }

    // Actions common to manual and automatic captcha decoding listeners
    protected void doOnPlateOwnerFoundNotGui(Plate plate, PlateOwner plateOwner) {
        Log.d("RESULT_FOUND", "Plate: " + plate + "; Owner: " + plateOwner);

        Bundle args = new Bundle();
        args.putString(ShowPlateOwnerFragment.PLATE_CANTON_ABBR_KEY, plate.getCanton().getAbbreviation());
        args.putString(ShowPlateOwnerFragment.PLATE_TYPE_KEY, plate.getType().getName());
        args.putInt(ShowPlateOwnerFragment.PLATE_NUMBER_KEY, plate.getNumber());

        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_NAME_KEY, plateOwner.getName());
        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_ADDRESS_KEY, plateOwner.getAddress());
        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_ADDRESS_COMPLEMENT_KEY, plateOwner.getAddressComplement());
        args.putInt(ShowPlateOwnerFragment.PLATE_OWNER_ZIP_KEY, plateOwner.getZip());
        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_TOWN_KEY, plateOwner.getTown());

        Fragment fragment = new ShowPlateOwnerFragment();
        fragment.setArguments(args);

        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, fragment).addToBackStack(null).commit();
    }

    @Override
    @Deprecated
    public void onPlateRequestException(Plate plate, PlateRequestException exception) {
        Log.w(getClass().getName(), "Got Plate request exception but not for the special speech-enabled version");
    }

    public void onPlateRequestException(Plate plate, PlateRequestException exception,
                                        SpeechRecognitionWrapper speech, boolean isSpeechEnabled) {
        /* MAKE SURE TO MODIFY IMPLEMENTATION CLASSES!!! */

        if (exception instanceof NumberOfRequestsExceededException) {
            if (isSpeechEnabled) speech.sayError();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.showDialog(MainDrawerActivity.DIALOG_TOO_MANY_REQUESTS);
                }
            });
        } else if (exception instanceof ProviderException) {
            if (isSpeechEnabled) speech.sayError();
            ProviderException providerException = (ProviderException) exception;
            Throwable cause = providerException.getCause();
            if (!(cause instanceof CaptchaException)) {
                showOtherException(plate, exception);
            }
        } else if (exception instanceof PlateOwnerNotFoundException) {
            if (isSpeechEnabled) speech.sayOwnerNotFound(plate);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.showDialog(MainDrawerActivity.DIALOG_OWNER_NOT_FOUND);
                }
            });
        } else if (exception instanceof PlateOwnerHiddenException) {
            if (isSpeechEnabled) speech.sayHiddenData(plate);
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.showDialog(MainDrawerActivity.DIALOG_OWNER_HIDDEN);
                }
            });
        } else {
            if (isSpeechEnabled) speech.sayError();
            showOtherException(plate, exception);
        }

    }

    protected void showOtherException(Plate plate, PlateRequestException exception) {
        Log.i(getClass().getSimpleName(), "Exception on plate request: " + plate, exception);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity.getApplicationContext(), R.string.request_error_try_again, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
