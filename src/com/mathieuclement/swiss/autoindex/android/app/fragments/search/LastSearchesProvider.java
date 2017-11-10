package com.mathieuclement.swiss.autoindex.android.app.fragments.search;

import android.content.Context;
import android.content.SharedPreferences;
import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.plate.PlateType;
import com.mathieuclement.swiss.autoindex.android.app.SwissAutoIndexApplication;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Mathieu Clément
 * @since 20.06.2013
 */
public class LastSearchesProvider {
    public static final String LAST_SEARCHES_PREF = "last_searches";
    public static final String HISTORY_SIZE_PREF = "pref_search_history_size";
    public static final int DEFAULT_NUMBER_OF_RESULTS = 20;

    private final Context context;
    private List<Plate> list;
    // List will hold only *capacity* elements. You may show less.
    // Without this, the list would only grow and grow (supposing you don't remove elements manually.)
    private static LastSearchesProvider INSTANCE;
    private SharedPreferences sharedPreferences;

    public static LastSearchesProvider getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new LastSearchesProvider();
        }
        return INSTANCE;
    }

    private LastSearchesProvider() {
        context = SwissAutoIndexApplication.getAppContext();
        sharedPreferences = context.getSharedPreferences(LAST_SEARCHES_PREF, Context.MODE_PRIVATE);
        restore();
    }

    /**
     * Returns the last (most recent) 20 searches. <br/>
     * You must not modify this list but use {@link #add(Plate)} and {@link #remove(Plate)} instead.
     *
     * @return the last (most recent) 20 searches.
     */
    public List<Plate> getLastSearches() {
        return list;
    }

    /**
     * Add item at the end of the list. If it already existed, it will be moved to the end.
     *
     * @param s item to add
     */
    public void add(Plate s) {
        int pos = pos(s);
        if (pos != -1) {
            list.remove(pos);
        }
        list.add(0, s); // Add to beginning of the list
        removeLastIfTooBig();
        save();
    }

    // Special pos method that only looks for the canton and number
    // Returns id of item
    private int pos(Plate plate) {
        for (int i = 0; i < list.size(); i++) {
            Plate otherPlate = list.get(i);
            if (otherPlate.getCanton().equals(plate.getCanton()) && otherPlate.getNumber() == plate.getNumber()) {
                return i;
            }
        }
        return -1;
    }

    // Remove oldest element if list is too big
    private void removeLastIfTooBig() {
        if (list.size() > getPrefHistorySize()) {
            list.remove(list.size() - 1);
        }
    }

    private int getPrefHistorySize() {
        return sharedPreferences.getInt(HISTORY_SIZE_PREF, DEFAULT_NUMBER_OF_RESULTS);
    }

    /**
     * Remove item from list
     *
     * @param s item
     */
    public void remove(Plate s) {
        list.remove(s);
        save();
    }

    public void clear() {
        list.clear();
        save();
    }

    protected void restore() {
        String str = sharedPreferences.getString(LAST_SEARCHES_PREF, null);
        if (str == null) {
            list = new LinkedList<Plate>();
        } else {
            list = listFromString(str);
        }
    }

    // Saving is done automatically by add and remove methods.
    protected void save() {
        if (list == null) {
            throw new IllegalStateException("Called save() but restore() has not been done yet. List is null.");
        }
        sharedPreferences.edit().putString(LAST_SEARCHES_PREF, listToString(list)).apply();
    }

    String listToString(List<Plate> theList) {
        StringBuilder sb = new StringBuilder("");
        for (Plate plate : theList) {
            if (plate == PlateSearchAdapter.MORE_RECENT_SEARCHES_PLATE) continue;

            sb.append(plate.getCanton().getAbbreviation());
            sb.append(",");
            sb.append(plate.getNumber());
            sb.append(',');
            sb.append(plate.getType().getName());
            sb.append("|");
        }
        if (!theList.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    List<Plate> listFromString(String str) {
        List<Plate> theList = new LinkedList<Plate>();
        StringTokenizer listTokenizer = new StringTokenizer(str, "|");
        while (listTokenizer.hasMoreElements()) {
            String plateStr = listTokenizer.nextToken();
            StringTokenizer plateTokenizer = new StringTokenizer(plateStr, ",");
            String abbr = plateTokenizer.nextToken();
            int number = Integer.valueOf(plateTokenizer.nextToken());
            String typeStr = plateTokenizer.nextToken();
            Canton canton = new Canton();
            canton.setAbbreviation(abbr);
            Plate plate = new Plate(number, new PlateType(typeStr), canton);
            theList.add(plate);
        }
        return theList;
    }

    public void fitSize() {
        // Remove older elements when size was changed from the preferences
        int prefHistorySize = getPrefHistorySize();
        while(list.size() > prefHistorySize) {
            removeLastIfTooBig();
        }
    }
}
