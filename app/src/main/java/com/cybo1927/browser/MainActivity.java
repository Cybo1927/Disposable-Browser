package com.cybo1927.browser;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    public String urlS = "https://start.duckduckgo.com";

    TextView urlBar;
    Button openMenu;
    SwipeRefreshLayout swipeRefreshLayout;
    ProgressBar progressBar;
    ImageView favicon;

    private WebView webView;

    // Enter/return key will search when in the URL bar
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            EditText editText = findViewById(R.id.url);
            if (editText.getText().toString().contains("://")) {
                webView.loadUrl(editText.getText().toString());
            } else {
                    webView.loadUrl("https://duckduckgo.com/?q=" + editText.getText().toString());
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    // Help check if there's a connection
    public boolean isConnected() {
        boolean connected = false;
        try {
            ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo nInfo = cm.getActiveNetworkInfo();
            connected = nInfo != null && nInfo.isAvailable() && nInfo.isConnected();
            return connected;
        } catch (Exception e) {
            Log.e("Connectivity Exception", e.getMessage());
        }
        return connected;
    }

    @SuppressLint({"SetJavaScriptEnabled", "NonConstantResourceId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Block screenshots or screen recording in the app
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // Send toast if there's no connection
        if (!isConnected()) {
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
        }

        webView = findViewById(R.id.webView);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(urlS);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setSafeBrowsingEnabled(false);
        webView.setVerticalScrollBarEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        progressBar = findViewById(R.id.progressBar);
        progressBar.setMax(100);
        favicon = findViewById(R.id.favicon);

        webView.setWebChromeClient(new WebChromeClient() {
            // Each page load will change the URL in the bottom bar
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                urlBar = findViewById(R.id.url);
                urlBar.setText(webView.getUrl());
            }
            // Progress bar
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progressBar.setProgress(newProgress);
                // Hide progress bar when done loading
                if (progressBar.getProgress() == 100) {
                    progressBar.setVisibility(View.INVISIBLE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
                super.onProgressChanged(view, newProgress);
            }
            // Set the website favicon as soon as it's loaded
            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                favicon.setImageBitmap(icon);
                super.onReceivedIcon(view, icon);
            }
        });
        // Swipe down from top to refresh webView
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);

        swipeRefreshLayout.setOnRefreshListener(() -> {
            webView.reload();
            new Handler().postDelayed(() -> swipeRefreshLayout.setRefreshing(false), 750);
        });
        // the popup menu in the bottom right
        openMenu = findViewById(R.id.btnOpenMenu);

        openMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_popup, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    // Refresh the page
                    case R.id.item_refresh:
                        webView.reload();
                        return true;
                    // View page source
                    case R.id.item_view_source:
                        webView.loadUrl("view-source:" + webView.getUrl());
                        return true;
                    // Share current URL
                    case R.id.item_share:
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("text/plain");
                        String title = webView.getTitle();
                        String link = webView.getUrl();
                        intent.putExtra(Intent.EXTRA_TEXT, title + '\n' + link);
                        startActivity(Intent.createChooser(intent, "Share"));
                        return true;
                    default:
                        return false;
                }
            });
            popupMenu.show();
        });
    }
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
            // Fix problem with this not updating
            urlBar = findViewById(R.id.url);
            urlBar.setText(webView.getUrl());
        } else {
            Toast.makeText(getApplicationContext(), "No previous page available", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        startService(new Intent(this, background.class));
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("pm clear com.cybo1927.browser");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(getApplicationContext(), "Clearing application data...", Toast.LENGTH_SHORT ).show();
        stopService(new Intent(this, background.class));
        super.onDestroy();
    }
}