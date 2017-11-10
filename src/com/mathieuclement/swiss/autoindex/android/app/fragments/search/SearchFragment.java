package com.mathieuclement.swiss.autoindex.android.app.fragments.search;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.cari.sync.FribourgAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.cari.sync.ValaisAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.ProgressListener;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.exception.PlateRequestException;
import com.mathieuclement.lib.autoindex.provider.exception.RequestCancelledException;
import com.mathieuclement.lib.autoindex.provider.proxy.CaptchaAutoIndexProxy;
import com.mathieuclement.lib.autoindex.provider.viacar.sync.ViacarAutoIndexProvider;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.beans.PlatePlateOwner;
import com.mathieuclement.swiss.autoindex.android.app.fragments.BookmarksViewFragment;
import com.mathieuclement.swiss.autoindex.android.app.fragments.SettingsFragment;
import com.mathieuclement.swiss.autoindex.android.app.fragments.WebTicinoFragment;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.captcha.MyPlateOwnerRequestListener;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.captcha.decoding.WebServiceBasedCaptchaHandler;
import com.mathieuclement.swiss.autoindex.android.app.fragments.sms_provider.RequestIsManualFragment;
import com.mathieuclement.swiss.autoindex.android.app.fragments.sms_provider.WarningFragment;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.DataManager;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecord;
import com.mathieuclement.swiss.autoindex.android.app.speech.SpeechRecognitionWrapper;
import com.mathieuclement.swiss.autoindex.android.app.util.PlateTypeUtils;

import java.lang.ref.WeakReference;
import java.util.*;

public class SearchFragment extends android.app.Fragment implements TextToSpeech.OnInitListener, SpeechRecognitionWrapper.OnNumberRecognizedListener {
    private List<String> cantonSpinnerItems;
    private ProgressDialog progressDialog;
    private ProgressListener progressListener;
    private AutoCompleteTextView plateNumberEditText;
    private ArrayAdapter<Plate> plateSearchAdapter;
    private Button recentSearchesButton;
    // To avoid showing the captcha dialog of a cancelled search
    public Plate currentPlate;
    private Spinner plateTypeSpinner;
    private int currentRequestId;
    private CaptchaAutoIndexProvider autoIndexProvider;

    private SpeechRecognitionWrapper speech;
    private Button vocalSearchButton;
    private boolean sayMessagesOutLoud = false;
    private List<Canton> cantons;
    private Spinner cantonSpinner;
    private Map<String, Integer> spinnerItemsLocalizations;

