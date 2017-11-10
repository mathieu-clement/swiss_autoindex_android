package com.mathieuclement.swiss.autoindex.android.app.fragments.sms_provider;

import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.fragments.ShowPlateOwnerFragment;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.DataManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Mathieu Clément
 * @since 15.09.2013
 */
public class SendReceiveFragment extends Fragment {
    private ProgressBar progressBar;
    private TextView statusTextView;

    private BroadcastReceiver smsSentReceiver;
    private boolean smsSentReceiverRegistered;

    private BroadcastReceiver smsReceivedReceiver;
    private boolean smsReceivedReceiverRegistered;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sms_send_receive, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Read extras
        int number = getArguments().getInt("number");
        String canton = getArguments().getString("canton").toUpperCase();

        statusTextView = (TextView) view.findViewById(R.id.sms_send_receive_status);
        progressBar = (ProgressBar) view.findViewById(R.id.sms_send_receive_progress);

        // TODO After answer is obtained, this must not fail, except if message is shown in the Messaging app.
        // Result will be bookmarked BY DEFAULT.

        // If response could not be parsed, show the raw response with some "Cannot parse response" message.
        // "Warning! This response will not be stored."


        // Init message
        String messageToSend = canton + " " + number;
        statusTextView.setText(
                String.format(getResources().getString(R.string.sms_provider_send_receive_sending),
                        messageToSend, 939));

