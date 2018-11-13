package com.shufti.shuftipro.cloud;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnectionHandler {

    private static HttpConnectionHandler instance = null;
    private boolean errorOccured = true;
    private String TAG = HttpConnectionHandler.class.getSimpleName();
    private static final String SHUFTIPRO_API_URL = "https://shuftipro.com/api/";
    private static final String SHUFTIPRO_STATUS_API_URL = "https://shuftipro.com/api/status/";
    private String CLIENT_ID;
    private String SECRET_KEY;
    private InputStream inputStream = null;

    public HttpConnectionHandler(String clientId, String secretKey) {
        this.CLIENT_ID = clientId;
        this.SECRET_KEY = secretKey;
    }

    public static HttpConnectionHandler getInstance(String clientId, String secretKey) {

        if (instance == null) {
            instance = new HttpConnectionHandler(clientId, secretKey);
        }
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

                        String cred = basic(CLIENT_ID, SECRET_KEY);
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
    public void getRequestStatus(final Context context, final String reference, final ReferenceResponseListener listener) {

        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                String resultResponse = "";
                JSONObject parameter = new JSONObject();
                try {
                    parameter.put("reference",reference);
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

                    String cred = basic(CLIENT_ID, SECRET_KEY);
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
                if (listener != null){
                    listener.onReceiveRequestStatus(result);
                }
            }
        }.execute();
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
        if (!haveConnectedMobile)
            Log.e("NetworkInfo", "No Mobile Network Available");
        else
            Log.e("NetworkInfo", "Mobile Network Available");

        return haveConnectedWifi || haveConnectedMobile;
    }

    private static String basic(String username, String password) {
        String usernameAndPassword = username + ":" + password;
        String encoded = Base64.encodeToString((usernameAndPassword).getBytes(), Base64.NO_WRAP);
        return "Basic " + encoded;
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

}
