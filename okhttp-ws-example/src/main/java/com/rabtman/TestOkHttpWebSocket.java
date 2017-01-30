package com.rabtman;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;

public class TestOkHttpWebSocket {

    private static final MockWebServer mockWebServer = new MockWebServer();
    private static WebSocket mWebSocket = null;
    private static int msgCount = 0; //消息发送次数
    private static Timer mTimer;

    public static void main(String[] args) {
        System.out.println("java test!");

        initMockServer();
        initWsClient("ws://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort() + "/");
    }

    //每秒发送一条消息
    private static void startTask() {
        mTimer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mWebSocket == null) return;
                msgCount++;
                boolean isSuccessed = mWebSocket.send("msg" + msgCount + "-" + System.currentTimeMillis());
                //除了文本内容外，还可以将如图像，声音，视频等内容转为ByteString发送
                //boolean send(ByteString bytes);
            }
        };
        mTimer.schedule(timerTask, 0, 1000);
    }

    private static void initWsClient(String wsUrl) {
        //新建client
        OkHttpClient client = new OkHttpClient.Builder()
                .build();
        //构造request对象
        Request request = new Request.Builder()
                .url(wsUrl)
                .build();

        //建立连接
        client.newWebSocket(request, new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                mWebSocket = webSocket;
                //打印一些内容
                System.out.println("client onOpen");
                System.out.println("client request header:" + response.request().headers());
                System.out.println("client response header:" + response.headers());
                System.out.println("client response:" + response);
                //开启消息定时发送
                startTask();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                //打印一些内容
                System.out.println("client onMessage");
                System.out.println("message:" + text);
            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("client onClosing");
                System.out.println("code:" + code + " reason:" + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                //打印一些内容
                System.out.println("client onClosed");
                System.out.println("code:" + code + " reason:" + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                //出现异常会进入此回调
                System.out.println("client onFailure");
                System.out.println("throwable:" + t);
                System.out.println("response:" + response);
            }
        });
    }

    private static void initMockServer() {
        mockWebServer.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                System.out.println("server onOpen");
                System.out.println("server request header:" + response.request().headers());
                System.out.println("server response header:" + response.headers());
                System.out.println("server response:" + response);
            }

            @Override
            public void onMessage(WebSocket webSocket, String string) {
                System.out.println("server onMessage");
                System.out.println("message:" + string);
                //接受到5条信息后，关闭消息定时发送器
                if (msgCount == 5) {
                    mTimer.cancel();
                    webSocket.close(1000, "close by server");
                    return;
                }
                webSocket.send("response-" + string);
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                System.out.println("server onClosing");
                System.out.println("code:" + code + " reason:" + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                //打印一些内容
                System.out.println("server onClosed");
                System.out.println("code:" + code + " reason:" + reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                //出现异常会进入此回调
                System.out.println("server onFailure");
                System.out.println("throwable:" + t);
                System.out.println("response:" + response);
            }

        }));
    }
}
