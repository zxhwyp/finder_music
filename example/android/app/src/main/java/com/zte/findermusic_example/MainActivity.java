package com.zte.findermusic_example;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import io.flutter.app.FlutterActivity;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GeneratedPluginRegistrant.registerWith(this);

        new MethodChannel(getFlutterView(), "android_app_retain").setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(MethodCall call, MethodChannel.Result result) {
                if (call.method.equals("sendToBackground")) {
                    Log.d("abc", "Backkkkk");
                    moveTaskToBack(true);
                }
            }
        });
    }
}
