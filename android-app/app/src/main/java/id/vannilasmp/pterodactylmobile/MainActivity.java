package id.vannilasmp.pterodactylmobile;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.SslError;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final int FILE_CHOOSER_REQUEST = 1401;
    private static final String DEFAULT_PANEL_URL = "https://panel2.vannilasmp.my.id";
    private static final String PREFS = "panel_settings";
    private static final String PANEL_URL_KEY = "panel_url";

    private WebView webView;
    private ProgressBar progressBar;
    private TextView titleView;
    private ValueCallback<Uri[]> filePathCallback;

    private String panelUrl() {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
            .getString(PANEL_URL_KEY, DEFAULT_PANEL_URL);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        configureWebView();
        webView.loadUrl(panelUrl());
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(12), dp(6), dp(8), dp(6));
        toolbar.setBackgroundColor(Color.rgb(28, 35, 48));

        titleView = new TextView(this);
        titleView.setText("Pterodactyl");
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(17);
        titleView.setSingleLine(true);
        toolbar.addView(titleView, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button addressButton = new Button(this);
        addressButton.setText("URL");
        addressButton.setTextSize(12);
        addressButton.setOnClickListener(view -> showPanelUrlDialog());
        toolbar.addView(addressButton, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, dp(44)
        ));

        Button refreshButton = new Button(this);
        refreshButton.setText("↻");
        refreshButton.setTextSize(18);
        refreshButton.setContentDescription("Refresh");
        refreshButton.setOnClickListener(view -> webView.reload());
        toolbar.addView(refreshButton, new LinearLayout.LayoutParams(dp(52), dp(44)));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setVisibility(View.GONE);

        webView = new WebView(this);
        root.addView(toolbar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(progressBar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(3)
        ));
        root.addView(webView, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));
        setContentView(root);
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getHost().equalsIgnoreCase(Uri.parse(panelUrl()).getHost())) {
                    return false;
                }

                if ("https".equalsIgnoreCase(uri.getScheme())) {
                    openExternal(uri);
                }
                return true;
            }

            @Override
            public void onReceivedSslError(
                WebView view,
                SslErrorHandler handler,
                SslError error
            ) {
                handler.cancel();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Uri uri = Uri.parse(url);
                titleView.setText(uri.getHost() == null ? "Pterodactyl" : uri.getHost());
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;

                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception exception) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, size) -> {
            Uri downloadUri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(downloadUri.getScheme())
                || downloadUri.getHost() == null
                || !downloadUri.getHost().equalsIgnoreCase(Uri.parse(panelUrl()).getHost())) {
                return;
            }
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
            request.setDescription("Downloading from Pterodactyl");
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimeType)
            );
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) {
                request.addRequestHeader("Cookie", cookie);
            }
            request.addRequestHeader("User-Agent", userAgent);
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
        });
    }

    private void showPanelUrlDialog() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(
            android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI
        );
        input.setText(panelUrl());
        input.setSelection(input.length());

        new AlertDialog.Builder(this)
            .setTitle("Panel URL")
            .setMessage("Masukkan alamat Panel HTTPS.")
            .setView(input)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", (dialog, which) -> {
                String value = input.getText().toString().trim();
                if (!value.matches("^https://[^\\s/]+(?:/.*)?$")) {
                    new AlertDialog.Builder(this)
                        .setMessage("URL harus memakai HTTPS.")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }
                Uri parsed = Uri.parse(value);
                if (parsed.getUserInfo() != null || parsed.getHost() == null) {
                    new AlertDialog.Builder(this)
                        .setMessage("URL Panel tidak valid.")
                        .setPositiveButton("OK", null)
                        .show();
                    return;
                }

                String normalized = value.replaceAll("/+$", "");
                getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(PANEL_URL_KEY, normalized)
                    .apply();
                webView.loadUrl(normalized);
            })
            .show();
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "No browser is available for this link.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) {
            return;
        }
        Uri[] results = resultCode == RESULT_OK && data != null && data.getData() != null
            ? new Uri[] {data.getData()}
            : null;
        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
