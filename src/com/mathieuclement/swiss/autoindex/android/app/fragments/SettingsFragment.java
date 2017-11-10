package com.mathieuclement.swiss.autoindex.android.app.fragments;

import android.app.AlertDialog;
import android.app.backup.BackupManager;
import android.app.backup.RestoreObserver;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.fragments.search.LastSearchesProvider;
import com.mathieuclement.swiss.autoindex.android.app.fragments.sms_provider.WarningFragment;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.DataManager;
import com.mathieuclement.swiss.autoindex.android.app.persistence.droid.plate_records.PlateRecord;
import com.mathieuclement.swiss.autoindex.android.app.util.RateMe;

public class SettingsFragment extends PreferenceFragment {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        // Clear search history
        Preference clearHistoryPref = findPreference("pref_clear_history"); // must match key from preferences.xml
        clearHistoryPref.setOnPreferenceClickListener(new ClearHistoryPreferenceListener());
        clearHistoryPref.setEnabled(!LastSearchesProvider.getInstance().getLastSearches().isEmpty());

        // Remove all bookmarks
        Preference removeAllBookmarksPref = findPreference("pref_remove_all_bookmarks"); // must match key from preferences.xml
        removeAllBookmarksPref.setOnPreferenceClickListener(new RemoveAllBookmarksPreferenceListener());

        // Search history size
        ListPreference historySizePref = (ListPreference) findPreference("pref_search_history_size");
        historySizePref.setOnPreferenceChangeListener(new SearchHistorySizePreferenceListener());
        historySizePref.setValue(Integer.toString(
                getActivity().getSharedPreferences(LastSearchesProvider.LAST_SEARCHES_PREF, Context.MODE_PRIVATE)
                        .getInt(LastSearchesProvider.HISTORY_SIZE_PREF, LastSearchesProvider.DEFAULT_NUMBER_OF_RESULTS)));

        // Clear warning user pref
        Preference clearWarningsPref = findPreference("pref_clear_warnings");
        clearWarningsPref.setOnPreferenceClickListener(new ClearWarningsPreferenceListener());
        clearWarningsPref.setEnabled(true);

        // Force backup restore
        Preference backupRestorePref = findPreference("pref_force_backup_restore");
        backupRestorePref.setOnPreferenceClickListener(new BackupRestorePreferenceListener());

        // License
        Preference licensePref = findPreference("pref_license");
        licensePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.content_frame, new LicenceFragment())
                        .addToBackStack(null)
                        .commit();
                return true;
            }
        });

        // Feedback
        Preference feedbackPref = findPreference("pref_feedback");
        feedbackPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                try {
                    final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.setType("plain/text");
                    String recipient = "android@freebourg.org";
                    String subject = "SwissAutoIndex Feedback";
                    String message = null;
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{recipient});
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

                    startActivity(Intent.createChooser(emailIntent, "Feedback..."));

                } catch (ActivityNotFoundException e) {
                    // cannot send email for some reason
                }

                return true;
            }
        });

        // Rate this app
        Preference rateThisAppPref = findPreference("pref_rate_this_app");
        rateThisAppPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                RateMe.rateThisApp(getActivity());
                return true;
            }
        });
    }


    /**
     * Backup restore preference button listener
     */
    private class BackupRestorePreferenceListener implements Preference.OnPreferenceClickListener {

        @Override
        public boolean onPreferenceClick(final Preference preference) {

            final Context context = getActivity().getApplicationContext();

            try {
                // Whether a restore happened
                final boolean[] hasDoneRestore = {false};

                new BackupManager(context).requestRestore(new RestoreObserver() {
                    @Override
                    public void restoreStarting(int numPackages) {
                        super.restoreStarting(numPackages);
                        Toast.makeText(context, Integer.toString(numPackages) + " packages", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onUpdate(int nowBeingRestored, String currentPackage) {
                        super.onUpdate(nowBeingRestored, currentPackage);
                    }

                    @Override
                    public void restoreFinished(int error) {
                        super.restoreFinished(error);
                        hasDoneRestore[0] = true;
                        if (error == 0) {
                            Toast.makeText(context, R.string.restore_completed_successfully, Toast.LENGTH_LONG).show();
                            preference.setEnabled(false);
                        } else {
                            Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_LONG).show();
                        }
                    }
                });

                // Inform user if there was nothing to restore
                if (!hasDoneRestore[0]) {
                    Toast.makeText(context, R.string.restore_nothing, Toast.LENGTH_SHORT).show();
                    preference.setEnabled(false);
                }

                return true;
            } catch (Exception e) {
                Log.e(SettingsFragment.class.getSimpleName(), "Exception while processing force restore", e);
                Toast.makeText(context, R.string.restore_failed, Toast.LENGTH_LONG).show();
            }

            return true;
        }

    }

    private class ClearHistoryPreferenceListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.do_you_want_to_clear_history);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LastSearchesProvider.getInstance().clear();
                    Toast.makeText(getActivity().getApplicationContext(), R.string.history_was_cleared, Toast.LENGTH_LONG).show();
                    preference.setEnabled(false);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();

            return true;
        }
    }

    private class ClearWarningsPreferenceListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            getActivity().getSharedPreferences(WarningFragment.class.getName(), Context.MODE_PRIVATE).edit().putBoolean(
                    WarningFragment.MUST_SHOW_WARNING_PREF, WarningFragment.MUST_SHOW_WARNING_DEFAULT)
                    .apply();
            new RateMe(getActivity()).reset();


            preference.setEnabled(false);

            return true;
        }
    }

    private class SearchHistorySizePreferenceListener implements Preference.OnPreferenceChangeListener {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            int size = Integer.parseInt((String) newValue);
            getActivity().getSharedPreferences(LastSearchesProvider.LAST_SEARCHES_PREF, Context.MODE_PRIVATE).edit().putInt(
                    LastSearchesProvider.HISTORY_SIZE_PREF, size)
                    .apply();
            LastSearchesProvider.getInstance().fitSize();
            return true;
        }
    }

    private class RemoveAllBookmarksPreferenceListener implements Preference.OnPreferenceClickListener {
        @Override
        public boolean onPreferenceClick(final Preference preference) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.do_you_want_to_remove_all_bookmarks);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DataManager dataManager = new DataManager(preference.getContext());
                    for (PlateRecord record : dataManager.getBookmarks()) {
                        dataManager.removeBookmark(record.getId());
                    }
                    dataManager.closeDb();

                    preference.setEnabled(false);
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return true;
        }
    }
}