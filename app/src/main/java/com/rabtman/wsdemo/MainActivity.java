package com.rabtman.wsdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.rabtman.wsmanager.WsManager;
import com.rabtman.wsmanager.listener.WsStatusListener;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Response;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private WsManager wsManager;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wsManager = new WsManager.Builder(this)
                .wsUrl("")
                .build();

        wsManager.startConnect();

        wsManager.setWsStatusListener(new WsStatusListener() {
            @Override
            public void onOpen(Response response) {
                super.onOpen(response);
            }

            @Override
            public void onMessage(String text) {
                super.onMessage(text);
            }

            @Override
            public void onMessage(ByteString bytes) {
                super.onMessage(bytes);
            }

            @Override
            public void onReconnect() {
                super.onReconnect();
            }

            @Override
            public void onClosing(int code, String reason) {
                super.onClosing(code, reason);
            }

            @Override
            public void onClosed(int code, String reason) {
                super.onClosed(code, reason);
            }

            @Override
            public void onFailure(Throwable t, Response response) {
                super.onFailure(t, response);
            }
        });

        final Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "ws status:" + wsManager.getCurrentStatus() + "|count:" + count);
                count++;
                wsManager.sendMessage("");
                if (count == 5) {
                    timer.cancel();
                    wsManager.stopConnect();
                }
            }
        };

        timer.schedule(timerTask, 0, 5000);
    }


}
