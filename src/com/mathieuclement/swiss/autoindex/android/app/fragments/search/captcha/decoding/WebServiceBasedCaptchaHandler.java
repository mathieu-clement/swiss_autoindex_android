package com.mathieuclement.swiss.autoindex.android.app.fragments.search.captcha.decoding;

import android.util.Log;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import com.mathieuclement.lib.autoindex.provider.cari.sync.CariAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.AutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaAutoIndexProvider;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaException;
import com.mathieuclement.lib.autoindex.provider.common.captcha.CaptchaHandler;
import com.mathieuclement.swiss.autoindex.android.app.R;
import com.mathieuclement.swiss.autoindex.android.app.activity.MainDrawerActivity;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

/**
 * @author Mathieu Clément
 * @since 29.12.2013
 */
public class WebServiceBasedCaptchaHandler implements CaptchaHandler {

    private WeakReference<android.app.Activity> activity;

    public WebServiceBasedCaptchaHandler(WeakReference<android.app.Activity> activity) {
        this.activity = activity;
    }

    public String handleCaptchaImage(int requestId, String captchaImageUrl, HttpClient httpClient, HttpHost httpHost,
                                     HttpContext httpContext,
                                     String httpHostHeaderValue,
                                     CaptchaAutoIndexProvider captchaAutoIndexProvider) throws CaptchaException {
        String s = null;
        File captchaImageFile = null;
        try {
            System.out.println("Downloading image...");
            BasicHttpRequest httpRequest = new BasicHttpRequest("GET", captchaImageUrl, HttpVersion.HTTP_1_1);
            httpRequest.setHeader("host", httpHostHeaderValue);
            HttpResponse httpResponse = httpClient.execute(httpHost, httpRequest, httpContext);
            captchaImageFile = File.createTempFile("captcha", ".jpg");
            FileOutputStream fos = new FileOutputStream(captchaImageFile);
            httpResponse.getEntity().writeTo(fos);
            fos.close();
            //httpResponse.getEntity().getContent().close();

            try {
                s = solveCaptcha(captchaImageFile, httpClient, captchaAutoIndexProvider).replace("\n", "");
            } catch (Exception e) {
                BugSenseHandler.sendException(e);
                Log.e(getClass().getName(), "Failed to decode captcha", e);
                if (activity.get() instanceof MainDrawerActivity) {
                    if (!captchaAutoIndexProvider.isCancelled(requestId))
                        activity.get().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(activity.get(), R.string.captcha_decoding_failed, Toast.LENGTH_LONG).show();
                            }
                        });
                }
                //throw e;
            }

        } catch (Exception e) {
            captchaAutoIndexProvider.cancel(requestId);
            if (!captchaAutoIndexProvider.isCancelled(requestId)) {
                captchaAutoIndexProvider.cancel(requestId);
                BugSenseHandler.sendException(e);
                Log.e(getClass().getName(), "Failed to download or open captcha image", e);
                activity.get().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.get().showDialog(MainDrawerActivity.DIALOG_CAPTCHA_RETRIEVE_ERROR_ID);
                    }
                });
                throw new CaptchaException(e);
            }
        } finally {
            if (captchaImageFile != null) {
                captchaImageFile.delete();
            }
        }
        return s;
    }

    private String solveCaptcha(File file, HttpClient httpClient, AutoIndexProvider autoIndexProvider) throws IOException {
        String system = (autoIndexProvider instanceof CariAutoIndexProvider) ? "cari" : "viacar";

        HttpPost httpPost = new HttpPost("https://clement.gotdns.ch/autoindex/captcha/" + system);
        // requires HTTPMime library
        FileBody bin = new FileBody(file);
        StringBody comment = null;
        try {
            comment = new StringBody("Image",
                    "image/" + (autoIndexProvider instanceof CariAutoIndexProvider ? "jpeg" : "png"),
                    Charset.defaultCharset());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        HttpEntity reqEntity = MultipartEntityBuilder.create()
                .addPart("file", bin)
                .addPart("comment", comment)
                .build();
        httpPost.setEntity(reqEntity);

        HttpResponse response = httpClient.execute(httpPost);
        System.out.println("----------------------------------------");
        System.out.println(response.getStatusLine());
        if (response.getStatusLine().getStatusCode() != 200)
            throw new RuntimeException("Could not send captcha " +
                    "(server error, code " + response.getStatusLine().getStatusCode() + ")");
        HttpEntity resEntity = response.getEntity();
        return EntityUtils.toString(resEntity);
    }

    @Override
    public void onCaptchaFailed() {
    }

    @Override
    public void onCaptchaSuccessful() {
    }
}
