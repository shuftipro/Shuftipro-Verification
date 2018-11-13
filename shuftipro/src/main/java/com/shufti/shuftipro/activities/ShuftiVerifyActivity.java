package com.shufti.shuftipro.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.shuftipro.R;
import com.shufti.shuftipro.cloud.HttpConnectionHandler;
import com.shufti.shuftipro.constants.Constants;
import com.shufti.shuftipro.helpers.IntentHelper;
import com.shufti.shuftipro.listeners.NetworkListener;
import com.shufti.shuftipro.listeners.ReferenceResponseListener;
import com.shufti.shuftipro.models.ShuftiVerificationRequestModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShuftiVerifyActivity extends AppCompatActivity implements NetworkListener, ReferenceResponseListener {

    public static LinearLayout rlLoadingProgress;
    private HashMap<String, String> responseSet;
    private TextView tvProgressLoading;
    private final String TAG = ShuftiVerifyActivity.class.getSimpleName();
    private ShuftiVerificationRequestModel shuftiVerificationRequestModel;
    private JSONObject requestedObject;
    public static boolean requestInProcess = false;
    private final int REQUEST_ID_MULTIPLE_PERMISSIONS = 100;
    private AlertDialog alertDialog;
    private String redirect_demo_url = "https://www.dummyurl.com/";
    private String requestReference = "";

    //Adding webview initializations
    private WebView webView;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final int FILE_CHOOSER_RESULT_CODE = 1;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;


    private static ShuftiVerifyActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_shufti_verify);

        //Set instance of this activtiy
        instance = this;

        tvProgressLoading = findViewById(R.id.tv_title_verify);
        webView = findViewById(R.id.webViewLayout);

        //Initializing UI elements
        rlLoadingProgress = findViewById(R.id.rl_progress_update);
        responseSet = new HashMap<>();
        requestedObject = new JSONObject();

        if (IntentHelper.getInstance().containsKey(Constants.KEY_DATA_MODEL)) {
            shuftiVerificationRequestModel = (ShuftiVerificationRequestModel) IntentHelper.getInstance().getObject(Constants.KEY_DATA_MODEL);
            requestedObject = shuftiVerificationRequestModel.getJsonObject();
        }

        //Return callbacks incase of wrong parameters sending..
        if (shuftiVerificationRequestModel != null) {
            String clientId = shuftiVerificationRequestModel.getClientId();
            if (clientId == null || clientId.isEmpty()) {
                returnErrorCallback("ClientId cannot be empty. Please, provide your client id.", true);
                return;
            }
            String secretKey = shuftiVerificationRequestModel.getSecretKey();
            if (secretKey == null || secretKey.isEmpty()) {
                returnErrorCallback("Secret key cannot be empty. Please, provide your secret key.", true);
                return;
            }
            //If user has given redirect_url then override otherwise add it
            if (requestedObject.has("redirect_url")) {
                try {
                    requestedObject.put("redirect_url", redirect_demo_url);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    requestedObject.put("redirect_url", redirect_demo_url);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }


        if (!requestInProcess) {
            sendRequestToShuftiproServer(requestedObject);
        }
    }


    public static ShuftiVerifyActivity getInstance() {
        return instance;
    }

    private void sendRequestToShuftiproServer(JSONObject requestedObject) {
        requestInProcess = true;
        rlLoadingProgress.setVisibility(View.VISIBLE);

        try {
            String clientId = shuftiVerificationRequestModel.getClientId();
            String secretKey = shuftiVerificationRequestModel.getSecretKey();

            boolean isSubmitted = HttpConnectionHandler.getInstance(clientId, secretKey).executeVerificationRequest(requestedObject,
                    ShuftiVerifyActivity.this, ShuftiVerifyActivity.this);

            if (!isSubmitted) {
                requestInProcess = false;
                responseSet.clear();

                responseSet.put("reference", "");
                responseSet.put("event", "");
                responseSet.put("error", "No Internet Connection");

                showDialog("No Internet Connection",
                        "No Internet",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
                                    shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
                                }
                                if (alertDialog.isShowing()) {
                                    alertDialog.dismiss();
                                }
                                ShuftiVerifyActivity.this.finish();
                            }
                        });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void showPermissionRejectionDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("App won't work without permissions. Please, restart app and give" +
                " access to the permissions.");
        alertDialog.setPositiveButton("Finish", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ShuftiVerifyActivity.this.finish();
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    @Override
    public void successResponse(String result) {
        requestInProcess = false;
        responseSet.clear();

        try {

            JSONObject jsonObject = new JSONObject(result);

            //Starting api response parsing
            String reference = "";
            String event = "";
            String error = "";
            String verification_url = "";
            String verification_result = "";
            String verification_data = "";

            if (jsonObject.has("reference")) {
                try {
                    reference = jsonObject.getString("reference");
                    requestReference = reference;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("event")) {
                try {
                    event = jsonObject.getString("event");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("error")) {
                try {
                    error = jsonObject.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_url")) {
                try {
                    verification_url = jsonObject.getString("verification_url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_result")) {
                try {
                    verification_result = jsonObject.getString("verification_result");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_data")) {
                try {
                    verification_data = jsonObject.getString("verification_data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (verification_url != null && !verification_url.isEmpty()) {
                initWebView(verification_url);
            } else {
                showDialog("Error", "" + error, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Sending response back to caller

                        if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
                            shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
                        }
                        //rlLoadingProgress.setVisibility(View.GONE);
                        if (alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }
                        ShuftiVerifyActivity.this.finish();
                    }
                });

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void errorResponse(String response) {
        requestInProcess = false;
        responseSet.clear();

        if (response == null) {
            return;
        }

        //Putting response in hash map
        try {

            JSONObject jsonObject = new JSONObject(response);

            //Starting api response parsing
            String reference = "";
            String event = "";
            String error = "";
            String verification_url = "";
            String verification_result = "";
            String verification_data = "";

            if (jsonObject.has("reference")) {
                try {
                    reference = jsonObject.getString("reference");
                    requestReference = reference;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("event")) {
                try {
                    event = jsonObject.getString("event");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("error")) {
                try {
                    JSONObject errorObject = new JSONObject(jsonObject.getString("error"));
                    error = errorObject.getString("message");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_url")) {
                try {
                    verification_url = jsonObject.getString("verification_url");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_result")) {
                try {
                    verification_result = jsonObject.getString("verification_result");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("verification_data")) {
                try {
                    verification_data = jsonObject.getString("verification_data");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


            //Putting response in hash map
            responseSet.put("reference", reference);
            responseSet.put("event", event);
            responseSet.put("error", error);
            responseSet.put("verification_url", verification_url);
            responseSet.put("verification_result", verification_result);
            responseSet.put("verification_data", verification_data);

            showDialog("Error", "" + error, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //Sending response back to caller

                    if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
                        shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
                    }
                    //rlLoadingProgress.setVisibility(View.GONE);
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                    ShuftiVerifyActivity.this.finish();
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    //Overrided back pressed method to stop user from quitting acciendently
    @Override
    public void onBackPressed() {
        AlertDialog.Builder alertClose = new AlertDialog.Builder(this);
        alertClose.setMessage("Are you sure you want to close verification process ?");
        alertClose.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestInProcess = false;
                ShuftiVerifyActivity.this.finish();
            }
        });

        alertClose.setNegativeButton("No", null);
        alertClose.show();
    }

    private void showDialog(String title, String message, View.OnClickListener clickListener) {

        rlLoadingProgress.setVisibility(View.GONE);
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ShuftiVerifyActivity.this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.response_dialog_layout, null);
        dialogBuilder.setView(dialogView);

        TextView tvMessage = dialogView.findViewById(R.id.tv_message_response);
        ImageView crossIconImageView = dialogView.findViewById(R.id.crossIconImageView);
        ImageView responseImageView = dialogView.findViewById(R.id.responseImageView);

        if (title.equalsIgnoreCase("Success")) {
            responseImageView.setImageResource(R.drawable.success_icon);
            tvMessage.setText("Successfully Verified");
        } else if (title.equalsIgnoreCase("Failure")) {
            responseImageView.setImageResource(R.drawable.failure_icon);
            tvMessage.setText("Verification Unsuccessful");
        } else {
            responseImageView.setImageResource(R.drawable.failure_icon);
            tvMessage.setText(message);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                alertDialog = dialogBuilder.create();
                alertDialog.setCancelable(false);
                alertDialog.show();

            }
        });

        crossIconImageView.setOnClickListener(clickListener);
    }

    private void returnErrorCallback(String error, boolean showCallbackResponse) {
        if (showCallbackResponse) {
            responseSet.put("error", error);
        }

        if (shuftiVerificationRequestModel != null && shuftiVerificationRequestModel.getShuftiVerifyListener() != null) {
            shuftiVerificationRequestModel.getShuftiVerifyListener().verificationStatus(responseSet);
        }
        ShuftiVerifyActivity.this.finish();
    }

    private void initWebView(String verification_url) {

        webView.setVisibility(View.VISIBLE);
        rlLoadingProgress.setVisibility(View.VISIBLE);

        checkPermissions();

        webView.setWebViewClient(new myWebClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAppCacheMaxSize(5 * 1024 * 1024); // 5MB cache.
        webView.getSettings().setAppCachePath(getApplicationContext().getCacheDir().getAbsolutePath());
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.clearCache(true);
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true);
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.getSettings().setMediaPlaybackRequiresUserGesture(true);


        CookieManager.getInstance().setAcceptCookie(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }

            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, FileChooserParams fileChooserParams) {

                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePath;
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                    File photoFile = null;
                    try {

                        photoFile = createImageFile();
                        takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                    } catch (IOException ex) {

                        Log.e(TAG, "Unable to create Image File", ex);
                    }

                    if (photoFile != null) {
                        mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        takePictureIntent = null;
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("image/*");
                Intent[] intentArray;
                if (takePictureIntent != null) {
                    intentArray = new Intent[]{takePictureIntent};
                } else {
                    intentArray = new Intent[0];
                }

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
                return true;
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;

                File imageStorageDir = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)
                        , "ShuftiPro");
                if (!imageStorageDir.exists()) {
                    imageStorageDir.mkdirs();
                }

                File file = new File(
                        imageStorageDir + File.separator + "IMG_"
                                + String.valueOf(System.currentTimeMillis())
                                + ".jpg");
                mCapturedImageURI = Uri.fromFile(file);

                final Intent captureIntent = new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");

                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");

                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                        , new Parcelable[]{captureIntent});

                startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType,
                                        String capture) {
                openFileChooser(uploadMsg, acceptType);
            }
        });

        webView.loadUrl(verification_url);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return imageFile;
    }


    private boolean checkPermissions() {

        int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA);
        }
        if (permissionStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onReceiveRequestStatus(String response) {
        responseSet.clear();

        if (response == null) {
            return;
        }
        //Starting api response parsing
        String reference = "";
        String event = "";
        String error = "";
        String email = "";

        //Putting response in hash map
        try {

            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.has("reference")) {
                try {
                    reference = jsonObject.getString("reference");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("event")) {
                try {
                    event = jsonObject.getString("event");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("error")) {
                try {
                    error = jsonObject.getString("error");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (jsonObject.has("email")) {
                try {
                    email = jsonObject.getString("email");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //Putting response in hash map
        responseSet.put("reference", reference);
        responseSet.put("event", event);
        responseSet.put("error", error);
        responseSet.put("email", email);
        returnErrorCallback("", false);
    }

    public class myWebClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);

            if (url.equalsIgnoreCase(redirect_demo_url)) {
                //returnErrorCallback("",false);
                //Get the request's status and send the response back to the user.
                getStatusRequest();
            }
        }

        private void getStatusRequest() {

            String clientId = shuftiVerificationRequestModel.getClientId();
            String secretKey = shuftiVerificationRequestModel.getSecretKey();
            HttpConnectionHandler.getInstance(clientId, secretKey).getRequestStatus(ShuftiVerifyActivity.this, requestReference, ShuftiVerifyActivity.this);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            //Get this redirect url and compare with the demo and redirect the user.

            if (url.equalsIgnoreCase(redirect_demo_url)) {
                //returnErrorCallback("",false);
                //Get the request's status and send the response back to the user.
                getStatusRequest();
            }
            return true;

        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            Log.e(TAG, description);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            rlLoadingProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        } else {
            finish();
            return true;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            Uri[] results = null;

            if (resultCode == Activity.RESULT_OK) {
                if (data == null) {

                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else {
                    String dataString = data.getDataString();
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILE_CHOOSER_RESULT_CODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data);
                return;
            }
            if (requestCode == FILE_CHOOSER_RESULT_CODE) {
                if (null == this.mUploadMessage) {
                    return;
                }
                Uri result = null;
                try {
                    if (resultCode != RESULT_OK) {
                        result = null;
                    } else {

                        result = data == null ? mCapturedImageURI : data.getData();
                    }
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "activity :" + e,
                            Toast.LENGTH_LONG).show();
                }
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        }
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();

                // Initialize the map with both permissions
                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);

                // Fill with actual results from user
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

                    // Check for all permissions
                    if (perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {

                    } else {
                        String message = "App won't work without permissions. Please, restart app and give" +
                                " access to the permissions.";
                        String button_text = "Finish";
                        showErrorMessageDialog(message, button_text);
                    }
                }
            }
        }

    }

    public void showErrorMessageDialog(String message, String button_text) {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage(message);
        alertDialog.setPositiveButton(button_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ShuftiVerifyActivity.this.finish();
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}



