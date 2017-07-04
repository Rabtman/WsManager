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
 * @author rabtman
 */

public class WsManager implements IWsManager {

  private final static int RECONNECT_INTERVAL = 10 * 1000;    //重连自增步长
  private final static long RECONNECT_MAX_TIME = 120 * 1000;   //最大重连间隔
  private Context mContext;
  private String wsUrl;
  private WebSocket mWebSocket;
  private OkHttpClient mOkHttpClient;
  private Request mRequest;
  private int mCurrentStatus = WsStatus.DISCONNECTED;     //websocket连接状态
  private boolean isNeedReconnect;          //是否需要断线自动重连
  private boolean isManualClose = false;         //是否为手动关闭websocket连接
  private WsStatusListener wsStatusListener;
  private Lock mLock;
  private Handler wsMainHandler = new Handler(Looper.getMainLooper());
  private int reconnectCount = 0;   //重连次数
  private Runnable reconnectRunnable = new Runnable() {
    @Override
    public void run() {
      if (wsStatusListener != null) {
        wsStatusListener.onReconnect();
      }
      buildConnect();
    }
  };
  private WebSocketListener mWebSocketListener = new WebSocketListener() {

    @Override
    public void onOpen(WebSocket webSocket, final Response response) {
      mWebSocket = webSocket;
      setCurrentStatus(WsStatus.CONNECTED);
      connected();
      if (wsStatusListener != null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
          wsMainHandler.post(new Runnable() {
            @Override
            public void run() {
              wsStatusListener.onOpen(response);
            }
          });
        } else {
          wsStatusListener.onOpen(response);
        }
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, final ByteString bytes) {
      if (wsStatusListener != null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
          wsMainHandler.post(new Runnable() {
            @Override
            public void run() {
              wsStatusListener.onMessage(bytes);
            }
          });
        } else {
          wsStatusListener.onMessage(bytes);
        }
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, final String text) {
      if (wsStatusListener != null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
          wsMainHandler.post(new Runnable() {
            @Override
            public void run() {
              wsStatusListener.onMessage(text);
            }
          });
        } else {
          wsStatusListener.onMessage(text);
        }
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, final int code, final String reason) {
      if (wsStatusListener != null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
          wsMainHandler.post(new Runnable() {
            @Override
            public void run() {
              wsStatusListener.onClosing(code, reason);
            }
          });
        } else {
          wsStatusListener.onClosing(code, reason);
        }
      }
    }

    @Override
    public void onClosed(WebSocket webSocket, final int code, final String reason) {
      if (wsStatusListener != null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
          wsMainHandler.post(new Runnable() {
            @Override
            public void run() {
              wsStatusListener.onClosed(code, reason);
            }
          });
        } else {
          wsStatusListener.onClosed(code, reason);
        }
      }
    }

    @Override
    public void onFailure(WebSocket webSocket, final Throwable t, final Response response) {
      tryReconnect();
      if (wsStatusListener != null) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
          wsMainHandler.post(new Runnable() {
            @Override
            public void run() {
              wsStatusListener.onFailure(t, response);
            }
          });
        } else {
          wsStatusListener.onFailure(t, response);
        }
      }
    }
  };

  public WsManager(Builder builder) {
    mContext = builder.mContext;
    wsUrl = builder.wsUrl;
    isNeedReconnect = builder.needReconnect;
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

  @Override
  public WebSocket getWebSocket() {
    return mWebSocket;
  }


  public void setWsStatusListener(WsStatusListener wsStatusListener) {
    this.wsStatusListener = wsStatusListener;
  }

  @Override
  public synchronized boolean isWsConnected() {
    return mCurrentStatus == WsStatus.CONNECTED;
  }

  @Override
  public synchronized int getCurrentStatus() {
    return mCurrentStatus;
  }

  @Override
  public synchronized void setCurrentStatus(int currentStatus) {
    this.mCurrentStatus = currentStatus;
  }

  @Override
  public void startConnect() {
    isManualClose = false;
    buildConnect();
  }

  @Override
  public void stopConnect() {
    isManualClose = true;
    disconnect();
  }

  private void tryReconnect() {
    if (!isNeedReconnect | isManualClose) {
      return;
    }

    if (!isNetworkConnected(mContext)) {
      setCurrentStatus(WsStatus.DISCONNECTED);
      return;
    }

    setCurrentStatus(WsStatus.RECONNECT);

    long delay = reconnectCount * RECONNECT_INTERVAL;
    wsMainHandler
        .postDelayed(reconnectRunnable, delay > RECONNECT_MAX_TIME ? RECONNECT_MAX_TIME : delay);
    reconnectCount++;
  }

  private void cancelReconnect() {
    wsMainHandler.removeCallbacks(reconnectRunnable);
    reconnectCount = 0;
  }

  private void connected() {
    cancelReconnect();
  }

  private void disconnect() {
    if (mCurrentStatus == WsStatus.DISCONNECTED) {
      return;
    }
    cancelReconnect();
    if (mOkHttpClient != null) {
      mOkHttpClient.dispatcher().cancelAll();
    }
    if (mWebSocket != null) {
      boolean isClosed = mWebSocket.close(WsStatus.CODE.NORMAL_CLOSE, WsStatus.TIP.NORMAL_CLOSE);
      //非正常关闭连接
      if (!isClosed) {
        if (wsStatusListener != null) {
          wsStatusListener.onClosed(WsStatus.CODE.ABNORMAL_CLOSE, WsStatus.TIP.ABNORMAL_CLOSE);
        }
      }
    }
    setCurrentStatus(WsStatus.DISCONNECTED);
  }

  private synchronized void buildConnect() {
    if (!isNetworkConnected(mContext)) {
      setCurrentStatus(WsStatus.DISCONNECTED);
      return;
    }
    switch (getCurrentStatus()) {
      case WsStatus.CONNECTED:
      case WsStatus.CONNECTING:
        break;
      default:
        setCurrentStatus(WsStatus.CONNECTING);
        initWebSocket();
    }
  }

  //发送消息
  @Override
  public boolean sendMessage(String msg) {
    return send(msg);
  }

  @Override
  public boolean sendMessage(ByteString byteString) {
    return send(byteString);
  }

  private boolean send(Object msg) {
    boolean isSend = false;
    if (mWebSocket != null && mCurrentStatus == WsStatus.CONNECTED) {
      if (msg instanceof String) {
        isSend = mWebSocket.send((String) msg);
      } else if (msg instanceof ByteString) {
        isSend = mWebSocket.send((ByteString) msg);
      }
      //发送消息失败，尝试重连
      if (!isSend) {
        tryReconnect();
      }
    }
    return isSend;
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
    private boolean needReconnect = true;
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

    public Builder needReconnect(boolean val) {
      needReconnect = val;
      return this;
    }

    public WsManager build() {
      return new WsManager(this);
    }
  }
}
