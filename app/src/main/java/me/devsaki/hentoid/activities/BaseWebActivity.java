package me.devsaki.hentoid.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Date;

import me.devsaki.hentoid.R;
import me.devsaki.hentoid.database.HentoidDB;
import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.enums.StatusContent;
import me.devsaki.hentoid.services.DownloadService;
import me.devsaki.hentoid.util.AndroidHelper;
import me.devsaki.hentoid.util.Constants;
import me.devsaki.hentoid.util.LogHelper;
import me.devsaki.hentoid.views.ObservableWebView;
import me.devsaki.hentoid.views.ObservableWebView.OnScrollChangedCallback;

/**
 * Browser activity which allows the user to navigate a supported source.
 * No particular source should be filtered/defined here.
 * The source itself should contain every method it needs to function.
 */
public class BaseWebActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(BaseWebActivity.class);

    private final static int STORAGE_PERMISSION_REQUEST = 1;
    protected ObservableWebView webView;
    private HentoidDB db;
    private Content currentContent;
    private Site site;
    private boolean webViewIsLoading;
    private FloatingActionButton fabRead, fabDownload, fabRefreshOrStop, fabDownloads;
    private boolean fabReadEnabled, fabDownloadEnabled;
    private SwipeRefreshLayout swipeLayout;

    protected Site getSite() {
        return site;
    }

    protected void setSite(Site site) {
        this.site = site;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new HentoidDB(this);

        setContentView(R.layout.activity_base_web);

        AndroidHelper.setNavBarColor(this, "#2b0202");

        if (site == null) {
            LogHelper.w(TAG, "Site is null!");
        }

        fabRead = (FloatingActionButton) findViewById(R.id.fabRead);
        fabDownload = (FloatingActionButton) findViewById(R.id.fabDownload);
        fabRefreshOrStop = (FloatingActionButton) findViewById(R.id.fabRefreshStop);
        fabDownloads = (FloatingActionButton) findViewById(R.id.fabDownloads);

        hideFab(fabRead);
        hideFab(fabDownload);

        initWebView();
        initSwipeLayout();

        String intentVar = getIntent().getStringExtra(Constants.INTENT_URL);
        webView.loadUrl(intentVar == null ? site.getUrl() : intentVar);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LogHelper.d(TAG, " onResume");
        checkPermissions();
    }

    // Validate permissions
    private void checkPermissions() {
        if (AndroidHelper.permissionsCheck(this,
                STORAGE_PERMISSION_REQUEST)) {
            LogHelper.d(TAG, "allow");

        } else {
            LogHelper.d(TAG, "deny");
            reset();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission Granted
                checkPermissions();
            } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // Permission Denied
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    reset();
                } else {
                    finish();
                }
            }
        }
    }

    private void reset() {
        AndroidHelper.commitFirstRun(true);
        Intent intent = new Intent(this, IntroSlideActivity.class);
        startActivity(intent);
        finish();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView() {
        webView = (ObservableWebView) findViewById(R.id.wbMain);
        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });
        webView.setLongClickable(false);
        webView.setHapticFeedbackEnabled(false);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress == 100) {
                    swipeLayout.setRefreshing(false);
                } else {
                    swipeLayout.setRefreshing(true);
                }
            }
        });
        webView.setOnScrollChangedCallback(new OnScrollChangedCallback() {
            @Override
            public void onScroll(int l, int t) {
                if (!webViewIsLoading) {
                    if (webView.canScrollVertically(1) || t == 0) {
                        fabRefreshOrStop.show();
                        fabDownloads.show();
                        if (fabReadEnabled) {
                            fabRead.show();
                        } else if (fabDownloadEnabled) {
                            fabDownload.show();
                        }
                    } else {
                        fabRefreshOrStop.hide();
                        fabDownloads.hide();
                        fabRead.hide();
                        fabDownload.hide();
                    }
                }
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setUserAgentString(Constants.USER_AGENT);
        webSettings.setDomStorageEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
    }

    private void initSwipeLayout() {
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(false);
                    }
                }, 5000);
                webView.reload();
            }
        });
        swipeLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
    }

    @SuppressWarnings("UnusedParameters")
    public void onRefreshStopFabClick(View view) {
        if (webViewIsLoading) {
            webView.stopLoading();
        } else {
            webView.reload();
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void onHomeFabClick(View view) {
        Intent intent = new Intent(this, DownloadsActivity.class);

        // If FLAG_ACTIVITY_CLEAR_TOP is not set,
        // it can interfere with Double-Back (press back twice) to exit
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @SuppressWarnings("UnusedParameters")
    public void onReadFabClick(View view) {
        if (currentContent != null) {
            currentContent = db.selectContentById(currentContent.getId());
            if (StatusContent.DOWNLOADED == currentContent.getStatus()
                    || StatusContent.ERROR == currentContent.getStatus()) {
                AndroidHelper.openContent(currentContent, this);
            } else {
                hideFab(fabRead);
            }
        }
    }

    @SuppressWarnings("UnusedParameters")
    public void onDownloadFabClick(View view) {
        processDownload();
    }

    protected void processDownload() {
        currentContent = db.selectContentById(currentContent.getId());
        if (StatusContent.DOWNLOADED == currentContent.getStatus()) {
            Toast.makeText(this, R.string.already_downloaded, Toast.LENGTH_SHORT).show();
            hideFab(fabDownload);

            return;
        }
        Toast.makeText(this, R.string.in_queue, Toast.LENGTH_SHORT).show();
        currentContent.setDownloadDate(new Date().getTime())
                .setStatus(StatusContent.DOWNLOADING);

        db.updateContentStatus(currentContent);
        Intent intent = new Intent(Intent.ACTION_SYNC, null, this, DownloadService.class);

        startService(intent);
        hideFab(fabDownload);
    }

    private void hideFab(FloatingActionButton fab) {
        fab.hide();
        if (fab == fabDownload) {
            fabDownloadEnabled = false;
        } else if (fab == fabRead) {
            fabReadEnabled = false;
        }
    }

    private void showFab(FloatingActionButton fab) {
        fab.show();
        if (fab == fabDownload) {
            fabDownloadEnabled = true;
        } else if (fab == fabRead) {
            fabReadEnabled = true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            WebBackForwardList webBFL = webView.copyBackForwardList();
            int i = webBFL.getCurrentIndex();
            do {
                i--;
            }
            while (i >= 0 && webView.getOriginalUrl()
                    .equals(webBFL.getItemAtIndex(i).getOriginalUrl()));
            if (webView.canGoBackOrForward(i - webBFL.getCurrentIndex())) {
                webView.goBackOrForward(i - webBFL.getCurrentIndex());
            } else {
                finish();
            }

            return true;
        }

        return false;
    }

    protected void processContent(Content content) {
        if (content == null) {
            return;
        }

        Content contentDB = db.selectContentById(content.getUrl().hashCode());
        if (contentDB != null) {
            content.setStatus(contentDB.getStatus())
                    .setImageFiles(contentDB.getImageFiles())
                    .setDownloadDate(contentDB.getDownloadDate());
        }
        db.insertContent(content);

        StatusContent contentStatus = content.getStatus();
        if (contentStatus != StatusContent.DOWNLOADED
                && contentStatus != StatusContent.DOWNLOADING) {
            currentContent = content;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showFab(fabDownload);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideFab(fabDownload);
                }
            });
        }
        if (contentStatus == StatusContent.DOWNLOADED
                || contentStatus == StatusContent.ERROR) {
            currentContent = content;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showFab(fabRead);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideFab(fabRead);
                }
            });
        }
    }

    protected class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            webViewIsLoading = true;
            fabRefreshOrStop.setImageResource(R.drawable.ic_action_stop);
            fabRefreshOrStop.show();
            fabDownloads.show();
            hideFab(fabDownload);
            hideFab(fabRead);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            webViewIsLoading = false;
            fabRefreshOrStop.setImageResource(R.drawable.ic_action_refresh);
        }
    }
}