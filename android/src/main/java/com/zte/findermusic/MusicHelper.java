package com.zte.findermusic;

/**
 * Created by pawankumar on 22/03/18.
 */
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

public class MusicHelper {
    public static final int STORAGE_PERMISSION_CODE = 10;

    public static boolean hasExternalStorageAccess(AppCompatActivity activity) {
        if(ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) return true;

        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE);

        return false;
    }

    public static boolean isAccessGranted(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == MusicHelper.STORAGE_PERMISSION_CODE) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                return true;
        }
        return false;
    }
}