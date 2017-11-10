package com.mathieuclement.swiss.autoindex.android.app.fragments.sms_provider;

import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.mathieuclement.swiss.autoindex.android.app.R;

/**
 * @author Mathieu Clément
 * @since 15.09.2013
 */
public class RequestIsManualFragment extends Fragment {
    private String cantonAbbr;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.manual_request, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        readExtras(savedInstanceState);

        if ("bs".equals(cantonAbbr))
            checkAndSet(R.string.complicated_bs_text, R.string.complicated_bs_url,
                    R.string.complicated_bs_email, R.string.complicated_bs_phone);
        else if ("ge".equals(cantonAbbr))
            checkAndSet(R.string.complicated_ge_text, R.string.complicated_ge_url,
                    R.string.complicated_ge_email, R.string.complicated_ge_phone);
        else if ("ne".equals(cantonAbbr))
            checkAndSet(R.string.complicated_ne_text, R.string.complicated_ne_url,
                    R.string.complicated_ne_email, R.string.complicated_ne_phone);
        else if ("nw".equals(cantonAbbr))
            checkAndSet(R.string.complicated_nw_text, R.string.complicated_nw_url,
                    R.string.complicated_nw_email, R.string.complicated_nw_phone);
        else if ("ow".equals(cantonAbbr))
            checkAndSet(R.string.complicated_ow_text, R.string.complicated_ow_url,
                    R.string.complicated_ow_email, R.string.complicated_ow_phone);
        else if ("sz".equals(cantonAbbr))
            checkAndSet(R.string.complicated_sz_text, R.string.complicated_sz_url,
                    R.string.complicated_sz_email, R.string.complicated_sz_phone);
        else if ("nw".equals(cantonAbbr))
            checkAndSet(R.string.complicated_nw_text, R.string.complicated_nw_url,
                    R.string.complicated_nw_email, R.string.complicated_nw_phone);
        else if ("ur".equals(cantonAbbr))
            checkAndSet(R.string.complicated_ur_text, R.string.complicated_ur_url,
                    R.string.complicated_ur_email, R.string.complicated_ur_phone);
        else if ("ju".equals(cantonAbbr))
            checkAndSet(R.string.complicated_ju_text, R.string.complicated_ju_url,
                    R.string.complicated_ju_email, R.string.complicated_ju_phone);
        else if ("vd".equals(cantonAbbr))
            checkAndSet(R.string.complicated_vd_text, R.string.complicated_vd_url,
                    R.string.complicated_vd_email, R.string.complicated_vd_phone);
        else if ("ti".equals(cantonAbbr))
            checkAndSet(R.string.complicated_ti_text, R.string.complicated_ti_url,
                    R.string.complicated_ti_email, R.string.complicated_ti_phone);
        else
            throw new RuntimeException("There are no texts for " + cantonAbbr + ".");

    }

    private void checkAndSet(int textId, int urlId, int emailId, int phoneId) {
        final String text = getResources().getText(textId, "").toString();
        final String url = getResources().getText(urlId, "").toString();
        final String email = getResources().getText(emailId, "").toString();
        final String phone = getResources().getText(phoneId, "").toString();

        if (!"".equals(text)) {
            TextView view = (TextView) getView().findViewById(R.id.manual_request_textview);
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        }
        if (!"".equals(url)) {
            TextView view = (TextView) getView().findViewById(R.id.manual_request_url);
            view.setText(url);
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                }
            });
        }
        if (!"".equals(email)) {
            TextView view = (TextView) getView().findViewById(R.id.manual_request_email);
            view.setText(email);
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.setType("plain/text");
                        String recipient = email;
                        String subject = "Abfrage / Demande";
                        String message = null;
                        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{recipient});
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

                        startActivity(Intent.createChooser(emailIntent, "E-Mail..."));

                    } catch (ActivityNotFoundException e) {
                        // cannot send email for some reason
                    }
                }
            });
        }
        if (!"".equals(phone)) {
            TextView view = (TextView) getView().findViewById(R.id.manual_request_phone);
            view.setText(phone);
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String uri = "tel:" + phone.trim().replace("(0)", "");
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse(uri));
                    startActivity(intent);
                }
            });
        }


    }

    private void readExtras(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            cantonAbbr = savedInstanceState.getString("canton").toLowerCase();
        } else if (getArguments() != null) {
            cantonAbbr = getArguments().getString("canton").toLowerCase();
        } else {
            throw new Error("Null extras!");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("canton", cantonAbbr);
    }
}
