package com.mathieuclement.swiss.autoindex.android.app.fragments.search;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.mathieuclement.lib.autoindex.canton.Canton;
import com.mathieuclement.lib.autoindex.plate.Plate;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.swiss.autoindex.android.app.R;

import java.util.List;

/**
 * @author Mathieu Clément
 * @since 23.06.2013
 */
public class PlateSearchAdapter extends ArrayAdapter<Plate> {
    public static Plate MORE_RECENT_SEARCHES_PLATE =
            new Plate(-1, null, new Canton(null, false, (AutoIndexProvider) null));

    public PlateSearchAdapter(Context context, int resId, List<Plate> items) {
        super(context, resId, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        Plate plate = getItem(position);
        if (plate == MORE_RECENT_SEARCHES_PLATE) {
            view.setText(view.getContext().getString(
                    R.string.recent_searches_more));
        } else {
            view.setText(plate.getCanton().getAbbreviation() + " " + plate.getNumber());
        }
        return view;
    }
}
