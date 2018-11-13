package com.shufti.shuftipro.models;

import android.app.Activity;

import com.shufti.shuftipro.listeners.ShuftiVerifyListener;

import org.json.JSONObject;

public class ShuftiVerificationRequestModel {
    private String clientId;
    private String secretKey;
    private JSONObject jsonObject;
    private Activity parentActivity;
    private ShuftiVerifyListener shuftiVerifyListener;

    public ShuftiVerificationRequestModel() {
    }

    public ShuftiVerificationRequestModel(JSONObject jsonObject, Activity parentActivity, ShuftiVerifyListener shuftiVerifyListener) {
        this.jsonObject = jsonObject;
        this.parentActivity = parentActivity;
        this.shuftiVerifyListener = shuftiVerifyListener;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public Activity getParentActivity() {
        return parentActivity;
    }

    public void setParentActivity(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public ShuftiVerifyListener getShuftiVerifyListener() {
        return shuftiVerifyListener;
    }

    public void setShuftiVerifyListener(ShuftiVerifyListener shuftiVerifyListener) {
        this.shuftiVerifyListener = shuftiVerifyListener;
    }
}
