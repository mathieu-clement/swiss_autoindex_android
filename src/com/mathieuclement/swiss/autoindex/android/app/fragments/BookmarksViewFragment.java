package com.mathieuclement.swiss.autoindex.android.app.fragments;

import android.app.*;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateOwner;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.DataManager;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecord;
import com.mathieuclement.swiss.autoindex.android.app.util.PlateTypeUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class BookmarksViewFragment extends ListFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, PopupMenu.OnMenuItemClickListener {

    private List<PlateRecord> plateRecordList;
    private DataManager dataManager;
    private int currentPosition; // position of bookmark item clicked, used for onMenuItemClick
    private ArrayAdapter<String> listAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bookmarks_list, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        List<String> items = new LinkedList<String>();

        dataManager = new DataManager(getActivity());
        plateRecordList = dataManager.getBookmarks();
        // dataManager.closeDb();

        // Order by plate number
        Collections.sort(plateRecordList, new Comparator<PlateRecord>() {
            @Override
            public int compare(PlateRecord plateRecord, PlateRecord otherPlateRecord) {
                return ((Integer) plateRecord.getNumber()).compareTo(otherPlateRecord.getNumber());
            }
        });

        for (PlateRecord plateRecord : plateRecordList) {
            StringBuilder shownString = new StringBuilder();
            String numberPartStr = plateRecord.getCanton() + " " + Plate.formatNumber(plateRecord.getNumber());
            if (PlateType.AUTOMOBILE_REPAIR_SHOP.equals(new PlateType(plateRecord.getType())) ||
                    PlateType.MOTORCYCLE_REPAIR_SHOP.equals(new PlateType(plateRecord.getType()))) {
                // append U for repair shops plates
                numberPartStr += " U";
            }

            shownString.append(numberPartStr);

            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_name_in_favorites", true)) {
                // If plate type is NOT "Auto (white)", display the type on the first line along with the plate number
                // and the name on a new line
                if (!PlateType.AUTOMOBILE.equals(new PlateType(plateRecord.getType()))) {
                    String typeStr;
                    try {
                        typeStr = getResources().getString(PlateTypeUtils.getResourceId(
                                new PlateType(plateRecord.getType())));
                    } catch (Resources.NotFoundException nfe) {
                        Log.e(getClass().getName(), "Resource string for " + plateRecord.getType() + " could not be " +
                                "found.");
                        typeStr = "Unknown";
                    }
                    shownString.append(" - ").append(typeStr);
                    if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                        shownString.append("\n      ");
                    } else {
                        shownString.append(" | ");
                    }
                    shownString.append(plateRecord.getName());
                } else {
                    // Otherwise show something like "FR 300 340 - Transports Publics Fribourgeois"
                    shownString.append(" - ").append(plateRecord.getName());
                }

            } else {
                shownString.append(" - ").append(getResources().getString(PlateTypeUtils.getResourceId(
                        new PlateType(plateRecord.getType()))));
            }

            items.add(shownString.toString());
        }

        ListView listView = (ListView) view.findViewById(android.R.id.list);

        listAdapter = new BookmarksListAdapter(getActivity(), android.R.layout.simple_list_item_1, items);
        listView.setAdapter(listAdapter);

        if (!plateRecordList.isEmpty()) {
            listView.setOnItemClickListener(this);
            listView.setOnItemLongClickListener(this);
        } else {
            items.add(getResources().getString(R.string.bookmarks_empty_list));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        dataManager.closeDb();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        if (!plateRecordList.isEmpty()) { // ignore click on "No bookmark added yet" item
            PlateRecord plateRecord = plateRecordList.get(position);
            openPlateRecord(plateRecord);
        }
    }

    private void openPlateRecord(PlateRecord plateRecord) {
        openPlateRecord(getFragmentManager(), plateRecord);
    }

    public static void openPlateRecord(FragmentManager fragmentManager, PlateRecord plateRecord) {
        Plate plate = plateRecord.toPlate();
        PlateOwner plateOwner = plateRecord.toPlateOwner();

        Bundle args = new Bundle();
        args.putString(ShowPlateOwnerFragment.PLATE_CANTON_ABBR_KEY, plate.getCanton().getAbbreviation());
        args.putString(ShowPlateOwnerFragment.PLATE_TYPE_KEY, plate.getType().getName());
        args.putInt(ShowPlateOwnerFragment.PLATE_NUMBER_KEY, plate.getNumber());

        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_NAME_KEY, plateOwner.getName());
        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_ADDRESS_KEY, plateOwner.getAddress());
        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_ADDRESS_COMPLEMENT_KEY, plateOwner.getAddressComplement());
        args.putInt(ShowPlateOwnerFragment.PLATE_OWNER_ZIP_KEY, plateOwner.getZip());
        args.putString(ShowPlateOwnerFragment.PLATE_OWNER_TOWN_KEY, plateOwner.getTown());

        args.putLong(ShowPlateOwnerFragment.PLATE_RECORD_ID_KEY, plateRecord.getId());

        Fragment fragment = new ShowPlateOwnerFragment();
        fragment.setArguments(args);

        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.content_frame, fragment).addToBackStack(null).commit();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        showPopupMenu(view, position);
        return true;
    }

    private void showPopupMenu(View v, int position) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), v);
        popupMenu.getMenuInflater().inflate(R.menu.bookmark_long_click_menu, popupMenu.getMenu());

        this.currentPosition = position;
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        PlateRecord plateRecord = plateRecordList.get(currentPosition);

        if (item.getItemId() == R.id.bookmark_long_click_popupmenu_open) {
            openPlateRecord(plateRecord);
        } else if (item.getItemId() == R.id.bookmark_long_click_popupmenu_remove_bookmark) {
            removeBookmark(plateRecord);
        } else {
            return false;
        }

        return true;
    }

    private void removeBookmark(PlateRecord plateRecord) {
        dataManager.removeBookmark(plateRecord.getId());
        listAdapter.remove(listAdapter.getItem(currentPosition));
        plateRecordList.remove(currentPosition);
        if (plateRecordList.isEmpty()) {
            listAdapter.add(getResources().getString(R.string.bookmarks_empty_list));
        }
        listAdapter.notifyDataSetChanged();
    }

    private class BookmarksListAdapter extends ArrayAdapter<String> {

        public BookmarksListAdapter(Context context, int viewId, List<String> items) {
            super(context, viewId, items);
        }

        // Bold style span
        StyleSpan boldStyle = new StyleSpan(android.graphics.Typeface.BOLD);

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getView(position, convertView, parent);
            if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean
                    ("pref_name_in_favorites", true)) {
                String wholeStr = getItem(position);
                SpannableStringBuilder sb = new SpannableStringBuilder(wholeStr);
                sb.setSpan(boldStyle, 0, numberLength(wholeStr), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                view.setText(sb);
            }
            return view;
        }
        // Returns number of characters including the last number
        // e.g. "FR 12 345" returns 9.
        private int numberLength(String str) {
            // Find first dash
            // We suppose we have an example such as "FR 12 345 - Owner name"
            int endIndex = str.indexOf('-');
            if (endIndex < 0) {
                //BugSenseHandler.sendEvent("In favorites, got the (unspannable) label: " + str);
                return 0;
            } else {
                return endIndex - 1;
            }
        }

    } // end BookmarksListAdapter
}
