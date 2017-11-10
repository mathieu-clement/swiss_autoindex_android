package com.mathieuclement.swiss.autoindex.android.app.speech;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.bugsense.trace.BugSenseHandler;
import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.swiss.autoindex.android.app.R;

import java.util.ArrayList;

/**
 * @author Mathieu Clément
 * @since 29.12.2013
 */
public class SpeechRecognitionWrapper {
    private TextToSpeech tts;
    private int SPEECH_REQUEST_CODE = 1234;

    private Fragment fragment;

    private final OnNumberRecognizedListener onNumberRecognizedListener;

    /**
     * Wrapper for speech recognition.
     * IMPORTANT: You have to call onActivityResult in the method of the same name in the activity.
     * Similarly, you have to call onDestroy before super.onDestroy in your activity.
     *
     * @param fragment       Fragment from whom the speech recognition is fired
     * @param onInitListener What to do after init, probably enabling a button or disabling it
     */
    public SpeechRecognitionWrapper(Fragment fragment, TextToSpeech.OnInitListener onInitListener,
                                    OnNumberRecognizedListener onNumberRecognizedListener) {
        this.fragment = fragment;
        this.onNumberRecognizedListener = onNumberRecognizedListener;
        tts = new TextToSpeech(fragment.getActivity(), onInitListener);
    }

    public interface OnNumberRecognizedListener {
        void onNumberRecognized(int number);

        void onSpeechRecognitionFinished(); // called when out of speech recognition process
    }

    public void listen() {
        tts.playSilence(50, TextToSpeech.QUEUE_FLUSH, null);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, fragment.getString(R.string.speech_say_number));
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);
        fragment.startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                ArrayList<String> matches = data
                        .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (matches.size() == 0) {
                    tts.speak(fragment.getResources().getString(R.string.speech_silent), TextToSpeech.QUEUE_ADD, null);
                } else {
                    String bestMatch = null;
                    for (String match : matches) {
                        if (match.matches(".*[0-9]+.*")) {
                            bestMatch = match;
                            break;
                        }
                    }
                    if (bestMatch == null) {
                        sayUnknownQuery();
                        return;
                    } else {
                        // Concatenate all numbers found
                        String[] split = bestMatch.split(" ");
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < split.length; i++) {
                            if (split[i].matches("[0-9]+")) {
                                sb.append(Integer.parseInt(split[i]));
                            }
                        }
                        final String numberStr = sb.toString();
                        if (numberStr.length() > 6 || numberStr.length() == 0) {
                            sayUnknownQuery();
                            return;
                        }

                        try {
                            onNumberRecognizedListener.onNumberRecognized(Integer.parseInt(numberStr));
                        } catch (NumberFormatException nfe) {
                            String msg = "Could not parse " + numberStr + " as a number";
                            BugSenseHandler.sendEvent(msg);
                            Log.e(getClass().getName(), msg);
                            sayUnknownQuery();
                            return;
                        }
                        onNumberRecognizedListener.onSpeechRecognitionFinished();
                    }
                }
            }
        }
    }

    private String splitCanton(Canton canton) {
        return String.format("%c %c, ",
                canton.getAbbreviation().charAt(0),
                canton.getAbbreviation().charAt(1));
    }

    private void say(String str) {
        tts.speak(str, TextToSpeech.QUEUE_ADD, null);
    }

    public void sayError() {
        say(fragment.getResources().getString(R.string.speech_error));
    }

    public void sayUnknownQuery() {
        say(fragment.getResources().getString(R.string.speech_unknown_query));
    }

    public void sayPendingSearch(Plate plate) {
        say(fragment.getResources().getString(R.string.speech_pending_search,
                splitCanton(plate.getCanton()), plate.getNumber()));
    }

    public void sayOwnerNotFound(Plate plate) {
        say(fragment.getResources().getString(R.string.speech_not_found,
                splitCanton(plate.getCanton()), plate.getNumber()));
    }

    public void sayHiddenData(Plate plate) {
        say(fragment.getResources().getString(R.string.speech_hidden_data,
                splitCanton(plate.getCanton()), plate.getNumber()));
    }

    public void sayOwnerFound(Plate plate, PlateOwner plateOwner) {
        say(fragment.getResources().getString(R.string.speech_owner_found,
                splitCanton(plate.getCanton()), plate.getNumber(),
                plateOwner.getName(),
                smartAddress(plateOwner.getAddress()), smartAddress(plateOwner.getAddressComplement()),
                plateOwner.getZip(), smartTown(plateOwner.getTown(), plate.getCanton().getAbbreviation())));
    }

    public String smartTown(String rawTown, String cantonAbbr) {
        String smartTown = rawTown;
        if (smartTown.endsWith(" " + cantonAbbr.toUpperCase()))
            smartTown = smartTown.replace(" " + cantonAbbr.toUpperCase(), "");
        if (smartTown.matches(".*[0-9]$")) smartTown = smartTown.replaceAll("[0-9]", "");

        return smartTown;
    }

    public String smartAddress(String rawAddr) {
        String smartAddr = rawAddr;
        if (smartAddr.startsWith("Ch. ")) smartAddr = smartAddr.replace("Ch. ", "Chemin ");
        if (smartAddr.startsWith("Rte ")) smartAddr = smartAddr.replace("Rte ", "Route ");
        if (smartAddr.startsWith("Str. ")) smartAddr = smartAddr.replace("Str. ", "Strasse ");
        if (smartAddr.startsWith("Bvd ")) smartAddr = smartAddr.replace("Bvd ", "Boulevard ");
        if (smartAddr.startsWith("Imp. ")) smartAddr = smartAddr.replace("Imp. ", "Impasse ");
        if (smartAddr.startsWith("Av. ")) smartAddr = smartAddr.replace("Av. ", "Avenue ");
        if (smartAddr.startsWith("Pl. ")) smartAddr = smartAddr.replace("Pl. ", "Place ");
        return smartAddr;
    }

    public void onDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
    }
}