package com.projectescicles.educem.skinscanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.projectescicles.educem.skinscanner.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_CHOOSER_REQUEST = 1887;
    private static final int MY_PERMISSIONS_REQUEST = 1888;

    //Select whether you want to upload multiple files (set 'true' for yes)
    private static final boolean MULTIPLE_FILES = false;

    private WebView mWebView;
    private String mCustomURL = "https://projectescicles.educem.com/";

    private ValueCallback<Uri> mUM;
    private ValueCallback<Uri[]> mUMA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        checkForAndAskForPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /*switch (requestCode) {
            case MY_PERMISSIONS_REQUEST:
               if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    createWebView();
                    return;
                } else {
                    //You did not accept the request can not use the functionality.
                }
                break;
            }*/
        //}
        // other 'case' lines to check for other
        // permissions this app might request

        // We show the Webview (We don't care if permissions are granted)
        createWebView();
    }

    private void createWebView() {
        mWebView = (WebView) findViewById(R.id.webview);
        setUpWebViewDefaults(mWebView);
        mWebView.loadUrl(mCustomURL);
        mWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {

                // getActivity().
                MainActivity.this.runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        // Below isn't necessary, however you might want to:
                        // 1) Check what the site is and perhaps have a blacklist
                        // 2) Have a pop up for the user to explicitly give permission
                        //if(request.getOrigin().toString().equals("https://projectescicles.educem.com/") ||
                        //request.getOrigin().toString().equals("https://projectescicles.educem.com/")) {
                        request.grant(request.getResources());
                        //} else {
                        //request.deny();
                        //}
                    }
                });
            }

            /*
             * openFileChooser is not a public Android API and has never been part of the SDK.
             */
            //handling input[type="file"] requests for android API 16+
            @SuppressLint("ObsoleteSdkInt")
            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                mUM = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                if (MULTIPLE_FILES && Build.VERSION.SDK_INT >= 18) {
                    i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(Intent.createChooser(i, "Selecciona una foto"), FILE_CHOOSER_REQUEST);
            }

            //Handling input[type="file"] requests for android API 21+
            @SuppressLint("InlinedApi")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (checkStoragePermission()) {
                    if (mUMA != null) {
                        mUMA.onReceiveValue(null);
                    }
                    mUMA = filePathCallback;

                    Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    contentSelectionIntent.setType("*/*");

                    if (MULTIPLE_FILES) {
                        contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    }

                    Intent[] intentArray;
                    intentArray = new Intent[0];

                    Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                    chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                    chooserIntent.putExtra(Intent.EXTRA_TITLE, "Selecciona una foto");
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                    startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST);
                    return true;
                } else {
                    return false;
                }
            }
        });
    }

    private void checkForAndAskForPermissions() {
        int permissionCAMERA = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        boolean storagePermission = checkStoragePermission();

        List<String> listPermissionsNeeded = new ArrayList<>();

        if (permissionCAMERA != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }

        if (!storagePermission) {
            listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            // If permissions are not set, shows a popup asking about them.
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MY_PERMISSIONS_REQUEST);
        }
        else {
            // If permissions are set, creates de WebView automatically.
            createWebView();
        }
    }

    private void setUpWebViewDefaults(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);

        if(Build.VERSION.SDK_INT >= 21){
            settings.setMixedContentMode(0);
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }else if(Build.VERSION.SDK_INT >= 19){
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            // Enable remote debugging via chrome://inspect
            WebView.setWebContentsDebuggingEnabled(true);
        }else {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(true);

        webView.clearCache(true);
        webView.clearHistory();
        webView.setWebViewClient(new Callback());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent){
        super.onActivityResult(requestCode, resultCode, intent);
        if(Build.VERSION.SDK_INT >= 21){
            Uri[] results = null;
            //checking if response is positive
            if(resultCode== Activity.RESULT_OK){
                if(requestCode == FILE_CHOOSER_REQUEST){
                    if(null == mUMA){
                        return;
                    }

                    String dataString = intent.getDataString();
                    if(dataString != null){
                        results = new Uri[]{Uri.parse(dataString)};
                    } else {
                        if(MULTIPLE_FILES) {
                            if (intent.getClipData() != null) {
                                final int numSelectedFiles = intent.getClipData().getItemCount();
                                results = new Uri[numSelectedFiles];
                                for (int i = 0; i < numSelectedFiles; i++) {
                                    results[i] = intent.getClipData().getItemAt(i).getUri();
                                }
                            }
                        }
                    }
                }
            }
            mUMA.onReceiveValue(results);
            mUMA = null;
        }else{
            if(requestCode == FILE_CHOOSER_REQUEST){
                if(null == mUM) return;
                Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
                mUM.onReceiveValue(result);
                mUM = null;
            }
        }
    }

    //callback reporting if error occurs
    public class Callback extends WebViewClient{
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl){
            Toast.makeText(getApplicationContext(), "S'ha produït un error al carregar l'aplicació!", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkStoragePermission(){
        int storagePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else {
            return true;
        }
    }

    //back/down key handling
    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event){
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode){
                case KeyEvent.KEYCODE_BACK:
                    if(mWebView.canGoBack()){
                        mWebView.goBack();
                    }else{
                        finish();
                    }
                    return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        super.onConfigurationChanged(newConfig);
    }
}
