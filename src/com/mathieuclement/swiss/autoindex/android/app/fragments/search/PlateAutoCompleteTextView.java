package com.mathieuclement.swiss.autoindex.android.app.fragments.search;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

/**
 * @author Mathieu Clément
 * @since 20.06.2013
 */
public class PlateAutoCompleteTextView extends AutoCompleteTextView {
    private Context context;

    public PlateAutoCompleteTextView(Context context) {
        super(context);
        this.context = context;
    }

    public PlateAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlateAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean enoughToFilter() {
        return true;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
    }

    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        // Avoid copying the string value of the "More..." list item
        // into the edit text.
        if (selectedItem == PlateSearchAdapter.MORE_RECENT_SEARCHES_PLATE) return "";
        else return super.convertSelectionToString(selectedItem);
    }
}
