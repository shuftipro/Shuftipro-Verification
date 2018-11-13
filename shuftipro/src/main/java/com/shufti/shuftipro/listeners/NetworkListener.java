package com.shufti.shuftipro.listeners;

public interface NetworkListener {

    void successResponse(String result);
    void errorResponse(String reason);
}
