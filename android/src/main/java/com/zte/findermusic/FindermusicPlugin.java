package com.zte.findermusic;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** FindermusicPlugin */
public class FindermusicPlugin implements FlutterPlugin, MethodCallHandler , PluginRegistry.RequestPermissionsResultListener, ActivityAware{
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;

  private static final int REQUEST_CODE_STORAGE_PERMISSION = 3777;

  private Activity activity;
  private Map<String, Object> arguments;
  private boolean executeAfterPermissionGranted;
  private static FindermusicPlugin instance;
  private Result pendingResult;

  //MusicPlayer
  private static AudioManager am;

  final Handler handler = new Handler();

  MediaPlayer mediaPlayer;
  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
     Context _context = flutterPluginBinding.getApplicationContext();
    am = (AudioManager)_context.getSystemService(Context.AUDIO_SERVICE);
    channel = new MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "findermusic");
    channel.setMethodCallHandler(this);
  }


  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "findermusic");
    channel.setMethodCallHandler(new FindermusicPlugin());
  }
  @Override
  public void onMethodCall(MethodCall call, Result result) {
    if (call.method.equals("getPlatformVersion")) {
      result.success("Android " + android.os.Build.VERSION.RELEASE);
    } else if (call.method.equals("getSongs")) {
      pendingResult = result;
      if (!(call.arguments instanceof Map)) {
        throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
      }
      arguments = (Map<String, Object>) call.arguments;
      boolean handlePermission = (boolean) arguments.get("handlePermissions");
      this.executeAfterPermissionGranted = (boolean) arguments.get("executeAfterPermissionGranted");
      checkPermission(handlePermission);
      // result.success(getData());

    } else if (call.method.equals("play")) {
      String url = ((HashMap) call.arguments()).get("url").toString();
      Boolean resPlay = play(url);
      result.success(1);
    } else if (call.method.equals("pause")) {
      pause();
      result.success(1);
    } else if (call.method.equals("stop")) {
      stop();
      result.success(1);
    } else if (call.method.equals("seek")) {
      double position = call.arguments();
      seek(position);
      result.success(1);
    } else if (call.method.equals("mute")) {
      Boolean muted = call.arguments();
      mute(muted);
      result.success(1);
    } else {
      result.notImplemented();
    }
  }

  private void checkPermission(boolean handlePermission) {
    if (checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      if (shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
        // TODO: user should be explained why the app needs the permission
        if (handlePermission) {
          requestPermissions();
        } else {
          setNoPermissionsError();
        }
      } else {
        if (handlePermission) {
          requestPermissions();
        } else {
          setNoPermissionsError();
        }
      }

    } else {
      pendingResult.success(getData());
      pendingResult = null;
      arguments = null;
    }
  }

  private void scanMusicFiles(File[] files) {
    for (File file: files) {
      if (file.isDirectory())  {
        scanMusicFiles(file.listFiles());
      } else {
        activity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://"
                + file.getAbsolutePath())));
      }
    }
  }

  ArrayList<HashMap> getData() {
    MusicFinder mf = new MusicFinder(activity.getContentResolver());

    // Scan all files under Music folder in external storage directory
    scanMusicFiles(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).listFiles());

    mf.prepare();
    List<MusicFinder.Song> allsongs = mf.getAllSongs();
    ArrayList<HashMap> songsMap = new ArrayList<>();
    for (MusicFinder.Song s : allsongs) {
      songsMap.add(s.toMap());
    }
    return songsMap;
  }

  @TargetApi(Build.VERSION_CODES.M)
  private void requestPermissions() {
    activity.requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE },
            REQUEST_CODE_STORAGE_PERMISSION);
  }

  private boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
    if (Build.VERSION.SDK_INT >= 23) {
      return activity.shouldShowRequestPermissionRationale(permission);
    }
    return false;
  }

  private int checkSelfPermission(Context context, String permission) {
    if (permission == null) {
      throw new IllegalArgumentException("permission is null");
    }
    return context.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid());
  }


  @Override
  public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];

        if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          if (grantResult == PackageManager.PERMISSION_GRANTED) {
            if (executeAfterPermissionGranted) {
              pendingResult.success(getData());
              pendingResult = null;
              arguments = null;
            }
          } else {
            setNoPermissionsError();
          }
        }
      }
    }
    return false;
  }

  private void setNoPermissionsError() {
    pendingResult.error("permission", "you don't have the user permission to access the camera", null);
    pendingResult = null;
    arguments = null;
  }

  private void mute(Boolean muted) {
    if (am == null)
      return;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
              muted ? AudioManager.ADJUST_MUTE : AudioManager.ADJUST_UNMUTE, 0);
    } else {
      am.setStreamMute(AudioManager.STREAM_MUSIC, muted);
    }
  }

  private void seek(double position) {
    mediaPlayer.seekTo((int) (position * 1000));
  }

  private void stop() {
    handler.removeCallbacks(sendData);
    if (mediaPlayer != null) {
      mediaPlayer.stop();
      mediaPlayer.release();
      mediaPlayer = null;
    }
  }

  private void pause() {
    mediaPlayer.pause();
    handler.removeCallbacks(sendData);
  }

  private Boolean play(String url) {
    if (mediaPlayer == null) {
      mediaPlayer = new MediaPlayer();
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      try {
        mediaPlayer.setDataSource(url);
      } catch (IOException e) {
        e.printStackTrace();
        Log.d("AUDIO", "invalid DataSource");
      }

      mediaPlayer.prepareAsync();
    } else {
      channel.invokeMethod("audio.onDuration", mediaPlayer.getDuration());

      mediaPlayer.start();
      channel.invokeMethod("audio.onStart", true);
    }

    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        channel.invokeMethod("audio.onDuration", mediaPlayer.getDuration());

        mediaPlayer.start();
        channel.invokeMethod("audio.onStart", true);
      }
    });

    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        stop();
        channel.invokeMethod("audio.onComplete", true);
      }
    });

    mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
      @Override
      public boolean onError(MediaPlayer mp, int what, int extra) {
        channel.invokeMethod("audio.onError", String.format("{\"what\":%d,\"extra\":%d}", what, extra));
        return true;
      }
    });

    handler.post(sendData);

    return true;
  }

  private final Runnable sendData = new Runnable() {
    public void run() {
      try {
        if (!mediaPlayer.isPlaying()) {
          handler.removeCallbacks(sendData);
        }
        int time = mediaPlayer.getCurrentPosition();
        channel.invokeMethod("audio.onCurrentPosition", time);

        handler.postDelayed(this, 200);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    activity = binding.getActivity();
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }
}
