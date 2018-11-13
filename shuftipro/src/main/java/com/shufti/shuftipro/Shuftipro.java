package com.shufti.shuftipro;

import android.app.Activity;
import android.content.Intent;

import com.shufti.shuftipro.activities.ShuftiVerifyActivity;
import com.shufti.shuftipro.constants.Constants;
import com.shufti.shuftipro.helpers.IntentHelper;
import com.shufti.shuftipro.listeners.ShuftiVerifyListener;
import com.shufti.shuftipro.models.ShuftiVerificationRequestModel;

import org.json.JSONObject;

public class Shuftipro {

    private static Shuftipro shuftipro = null;
    private String clientId;
    private String secretKey;

    private Shuftipro(String clientId, String secretKey){
        this.clientId = clientId;
        this.secretKey = secretKey;
    }

    public static Shuftipro getInstance(String clientId, String secretKey){
        if(shuftipro == null){
            shuftipro = new Shuftipro(clientId,secretKey);
        }

        return shuftipro;
    }
    public void shuftiproVerification(JSONObject requestedObject, Activity parentActivity, ShuftiVerifyListener listener){

        ShuftiVerificationRequestModel verificationRequestModel = new ShuftiVerificationRequestModel();
        verificationRequestModel.setClientId(clientId);
        verificationRequestModel.setSecretKey(secretKey);
        verificationRequestModel.setJsonObject(requestedObject);
        verificationRequestModel.setParentActivity(parentActivity);
        verificationRequestModel.setShuftiVerifyListener(listener);

        //Pas this request to the ShuftiVerification Activity
        IntentHelper.getInstance().insertObject(Constants.KEY_DATA_MODEL,verificationRequestModel);
        Intent intent = new Intent(parentActivity, ShuftiVerifyActivity.class);
        parentActivity.startActivity(intent);
    }
}
