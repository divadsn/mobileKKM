package de.codebucket.mkkm;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class KKMWebviewClient extends WebViewClient {

    private static final String TAG = "WebviewClient";
    private static final String WEBAPP_URL = "https://m.kkm.krakow.pl";

    public static final String PAGE_OVERVIEW = "home";
    public static final String PAGE_CONTROL = "control";
    public static final String PAGE_PURCHASE = "ticket";
    public static final String PAGE_ACCOUNT = "account";

    private Context mContext;
    private SwipeRefreshLayout mSwipeLayout;
    private OnPageChangedListener mPageListener;

    private boolean hasInjected = false;

    public KKMWebviewClient(Activity context, OnPageChangedListener listener) {
        mContext = context;
        mSwipeLayout = (SwipeRefreshLayout) context.findViewById(R.id.swipe);
        mPageListener = listener;

        // Disable swipe down gesture
        mSwipeLayout.setRefreshing(false);
        mSwipeLayout.setEnabled(false);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);
        mSwipeLayout.setEnabled(true);
        mSwipeLayout.setRefreshing(true);

        // Reset injection if url is webapp
        if (url.startsWith(WEBAPP_URL)) {
            hasInjected = false;
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        mSwipeLayout.setRefreshing(false);
        mSwipeLayout.setEnabled(false);

        // Remove navbar after page has finished loading
        if (url.startsWith(WEBAPP_URL) && !hasInjected) {
            AssetManager assetManager = mContext.getAssets();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            InputStream inputStream = null;
            String inject = null;

            try {
                // Read webview.js from local assets
                inputStream = assetManager.open("webview.js");
                byte buf[] = new byte[8192];
                int len;
                while ((len = inputStream.read(buf)) != -1) {
                    outputStream.write(buf, 0, len);
                }

                outputStream.close();
                inputStream.close();
                inject = outputStream.toString();
            } catch (IOException ex) {
                Log.e(TAG, "Error injecting script: " + ex);
                ex.printStackTrace();
            }

            view.addJavascriptInterface(new ScriptInjectorCallback(), "ScriptInjector");
            view.evaluateJavascript(inject, null);
        }

        String page = url.substring(WEBAPP_URL.length() + 4).split("/")[0];
        mPageListener.onPageChanged(view, page);
    }

    public static String getPageUrl(String page) {
        return String.format("%s/#!/%s", WEBAPP_URL, page);
    }

    public class ScriptInjectorCallback {
        @JavascriptInterface
        public void callback() {
            Log.d(TAG, "Script injected!");
            hasInjected = true;
        }
    }

    public interface OnPageChangedListener {
        void onPageChanged(WebView view, String page);
    }
}
