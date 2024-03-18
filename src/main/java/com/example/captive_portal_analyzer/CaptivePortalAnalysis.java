package com.example.captive_portal_analyzer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Bundle;
import android.os.FileUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.captive_portal_analyzer.volley.NetworkRequest;
import com.google.gson.Gson;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HostnameVerifier;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CaptivePortalAnalysis extends AppCompatActivity {
    private static final String TAG = "FindCaptivePortalActivity";

    Context context = this;

    private static final String URL = "http://connectivitycheck.gstatic.com/generate_204";
    WifiManager wifiManager;
    private String wifiName;

    private WebView webView;
    String requestQueue = "";
    String currentUrl = "";
    private final CookieManager cookieManager = CookieManager.getInstance();

    SimpleDateFormat time = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
    Date now = new Date();
    String dateStr = time.format(now);

    private String path;
    private int round = 1;
    private int roundPP = 1;
    private int exitCount = 0;
    boolean zipped = false;

    private static final String serverIPAddress = "192.168.0.106";

    private static final List<String> keywordsPrivacy = new ArrayList<>(Arrays.asList(
            "поверителност", "политика за данни", "политика лд", "лични данни", "бисквитки",
            "условия", "soukromí", "používání dat", "ochrana dat", "osobních údajů", "cookie",
            "personlige oplysninger", "datapolitik", "privatliv", "personoplysninger",
            "regler om fortrolighed", "personlige data", "persondata", "datenschutz",
            "datenrichtlinie", "privatsphäre", "απορρήτου", "απόρρητο", "προσωπικά δεδομένα",
            "εμπιστευτικότητας", "ιδιωτικότητας", "πολιτική δεδομένων", "προσωπικών δεδομένων",
            "privacy", "data policy", "data protection", "privacidad", "datos personales",
            "política de datos", "privaatsus", "konfidentsiaalsus", "isikuandmete", "andmekaitse",
            "küpsis", "yksityisyy", "tietokäytäntö", "tietosuoja", "henkilötie", "eväste",
            "confidentialite", "confidentialité", "vie privée", "vie privee",
            "données personnelles", "donnees personnelles", "utilisation des données",
            "utilisation des donnees", "rgpd", "príobháideach", "cosaint sonraí", "cosanta sonraí",
            "fianáin", "fianán", "privatnost", "osobnih podataka", "upotrebi podataka",
            "zaštita podataka", "obradi podataka", "kolačić", "adatvédel", "adatkezel",
            "személyes adatok védelme", "riservatezza", "privatezza", "dati personali", "privātum",
            "sīkdat", "privatum", "konfidencialumas", "asmens duomenų", "duomenų sauga", "slapuk",
            "gegevensbescherming", "gegevensbeleid", "prywatnoś", "dane osobowe",
            "przetwarzanie danych", "zasady przetwarzania danych", "zasady dotyczące danych",
            "ochrona danych", "privacidade", "dados pessoais", "política de dados", "rpgd",
            "direitos do titular dos dados", "confidențialitate", "confidentialitate",
            "protecția datelor", "súkromi", "využívania údajov", "ochrana údajov",
            "osobných údajov", "zásady ochrany osobných", "osobné údaje", "gdpr", "zasebnost",
            "osebnih podatkov", "piškotki", "varstvo podatkov", "sekretess", "datapolicy",
            "personuppgifter", "integritet", "kakor", "informationskapslar"
    ));

    class JSInterface {
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void processHTML(String html){
            String pathUse = path + roundPP + "/";


            new Thread(() -> {
                String url = currentUrl;
                if (url.length() > 150) {
                    url = url.substring(0, 150);
                }
                String sourceFilename = url + "_SourceCode_.html";
                saveText(html, sourceFilename, pathUse);
                downloadPPs(html, pathUse);
            }).start();
            roundPP += 1;
        }
    }


    private final WebViewClient webClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap facIcon) {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view,
                                                          WebResourceRequest request) {


            String method = request.getMethod();
            String requestHeaders = request.getRequestHeaders().toString();
            String url = request.getUrl().toString();
            String hasGesture = String.valueOf(request.hasGesture());
            String isForMainFrame = String.valueOf(request.isForMainFrame());
            String isRedirect = String.valueOf(request.isRedirect());

            Map<String, String> request_save = new HashMap<>();
            request_save.put("method", method);
            request_save.put("requestHeaders", requestHeaders);
            request_save.put("url", url);
            request_save.put("hasGesture", hasGesture);
            request_save.put("isForMainFrame", isForMainFrame);
            request_save.put("isRedirect", isRedirect);
            Gson gson = new Gson();
            String request_json = gson.toJson(request_save);
            request_json = request_json + ",\n\n";

            requestQueue = requestQueue.concat(request_json);
            return super.shouldInterceptRequest(view, request);
        }
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            currentUrl = url;
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

            // Some websites have more then on iframe leading to calling this function more then once!
            if (view.getProgress() == 100) {
                runOnUiThread(() -> webView.loadUrl("javascript:window.site.processHTML(document.getElementsByTagName('html')[0].outerHTML);") );
                Timer timer = new Timer();
                TimerTask startDownloading = new TimerTask() {
                    @Override
                    public void run() {
                        new Thread(() -> saveContent(currentUrl, webView)).start();
                    }
                };
                timer.schedule(startDownloading, 1000);
            }
        }
    };

    private static final HostnameVerifier verifier = (hostname, session) -> hostname.equals(serverIPAddress);

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView.enableSlowWholeDocumentDraw();

        setContentView(R.layout.activity_captive_portal_analysis);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        WifiInfo info = wifiManager.getConnectionInfo();
        wifiName  = info.getSSID().replaceAll("[^a-zA-Z0-9.\\-]", "_");

        path = "/wifiTracker/" + wifiName + "_" + dateStr + "/";

        new Thread(this::prepareNetwork).start();



        // final RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        webView = findViewById(R.id.webView);
        prepareWebView();

    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if (exitCount == 2){
                this.finish();
            }else{
                exitCount += 1;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if  (item.getItemId() == R.id.end) {
            if (round == roundPP) {
                // Entfernen Sie den zipped Check, wenn Sie das ZIP direkt nach dem Ende der Analyse teilen möchten
                if (findViewById(R.id.webView).getVisibility() == View.VISIBLE) {
                    File src = context.getDir("webview", Context.MODE_PRIVATE);
                    dropWebViewProfile(src, path);

                    new Thread(() -> {
                        zip(); // Erstellen Sie das ZIP-Archiv im Hintergrund
                        runOnUiThread(this::shareZipFile); // Teilen Sie das ZIP-Archiv, nachdem es erstellt wurde
                    }).start();
                    clearWebView();
                    //removeWifiSuggestions();
                }
            }else{
                runOnUiThread(() -> Toast.makeText(this, "Not done downloading, please try again", Toast.LENGTH_SHORT).show());
            }
        }
        return true;
    }


    private void clearWebView(){
        cookieManager.removeAllCookies(null);
        WebStorage.getInstance().deleteAllData();
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearSslPreferences();
        cookieManager.flush();

        File webViewDataSource = context.getDir("webview", Context.MODE_PRIVATE);
        deleteProfile(webViewDataSource);
    }

    private void removeWifiSuggestions(){
        try {
            List<WifiNetworkSuggestion> emptySuggestionsList = new ArrayList<>();
            wifiManager.removeNetworkSuggestions(emptySuggestionsList);
        }catch (Exception e){
            Log.e(TAG, "Error removingWifiSuggestions" + e.toString());
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        // remove all suggestions made by the application, so they do not get stuck to wifi
        // because of bug in google wifi does not get disconnected directly
        clearWebView();
        // Not needed as feature of connecting is disabled
        //removeWifiSuggestions();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void prepareWebView(){
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadWithOverviewMode(true);

        cookieManager.acceptCookie();
        cookieManager.acceptThirdPartyCookies(webView);


        /* Register a new JavaScript interface called site */
        webView.addJavascriptInterface(new JSInterface(), "site");

        webView.setWebViewClient(webClient);
    }

    private void prepareNetwork(){
        final android.net.NetworkRequest request =
                new android.net.NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();

        final ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                connectivityManager.bindProcessToNetwork(network);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
            }

        };

        connectivityManager.requestNetwork(request, networkCallback);

        final RequestQueue queue = Volley.newRequestQueue(this);

        NetworkRequest checkConnectionRequest = new NetworkRequest(Request.Method.GET, URL,
                response -> {
                    try {
                        if (response.statusCode == 204) {
                            findViewById(R.id.internetAvailable).setVisibility(View.VISIBLE);
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            // TODO For testing delete please
                            // findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                            // findViewById(R.id.downloadingResources).setVisibility(View.VISIBLE);
                            // webView.loadUrl("https://de.wikipedia.org/wiki/Betty_White");

                        }else {
                            if (response.headers != null) {
                                if (response.headers.containsKey("Location")) {

                                    runOnUiThread(() -> {
                                        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                                        findViewById(R.id.downloadingResources).setVisibility(View.VISIBLE);

                                        webView.loadUrl(response.headers.get("Location"));
                                    });
                                }
                            }
                            if (response.statusCode == 200) {
                                byte[] dataBytes = response.data;
                                String dataString = new String(dataBytes, StandardCharsets.UTF_8);
                                ArrayList<String> links = extractStringsFromString(dataString);

                                if (!links.isEmpty()) {
                                    runOnUiThread(() -> {
                                        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                                        findViewById(R.id.downloadingResources).setVisibility(View.VISIBLE);

                                        webView.loadUrl(links.get(0));
                                    });
                                }
                            }
                        }
                    }catch(Exception e){
                        Log.e(TAG, e.toString());
                        findViewById(R.id.noCaptivePortalFound).setVisibility(View.VISIBLE);
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }
                },
                error -> {
                    try {
                        if (error.networkResponse.headers != null) {
                            runOnUiThread(() ->  {
                                findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                                findViewById(R.id.downloadingResources).setVisibility(View.VISIBLE);

                                webView.loadUrl(error.networkResponse.headers.get("Location"));
                            } );
                        } else {
                            findViewById(R.id.noCaptivePortalFound).setVisibility(View.VISIBLE);
                            findViewById(R.id.progressBar).setVisibility(View.GONE);

                        }
                    }catch (Exception e){
                        Log.e(TAG, e.toString());
                        findViewById(R.id.noCaptivePortalFound).setVisibility(View.VISIBLE);
                        findViewById(R.id.progressBar).setVisibility(View.GONE);
                    }
                });

        queue.add(checkConnectionRequest);

    }

    private void saveContent(String url, WebView view){

        String pathUse = path + round + "/";

        // lengthCheck is mandatory, else you could have an index error
        if (url.length() > 150){
            url = url.substring(0, 150);
        }

        String filenameRequests = url + "_requests_.json";
        requestQueue = requestQueue.substring(0, requestQueue.length() - 3);
        saveText("[" + requestQueue + "]", filenameRequests, pathUse);
        requestQueue = "";

        Bitmap screenBitmap = getBitmap(view);
        if (screenBitmap != null) {
            String screenshotFilename = url + ".jpeg";
            saveScreenshot(screenBitmap, screenshotFilename, pathUse);
            screenBitmap.recycle();
        }

        File src = context.getDir("webview", Context.MODE_PRIVATE);

        dropWebViewProfile(src, pathUse);

        round += 1;

        runOnUiThread(() -> {
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            findViewById(R.id.downloadingResources).setVisibility(View.GONE);

            findViewById(R.id.webView).setVisibility(View.VISIBLE);
        } );
    }

    private void zip(){
        Timer timer = new Timer();

        // Deal with view here, must be on mail UI because this is run from a different thread
        runOnUiThread(() -> {
            findViewById(R.id.webView).setVisibility(View.INVISIBLE);
            findViewById(R.id.end).setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);

        } );

        String DToZip = getFilesDir() + path;
        zipProfile(DToZip);
        File profileToDelete = new File(getFilesDir() + path);
        deleteProfile(profileToDelete);


        TimerTask hideProgressbar = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    findViewById(R.id.b1_upload).setVisibility(View.VISIBLE);
                });

            }
        };
        timer.schedule(hideProgressbar, 1000);
    }

    private void deleteProfile(File profileToDelete){

        File[] entries =  profileToDelete.listFiles();
        if (entries != null) {
            for (File s : entries) {
                if (s.isDirectory()) {
                    deleteProfile(s);
                } else {
                    s.delete();
                }

            }
        }
        profileToDelete.delete();

    }

    private void downloadPPs(String html, String path){
        try {
            Document doc = Jsoup.parse(html);

            Elements links = doc.select("a[href]");
            List<String> ppLinks;
            ppLinks = new ArrayList<>();
            for (int i = 0, linksSize = links.size(); i < linksSize; i++) {
                Element link = links.get(i);
                for (String pp_word : keywordsPrivacy)
                    if ((link.text().toLowerCase().trim().contains(pp_word) || link.attr("href").toLowerCase().trim().contains(pp_word)) && (!ppLinks.contains(link.attr("href")))) {

                        java.net.URL currentBase = new URL(currentUrl);
                        java.net.URL url = new URL(currentBase, link.attr("href"));
                        downloadPPFile(url, path);
                        ppLinks.add(link.attr("href"));
                    }
            }
        }catch (Exception e){
            Log.e(TAG, "DownloadPPs error" + e.toString());
        }
    }

    private void saveText(String content, String filename, String path){
        try{
            filename = filename.replace("http://", "").replace("https://", "").replace("www.", "").replace("/", "_").replace("?", "_");
            filename = filename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            File storagePath = new File(getFilesDir() + path );
            if ( (!storagePath.mkdirs() && !storagePath.isDirectory())) {
                Log.e(TAG, "cannot create folder");
            }
            File file = new File(storagePath, filename);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

    public static Bitmap getBitmap(WebView webView) {

        webView.measure(View.MeasureSpec.makeMeasureSpec(
                View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        webView.layout(0, 0, webView.getMeasuredWidth(), webView.getMeasuredHeight());


        Bitmap bitmap = null;

        try {
            bitmap = Bitmap.createBitmap(webView.getMeasuredWidth(), webView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        }catch(Exception e){
            Log.e(TAG, e.toString());
        }

        if (bitmap != null) {
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            int iHeight = bitmap.getHeight();
            canvas.drawBitmap(bitmap, 0, iHeight, paint);
            webView.draw(canvas);
        }
        return bitmap;

    }

    private void saveScreenshot(Bitmap bitmap, String filename, String path){

        try{
            filename = filename.replace("http://", "").replace("https://", "").replace("www.", "").replace("/", "_").replace("?", "_");
            filename = filename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            File pS = new File(getFilesDir() + path);
            if ((!pS.mkdirs() && !pS.isDirectory())) {
                Log.e(TAG, "cannot create folder");
            }
            File f = new File(pS, filename);
            FileOutputStream fo = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fo);
            fo.close();
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

    private void dropWebViewProfile(File src, String path){

        File d = new File(getFilesDir() + path + "/webViewProfile");

        if ((!d.mkdirs() && !d.isDirectory())) {
            Log.e(TAG, "cannot create folder");
        }

        dropDir(src, d);

    }

    private void dropDir(File src, File dest){
        File[] files = src.listFiles();

        if (files != null && files.length > 0){
            for (File file: files){
                if (file.isDirectory()){
                    File newDir = new File(dest, file.getName());
                    if ((!newDir.mkdirs() && !newDir.isDirectory())) {
                        Log.e(TAG, "cannot create folder");
                    }
                    dropDir(file, newDir);
                }else{
                    File destFile = new File(dest, file.getName());
                    copyFile(file, destFile);
                }
            }
        }
    }

    private void copyFile(File srcFile, File destFile){
        try {
            if (destFile.createNewFile()) {
                FileInputStream in = new FileInputStream(srcFile);
                FileOutputStream out = new FileOutputStream(destFile);
                FileUtils.copy(in, out);
                in.close();
                out.close();
            } else {
                Log.e(TAG, "could not create file" + destFile.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void downloadPPFile(java.net.URL url, String path){
        int countingProgress;
        try {
            URLConnection connection = url.openConnection();
            connection.connect();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream(),
                    8192);

            String filename = url.toString().replace("http://", "").replace("https://", "").replace("www.", "").replace("/", "_").replace("?", "_");
            filename = filename.replaceAll("[^a-zA-Z0-9.\\-]", "_");
            // hashtag at the end causes some problem when fetching data
            if (filename.endsWith("#")) {
                filename = filename.substring(1, filename.length() - 1);
            }
            String extension = ".html";
            if (filename.endsWith(".txt")) {
                extension = ".txt";
            } else if (filename.endsWith(".pdf")) {
                extension = ".pdf";
            }

            if (filename.length() > 150) {
                filename = filename.substring(0, 150) + extension;
            }

            if (!filename.endsWith(extension)) {
                filename = filename + extension;
            }

            File d = new File(getFilesDir() + path + "/ppFiles");

            if ((!d.mkdirs() && !d.isDirectory())) {
                Log.e(TAG, "cannot create folder");
            }

            File file = new File(d, filename);
            FileOutputStream output = new FileOutputStream(file);

            // Output stream
            //OutputStream output = new FileOutputStream(d.getName() + "/" + filename);

            byte[] data = new byte[1024];

            while ((countingProgress = input.read(data)) != -1) {
                // writing data to file
                output.write(data, 0, countingProgress);
            }

            // flushing output
            output.flush();

            // closing streams
            output.close();
            input.close();

        } catch (Exception e) {
            Log.e("DownloadPPs error: ", e.getMessage());
        }
    }

    private void zipProfile(String input){
        try {
            FileOutputStream fileWriter = new FileOutputStream(getFilesDir() + "/wifiTracker/" +  wifiName + "_" + dateStr +".zip");
            ZipOutputStream zip = new ZipOutputStream(fileWriter);
            zipFolder("", input, zip);
            zip.flush();
            zip.close();
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }
    }

    private void zipFolder(String path, String srcFolder, ZipOutputStream zip){
        File folder = new File(srcFolder);
        for (String fileName : Objects.requireNonNull(folder.list())) {
            if (path.equals("")) {
                zipFile(folder.getName(), srcFolder + "/" + fileName, zip);
            } else {
                zipFile(path + "/" + folder.getName(), srcFolder + "/"
                        + fileName, zip);
            }
        }
    }

    private void zipFile(String path, String srcFile, ZipOutputStream zip) {
        try {
            File folder = new File(srcFile);
            if (folder.isDirectory()) {
                zipFolder(path, srcFile, zip);
            } else {
                byte[] buf = new byte[1024];
                int len;
                FileInputStream in = new FileInputStream(srcFile);
                zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    public void upload(View view){
        new Thread(() -> {
            File srcPath = new File(getFilesDir() + "/wifiTracker");
            File[] files = srcPath.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isDirectory()){
                        try {
                            uploadFile(file);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();

        findViewById(R.id.b1_upload).setVisibility(View.GONE);
        findViewById(R.id.b2_deleteUnneededFolders).setVisibility(View.VISIBLE);
        findViewById(R.id.end).setVisibility(View.GONE);
        findViewById(R.id.unneededFilesDelete).setVisibility(View.VISIBLE);

    }

    public void uploadFile(File file) throws IOException {

        OkHttpClient.Builder builder = new OkHttpClient.Builder().hostnameVerifier(verifier);
        builder.connectTimeout(30, TimeUnit.SECONDS);
        builder.readTimeout(30, TimeUnit.SECONDS);

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("token", "sendToken")
                .addFormDataPart("files", file.getName(),
                        RequestBody.create(file, MediaType.parse("application/zip")))
                .build();

        okhttp3.Request requestBuilder = new okhttp3.Request.Builder()
                .url("https://" + serverIPAddress + ":8000/upload")
                .post(requestBody)
                .build();

        builder.addInterceptor(chain -> {
            okhttp3.Request request = chain.request();

            // try the request
            Response response = chain.proceed(request);

            int tryCount = 0;
            int maxLimit = 3;
            while (!response.isSuccessful() && tryCount < maxLimit) {
                response.close();
                tryCount++;

                response = chain.proceed(request);
            }
            if (response.isSuccessful()){
                file.delete();
            }

            return response;
        });

        OkHttpClient client = builder.build();

        client.newCall(requestBuilder).execute();
    }

    public void deleteUnneededFolders(View view){
        new Thread(() -> {
            File srcPath = new File(getFilesDir() + "/wifiTracker");
            File[] files = srcPath.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.getName().endsWith(".zip")){
                        deleteProfile(file);

                    }
                }
            }
        }).start();

        runOnUiThread(() -> {
            findViewById(R.id.b2_deleteUnneededFolders).setVisibility(View.GONE);
            findViewById(R.id.unneededFilesDelete).setVisibility(View.GONE);
            findViewById(R.id.closeApp).setVisibility(View.VISIBLE);
        });
    }

    private ArrayList<String> extractStringsFromString(String text)
    {
        ArrayList<String> links = new ArrayList<>();

        String regex = "\\(?\\b(https?://|www[.]|ftp://)[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);

        while(m.find())
        {
            String urlStr = m.group();

            if (urlStr.startsWith("(") && urlStr.endsWith(")"))
            {
                urlStr = urlStr.substring(1, urlStr.length() - 1);
            }

            links.add(urlStr);
        }

        return links;
    }

    private void shareZipFile() {
        try {
            File zipFile = new File(getFilesDir() + "/wifiTracker/" + wifiName + "_" + dateStr + ".zip");
            Uri contentUri = FileProvider.getUriForFile(this, "com.example.captive_portal_analyzer.fileprovider", zipFile);

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.setType("application/zip");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share ZIP"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sharing zip file.", Toast.LENGTH_SHORT).show();
        }
    }


}