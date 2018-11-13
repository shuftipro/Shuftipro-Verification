package com.shufti.shuftipro.helpers;

import java.util.HashMap;

public class IntentHelper {

    private static IntentHelper intentHelper = null;
    private HashMap<String,Object> hashMap;

    private IntentHelper(){
        hashMap = new HashMap<>();
    }

    public static IntentHelper getInstance(){
        if(intentHelper == null){
            intentHelper = new IntentHelper();
        }

        return intentHelper;
    }

    public void insertObject(String key, Object data){
        if(hashMap != null){
            hashMap.put(key,data);
        }
    }

    public Object getObject(String key){

        if(hashMap.containsKey(key)){
            Object found = hashMap.get(key);
            hashMap.remove(key);
            return found;

        }

        return null;

    }

    public boolean containsKey(String key){
        return hashMap.containsKey(key);
    }
}
