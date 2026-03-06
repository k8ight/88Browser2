package netai.webbrowser.WebBrowser;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.*;
import android.print.*;
import android.provider.MediaStore;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.webkit.*;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsetsController;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    EditText ed1;
    Button bt2, bt3, menubtn, bt5, prnt, psrc, tabsBtn;
    ImageButton back, fwd, Refresh, downlod;
    ProgressBar pbar;
    View menuv;
    RelativeLayout webContainer;
    Dialog tabsDialog;
    ArrayList<WebView> tabs = new ArrayList<>();
    int currentTab = 0;
    HashMap<String,String> extraHeaders = new HashMap<>();
    View menuOverlay;
    String Address;
    String currentUrl;

    private static final int FILE_CHOOSER_REQUEST = 1001;
    private static final int CAMERA_PERMISSION_REQUEST = 2001;

    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraUri;
    private boolean pendingFileChooser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();

        WindowCompat.setDecorFitsSystemWindows(window, true);

        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(window, window.getDecorView());

        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(false);


        ed1 = findViewById(R.id.editText);
        bt2 = findViewById(R.id.button4);
        bt3 = findViewById(R.id.button2);
        menubtn = findViewById(R.id.menubtn);
        bt5 = findViewById(R.id.button5);
        prnt = findViewById(R.id.print);
        psrc = findViewById(R.id.psrc);
        tabsBtn = findViewById(R.id.tabs);
        menuOverlay = findViewById(R.id.menu_overlay);
        back = findViewById(R.id.back);
        fwd = findViewById(R.id.fwd);
        Refresh = findViewById(R.id.refresh);
        downlod = findViewById(R.id.downlod);

        menuv = findViewById(R.id.MenuView);
        pbar = findViewById(R.id.progressBar1);
        webContainer = findViewById(R.id.web_container);
        extraHeaders.put("DNT","1");
        Address = "file:///android_asset/indexx.html";

        createNewTab(Address);

        bt2.setBackgroundResource(R.drawable.ic_lock);

        tabsBtn.setOnClickListener(v -> openTabsWindow());

        menubtn.setOnClickListener(v -> {

            if(menuv.getVisibility() == View.VISIBLE){

                menuv.setVisibility(View.GONE);
                menuOverlay.setVisibility(View.GONE);

            }else{

                menuv.setVisibility(View.VISIBLE);
                menuOverlay.setVisibility(View.VISIBLE);
            }
        });

        menuOverlay.setOnClickListener(v -> {

            menuv.setVisibility(View.GONE);
            menuOverlay.setVisibility(View.GONE);

        });

        bt3.setOnClickListener(v -> loadUrl("file:///android_asset/indexx.html"));

        ed1.setOnEditorActionListener((v, actionId, event) -> {

            if(actionId == EditorInfo.IME_ACTION_SEARCH){

                String input = ed1.getText().toString();

                if(input.startsWith("http"))
                    loadUrl(input);
                else if(input.contains("."))
                    loadUrl("http://" + input);
                else
                    loadUrl("https://duckduckgo.com/?q="+input);
            }

            return true;
        });
        bt2.setOnClickListener(v -> {

            WebView web = getCurrentWeb();
            if(web == null) return;

            String url = web.getUrl();

            if(url != null && url.startsWith("https")){
                showCertificateInfo();
            }else{
                Toast.makeText(MainActivity.this,
                        "Connection is not secure",
                        Toast.LENGTH_SHORT).show();
            }
        });


        back.setOnClickListener(v -> {
            WebView w = getCurrentWeb();
            if(w.canGoBack()) w.goBack();
        });

        fwd.setOnClickListener(v -> {
            WebView w = getCurrentWeb();
            if(w.canGoForward()) w.goForward();
        });

        Refresh.setOnClickListener(v -> getCurrentWeb().reload());

        downlod.setOnClickListener(v ->
                startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS))
        );

        bt5.setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id="+getPackageName())))
        );

        prnt.setOnClickListener(v -> createWebPrintJob(getCurrentWeb()));

        psrc.setOnClickListener(v -> {
            WebView w = getCurrentWeb();
            w.loadUrl("view-source:"+w.getUrl());
        });

        if (savedInstanceState != null) {
            pendingFileChooser = savedInstanceState.getBoolean("pendingFileChooser", false);
        }
    }
    @Override
    public void onUserLeaveHint() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            PictureInPictureParams params =
                    new PictureInPictureParams.Builder().build();

            enterPictureInPictureMode(params);
        }
    }
    /* ---------------- TAB SYSTEM ---------------- */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("pendingFileChooser", pendingFileChooser);
    }
    public void openTabsWindow() {

        // If already open, do nothing
        if(tabsDialog != null && tabsDialog.isShowing()){
            return;
        }

        tabsDialog = new Dialog(this);
        tabsDialog.setContentView(R.layout.tabs_view);

        GridView grid = tabsDialog.findViewById(R.id.tabs_grid);
        Button newTab = tabsDialog.findViewById(R.id.new_tab_btn);
        String current = getCurrentWeb().getUrl();

        if(current != null && current.startsWith("file:///android_asset/")){
            newTab.setEnabled(false);
            newTab.setAlpha(0.4f); // visually disabled
        }else{
            newTab.setEnabled(true);
        }
        TabsAdapter adapter = new TabsAdapter();
        grid.setAdapter(adapter);
        grid.invalidateViews();

        newTab.setOnClickListener(v -> {

            createNewTab("file:///android_asset/indexx.html");

            if(tabsDialog != null)
                tabsDialog.dismiss();
        });

        grid.setOnItemClickListener((parent, view, pos, id) -> {

            int realIndex = adapter.visibleTabs.get(pos);

            currentTab = realIndex;

            webContainer.removeAllViews();
            webContainer.addView(tabs.get(realIndex));

            if(tabsDialog != null)
                tabsDialog.dismiss();
        });

        tabsDialog.show();
    }

    public void createNewTab(String url){

        WebView web = new WebView(this);

        RelativeLayout.LayoutParams params =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                );

        web.setLayoutParams(params);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(false);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setSupportMultipleWindows(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccessFromFileURLs(false);
        ws.setAllowUniversalAccessFromFileURLs(false);
        ws.setSavePassword(false);
        // Private session style cache
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setSaveFormData(false);
        ws.setDatabaseEnabled(false);

        ws.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ws.setSafeBrowsingEnabled(true);
        }
        web.setWebChromeClient(new BrowserChromeClient());
        web.setWebViewClient(new BrowserClient());
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(web, false);
        web.setDownloadListener((url1,userAgent,contentDisposition,mimetype,contentLength)->{

            DownloadManager.Request request =
                    new DownloadManager.Request(Uri.parse(url1));

            request.setMimeType(mimetype);

            // important for authenticated downloads
            request.addRequestHeader(
                    "cookie",
                    CookieManager.getInstance().getCookie(url1)
            );

            request.addRequestHeader("User-Agent", userAgent);

            request.setTitle(
                    URLUtil.guessFileName(url1,contentDisposition,mimetype)
            );

            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );

            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url1,contentDisposition,mimetype)
            );

            DownloadManager dm =
                    (DownloadManager)getSystemService(DOWNLOAD_SERVICE);

            dm.enqueue(request);

            Toast.makeText(
                    MainActivity.this,
                    "Downloading...",
                    Toast.LENGTH_LONG
            ).show();
        });

        web.loadUrl(url, extraHeaders);

        tabs.add(web);
        currentTab = tabs.size() - 1;

        webContainer.removeAllViews();
        webContainer.addView(web);

        updateTabCounter();
    }

    public void updateTabCounter(){
        tabsBtn.setText(String.valueOf(tabs.size()));
    }

    public WebView getCurrentWeb(){
        return tabs.get(currentTab);
    }

    public void loadUrl(String url){
        getCurrentWeb().loadUrl(url);
    }

    /* ---------------- FILE UPLOAD + CAMERA ---------------- */

    class BrowserChromeClient extends WebChromeClient {

        @Override
        public boolean onShowFileChooser(
                WebView webView,
                ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams
        ) {

            MainActivity.this.filePathCallback = filePathCallback;

            // Check camera permission
            if (ContextCompat.checkSelfPermission(
                    MainActivity.this,
                    android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED) {

                pendingFileChooser = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_PERMISSION_REQUEST
                    );
                }

                return true;
            }

            openFileChooser();

            return true;
        }

        public void onProgressChanged(WebView view, int progress) {
            pbar.setVisibility(View.VISIBLE);
            pbar.setProgress(progress);

            if (progress == 100) {
                pbar.setVisibility(View.GONE);
            }
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == FILE_CHOOSER_REQUEST) {

            Uri[] results = null;

            if (resultCode == RESULT_OK) {

                if (data != null && data.getData() != null) {
                    results = new Uri[]{data.getData()};
                } else if (cameraUri != null) {
                    results = new Uri[]{cameraUri};
                }
            }

            if (filePathCallback != null)
                filePathCallback.onReceiveValue(results);

            filePathCallback = null;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (pendingFileChooser) {
                    pendingFileChooser = false;
                    openFileChooser();
                }

            } else {

                Toast.makeText(
                        MainActivity.this,
                        "Camera permission required",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
    private void openFileChooser() {

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = new File(
                getExternalCacheDir(),
                "camera_" + System.currentTimeMillis() + ".jpg"
        );

        cameraUri = androidx.core.content.FileProvider.getUriForFile(
                MainActivity.this,
                getPackageName() + ".provider",
                photoFile
        );

        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
        fileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        fileIntent.setType("*/*");

        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        chooser.putExtra(Intent.EXTRA_INTENT, fileIntent);
        chooser.putExtra(Intent.EXTRA_TITLE, "Select File");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

        startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
    }
    /* ---------------- WEBVIEW CLIENT ---------------- */

    class BrowserClient extends WebViewClient{

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon){

            // Do not show asset page path
            if(url.startsWith("file:///android_asset/")){
                ed1.setText("");
            }else{
                ed1.setText(url);
            }
        }

        @Override
        public void onReceivedSslError(WebView view,
                                       final SslErrorHandler handler,
                                       SslError error){

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Invalid SSL Certificate. Continue?");
            builder.setPositiveButton("Continue",(d,w)->handler.proceed());
            builder.setNegativeButton("Cancel",(d,w)->handler.cancel());
            builder.show();
        }

        @Override
        public void onPageFinished(WebView view,String url){

            currentUrl = url;

            // -------- ASSET PAGE --------
            if(url.startsWith("file:///android_asset/")){

                ed1.setText(""); // blank search bar

                bt2.setBackgroundResource(R.drawable.ic_lock);

            }

            // -------- HTTPS --------
            else if(URLUtil.isHttpsUrl(url)){

                ed1.setText(url);

                bt2.setBackgroundResource(R.drawable.ic_lock);

            }

            // -------- HTTP --------
            else{

                ed1.setText(url);

                bt2.setBackgroundResource(R.drawable.ic_unlock);

            }
        }
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {

            String url = request.getUrl().toString().toLowerCase();

            if(isTracker(url)){
                return new WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        new java.io.ByteArrayInputStream("".getBytes())
                );
            }

            return super.shouldInterceptRequest(view, request);
        }
        public boolean isTracker(String url){

            String[] trackers = {
                    "google-analytics",
                    "doubleclick",
                    "googletagmanager",
                    "facebook.net",
                    "facebook.com/tr",
                    "mixpanel",
                    "hotjar",
                    "segment.io",
                    "amplitude",
                    "branch.io",
                    "scorecardresearch",
                    "adsystem",
                    "adservice",
                    "googlesyndication",
                    "adservice.google",
                    "taboola",
                    "outbrain",
                    "yandex",
                    "adnxs",
                    "amazon-adsystem"
            };

            for(String t : trackers){
                if(url.contains(t))
                    return true;
            }

            return false;
        }
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();

            if (handleExternalApps(url)) {
                return true;
            }

            return false;

        }
    }
    public boolean isPdf(String url){

        if(url == null) return false;

        url = url.toLowerCase();

        // Direct PDF files
        if(url.endsWith(".pdf")) return true;

        // PDF with query parameters
        if(url.contains(".pdf?")) return true;

        // DOMPDF / report generators
        if(url.contains("format=pdf")) return true;
        if(url.contains("type=pdf")) return true;

        // Some generators use download parameter
        if(url.contains("download=pdf")) return true;

        return false;
    }
    /* ---------------- TAB THUMBNAILS ---------------- */
    @Override
    protected void onDestroy() {

        super.onDestroy();

        for(WebView w : tabs){
            try{
                w.clearHistory();
                w.clearCache(true);
                w.clearFormData();
                w.destroy();
            }catch(Exception e){}
        }
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(null);
        cookieManager.flush();
        tabs.clear();
    }
    public Bitmap getTabThumbnail(WebView web){

        try{

            String url = web.getUrl();

            // Do not capture asset home page
            if(url != null && url.startsWith("file:///android_asset/")){
                return null;
            }

            int width = web.getWidth();
            int height = web.getHeight();

            if(width <= 0 || height <= 0)
                return null;

            Bitmap bmp = Bitmap.createBitmap(width,height,Bitmap.Config.RGB_565);

            Canvas canvas = new Canvas(bmp);
            web.draw(canvas);

            return Bitmap.createScaledBitmap(bmp,300,400,true);

        }catch(Exception e){
            return null;
        }
    }

    /* ---------------- TABS ADAPTER ---------------- */

    class TabsAdapter extends BaseAdapter {

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);

        ArrayList<Integer> visibleTabs = new ArrayList<>();

        TabsAdapter(){

            visibleTabs.clear();

            for(int i=0;i<tabs.size();i++){

                WebView w = tabs.get(i);
                String url = w.getUrl();

                if(url == null || !url.startsWith("file:///android_asset/")){
                    visibleTabs.add(i);
                }
            }
        }

        @Override
        public int getCount() {
            return visibleTabs.size();
        }

        @Override
        public Object getItem(int position) {
            return tabs.get(visibleTabs.get(position));
        }

        @Override
        public long getItemId(int position) {
            return visibleTabs.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){

            if(convertView == null)
                convertView = inflater.inflate(R.layout.tab_item,parent,false);

            ImageView preview = convertView.findViewById(R.id.tab_preview);
            ImageButton close = convertView.findViewById(R.id.tab_close);
            TextView urlText = convertView.findViewById(R.id.tab_url);

            // get correct tab index
            int realIndex = visibleTabs.get(position);

            if(realIndex >= tabs.size())
                return convertView;

            WebView web = tabs.get(realIndex);

            // highlight active tab
            if(realIndex == currentTab){
                convertView.setAlpha(1f);
            }else{
                convertView.setAlpha(0.7f);
            }

            // thumbnail
            Bitmap thumb = getTabThumbnail(web);

            if(thumb != null)
                preview.setImageBitmap(thumb);
            else
                preview.setImageResource(R.drawable.ic_home);

            // url text
            String url = web.getUrl();

            if(url != null){
                urlText.setText(
                        url.replace("https://","")
                                .replace("http://","")
                );
            }

            /* ---------------- TAB CLICK ---------------- */

            convertView.setOnClickListener(v -> {

                currentTab = realIndex;

                webContainer.removeAllViews();
                webContainer.addView(tabs.get(realIndex));

                if(tabsDialog != null)
                    tabsDialog.dismiss();
            });

            /* ---------------- CLOSE TAB ---------------- */

            close.setOnClickListener(v -> {

                if(realIndex >= tabs.size()) return;

                tabs.remove(realIndex);

                // adjust current tab
                if(currentTab >= tabs.size())
                    currentTab = tabs.size()-1;

                if(tabs.size()==0){
                    createNewTab("file:///android_asset/indexx.html");
                }else{
                    webContainer.removeAllViews();
                    webContainer.addView(tabs.get(currentTab));
                }

                // rebuild visible tab index
                visibleTabs.clear();

                for(int i=0;i<tabs.size();i++){

                    WebView w = tabs.get(i);
                    String u = w.getUrl();

                    if(u == null || !u.startsWith("file:///android_asset/")){
                        visibleTabs.add(i);
                    }
                }

                notifyDataSetChanged();
                updateTabCounter();
            });

            return convertView;
        }
    }

    /* ---------------- PRINT ---------------- */
    public void showCertificateInfo() {

        WebView web = getCurrentWeb();

        if(web == null) return;

        SslCertificate cert = web.getCertificate();

        if(cert == null){
            Toast.makeText(this,"No certificate information available",Toast.LENGTH_LONG).show();
            return;
        }

        SslCertificate.DName issuedTo = cert.getIssuedTo();
        SslCertificate.DName issuedBy = cert.getIssuedBy();

        String message =
                "Issued To:\n" +
                        "Common Name: " + issuedTo.getCName() + "\n" +
                        "Organization: " + issuedTo.getOName() + "\n\n" +

                        "Issued By:\n" +
                        "Common Name: " + issuedBy.getCName() + "\n" +
                        "Organization: " + issuedBy.getOName() + "\n\n" +

                        "Valid From:\n" + cert.getValidNotBeforeDate() + "\n\n" +
                        "Valid Until:\n" + cert.getValidNotAfterDate();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Connection Secure");
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);

        builder.show();
    }
    public void createWebPrintJob(WebView view){

        PrintManager printManager =
                (PrintManager)getSystemService(Context.PRINT_SERVICE);

        PrintDocumentAdapter printAdapter =
                view.createPrintDocumentAdapter(view.getTitle());

        printManager.print("Browser Print",
                printAdapter,
                new PrintAttributes.Builder().build());
    }

    /* ---------------- BACK BUTTON ---------------- */

    public boolean onKeyDown(int keyCode,KeyEvent event){

        WebView w = getCurrentWeb();

        if(keyCode==KeyEvent.KEYCODE_BACK && w.canGoBack()){
            w.goBack();
            return true;
        }

        return super.onKeyDown(keyCode,event);
    }
    private boolean handleExternalApps(String url) {

        Uri uri = Uri.parse(url);
        String lower = url.toLowerCase();

        try {

            // INTENT LINKS (Android apps)
            if (lower.startsWith("intent://")) {

                Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                } else {

                    String fallback = intent.getStringExtra("browser_fallback_url");

                    if (fallback != null) {
                        getCurrentWeb().loadUrl(fallback);
                    }
                }

                return true;
            }

            // Tel / Email / SMS
            if (lower.startsWith("tel:")
                    || lower.startsWith("mailto:")
                    || lower.startsWith("sms:")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // WhatsApp
            if (lower.contains("wa.me")
                    || lower.contains("api.whatsapp.com")
                    || lower.startsWith("whatsapp://")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // Telegram
            if (lower.startsWith("tg://")
                    || lower.contains("t.me")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // UPI Payments
            if (lower.startsWith("upi://")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // Google Maps
            if (lower.startsWith("geo:")
                    || lower.contains("maps.google")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // Play Store
            if (lower.startsWith("market://")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // Zoom
            if (lower.startsWith("zoommtg://")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // Instagram
            if (lower.contains("instagram.com")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

            // YouTube
            if (lower.contains("youtube.com")
                    || lower.contains("youtu.be")) {

                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Required app not installed",
                    Toast.LENGTH_SHORT
            ).show();

            return true;
        }

        return false;
    }
}