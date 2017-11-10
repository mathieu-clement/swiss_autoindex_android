package com.mathieuclement.swiss.autoindex.android.app.fragments;

import android.app.ProgressDialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.bugsense.trace.BugSenseHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.lib.autoindex.provider.common.captcha.event.AsyncAutoIndexProvider;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.SearchFragment;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.DataManager;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.bookmarks.BookmarkRecord;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecord;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class ShowPlateOwnerFragment extends android.app.Fragment {

    public static final String PLATE_CANTON_ABBR_KEY = "plate-canton-abbr";
    public static final String PLATE_TYPE_KEY = "plate-type";
    public static final String PLATE_NUMBER_KEY = "plate-number";

    public static final String PLATE_OWNER_NAME_KEY = "plate-owner-name";
    public static final String PLATE_OWNER_ADDRESS_KEY = "plate-owner-address";
    public static final String PLATE_OWNER_ADDRESS_COMPLEMENT_KEY = "plate-owner-address-complement";
    public static final String PLATE_OWNER_ZIP_KEY = "plate-owner-zip";
    public static final String PLATE_OWNER_TOWN_KEY = "plate-owner-town";

    public static final String PLATE_RECORD_ID_KEY = "plate-record-id";
    public static final String DEFAULT_CONTACT_COUNTRY = "Switzerland";

    private PlateOwner plateOwner;
    private Plate plate;
    private long plateRecordId = 0L;
    private View.OnClickListener bookmarkButtonOnClickListener;
    private View.OnClickListener removeBookmarkButtonOnClickListener;
    private DataManager dataManager;
    private Button bookmarkButton;
    private PlateRecord searchedPlateRecord;
    private String contactLookupKey;
    private String remarks = "";
    private Button contactButton;
    private boolean remarksChanged = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.plate_owner_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        // No title bar in landscape mode
        /*if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }*/

        readExtras(savedInstanceState);

        /**
         * Plate
         */
        TextView plateView = (TextView) view.findViewById(R.id.plate_owner_plate_textView);
        plateView.setText(niceFormatPlate(plate));

        // Change colours to match plate types
        // See ASA website
        LinearLayout plateLayout = (LinearLayout) view.findViewById(R.id.plate_owner_plate_linear_layout);
        plateLayout.setBackgroundColor(makePlateBackgroundColor(plate.getType()));

        /**
         * Canton flag
         */
        ImageView cantonFlagImgView = (ImageView) view.findViewById(R.id.plate_owner_canton_flag_imageview);
        cantonFlagImgView.setImageResource(SearchFragment.flagsMapping.get(
                plate.getCanton().getAbbreviation().toLowerCase()));

        /**
         * Owner
         */
        TextView ownerTextView = (TextView) view.findViewById(R.id.plate_owner_textview);
        ownerTextView.setText(niceFormatOwner(plateOwner));

        /**
         * Remarks EditText
         */
        final EditText remarksEditText = (EditText) view.findViewById(R.id.plate_owner_remarks_edittext);
        if (plateRecordId != 0L) { // If contact added or bookmarked
            // there will be an ID
            // because there is an entry in plate records
            // for it
            remarksEditText.setVisibility(View.VISIBLE); // Invisible by default

            dataManager = new DataManager(getActivity());
            try {
                remarks = findRemarksInDatabase(plate);
            } finally {
                dataManager.closeDb();
            }
            if (remarks != null) {
                remarksEditText.setText(remarks);
            }
        }
        remarksEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                remarksChanged = true;
            }
        });

        /**
         * Bookmark button
         */
        bookmarkButton = (Button) view.findViewById(R.id.plate_owner_bookmark_button);
        bookmarkButtonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataManager = new DataManager(getActivity());
                try {
                    plateRecordId = dataManager.addBookmark(plate, plateOwner);
                } catch (Exception e) {
                    BugSenseHandler.sendException(e);
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Error. Try again.", Toast.LENGTH_SHORT).show();
                }
                dataManager.closeDb();
                bookmarkButton.setText(R.string.plate_owner_remove_bookmark);
                bookmarkButton.setOnClickListener(removeBookmarkButtonOnClickListener);
                remarksEditText.setVisibility(View.VISIBLE);
            }
        };
        removeBookmarkButtonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Save previous content of remarks
                String editTextContent = remarksEditText.getText().toString();
                if (remarksEditText.getText() != null) {
                    remarks = editTextContent; // Store old remarks in case the user clicked "Remove bookmark" by mistake.
                }
                dataManager = new DataManager(getActivity());
                try {
                    dataManager.removeBookmark(plateRecordId != 0L ? plateRecordId : searchedPlateRecord.getId());
                } finally {
                    dataManager.closeDb();
                }
                bookmarkButton.setText(R.string.plate_owner_bookmark);
                bookmarkButton.setOnClickListener(bookmarkButtonOnClickListener);
                remarksEditText.setVisibility(View.INVISIBLE);
            }
        };

        /**
         * Remove / Add as contact button
         */
        contactButton = (Button) view.findViewById(R.id.plate_owner_save_as_contact_button);

        if (plateRecordId != 0L) { // If contact added or bookmarked
            // there will be an ID
            // because there is an entry in plate records
            // for it
            dataManager = new DataManager(getActivity());
            try {
                contactLookupKey = findContactLookupKeyInDatabase(plate);
            } finally {
                dataManager.closeDb();
            }
            if (contactLookupKey != null) {
                contactButton.setText(R.string.plate_owner_remove_contact);
            }
        }

        contactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (contactLookupKey != null) {
                    // remove
                    try {
                        dataManager = new DataManager(getActivity());
                        try {
                            removeContact(contactLookupKey);
                        } finally {
                            dataManager.closeDb();
                        }
                        // Now the button will add the contact instead
                        contactLookupKey = null;
                        ((Button) view).setText(R.string.plate_owner_save_as_contact);
                    } catch (IllegalStateException ise) {
                        Log.e(((Object) getActivity()).getClass().getName(), ise.getMessage(), ise);
                        Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                        BugSenseHandler.sendException(ise);
                    }
                } else {
                    // add
                    view.setEnabled(false);

                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... params) {
                            try {
                                insertContact(plateOwner);
                                contactLookupKey = findContactLookupKeyInContacts(getActivity(), plateOwner);
                                dataManager = new DataManager(getActivity());
                                try {
                                    insertContactInDatabase(plate, plateOwner, contactLookupKey);
                                } finally {
                                    dataManager.closeDb();
                                }
                            } catch (RemoteException e) {
                                Log.e(getClass().getSimpleName(), "Could not save plate owner as contact: " + plateOwner, e);
                                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                                BugSenseHandler.sendException(e);
                            } catch (OperationApplicationException e) {
                                Log.e(getClass().getSimpleName(), "Could not save plate owner as contact: " + plateOwner, e);
                                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                                BugSenseHandler.sendException(e);
                            } catch (Exception e) {
                                Log.e(getClass().getSimpleName(), "Could not save plate owner as contact in DB : " +
                                        plateOwner, e);
                                Toast.makeText(getActivity(), "Error", Toast.LENGTH_SHORT).show();
                                BugSenseHandler.sendException(e);
                            } // end try-catch

                            return null;
                        } // end doInBackground()

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);

                            // Now the button will remove contact instead
                            ((Button) view).setText(R.string.plate_owner_remove_contact);
                            view.setEnabled(true);
                        } // end onPostExecute()
                    }.execute();

                } // end if
            } // new Listener
        }); // add Listener

        /**
         * Lookup in Phone directory button
         */
        Button phoneDirButton = (Button) view.findViewById(R.id.plate_owner_lookup_in_phone_directory_button);
        // Open local.ch application to look for a phone number for instance
        phoneDirButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String localChAppPackage = "ch.local.android";
                /*
                    Check local.ch application is installed, then launch it and look for the name and address
                    Otherwise open the application page on Google Play Store.
                 */
                try {
                    // The next line serves as a "if ( local.ch application is installed )"
                    getActivity().getPackageManager().getApplicationInfo(localChAppPackage, 0);
                    // then ...
                    String url = makeLocalChIntentUrl(plateOwner.getName(), plateOwner.getAddress(),
                            plateOwner.getZip(), plateOwner.getTown());
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) { // PackageManager.NameNotFoundException + ActivityNotFoundException
                    // Application Local.ch is not installed
                    e.printStackTrace();

                    // Open Google Play Store
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + localChAppPackage));
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException anfe) {
                        // getActivity() is not supposed to happen if the application was acquired legally on Google Play, but we never know...
                        // Also getActivity() exception will be thrown when running the application in the emulator,
                        Toast.makeText(getActivity(), "Cannot find Google Play Store application on getActivity() device.", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        /**
         * See on map button
         */
        final Button mapButton = (Button) view.findViewById(R.id.plate_owner_see_on_map_button);
        // Copying to final variable to use it in anonymous class
        mapButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                new AsyncTask<Void, Throwable, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            String mapUri = makeGeoUrlString();

                            if (mapUri != null) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(mapUri));
                                startActivity(intent);
                            }
                        } catch (IOException ioe) {
                            Log.e(ShowPlateOwnerFragment.class.getSimpleName(), ioe.getMessage(), ioe);
                            publishProgress(ioe);
                        } catch (URISyntaxException e) {
                            Log.e(ShowPlateOwnerFragment.class.getSimpleName(), e.getMessage(), e);
                            publishProgress(e);
                        }

                        return null;
                    }

                    @Override
                    protected void onProgressUpdate(Throwable... values) {
                        Toast.makeText(getActivity(), "Error geocoding address: " + values[0].getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }

                    ProgressDialog progressDialog;

                    @Override
                    protected void onPreExecute() {
                        mapButton.setEnabled(false);
                        progressDialog = new ProgressDialog(getActivity());
                        progressDialog.setIndeterminate(true);
                        progressDialog.show();
                    }

                    @Override
                    protected void onPostExecute(Void aVoid) {
                        progressDialog.dismiss();
                        mapButton.setEnabled(true);
                    }
                }.execute();
            }
        });

        /**
         * Share button
         */
        Button shareButton = (Button) view.findViewById(R.id.plate_owner_share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // http://mobile.tutsplus.com/tutorials/android/android-sdk-implement-a-share-intent/

                // Create a sharing intent
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");

                // Define subject and body
                // Text shared: FR 12345 | Université de Fribourg, Bvd de Pérolles 90, 1700 Fribourg
                String shareBody = plate.getCanton().getAbbreviation() + " " + plate.getNumber() +
                        " | " + plateOwner.getName() + ", " + makeAddress();
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
                String shareSubject = plate.toString();
                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, shareSubject);

                // Show chooser
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.plate_owner_share)));
            }
        });
    }

    private void insertContactInDatabase(Plate plate, PlateOwner plateOwner, String contactLookupKey) throws Exception {
        dataManager.addContact(plate, plateOwner, contactLookupKey);
    }

    // Remove contact
    private void removeContact(String lookupKey) {
        ContentResolver cr = getActivity().getContentResolver();
        int rowsDeleted = cr.delete(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey),
                null, null);
