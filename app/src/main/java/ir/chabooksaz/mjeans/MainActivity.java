package ir.chabooksaz.mjeans;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Mjeans";
    private static final String HOME_URL = "https://mjeans.ir/";
    private static final String HOST = "mjeans.ir";

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorLayout;
    private Button retryButton;
    private SwipeRefreshLayout swipeRefresh;

    private ValueCallback<Uri[]> filePathCallback;
    private final ActivityResultLauncher<Intent> fileChooserLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (filePathCallback == null) return;
                Uri[] results = null;
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String dataString = result.getData().getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    } else if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        results = new Uri[count];
                        for (int i = 0; i < count; i++) {
                            results[i] = result.getData().getClipData().getItemAt(i).getUri();
                        }
                    }
                }
                filePathCallback.onReceiveValue(results);
                filePathCallback = null;
            });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge-to-edge layout with light status/nav bars
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, true);
        window.setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.setNavigationBarColor(Color.WHITE);
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            insetsController.setAppearanceLightNavigationBars(true);
        }

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        retryButton = findViewById(R.id.retryButton);
        swipeRefresh = findViewById(R.id.swipeRefresh);

        // FIX: SwipeRefresh should only trigger when WebView is at the very top.
        swipeRefresh.setOnChildScrollUpCallback((parent, child) -> {
            if (webView == null) return false;
            return webView.getScrollY() > 0;
        });

        swipeRefresh.setOnRefreshListener(() -> {
            if (webView.getUrl() != null) {
                webView.reload();
            } else {
                webView.loadUrl(HOME_URL);
            }
        });
        swipeRefresh.setColorSchemeColors(Color.parseColor("#1A1A1A"));

        retryButton.setOnClickListener(v -> {
            errorLayout.setVisibility(View.GONE);
            webView.loadUrl(HOME_URL);
        });

        configureWebView();

        Intent intent = getIntent();
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            String url = intent.getData().toString();
            Log.i(TAG, "Launched via App Link: " + url);
            webView.loadUrl(url);
        } else {
            webView.loadUrl(HOME_URL);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);

        // *** FIX for tab-bar / popup / target="_blank" issues:
        // By default WebView blocks window.open and target=_blank. Many sites use these
        // for navigation. Enabling multi-window support + onCreateWindow makes them work.
        s.setSupportMultipleWindows(true);

        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);

        // Better rendering — pre-rasterize off-screen content for smoother scroll
        s.setOffscreenPreRaster(true);

        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setUserAgentString(s.getUserAgentString() + " MjeansApp/1.5 (Android)");
        s.setGeolocationEnabled(false);

        // Force-enable viewport-fit=cover so env(safe-area-inset-*) works
        // (already in viewport meta, but enforcing doesn't hurt)

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setBackgroundColor(Color.WHITE);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setWebViewClient(new MjeansWebViewClient());
        webView.setWebChromeClient(new MjeansWebChromeClient());

        webView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
            }
            return false;
        });
    }

    /**
     * Inject CSS into the page after it loads. This fixes WebView-specific issues
     * that the mjeans.ir site suffers from when rendered inside an Android WebView.
     *
     * IMPORTANT LESSON (v1.4 → v1.5): Do NOT change the site's `position: fixed`
     * on `.tab-bar` to `position: sticky`. The site's body is a flex column
     * (`display: flex; flex-direction: column; min-height: 100vh`), so making the
     * last child `position: sticky; bottom: 0` causes the bar to stick to the
     * bottom of the flex container (which can be ABOVE the viewport), pushing
     * the bar to the top of the screen — exactly the "نوار پریده رفته بالا" bug.
     *
     * The site's `position: fixed; bottom: 0` works fine in WebView too — we just
     * need to make sure the hit-region stays stable after scroll. We do that by:
     *   1. Forcing GPU compositing on the fixed element (translateZ(0))
     *   2. Setting a high z-index
     *   3. Adding `touch-action: manipulation` on the tab items to remove the
     *      300ms click delay and prevent the click-region from drifting
     */
    private void injectWebViewFixes() {
        String css = ""
            + ".tab-bar {"
            + "  transform: translateZ(0) !important;"
            + "  -webkit-transform: translateZ(0) !important;"
            + "  will-change: transform !important;"
            + "  z-index: 200 !important;"
            + "}"
            + ".app-header {"
            + "  transform: translateZ(0) !important;"
            + "  -webkit-transform: translateZ(0) !important;"
            + "  will-change: transform !important;"
            + "}"
            + ".tab-bar .tab-item, .app-header a, .app-header button {"
            + "  -webkit-tap-highlight-color: transparent;"
            + "  touch-action: manipulation;"
            + "}";

        String js = "(function(){"
            + "  try {"
            + "    var style = document.getElementById('mjeans-webview-fix');"
            + "    if (!style) {"
            + "      style = document.createElement('style');"
            + "      style.id = 'mjeans-webview-fix';"
            + "      style.textContent = " + jsonQuote(css) + ";"
            + "      (document.head || document.documentElement).appendChild(style);"
            + "    }"
            + "  } catch(e) {}"
            + "})();";

        webView.evaluateJavascript(js, null);
    }

    /** Escape a CSS string into a JSON-safe JavaScript string literal (with surrounding quotes). */
    private static String jsonQuote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private class MjeansWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Uri uri = request.getUrl();

            String host = uri.getHost();
            if (HOST.equals(host) || ("www." + HOST).equals(host)) {
                return false;
            }

            if ("intent".equals(uri.getScheme())) {
                try {
                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                        return true;
                    }
                    String fallback = intent.getStringExtra("browser_fallback_url");
                    if (fallback != null) {
                        view.loadUrl(fallback);
                        return true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to parse intent URL", e);
                }
                return true;
            }

            String scheme = uri.getScheme();
            if (scheme != null && !scheme.equals("http") && !scheme.equals("https")) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                    return true;
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(MainActivity.this, "برنامه‌ای برای باز کردن این لینک یافت نشد", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }

            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return true;
            } catch (ActivityNotFoundException e) {
                return false;
            }
        }

        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            progressBar.setProgress(0);
            progressBar.setVisibility(View.VISIBLE);
            errorLayout.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progressBar.setVisibility(View.GONE);
            if (swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(false);
            }
            // Inject WebView-specific CSS fixes for the tab bar
            injectWebViewFixes();
            CookieManager.getInstance().flush();
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
            super.onReceivedError(view, request, error);
            if (request.isForMainFrame()) {
                showError();
            }
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            if (request.isForMainFrame() && errorResponse.getStatusCode() >= 500) {
                showError();
            }
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            handler.cancel();
            showError();
        }
    }

    private class MjeansWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            progressBar.setProgress(newProgress);
            if (newProgress >= 100) {
                progressBar.setVisibility(View.GONE);
            }
        }

        /**
         * Handle window.open() and target="_blank" links.
         * The site uses these for some navigation paths. Without this, they silently fail
         * and the user gets "stuck" — exactly the bug the user reported.
         *
         * Strategy: open the new URL in the SAME WebView (replace current page).
         * This matches what mobile browsers do for non-popup window.open calls.
         */
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, final Message resultMsg) {
            if (resultMsg == null) return false;

            // Create a temporary WebView to capture the URL that window.open was called with
            WebView newWebView = new WebView(MainActivity.this);
            newWebView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                    // Got the URL — load it in the main WebView
                    String url = request.getUrl().toString();
                    Log.i(TAG, "window.open captured: " + url);
                    webView.loadUrl(url);
                    // Tell the message target we handled it
                    try {
                        android.os.Message href = (android.os.Message) resultMsg;
                        href.sendToTarget();
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to send resultMsg", e);
                    }
                    return true;
                }
            });

            // Hand the transport over to the new WebView so it can capture the URL
            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(newWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            // No-op — we don't actually open new windows
        }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams fileChooserParams) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(null);
            }
            filePathCallback = callback;
            Intent intent = fileChooserParams.createIntent();
            try {
                fileChooserLauncher.launch(intent);
            } catch (ActivityNotFoundException e) {
                filePathCallback = null;
                Toast.makeText(MainActivity.this, "مرورگر فایل در دسترس نیست", Toast.LENGTH_SHORT).show();
                return false;
            }
            return true;
        }
    }

    private void showError() {
        new Handler(Looper.getMainLooper()).post(() -> {
            progressBar.setVisibility(View.GONE);
            if (swipeRefresh.isRefreshing()) {
                swipeRefresh.setRefreshing(false);
            }
            errorLayout.setVisibility(View.VISIBLE);
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            String url = intent.getData().toString();
            Log.i(TAG, "onNewIntent App Link: " + url);
            webView.loadUrl(url);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
