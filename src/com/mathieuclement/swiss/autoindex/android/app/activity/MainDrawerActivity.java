package com.mathieuclement.swiss.autoindex.android.app.activity;

import android.app.*;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.fragments.BookmarksViewFragment;
import com.mathieuclement.swiss.autoindex.android.app.fragments.SettingsFragment;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.SearchFragment;
import com.mathieuclement.swiss.autoindex.android.app.util.RateMe;

import java.util.LinkedList;
import java.util.List;

public class MainDrawerActivity extends Activity implements AdapterView.OnItemClickListener {
    public static final int DIALOG_CAPTCHA_RETRIEVE_ERROR_ID = 0;
    public static final int DIALOG_OWNER_NOT_FOUND = 2;
    public static final int DIALOG_OWNER_HIDDEN = 3;
    public static final int DIALOG_TOO_MANY_REQUESTS = 4;

    private RateMe rateMe;
    private DrawerLayout drawerLayout;
    private ListView drawerListView;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_with_drawer);

        initDrawer(savedInstanceState);
        initRateMe();

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private void initRateMe() {
        rateMe = new RateMe(this);
        rateMe.advertRatingIfNeeded();
    }

    private void initDrawer(Bundle savedInstanceState) {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        List<String> items = new LinkedList<String>();
        items.add(getResources().getString(R.string.welcome_item_search));      // 0
        items.add(getResources().getString(R.string.welcome_item_bookmarks));   // 1
        items.add(getResources().getString(R.string.welcome_item_settings));    // 2

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, items);
        drawerListView = (ListView) findViewById(R.id.left_drawer);
        drawerListView.setAdapter(adapter);

        drawerListView.setOnItemClickListener(this);

        drawerToggle = new ActionBarDrawerToggle(MainDrawerActivity.this,
                drawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close);
        drawerLayout.setDrawerListener(drawerToggle);

        if (savedInstanceState == null) selectItem(0);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectItem(position);
    }

    private void closeDrawer() {
        drawerLayout.closeDrawer(drawerListView);
    }

    private void selectItem(int position) {
        Fragment fragment = null;
        if (position == 0) {
            fragment = new SearchFragment();
        } else if (position == 1) {
            fragment = new BookmarksViewFragment();
        } else if (position == 2) {
            fragment = new SettingsFragment();
        }

        if (fragment != null) {
            SearchFragment.hideKeyboard(this);
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
            closeDrawer();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        final Dialog[] dialog = {null};

        switch (id) {
            case DIALOG_CAPTCHA_RETRIEVE_ERROR_ID:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainDrawerActivity.this);
                        builder1.setTitle(R.string.captcha_retrieve_dialog_error_title);
                        builder1.setMessage(R.string.captcha_retrieve_dialog_error_message);
                        builder1.setNeutralButton(R.string.dialog_dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        dialog[0] = builder1.create();
                    }
                });
                break;

            case DIALOG_OWNER_NOT_FOUND:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainDrawerActivity.this);
                        builder1.setTitle(R.string.plate_owner_not_found_dialog_title);
                        builder1.setMessage(R.string.plate_owner_not_found_dialog_message);
                        builder1.setNeutralButton(R.string.dialog_dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        dialog[0] = builder1.create();
                    }
                });
                break;

            case DIALOG_OWNER_HIDDEN:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainDrawerActivity.this);
                        builder1.setTitle(R.string.plate_owner_hidden_dialog_title);
                        builder1.setMessage(R.string.plate_owner_hidden_dialog_message);
                        builder1.setNeutralButton(R.string.dialog_dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        dialog[0] = builder1.create();
                    }
                });
                break;

            case DIALOG_TOO_MANY_REQUESTS:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainDrawerActivity.this);
                        builder1.setTitle(R.string.too_many_requests_title);
                        builder1.setMessage(R.string.too_many_requests_message);
                        builder1.setNeutralButton(R.string.dialog_dismiss, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        dialog[0] = builder1.create();
                    }
                });
                break;
        }

        return dialog[0];
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }
}