        // Do the sending
        SmsManager smsManager = SmsManager.getDefault();
        smsSentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent receivedIntent) {
                // That's "On SMS ***sent***" not response received!

                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        statusTextView.setText(R.string.sms_provider_send_receive_sent);
                        progressBar.setProgress(50);
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        // not translated because these failures are rare
                        statusTextView.setText("Error: Generic Failure");
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        // Can happen when there is not network available.
                        // Not translated, it's understable in FR and DE.
                        statusTextView.setText("Error: No Service");
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        // This one probably can't even happen because we have always have a text.
                        statusTextView.setText("Error: Null PDU");
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        statusTextView.setText(R.string.sms_provider_send_receive_radio_off);
                        break;
                    default:
                        break;
                }

            }
        };
        getActivity().registerReceiver(smsSentReceiver, new IntentFilter("SMS_SENT"));
        smsSentReceiverRegistered = true;

        smsReceivedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receivedContext, Intent receivedIntent) {
                String responseText = receivedIntent.getExtras().getString("message");

                // Show ShowPlateOwnerFragment
                try {
                    Fragment fragment = new ShowPlateOwnerFragment();
                    Bundle parse = parse(responseText);
                    fragment.setArguments(parse);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                            .commit();
                } catch (ParseException pe) {
                    Log.e(getClass().getName(), pe.getMessage(), pe);
                    BugSenseHandler.sendException(pe);
                    statusTextView.setText(String.format(
                            getResources().getString(R.string.sms_provider_send_receive_unknown_response).toString(),
                            responseText.split("\\*SEP_PDU\\*")[0].split("\\*SEP\\*")[1]));
                }
            }
        };
        getActivity().registerReceiver(smsReceivedReceiver, new IntentFilter("com.mathieuclement.swiss.autoindex.android.app.SMS_RECEIVED"));
        smsReceivedReceiverRegistered = true;

        smsManager.sendTextMessage("939", null, messageToSend,
                PendingIntent.getBroadcast(getActivity(), 0, new Intent("SMS_SENT"), 0),
                PendingIntent.getBroadcast(getActivity(), 0, new Intent("SMS_DELIVERED"), 0) // actually ignored
        );
        progressBar.setProgress(25);

        // Do the receiving
        // Try to parse, else show the error message that we couldn't parse.
        progressBar.setProgress(progressBar.getMax());
    }

    private Bundle parse(String responseText) throws ParseException {
        Bundle extras = new Bundle();

        // SMS received:

        // "Abfrage lieferte kein gültiges Ergebnis. Diese Nachricht kostet 20 Rp. L'interrogation n'a pas généré de
        // résultat positif."

        // IMPORTANT: It looks accents make the emulator crash when trying to send a SMS.
        // Must be verified on real hardware.
        // "BE 80546, Iseli Peter, Walkestr. 28, 3110 Muensingen - Diese Nachricht kostet 1 SFr."

        // From SmsReceiver we will have something like this:
        // "939*SEP*BE 80546, Iseli Peter, Walkestr. 28, 3110 Münsingen - Diese Nachricht kostet 1 SFr.*SEP_PDU*"

        try {
            String intentMsg = responseText.split("\\*SEP_PDU\\*")[0];
            String[] spl = intentMsg.split("\\*SEP\\*");
            String numberStr = spl[0];
            String txtMsg = spl[1];

            // Try to do the real parsing of the thing
            txtMsg = txtMsg.split(" - Diese Nachricht")[0];

            // And here comes the regular expression at least
            Pattern pattern;

            int commas = numberOf(',', txtMsg);
            boolean hasComplement = commas > 3;

            if (commas <= 3) {
                pattern = Pattern.compile("(([A-Z]{2}) ([0-9]+)), (.*), (.*), ([0-9]{4}) (.*)");
            } else {
                pattern = Pattern.compile("(([A-Z]{2}) ([0-9]+)), (.*), (.*), (.*), ([0-9]{4}) (.*)");
            }
            Matcher matcher = pattern.matcher(txtMsg);
            if (!matcher.find()) {
                BugSenseHandler.addCrashExtraData("txtMsg", txtMsg);
                throw new ParseException("Got '" + txtMsg + "' for '" + numberStr + "' but could not parse that.");
            }

            Log.d(getClass().getName(), "Group count: " + matcher.groupCount());

            String canton = matcher.group(2);
            int number = Integer.parseInt(matcher.group(3));
            String name = matcher.group(4);
            String street = matcher.group(5);
            String complement = null;
            int zip;
            String town;
            if (hasComplement) {
                complement = matcher.group(6);
                zip = Integer.parseInt(matcher.group(7));
                town = matcher.group(8);
            } else {
                zip = Integer.parseInt(matcher.group(6));
                town = matcher.group(7);
            }

            extras.putString(ShowPlateOwnerFragment.PLATE_CANTON_ABBR_KEY, canton);
            extras.putString(ShowPlateOwnerFragment.PLATE_TYPE_KEY, "automobile");
            extras.putInt(ShowPlateOwnerFragment.PLATE_NUMBER_KEY, number);

            extras.putString(ShowPlateOwnerFragment.PLATE_OWNER_NAME_KEY, name);
            extras.putString(ShowPlateOwnerFragment.PLATE_OWNER_ADDRESS_KEY, street);
            if (hasComplement && complement != null) {
                extras.putString(ShowPlateOwnerFragment.PLATE_OWNER_ADDRESS_COMPLEMENT_KEY, complement);
            }
            extras.putInt(ShowPlateOwnerFragment.PLATE_OWNER_ZIP_KEY, zip);
            extras.putString(ShowPlateOwnerFragment.PLATE_OWNER_TOWN_KEY, town);

            try {
                DataManager dataManager = new DataManager(getActivity());
                try {
                    Plate plate = new Plate(number, PlateType.AUTOMOBILE, new Canton(canton, false, (AutoIndexProvider) null));
                    PlateOwner  plateOwner = new PlateOwner(name, street, complement != null ? complement : "", zip, town);
                    long plateRecordId = dataManager.addBookmark(plate, plateOwner);
                    extras.putLong(ShowPlateOwnerFragment.PLATE_RECORD_ID_KEY, plateRecordId);
                } catch (Exception e) {
                    BugSenseHandler.sendException(e);
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Error. Try again.", Toast.LENGTH_SHORT).show();
                }
                dataManager.closeDb();
            } catch (Exception e) {
                BugSenseHandler.sendException(e);
                e.printStackTrace();
            }

        } catch (Exception e) {
            throw new ParseException("Could not parse \"" + responseText + "\"", e);
        }

        return extras;
    }

    private static int numberOf(char c, String string) {
        int count = 0;
        char[] chars = string.toCharArray();
        for (char aChar : chars) {
            if (aChar == c) count++;
        }
        return count;
    }

    private class ParseException extends Exception {
        private ParseException(String detailMessage) {
            super(detailMessage);
        }

        private ParseException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    /*
    @Override
    protected void onResume() {
        super.onResume();

        registerReceiver(smsSentReceiver, new IntentFilter("SMS_SENT"));
    }
    */



    @Override
    public void onStop() {
        super.onStop();

        if (smsSentReceiverRegistered) {
            getActivity().unregisterReceiver(smsSentReceiver);
        }
        if (smsReceivedReceiverRegistered) {
            getActivity().unregisterReceiver(smsReceivedReceiver);
        }
    }
}
