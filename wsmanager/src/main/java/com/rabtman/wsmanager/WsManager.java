package com.rabtman.wsmanager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import com.rabtman.wsmanager.listener.WsStatusListener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

/**
 * @author zjm
 * @Description:
 * @date 2017/1/12
 */

public class WsManager implements IWsManager {
    //重连相关
    private final static int RECONNECT_INTERVAL = 10 * 1000;
    private final static long RECONNECT_MAX_TIME = 120 * 1000;
    private Context mContext;
    private String wsUrl;
    private WebSocket mWebSocket;
    private OkHttpClient mOkHttpClient;
    private Request mRequest;
    private ConnectStatus mCurrentStatus = ConnectStatus.DISCONNECTED;
    private boolean isNeedReconnect = true;
    private WsStatusListener wsStatusListener;
    private Lock mLock;
    private Handler wsHandler = new Handler(Looper.getMainLooper());
    private int reconnectCount = 0;   //重连次数
    private WebSocketListener mWebSocketListener = new WebSocketListener() {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            mWebSocket = webSocket;
            mCurrentStatus = ConnectStatus.CONNECTED;
            connected();
            if (wsStatusListener != null) wsStatusListener.onOpen(response);
        }

        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            if (wsStatusListener != null) wsStatusListener.onMessage(bytes);
        }

        @Override
        public void onMessage(final WebSocket webSocket, String text) {
            if (wsStatusListener != null) wsStatusListener.onMessage(text);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            if (wsStatusListener != null) wsStatusListener.onClosing(code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            mCurrentStatus = ConnectStatus.DISCONNECTED;
            disconnected();
            if (wsStatusListener != null) wsStatusListener.onClosed(code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            tryReconnect();
            if (wsStatusListener != null) wsStatusListener.onFailure(t, response);
        }
    };
    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            buildConnect();
        }
    };

    public WsManager(Builder builder) {
        mContext = builder.mContext;
        wsUrl = builder.wsUrl;
        mOkHttpClient = builder.mOkHttpClient;
        this.mLock = new ReentrantLock();
    }

    private void initWebSocket() {
        if (mOkHttpClient == null) {
            mOkHttpClient = new OkHttpClient.Builder()
                    .retryOnConnectionFailure(true)
                    .build();
        }
        if (mRequest == null) {
            mRequest = new Request.Builder()
                    .url(wsUrl)
                    .build();
        }
        mOkHttpClient.dispatcher().cancelAll();
        try {
            mLock.lockInterruptibly();
            try {
                mOkHttpClient.newWebSocket(mRequest, mWebSocketListener);
            } finally {
                mLock.unlock();
            }
        } catch (InterruptedException e) {
        }
    }

    public WebSocket getWebSocket() {
        return mWebSocket;
    }


    public void setWsStatusListener(WsStatusListener wsStatusListener) {
        this.wsStatusListener = wsStatusListener;
    }


    public boolean isWsConnected() {
        return mCurrentStatus == ConnectStatus.CONNECTED;
    }

    public ConnectStatus getCurrentStatus() {
        return mCurrentStatus;
    }

    public void startConnect() {
        isNeedReconnect = true;
        if (mCurrentStatus == ConnectStatus.CONNECTED | mCurrentStatus == ConnectStatus.CONNECTING | !isNetworkConnected(mContext))
            return;
        buildConnect();
    }

    public void stopConnect() {
        isNeedReconnect = false;
        if (mCurrentStatus == ConnectStatus.DISCONNECTED) return;
        disconnected();
    }

    public void tryReconnect() {
        if (!isNeedReconnect) return;
        mCurrentStatus = ConnectStatus.RECONNECT;

        if (!isNetworkConnected(mContext)) return;

        long delay = reconnectCount * RECONNECT_INTERVAL;
        wsHandler.postDelayed(reconnectRunnable, delay > RECONNECT_MAX_TIME ? RECONNECT_MAX_TIME : delay);
        reconnectCount++;
    }

    public void cancelReconnect() {
        wsHandler.removeCallbacks(reconnectRunnable);
        reconnectCount = 0;
    }

    @Override
    public void connected() {
        cancelReconnect();
    }

    @Override
    public void disconnected() {
        cancelReconnect();
        if (mOkHttpClient != null) mOkHttpClient.dispatcher().cancelAll();
        mWebSocket = null;
    }

    @Override
    public void buildConnect() {
        mCurrentStatus = ConnectStatus.CONNECTING;
        initWebSocket();
    }

    //发送消息
    @Override
    public void sendMessage(String msg) {
        if (mWebSocket != null && mCurrentStatus == ConnectStatus.CONNECTED) {
            boolean isSend = mWebSocket.send(msg);
            if (isSend) return;
        }

        tryReconnect();
    }

    //检查网络是否连接
    private boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager
                    .getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public static final class Builder {
        private Context mContext;
        private String wsUrl;
        private OkHttpClient mOkHttpClient;

        public Builder(Context val) {
            mContext = val;
        }

        public Builder wsUrl(String val) {
            wsUrl = val;
            return this;
        }

        public Builder client(OkHttpClient val) {
            mOkHttpClient = val;
            return this;
        }

        public WsManager build() {
            return new WsManager(this);
        }
    }
}
