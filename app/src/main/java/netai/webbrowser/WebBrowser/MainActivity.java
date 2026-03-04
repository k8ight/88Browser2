package netai.webbrowser.WebBrowser;

import android.app.*;
import android.content.*;
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
import androidx.core.content.ContextCompat;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import android.os.Build;
import android.view.Window;
import android.view.WindowInsetsController;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

public class MainActivity extends Activity {

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

    ValueCallback<Uri[]> filePathCallback;
    Uri cameraUri;

    static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        /* Set status bar color */
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimary));

        /* Remove light status bar (this is causing white) */
        View decor = window.getDecorView();
        decor.setSystemUiVisibility(
                decor.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );


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
    }

    /* ---------------- TAB SYSTEM ---------------- */

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

        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setBuiltInZoomControls(true);
        ws.setDisplayZoomControls(false);
        ws.setSupportMultipleWindows(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setSavePassword(false);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        // Private session style cache
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);

        ws.setUserAgentString(
                "Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        );

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
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {

            MainActivity.this.filePathCallback = filePathCallback;

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File photoFile = new File(getExternalCacheDir(),
                    "camera_" + System.currentTimeMillis() + ".jpg");

            cameraUri = Uri.fromFile(photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);

            Intent fileIntent = new Intent(Intent.ACTION_GET_CONTENT);
            fileIntent.setType("*/*");

            Intent chooser = Intent.createChooser(fileIntent, "Select File");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});

            startActivityForResult(chooser, FILE_CHOOSER_REQUEST);

            return true;
        }

        public void onProgressChanged(WebView view,int progress){
            pbar.setVisibility(View.VISIBLE);
            pbar.setProgress(progress);
            if(progress==100) pbar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == FILE_CHOOSER_REQUEST){

            Uri[] results = null;

            if(resultCode == RESULT_OK){

                if(data != null)
                    results = new Uri[]{data.getData()};
                else
                    results = new Uri[]{cameraUri};
            }

            if(filePathCallback != null)
                filePathCallback.onReceiveValue(results);

            filePathCallback = null;
        }

        super.onActivityResult(requestCode,resultCode,data);
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
                    "adservice"
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

            if (url == null) return false;

            String lower = url.toLowerCase();

            // -------- PDF HANDLING --------
            if (isPdf(lower)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Open PDF");

                builder.setItems(new String[]{
                        "Preview in Browser",
                        "Open with PDF App",
                        "Download"
                }, (dialog, which) -> {

                    switch (which) {

                        case 0:
                            view.loadUrl("https://docs.google.com/gview?embedded=true&url=" + url);
                            break;

                        case 1:
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.parse(url), "application/pdf");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                                startActivity(Intent.createChooser(intent, "Open PDF with"));
                            } catch (Exception e) {
                                Toast.makeText(MainActivity.this,
                                        "No PDF viewer installed",
                                        Toast.LENGTH_LONG).show();
                            }
                            break;

                        case 2:

                            DownloadManager.Request req =
                                    new DownloadManager.Request(Uri.parse(url));

                            req.setMimeType("application/pdf");

                            req.addRequestHeader(
                                    "cookie",
                                    CookieManager.getInstance().getCookie(url)
                            );

                            req.addRequestHeader("User-Agent",
                                    view.getSettings().getUserAgentString());

                            req.setNotificationVisibility(
                                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                            );

                            req.setDestinationInExternalPublicDir(
                                    Environment.DIRECTORY_DOWNLOADS,
                                    URLUtil.guessFileName(url, null, "application/pdf")
                            );

                            DownloadManager dm =
                                    (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

                            dm.enqueue(req);

                            Toast.makeText(MainActivity.this,
                                    "Downloading PDF",
                                    Toast.LENGTH_LONG).show();
                            break;
                    }
                });

                builder.show();
                return true;
            }

            // -------- PHONE --------
            if (lower.startsWith("tel:")) {
                startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse(url)));
                return true;
            }

            // -------- EMAIL --------
            if (lower.startsWith("mailto:")) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                return true;
            }

            // -------- SMS --------
            if (lower.startsWith("sms:")) {
                startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse(url)));
                return true;
            }

            // -------- WHATSAPP --------
            if (lower.startsWith("whatsapp://") || lower.startsWith("https://wa.me/")) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,
                            "WhatsApp not installed",
                            Toast.LENGTH_LONG).show();
                }
                return true;
            }

            // -------- MAPS --------
            if (lower.startsWith("geo:") || lower.contains("maps.google.com")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }

            // -------- PLAY STORE --------
            if (lower.startsWith("market://")) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                return true;
            }

            // -------- INTENT LINKS --------
            if (lower.startsWith("intent://")) {

                try {

                    Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {

                        String fallback = intent.getStringExtra("browser_fallback_url");

                        if (fallback != null)
                            view.loadUrl(fallback, extraHeaders);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return true;
            }

            if(lower.startsWith("file://")){
                return false;
            }

            // block unknown schemes
            if(!lower.startsWith("http://") && !lower.startsWith("https://")){
                return true;
            }

            // -------- NORMAL WEB PAGE --------
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

            Bitmap bmp = Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);

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
}