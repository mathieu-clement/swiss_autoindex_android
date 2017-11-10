package com.mathieuclement.swiss.autoindex.android.app.fragments.sms_provider;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import com.mathieuclement.swiss.autoindex.android.app.R;

import java.util.Calendar;

/**
 * @author Mathieu Clément
 * @since 15.09.2013
 */
public class WarningFragment extends Fragment {

    public static final String MUST_SHOW_WARNING_PREF = "must_show_warning";
    public static final boolean MUST_SHOW_WARNING_DEFAULT = true;

    private CheckBox checkBox;
    private Button nextButton;
    private SharedPreferences prefs;
    // Transiting extras
    private String canton;
    private int number;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sms_warning, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = getActivity().getSharedPreferences(getClass().getName(), Context.MODE_PRIVATE);

        if(savedInstanceState != null) {
            canton = savedInstanceState.getString("canton");
            number = savedInstanceState.getInt("number");
        } else {
            canton = getArguments().getString("canton");
            number = getArguments().getInt("number");
        }

        // Go to next step if user disabled the warning
        if (!prefs.getBoolean(MUST_SHOW_WARNING_PREF, MUST_SHOW_WARNING_DEFAULT)) {
            nextButtonClicked();
            return;
        }

        checkBox = (CheckBox) view.findViewById(R.id.sms_warning_do_not_show_checkbox);

        nextButton = (Button) view.findViewById(R.id.sms_warning_i_understand_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prefs.edit().putBoolean(MUST_SHOW_WARNING_PREF, !checkBox.isChecked()).commit();

                nextButtonClicked();
            }
        });
    }

    // Method called when Next button is clicked.
    // (Must not handle the "Do not show this warning" warning)
    private void nextButtonClicked() {
        if (outsideHours()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.warn_outside_sms_hours);
            builder.setPositiveButton(R.string.warn_outside_sms_hours_continue_anyway, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startSendReceive();
                }
            });
            builder.setCancelable(true);
            builder.setNegativeButton(R.string.warn_outside_sms_hours_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getFragmentManager().popBackStack(); // Go back to search
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        } else {
            startSendReceive();
        }
    }

    private boolean outsideHours() {
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        return currentHour < 6 || currentHour >= 22;
    }

    private void startSendReceive() {
        Fragment fragment = new SendReceiveFragment();
        Bundle args = new Bundle();

        // "Transfer" the extras...
        args.putString("canton", canton);
        args.putInt("number", number);
        fragment.setArguments(args);
        // Next activity will not be present in stack if addToBackStack(null) not included

        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }
}
