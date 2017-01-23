package com.rabtman.wsmanager.listener;

import okhttp3.Response;
import okio.ByteString;

/**
 * @author rabtman
 * 可用于监听ws连接状态并进一步拓展
 */
public abstract class WsStatusListener {
    public void onOpen(Response response) {
    }

    public void onMessage(String text) {
    }

    public void onMessage(ByteString bytes) {
    }

    public void onReconnect() {

    }

    public void onClosing(int code, String reason) {
    }


    public void onClosed(int code, String reason) {
    }

    public void onFailure(Throwable t, Response response) {
    }
}
