package com.shufti.shuftipro.cloud;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.shufti.shuftipro.activities.ShuftiVerifyActivity;
import com.shufti.shuftipro.listeners.NetworkListener;
import com.shufti.shuftipro.listeners.ReferenceResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnectionHandler {

    private static HttpConnectionHandler instance = null;
    private boolean errorOccured = true;
    private String TAG = HttpConnectionHandler.class.getSimpleName();
    private static final String SHUFTIPRO_API_URL = "https://api.shuftipro.com/";
    private static final String SHUFTIPRO_STATUS_API_URL = "https://api.shuftipro.com/sdk/request/status/";
    private static final String SDK_STACKTRACE_URL = "https://api.shuftipro.com/v3/sdk/error/report/";
    private String CLIENT_ID;
    private String SECRET_KEY;
    private String ACCESS_TOKEN;
    private InputStream inputStream = null;

    public HttpConnectionHandler(String clientId, String secretKey,String accessToken) {
        this.CLIENT_ID = clientId;
        this.SECRET_KEY = secretKey;
        this.ACCESS_TOKEN = accessToken;
    }

    public static HttpConnectionHandler getInstance(String clientId, String secretKey, String accessToken) {

        instance = new HttpConnectionHandler(clientId, secretKey, accessToken);
        return instance;
    }

    @SuppressLint("StaticFieldLeak")
    public boolean executeVerificationRequest(final JSONObject requestedObject, final
    NetworkListener networkListener, final Context context) {

        if (networkAvailable(context)) {

            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... voids) {
                    String resultResponse = "";
                    try {
                        URL url = new URL(SHUFTIPRO_API_URL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setAllowUserInteraction(false);
                        connection.setRequestProperty("Connection", "Keep-Alive");
                        connection.setConnectTimeout(900000);
                        connection.setReadTimeout(900000);

                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        connection.setRequestProperty("Accept", "application/json");

                        String cred;
                        if (CLIENT_ID == null || CLIENT_ID.isEmpty() || SECRET_KEY == null || SECRET_KEY.isEmpty()) {
                            cred = bearer(ACCESS_TOKEN);
                        } else {
                            cred = basic(CLIENT_ID, SECRET_KEY);

                        }
                        connection.setRequestProperty("Authorization", cred);
                        connection.connect();

                        DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                        os.writeBytes(requestedObject.toString());

                        os.flush();
                        os.close();

                        int responseCode = ((HttpURLConnection) connection).getResponseCode();
                        if ((responseCode >= HttpURLConnection.HTTP_OK)
                                && responseCode < 300) {
                            inputStream = connection.getInputStream();
                            errorOccured = false;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 400) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 401) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 402) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 403) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 404) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 409) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else if (responseCode == 503) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        } else {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultResponse = e.getMessage();
                        return resultResponse;
                    }
                    return resultResponse;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    //If user has already kill the request do not send him response of previous request.
                    if (ShuftiVerifyActivity.getInstance() == null) {
                        return;
                    }
                    if (networkListener != null) {
                        if (!errorOccured) {
                            networkListener.successResponse(result);
                        } else {
                            networkListener.errorResponse(result);
                        }
                    }

                }
            }.execute();
            return true;

        } else {
            return false;
        }
    }

    @SuppressLint("StaticFieldLeak")
    public boolean getRequestStatus(final Context context, final String reference, final ReferenceResponseListener listener) {

        if (networkAvailable(context)) {

            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... voids) {
                    String resultResponse = "";
                    JSONObject parameter = new JSONObject();
                    try {
                        parameter.put("reference", reference);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    try {
                        URL url = new URL(SHUFTIPRO_STATUS_API_URL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setAllowUserInteraction(false);
                        connection.setRequestProperty("Connection", "Keep-Alive");
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);

                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                        connection.setRequestProperty("Accept", "application/json");

                        String cred;
                        if (CLIENT_ID == null || CLIENT_ID.isEmpty() || SECRET_KEY == null || SECRET_KEY.isEmpty()) {
                            cred = bearer(ACCESS_TOKEN);
                        } else {
                            cred = basic(CLIENT_ID, SECRET_KEY);

                        }
                        connection.setRequestProperty("Authorization", cred);
                        connection.connect();

                        //Adding reference parameter


                        DataOutputStream os = new DataOutputStream(connection.getOutputStream());
                        os.writeBytes(parameter.toString());

                        os.flush();
                        os.close();

                        int responseCode = ((HttpURLConnection) connection).getResponseCode();
                        if ((responseCode >= HttpURLConnection.HTTP_OK)
                                && responseCode < 300) {
                            inputStream = connection.getInputStream();
                            errorOccured = false;
                            resultResponse = inputStreamToString(inputStream);
                            Log.d(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 400) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 401) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 402) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 403) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 404) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 409) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else if (responseCode == 503) {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        } else {
                            inputStream = connection.getErrorStream();
                            errorOccured = true;
                            resultResponse = inputStreamToString(inputStream);
                            Log.e(TAG, "Response : " + resultResponse);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        resultResponse = e.getMessage();
                        return resultResponse;
                    }
                    return resultResponse;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    //If user has already kill the request do not send him response of previous request.

                    if (ShuftiVerifyActivity.getInstance() == null) {
                        return;
                    }
                    if (listener != null) {
                        listener.onReceiveRequestStatus(result);
                    }
                }
            }.execute();

            return true;
        }

        return false;
    }

    private static boolean networkAvailable(Context context) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;

            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }

        if (!haveConnectedWifi)
            Log.e("NetworkInfo", "No WIFI Available");
        else
            Log.e("NetworkInfo", "WIFI Available");

        if (!haveConnectedWifi){

            if (!haveConnectedMobile)
                Log.e("NetworkInfo", "No Mobile Network Available");
            else
                Log.e("NetworkInfo", "Mobile Network Available");
        }

        return haveConnectedWifi || haveConnectedMobile;
    }

    private static String basic(String username, String password) {
        String usernameAndPassword = username + ":" + password;
        String encoded = Base64.encodeToString((usernameAndPassword).getBytes(), Base64.NO_WRAP);
        return "Basic " + encoded;
    }

    private static String bearer(String access_token) {
        return "Bearer " + access_token;
    }


    private static String inputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line);
        }
        reader.close();
        return out.toString();
    }

    @SuppressLint("StaticFieldLeak")
    public void sendStacktraceReport(final Context context, final String clientId, final String threadName, final String stackTrace,
                                     final String message, final String deviceInformation, final String timeStamp, final String sdkVersion, final String exceptionClassname) {
        if (networkAvailable(context)) {
            new AsyncTask<Void, Void, String>() {

                @Override
                protected String doInBackground(Void... voids) {
                    String resultResponse = "";
                    try {
                        URL url = new URL(SDK_STACKTRACE_URL);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoOutput(true);
                        connection.setDoInput(true);
                        connection.setAllowUserInteraction(false);
                        connection.setRequestProperty("Connection", "Keep-Alive");
                        connection.setConnectTimeout(9000);
                        connection.setReadTimeout(9000);

                        connection.setRequestMethod("POST");

                        Uri.Builder builder = new Uri.Builder()
                                .appendQueryParameter("clientId", clientId)
                                .appendQueryParameter("threadName", threadName)
                                .appendQueryParameter("stackTrace", stackTrace)
                                .appendQueryParameter("message", message)
                                .appendQueryParameter("deviceInformation", deviceInformation)
                                .appendQueryParameter("SDKVersion", sdkVersion)
                                .appendQueryParameter("timeStamp", timeStamp)
                                .appendQueryParameter("ExceptionClassName", exceptionClassname);

                        Log.e("REQUEST", builder.toString());
                        String query = builder.build().getEncodedQuery();

                        byte[] outputBytes = query.getBytes("UTF-8");

                        OutputStream os = connection.getOutputStream();
                        os.write(outputBytes);
                        os.close();

                        connection.connect();
                        int responseCode = ((HttpURLConnection) connection).getResponseCode();
                        if ((responseCode >= HttpURLConnection.HTTP_OK)
                                && responseCode < 300) {
                            Log.e("REQUEST", "Response: " + resultResponse);
                            inputStream = connection.getInputStream();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        return resultResponse;
                    }
                    return resultResponse;
                }

                protected void onProgressUpdate(Void... values) {
                    super.onProgressUpdate(values);
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    try {


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }.execute();
        }
    }

}