    private SharedPreferences sharedPreferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View inflatedView = inflater.inflate(R.layout.search_fragment, container, false);
        return inflatedView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        plateNumberEditText.setText("");
        plateNumberEditText.dismissDropDown(); // Avoid drop down to be opened when coming back to fragment
    }

    private void init(final Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final View view = getView();
        vocalSearchButton = (Button) view.findViewById(R.id.vocal_search);
        speech = new SpeechRecognitionWrapper(this, this, this);
        vocalSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sayMessagesOutLoud = true;
                speech.listen();
            }
        });

        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle(R.string.please_wait);
        progressDialog.setMessage(getResources().getString(R.string.search_in_progress_message));
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.cancel();
                cancelPlate();
                autoIndexProvider.cancel(currentRequestId);
            }
        });
        progressDialog.setCanceledOnTouchOutside(false);

        cantons = new LinkedList<Canton>();

        // TODO Connection should remain open to allow for fast processing of many requests

        // TODO: We can now afford "Bookmark auto-update" (no user interaction needed, can be disabled in the settings)
        // for Fribourg and Valais)
        WebServiceBasedCaptchaHandler captchaHandler =
                new WebServiceBasedCaptchaHandler(new WeakReference<Activity>(activity));

        String email = getGoogleAccount(getActivity());

        for (String c : new String[]{"AG", "LU", "SH", "ZG", "ZH"}) {
            cantons.add(new Canton(c, true, new CaptchaAutoIndexProxy(new ViacarAutoIndexProvider(c, captchaHandler), email, captchaHandler)));
        }
        for (String c : new String[]{"AI", "AR", "BE", "BL", "GL", "GR", "SG", "SO", "TG", // SMS 939
                "BS", "GE", "NE", "NW", "OW", "SZ", "UR", "JU", "VD", "TI" // Manual
        }) {
            // false because regular online AutoIndex is not supported,
            // We will use SMS instead.
            cantons.add(new Canton(c, false, (AutoIndexProvider) null));
        }
        cantons.add(new Canton("FR", true, new CaptchaAutoIndexProxy(new FribourgAutoIndexProvider(captchaHandler), email, captchaHandler)));
        cantons.add(new Canton("VS", true, new CaptchaAutoIndexProxy(new ValaisAutoIndexProvider(captchaHandler), email, captchaHandler)));

        // Re-order cantons by abbreviation
        Collections.sort(cantons, new Comparator<Canton>() {
            @Override
            public int compare(Canton lhs, Canton rhs) {
                return lhs.getAbbreviation().compareTo(rhs.getAbbreviation());
            }
        });

        // Plate types
        final List<String> plateTypeSpinnerItems = new LinkedList<String>();
        // (There is no item in the list initially)

        plateTypeSpinner = (Spinner) view.findViewById(R.id.search_plate_type_spinner);
        final ArrayAdapter<String> plateTypeAdapter = new ArrayAdapter<String>(activity, android.R.layout.simple_spinner_item, plateTypeSpinnerItems);
        plateTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        plateTypeSpinner.setAdapter(plateTypeAdapter);

        // Populate the spinner with the canton abbreviations
        cantonSpinnerItems = new LinkedList<String>();
        for (Canton canton : cantons) {
            cantonSpinnerItems.add(canton.getAbbreviation());
        }

        cantonSpinner = (Spinner) view.findViewById(R.id.search_canton_spinner);
        ArrayAdapter<String> cantonAdapter = new CantonSpinnerArrayAdapter<String>(activity, R.layout.white_list_item, cantonSpinnerItems);
        cantonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cantonSpinner.setAdapter(cantonAdapter);

        // Remember last canton selected
        sharedPreferences = activity.getSharedPreferences(((Object) this).getClass().getName(), Context.MODE_PRIVATE);

        final String selectedCantonPrefKey = "pref_selected_canton";
        // Get previously selected canton (first element will be selected if preference cannot be found) and select it
        cantonSpinner.setSelection(sharedPreferences.getInt(selectedCantonPrefKey, 0));

        spinnerItemsLocalizations = new HashMap<String, Integer>();

        final ImageView cantonFlagImageView = (ImageView) view.findViewById(R.id.search_canton_imageview);
        Integer drawableId = flagsMapping.get(cantonSpinnerItems.get(cantonSpinner.getSelectedItemPosition()).toLowerCase());
        if (drawableId != null) {
            cantonFlagImageView.setImageResource(drawableId);
        }
        cantonFlagImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cantonSpinner.performClick();
            }
        });

        AdapterView.OnItemSelectedListener cantonSpinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                // Save selection in preferences (this way people always looking for plates in Valais don't need to select it every time
                // they use it)
                sharedPreferences.edit().putInt(selectedCantonPrefKey, position).commit();

                // Change available plate types
                plateTypeSpinnerItems.clear();
                Canton canton = cantons.get(position);
                if (canton.isAutoIndexSupported() && canton.getSyncAutoIndexProvider() != null) {
                    for (PlateType plateType : PlateType.values()) {
                        // Except if defined otherwise (for online auto indexes)
                        if (canton.getSyncAutoIndexProvider().isPlateTypeSupported(plateType)) {
                            int resourceId = PlateTypeUtils.getResourceId(plateType);
                            String localizedString = getResources().getString(resourceId);
                            spinnerItemsLocalizations.put(localizedString, resourceId);
                            plateTypeSpinnerItems.add(localizedString);
                        }
                    }
                } else {
                    int resourceId = PlateTypeUtils.getResourceId(PlateType.AUTOMOBILE);

                    String localizedString = getResources().getString(resourceId);
                    spinnerItemsLocalizations.put(localizedString, resourceId);
                    plateTypeSpinnerItems.add(localizedString);
                }

                plateTypeAdapter.notifyDataSetChanged();

                if (plateTypeSpinnerItems.size() < 2) {
                    plateTypeSpinner.setEnabled(false);
                } else {
                    plateTypeSpinner.setEnabled(true);

                    plateTypeSpinner.setSelection(
                            savedInstanceState != null ? savedInstanceState.getInt("plate_type_spinner_current_index", 0) : 0);

                }

                Integer drawableId = flagsMapping.get(cantonSpinnerItems.get(position).toLowerCase());
                if (drawableId != null) {
                    cantonFlagImageView.setImageResource(drawableId);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        };
        cantonSpinner.setOnItemSelectedListener(cantonSpinnerListener);

        // Plate number edit text
        plateNumberEditText = (PlateAutoCompleteTextView) view.findViewById(R.id.search_plate_edittext);
        plateNumberEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // OK or DONE or ENTER key from keyboard starts search
                if (KeyEvent.KEYCODE_ENTER == keyCode) {
                    searchButton(plateNumberEditText);
                    return true;
                } else {
                    return false;
                }
            }
        });
        final List<Plate> lastSearches = LastSearchesProvider.getInstance().getLastSearches();
        if (!lastSearches.isEmpty() && !lastSearches.contains(PlateSearchAdapter.MORE_RECENT_SEARCHES_PLATE)) {
            lastSearches.add(PlateSearchAdapter.MORE_RECENT_SEARCHES_PLATE);
        }
        plateSearchAdapter = new PlateSearchAdapter(activity,
                android.R.layout.simple_dropdown_item_1line,
                lastSearches);
        plateNumberEditText.setAdapter(plateSearchAdapter);
        plateNumberEditText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CharSequence currentText = ((TextView) view).getText();
                if (getString(R.string.recent_searches_more).equals(currentText.toString())) {
                    // Launch Settings Activity
                    FragmentManager fragmentManager = getFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.content_frame, new SettingsFragment())
                            .commit();
                } else {
                    final Plate selectedPlate = lastSearches.get(findLastSearchesItemPosition(currentText));
                    String cantonAbbr = selectedPlate.getCanton().getAbbreviation();
                    selectCanton(cantonAbbr);

                    plateNumberEditText.setText(Integer.toString(selectedPlate.getNumber()));
                }
            }

            private int findLastSearchesItemPosition(CharSequence text) {
                String[] split = text.toString().split(" ");
                String canton = split[0];
                int number = Integer.valueOf(split[1]);
                for (int i = 0; i < lastSearches.size(); i++) {
                    Plate plate = lastSearches.get(i);
                    if (plate.getNumber() == number && plate.getCanton().getAbbreviation().equalsIgnoreCase(canton)) {
                        return i;
                    }
                }
                return -1;
            }
        });

        // Recent searches button
        recentSearchesButton = (Button) view.findViewById(R.id.recent_searches_button);
        recentSearchesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyboard(getActivity());
                plateSearchAdapter.getFilter().filter("");
                plateNumberEditText.showDropDown();
            }
        });
        if (lastSearches.isEmpty()) {
            recentSearchesButton.setEnabled(false);
        }

        // Search button
        final Button searchButton = (Button) view.findViewById(R.id.search_owner_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchButton(plateNumberEditText);
            }
        });
        searchButton.setEnabled(false);

        // Activate button if text length > 0
        plateNumberEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                searchButton.setEnabled(s.length() > 0);
            }
        });


        // Open soft keyboard automatically
        //activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void selectCanton(String cantonAbbr) {
        int cantonId = cantonSpinnerItems.indexOf(cantonAbbr);
        cantonSpinner.setSelection(cantonId);
    }


    public void cancelPlate() {
        //LastSearchesProvider.getInstance().remove(currentPlate);
        //plateSearchAdapter.remove(currentPlate);
        currentPlate = null;
    }

    private void search(int number) {
        hideKeyboard(getActivity());

        final Canton selectedCanton = cantons.get((int) cantonSpinner.getSelectedItemId());
        PlateType plateType = PlateTypeUtils.getPlateType(spinnerItemsLocalizations.get(plateTypeSpinner.getSelectedItem()));

        final Plate plate = new Plate(number, plateType, selectedCanton);

        LastSearchesProvider.getInstance().add(plate);
        recentSearchesButton.setEnabled(true);
        plateSearchAdapter.clear();
        plateSearchAdapter.addAll(LastSearchesProvider.getInstance().getLastSearches());
        if (!LastSearchesProvider.getInstance().getLastSearches().contains(
                PlateSearchAdapter.MORE_RECENT_SEARCHES_PLATE
        )) {
            plateSearchAdapter.add(PlateSearchAdapter.MORE_RECENT_SEARCHES_PLATE);
        }

        if (sayMessagesOutLoud) {
            Toast.makeText(getActivity(), plate.toString(), Toast.LENGTH_SHORT).show();
            speech.sayPendingSearch(plate);
        }

        if (plate.getCanton().getAbbreviation().toUpperCase().equals("TI")) {
            Fragment fragment = new WebTicinoFragment();
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .addToBackStack(null)
                    .commit();
        } else {
            doSearch(plate);
        }
    }

    private void searchButton(EditText plateNumberEditText) {
        if (plateNumberEditText.getText().length() > 0) { // Do not do anything if user didn't type any number
            search(Integer.valueOf(plateNumberEditText.getText().toString()));
        } // end if plate length > 0
    }

    private void doSearch(final Plate plate) {
        currentPlate = plate;

        // Look in bookmarks and open it from there if it exists
        DataManager dataManager = new DataManager(getActivity());
        PlateRecord record = null;
        try {
            record = dataManager.getPlateRecordByPlate(plate);
        } finally {
            dataManager.closeDb();
        }
        if (record != null) {
            if (sayMessagesOutLoud) {
                speech.sayOwnerFound(plate, record.toPlateOwner());
            }
            BookmarksViewFragment.openPlateRecord(getFragmentManager(), record);
        } else {
            // Create task and execute it
            new AsyncTask<Void, Exception, PlatePlateOwner>() {
                @Override
                protected void onPreExecute() {
                    if (plate.getCanton().isAutoIndexSupported()) {
                        autoIndexProvider = (CaptchaAutoIndexProvider) plate.getCanton().getSyncAutoIndexProvider();
                        registerProgressListener();
                        progressDialog.show();
                    }
                }

                public boolean inArray(String item, String[] arr) {
                    for (String s : arr) {
                        if (s.equals(item)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                protected PlatePlateOwner doInBackground(Void... voids) {
                    if (plate.getCanton().isAutoIndexSupported()) {
                        ;
                        try {
                            currentRequestId = random.nextInt();
                            final PlateOwner plateOwner = autoIndexProvider.getPlateOwner(plate, currentRequestId);
                            if (sayMessagesOutLoud)
                                speech.sayOwnerFound(plate, plateOwner);
                            return new PlatePlateOwner(plate, plateOwner);

                        } catch (final PlateRequestException pre) {
                            Log.e(getClass().getName(), "Could not fetch plate owner: " + pre.getMessage(), pre);
                            if (pre instanceof RequestCancelledException) {
                                // Nothing special to do...
                            } else {
                                publishProgress(pre);
                                return null;
                            }
                        } catch (CaptchaException ce) {
                            if (sayMessagesOutLoud) speech.sayError();

                            // Handling already done in WebServiceBasedCaptchaHandler
                        }

                    }

                    return new PlatePlateOwner(plate, null);
                }

                @Override
                protected void onProgressUpdate(Exception... pre) {
                    if (!getActivity().isFinishing()) {
                        new MyPlateOwnerRequestListener(getActivity(), progressDialog).onPlateRequestException(plate, (PlateRequestException) pre[0], speech, sayMessagesOutLoud);
                    }
                }

                @Override
                protected void onPostExecute(PlatePlateOwner ppo) {
                    try {
                        unregisterProgressListener();
                        sayMessagesOutLoud = false;
                        if(progressDialog.isShowing()) progressDialog.dismiss();
                    } catch (IllegalArgumentException iae) {
                        // ignore
                    }

                    if (ppo == null) return;

                    if (plate.getCanton().isAutoIndexSupported()) {
                        new MyPlateOwnerRequestListener(getActivity(), progressDialog).onPlateOwnerFound(ppo.getPlate(), ppo.getPlateOwner());
                    } else {
                        // The following cantons use SMS to 939
                        String[] smsCantons = {
                                "AI", "AR", "BE", "BL", "GL", "GR", "SG", "SO", "TG"
                        };
                        if (inArray(plate.getCanton().getAbbreviation(), smsCantons)) {
                            // Start SMS Provider activity
                            Fragment fragment = new WarningFragment();
                            Bundle args = new Bundle();
                            args.putString("canton", plate.getCanton().getAbbreviation());
                            args.putInt("number", plate.getNumber());
                            fragment.setArguments(args);
                            getFragmentManager().beginTransaction()
                                    .replace(R.id.content_frame, fragment)
                                    .addToBackStack(null) // it's possible to go back
                                    .commit();
                        }

                        // The following cantons provide no easy, free and automated service
                        // (so they use E-Mail, a costly website, telephone, etc.)
                        String[] manualCantons = {"BS", "GE", "NW", "OW", "SZ", "UR", "NE", "JU", "VD", "TI"};
                        if (inArray(plate.getCanton().getAbbreviation(), manualCantons)) {
                            Fragment fragment = new RequestIsManualFragment();
                            Bundle args = new Bundle();
                            args.putString("canton", plate.getCanton().getAbbreviation().toLowerCase());
                            fragment.setArguments(args);
                            getFragmentManager().beginTransaction().
                                    replace(R.id.content_frame, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                }
            }.execute();
        }
    }

    public static Map<String, Integer> flagsMapping = new LinkedHashMap<String, Integer>();

    static {
        flagsMapping.put("ag", R.drawable.flag_ag);
        flagsMapping.put("ai", R.drawable.flag_ai);
        flagsMapping.put("ar", R.drawable.flag_ar);
        flagsMapping.put("be", R.drawable.flag_be);
        flagsMapping.put("bl", R.drawable.flag_bl);
        flagsMapping.put("bs", R.drawable.flag_bs);
        flagsMapping.put("ch", R.drawable.flag_ch);
        flagsMapping.put("fl", R.drawable.flag_fl);
        flagsMapping.put("fr", R.drawable.flag_fr);
        flagsMapping.put("ge", R.drawable.flag_ge);
        flagsMapping.put("gl", R.drawable.flag_gl);
        flagsMapping.put("gr", R.drawable.flag_gr);
        flagsMapping.put("ju", R.drawable.flag_ju);
        flagsMapping.put("lu", R.drawable.flag_lu);
        flagsMapping.put("ne", R.drawable.flag_ne);
        flagsMapping.put("nw", R.drawable.flag_nw);
        flagsMapping.put("ow", R.drawable.flag_ow);
        flagsMapping.put("sg", R.drawable.flag_sg);
        flagsMapping.put("sh", R.drawable.flag_sh);
        flagsMapping.put("so", R.drawable.flag_so);
        flagsMapping.put("sz", R.drawable.flag_sz);
        flagsMapping.put("tg", R.drawable.flag_tg);
        flagsMapping.put("ti", R.drawable.flag_ti);
        flagsMapping.put("ur", R.drawable.flag_ur);
        flagsMapping.put("vd", R.drawable.flag_vd);
        flagsMapping.put("vs", R.drawable.flag_vs);
        flagsMapping.put("zg", R.drawable.flag_zg);
        flagsMapping.put("zh", R.drawable.flag_zh);
    }

    private class CantonSpinnerArrayAdapter<String> extends ArrayAdapter<String> {

        public CantonSpinnerArrayAdapter(Context context, int textViewResourceId, List<String> objects) {
            super(context, textViewResourceId, objects);
        }


        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getCustomView(position, parent);
        }

        private View getCustomView(int position, ViewGroup parent) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View row = inflater.inflate(R.layout.image_and_text_spinner_row, parent, false);
            TextView label = (TextView) row.findViewById(R.id.image_and_text_spinner_row_text);
            label.setText(getItem(position).toString());

            ImageView icon = (ImageView) row.findViewById(R.id.image_and_text_spinner_row_image);
            Integer drawableId = flagsMapping.get(cantonSpinnerItems.get(position).toLowerCase());
            if (drawableId != null) {
                icon.setImageResource(drawableId);
            }

            return row;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Remember selected plate type (useful when switching orientation)
        outState.putInt("plate_type_spinner_current_index", plateTypeSpinner.getSelectedItemPosition());
    }

    private Random random = new Random();

    public boolean isCurrentRequestCancelled() {
        return autoIndexProvider.isCancelled(currentRequestId);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        speech.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        speech.onDestroy();
        unregisterProgressListener();
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            vocalSearchButton.setEnabled(true);
        } else if (status == TextToSpeech.ERROR) {
            vocalSearchButton.setEnabled(false);
        }
    }

    @Override
    public void onNumberRecognized(int number) {
        search(number);
    }

    @Override
    public void onSpeechRecognitionFinished() {

    }

    private void registerProgressListener() {
        progressDialog.setIndeterminate(autoIndexProvider.isIndeterminateProgress());
        if (!autoIndexProvider.isIndeterminateProgress()) {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressListener = new ProgressListener() {
                @Override
                public void onProgress(int current, int maximum) {
                    progressDialog.setMax(maximum);
                    progressDialog.setProgress(current);
                }
            };
            autoIndexProvider.addListener(progressListener);
        } else {
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        }
    }

    private void unregisterProgressListener() {
        if (progressListener != null) {
            autoIndexProvider.removeListener(progressListener);
        }
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus:
        View view = activity.getCurrentFocus();
        if (view != null) {
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private String getGoogleAccount(Context context) {
        Account[] accounts = AccountManager.get(context).getAccounts();
        if (accounts.length > 0) {
            return accounts[0].name;
        } else {
            return "";
        }
    }

}