//        if (rowsDeleted != 1) {
//            // It means there is no contact with getActivity() lookup key anymore.
//            throw new IllegalStateException(Integer.toString(rowsDeleted) + " row(s) have been deleted, expected 1.");
//        }

        dataManager.removeContact(plate);
    }

    private String findContactLookupKeyInDatabase(Plate plate) {
        try {
            return dataManager.getContactLookupKey(plate);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    private String findRemarksInDatabase(Plate plate) {
        try {
            return dataManager.getRemarks(plate);
        } catch (NullPointerException npe) {
            return null;
        }
    }

    // Returns the id of the contact with the exact same info as the one getActivity() app created.
    // Returns null if not found
    private static String findContactLookupKeyInContacts(Context context, PlateOwner plateOwner) {

//                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));

        String returnValue = null;

        // Query using Display name
        Cursor detailsCur = context.getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.StructuredPostal.STREET,
                        ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE,
                        ContactsContract.CommonDataKinds.StructuredPostal.CITY,
                        ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY
                }
                , null, null, null);

        if (detailsCur.getCount() > 0) {

            detailsCur.moveToFirst();

            do {
                String lookupKey = detailsCur.getString(detailsCur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));

                String country = detailsCur.getString(detailsCur.getColumnIndex(
                        ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));

                // Most contacts will fail the Country test, which speeds up things.
                if (DEFAULT_CONTACT_COUNTRY.equals(country)) {
                    String name = detailsCur.getString(detailsCur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                    String address = detailsCur.getString(detailsCur.getColumnIndex(
                            ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                    int zip = detailsCur.getInt(detailsCur.getColumnIndex(
                            ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                    String city = detailsCur.getString(detailsCur.getColumnIndex(
                            ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                    if (plateOwner.getName().equals(name) &&
                            plateOwner.getAddress().equals(address) &&
                            plateOwner.getZip() == zip &&
                            plateOwner.getTown().equals(city)) {
                        detailsCur.close();
                        returnValue = lookupKey;
                        return lookupKey;
                    }
                }

            } while (detailsCur.moveToNext());
        }

        detailsCur.close();
        return returnValue;
    }

    private String niceFormatOwner(PlateOwner plateOwner) {
        String newLine = "\r\n";
        String plateOwnerText = plateOwner.getName() + newLine
                + plateOwner.getAddress() + newLine;
        if (plateOwner.getAddressComplement() != null && !"".equals(plateOwner.getAddressComplement())) {
            plateOwnerText += plateOwner.getAddressComplement() + newLine;
        }
        plateOwnerText += plateOwner.getZip() + " " + plateOwner.getTown();
        return plateOwnerText;
    }

    private CharSequence niceFormatPlate(Plate plate) {
        StringBuilder plateTextBuilder = new StringBuilder();
        // \u00B7 is U+00B7 unicode for middle point.
        plateTextBuilder.append(plate.getCanton().getAbbreviation()).append(" \u00B7 ")
                .append(Plate.formatNumber((plate.getNumber())));

        if (PlateType.AUTOMOBILE_REPAIR_SHOP.equals(plate.getType()) || PlateType.MOTORCYCLE_REPAIR_SHOP.equals(plate.getType())) {
            // append U for repair shops plates
            plateTextBuilder.append(" \u00B7 U");
        } else if (PlateType.AUTOMOBILE_TEMPORARY.equals(plate.getType()) || PlateType.MOTORCYCLE_TEMPORARY.equals(plate.getType())) {
            // Append unfilled rectangle to temporary plates
            plateTextBuilder.append(" \u25AF");
        }
        return plateTextBuilder.toString();
    }

    private void insertContact(PlateOwner plateOwner) throws RemoteException, OperationApplicationException {
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        int rawContactInsertIndex = 0;

        operations.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, plateOwner.getName()).build());
        operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactInsertIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, plateOwner.getAddress())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, plateOwner.getZip())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, plateOwner.getTown())
                        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, DEFAULT_CONTACT_COUNTRY).build()
        );

        getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, operations);
    }

    /*
    private static String urlEncode(String decodedURL) {
        try {
            return URLEncoder.encode(decodedURL, "UTF-8").toString();
        } catch (UnsupportedEncodingException e) {
            BugSenseHandler.sendException(e);
            throw new RuntimeException(e);
        }
    }
    */

    private static String makeLocalChIntentUrl(String name, String street, int zip, String town) {
        StringBuilder sb = new StringBuilder(300);
        sb.append("localch://tel/search?what=")
                .append(name)
                .append("&where=")
                .append(street)
                .append(", ")
                .append(town);
        return sb.toString();
    }

    private int makePlateBackgroundColor(PlateType type) {
        String colorStr;

        if (PlateType.AGRICULTURAL.equals(type)) {
            colorStr = "#49ddb1";
        } else if (PlateType.INDUSTRIAL.equals(type)) {
            colorStr = "#6e91bf";
        } else if (PlateType.AUTOMOBILE_BROWN.equals(type)) {
            colorStr = "#c28e5c";
        } else if (PlateType.MOTORCYCLE_YELLOW.equals(type)) {
            colorStr = "#e6b348";
        } else if (PlateType.MOTORCYCLE_BROWN.equals(type)) {
            colorStr = "#c28e5c";
        } else {
            //colorStr = "#eaedea"; // blanc cassé - gris clair
            colorStr = "#ffffff";
        }

        return Color.parseColor(colorStr);
    }

    private String makeGeoUrlString() throws IOException, URISyntaxException {
        String addressStr = plateOwner.getAddress() + ", " + plateOwner.getZip() + " " + plateOwner.getTown() + ", Switzerland";
        // We don't put the "Complément d'adresse" as it is often found to be a postal box (case postale)

        String encodedAddressStr = URLEncoder.encode(addressStr, "utf-8");

        String jsonStr = IOUtils.toString(new URI("https://maps.googleapis.com/maps/api/geocode/json?address=" + encodedAddressStr));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonStr);

        JsonNode geometryNode = node.findValue("geometry");
        if (geometryNode == null) return null;

        JsonNode locationNode = geometryNode.get("location");
        double latitude = locationNode.get("lat").asDouble();
        double longitude = locationNode.get("lng").asDouble();

        return "geo:" + latitude + "," + longitude + "?q=" + encodedAddressStr;
    }

    /**
     * Concatenate the currently displayed plate owner's address, zip code and town.
     *
     * @return the currently displayed plate owner's address, zip code and town.
     */
    private String makeAddress() {
        if (plateOwner.getAddressComplement() != null && !"".equals(plateOwner.getAddressComplement())) {
            return plateOwner.getAddress() + ", " + plateOwner.getAddressComplement() + ", "
                    + plateOwner.getZip() + " " + plateOwner.getTown();
        } else {
            return plateOwner.getAddress() + ", " + plateOwner.getZip() + " " + plateOwner.getTown();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        dataManager = new DataManager(getActivity());

        try {

            if (!dataRead) {
                Log.e(getActivity().getClass().getName(), "CONCEPTION ERROR: No data read! Go back to SearchActivity.");
            }

            // Bookmark button behaves differently and has a different text if plate is already bookmarked or not.
            searchedPlateRecord = dataManager.getPlateRecordByPlate(plate);
            if (searchedPlateRecord == null) {
                bookmarkButton.setOnClickListener(bookmarkButtonOnClickListener);
            } else {
                bookmarkButton.setText(R.string.plate_owner_remove_bookmark);
                bookmarkButton.setOnClickListener(removeBookmarkButtonOnClickListener);
            }
        } finally {
            dataManager.closeDb();
        }
    }


    @Override
    public void onStop() {
        super.onStop();

        try {
            EditText remarksEditText = (EditText) getActivity().findViewById(R.id.plate_owner_remarks_edittext);
            if (remarksEditText.getVisibility() == View.VISIBLE) { // if bookmarked

                String editTextContent = remarksEditText.getText().toString();
                if (remarksEditText.getText() != null && remarksChanged) {
                    dataManager = new DataManager(getActivity());
                    BookmarkRecord bookmarkRecord = dataManager.getBookmarkRecordDao().get(plateRecordId);
                    if (bookmarkRecord != null) {
                        bookmarkRecord.setRemarks(editTextContent);
                        try {
                            dataManager.getBookmarkRecordDao().update(bookmarkRecord, plateRecordId);
                            if (!"".equals(editTextContent)) {
                                Toast.makeText(getActivity(), R.string.plate_owner_remarks_saved_toast, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            BugSenseHandler.sendException(e);
                        }
                    }
                }
            }
        } finally {
            dataManager.closeDb();
        }
    }

    // data was read from extras passed to the bundle that launched the activity. See readExtras(Bundle).
    private boolean dataRead = false;

    /**
     * Set attributes of getActivity() class (related to plate owner, plate record and so on) by reading the bundle passed to the intent.
     *
     * @param savedInstanceState Bundle passed to the intent
     */
    private void readExtras(Bundle savedInstanceState) {
        String cantonAbbr = "";
        String plateTypeStr = "";
        int plateNumber = -1;

        String plateOwnerName = "";
        String plateOwnerAddress = "";
        String plateOwnerAddressComplement = null;
        int plateOwnerZip = -1;
        String plateOwnerTown = "";

        Bundle extras;
        if (savedInstanceState == null) {
            extras = getActivity().getIntent().getExtras();
            if (extras != null && extras.getInt(PLATE_NUMBER_KEY) != 0) {
                cantonAbbr = extras.getString(PLATE_CANTON_ABBR_KEY);
                plateTypeStr = extras.getString(PLATE_TYPE_KEY);
                plateNumber = extras.getInt(PLATE_NUMBER_KEY);

                plateOwnerName = extras.getString(PLATE_OWNER_NAME_KEY);
                plateOwnerAddress = extras.getString(PLATE_OWNER_ADDRESS_KEY);
                plateOwnerAddressComplement = extras.getString(PLATE_OWNER_ADDRESS_COMPLEMENT_KEY);
                plateOwnerZip = extras.getInt(PLATE_OWNER_ZIP_KEY);
                plateOwnerTown = extras.getString(PLATE_OWNER_TOWN_KEY);

                plateRecordId = extras.getLong(PLATE_RECORD_ID_KEY);
            } else {
                if (getArguments() == null) {
                    throw new Error("Null extras!");
                } else {
                    cantonAbbr = getArguments().getString(PLATE_CANTON_ABBR_KEY);
                    plateTypeStr = getArguments().getString(PLATE_TYPE_KEY);
                    plateNumber = getArguments().getInt(PLATE_NUMBER_KEY);

                    plateOwnerName = getArguments().getString(PLATE_OWNER_NAME_KEY);
                    plateOwnerAddress = getArguments().getString(PLATE_OWNER_ADDRESS_KEY);
                    plateOwnerAddressComplement = getArguments().getString(PLATE_OWNER_ADDRESS_COMPLEMENT_KEY);
                    plateOwnerZip = getArguments().getInt(PLATE_OWNER_ZIP_KEY);
                    plateOwnerTown = getArguments().getString(PLATE_OWNER_TOWN_KEY);

                    plateRecordId = getArguments().getLong(PLATE_RECORD_ID_KEY);
                }
            }
        } else {
            cantonAbbr = savedInstanceState.getString(PLATE_CANTON_ABBR_KEY);
            plateTypeStr = savedInstanceState.getString(PLATE_TYPE_KEY);
            plateNumber = savedInstanceState.getInt(PLATE_NUMBER_KEY);

            plateOwnerName = savedInstanceState.getString(PLATE_OWNER_NAME_KEY);
            plateOwnerAddress = savedInstanceState.getString(PLATE_OWNER_ADDRESS_KEY);
            plateOwnerAddressComplement = savedInstanceState.getString(PLATE_OWNER_ADDRESS_COMPLEMENT_KEY);
            plateOwnerZip = savedInstanceState.getInt(PLATE_OWNER_ZIP_KEY);
            plateOwnerTown = savedInstanceState.getString(PLATE_OWNER_TOWN_KEY);

            plateRecordId = savedInstanceState.getLong(PLATE_RECORD_ID_KEY);
        }

        if (cantonAbbr != null && !"".equals(cantonAbbr)) {
            dataRead = true;
        }

        plate = new Plate(plateNumber, new PlateType(plateTypeStr), new Canton(cantonAbbr, false, (AsyncAutoIndexProvider) null));
        plateOwner = new PlateOwner(plateOwnerName, plateOwnerAddress, plateOwnerAddressComplement, plateOwnerZip, plateOwnerTown);

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(PLATE_CANTON_ABBR_KEY, plate.getCanton().getAbbreviation());
        outState.putString(PLATE_TYPE_KEY, plate.getType().getName());
        outState.putInt(PLATE_NUMBER_KEY, plate.getNumber());

        outState.putString(PLATE_OWNER_NAME_KEY, plateOwner.getName());
        outState.putString(PLATE_OWNER_ADDRESS_KEY, plateOwner.getAddress());
        outState.putString(PLATE_OWNER_ADDRESS_COMPLEMENT_KEY, plateOwner.getAddressComplement());
        outState.putInt(PLATE_OWNER_ZIP_KEY, plateOwner.getZip());
        outState.putString(PLATE_OWNER_TOWN_KEY, plateOwner.getTown());

        outState.putLong(PLATE_RECORD_ID_KEY, plateRecordId);
    }


}
