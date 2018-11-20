package com.shufti.shuftipro.activities;

import android.Manifest;
import android.annotation.SuppressLint;
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
import java.util.Arrays;
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
    private static final int FILECHOOSER_RESULTCODE = 1;
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private AlertDialog alertDialog;
    private String requestReference = "";

    private WebView webView;
    private ValueCallback<Uri> mUploadMessage;
    private Uri mCapturedImageURI = null;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;

    private boolean containVideoTag = false;

    private static ShuftiVerifyActivity instance = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_shufti_verify);
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

        //Return callbacks incase of wrong parameters are set..
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
                    requestedObject.put("redirect_url", Constants.redirect_demo_url);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    requestedObject.put("redirect_url", Constants.redirect_demo_url);
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

    @Override
    public void successResponse(String result) {
        requestInProcess = false;
        responseSet.clear();

        try {

            JSONObject jsonObject = new JSONObject(result);

            String reference = "";
            String error = "";
            String verification_url = "";

            if (jsonObject.has("reference")) {
                try {
                    reference = jsonObject.getString("reference");
                    requestReference = reference;
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

            if (verification_url != null && !verification_url.isEmpty()) {
                initWebView(verification_url);
            } else {
                showDialog("Error", "" + error, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

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

        try {

            JSONObject jsonObject = new JSONObject(response);

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

    //Overrided back pressed method to stop user from quitting accidentally
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

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(String verification_url) {

        webView.setVisibility(View.VISIBLE);
        rlLoadingProgress.setVisibility(View.VISIBLE);

        checkPermissions();

        webView.setWebViewClient(new myWebClient());
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.loadUrl(verification_url);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);


        CookieManager.getInstance().setAcceptCookie(true);
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);

        webView.setWebChromeClient(new WebChromeClient() {

            // For Android 5.0
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
                // Double check that we don't have any existing callbacks
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(null);
                }
                mFilePathCallback = filePath;


                //Checking for the supported types
                if (Build.VERSION.SDK_INT >= 21) {
                    String[] supportedMimeTypes = fileChooserParams.getAcceptTypes();
                    if (supportedMimeTypes.length > 0) {

                        if (Arrays.asList(supportedMimeTypes).contains("video/*")) {
                            containVideoTag = true;
                        }
                    }
                }

                Intent takePictureIntent = null;

                if (!containVideoTag) {
                    takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createImageFile();
                            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            Log.e(TAG, "Unable to create Image File", ex);
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                } else {
                    takePictureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                        // Create the File where the photo should go
                        File photoFile = null;
                        try {
                            photoFile = createVideoFile();
                            takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                        } catch (IOException ex) {
                            // Error occurred while creating the File
                            Log.e(TAG, "Unable to create Image File", ex);
                        }
                        // Continue only if the File was successfully created
                        if (photoFile != null) {
                            mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                    Uri.fromFile(photoFile));
                        } else {
                            takePictureIntent = null;
                        }
                    }
                }

                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);

                if (!containVideoTag) {
                    contentSelectionIntent.setType("image/*");
                } else {
                    contentSelectionIntent.setType("video/*");
                }

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

            // openFileChooser for Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                // Create AndroidExampleFolder at sdcard
                // Create AndroidExampleFolder at sdcard
                File imageStorageDir = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_PICTURES)
                        , "ShuftiPro");
                if (!imageStorageDir.exists()) {
                    // Create AndroidExampleFolder at sdcard
                    imageStorageDir.mkdirs();
                }
                // Create camera captured image file path and name
                File file = new File(
                        imageStorageDir + File.separator + "IMG_"
                                + String.valueOf(System.currentTimeMillis())
                                + ".jpg");
                mCapturedImageURI = Uri.fromFile(file);
                // Camera capture image intent
                final Intent captureIntent = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI);
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                // Create file chooser intent
                Intent chooserIntent = Intent.createChooser(i, "Image Chooser");
                // Set camera intent to file chooser
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS
                        , new Parcelable[]{captureIntent});
                // On select image call onActivityResult method of activity
                startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE);
            }

            // openFileChooser for Android < 3.0
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                openFileChooser(uploadMsg, "");
            }

            //openFileChooser for other Android versions
            public void openFileChooser(ValueCallback<Uri> uploadMsg,
                                        String acceptType,
                                        String capture) {
                openFileChooser(uploadMsg, acceptType);
            }
        });
    }

    private File createImageFile() throws IOException {
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

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "MP4_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File videoFile = File.createTempFile(
                imageFileName,
                ".mp4",
                storageDir
        );
        return videoFile;
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

            if (url.equalsIgnoreCase(Constants.redirect_demo_url)) {
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
            if (url.equalsIgnoreCase(Constants.redirect_demo_url)) {
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
                    // If there is not data, then we may have taken a photo
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
        }
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();

                perms.put(Manifest.permission.CAMERA, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.RECORD_AUDIO, PackageManager.PERMISSION_GRANTED);

                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);

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



