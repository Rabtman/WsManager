package com.rabtman.wsdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.rabtman.wsmanager.WsManager;
import com.rabtman.wsmanager.listener.WsStatusListener;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    private WsManager wsManager;

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
                Log.d(TAG, "onOpen:" + response.toString());
            }

            @Override
            public void onMessage(String text) {
                Log.d(TAG, "text:" + text);
            }
        });

        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                wsManager.sendMessage("test");
            }
        };
        timer.schedule(timerTask, 0, 5000);
    }


}
