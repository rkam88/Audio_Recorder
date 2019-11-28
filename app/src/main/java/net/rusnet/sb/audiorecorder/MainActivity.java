package net.rusnet.sb.audiorecorder;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements NewRecordingBroadCastReceiver.NewRecordingListener {

    public static final int PERMISSION_RECORD_AUDIO = 0;
    public static final String ACTION_NEW_RECORD_ADDED = "net.rusnet.sb.audiorecorder.ACTION_NEW_RECORD_ADDED";

    private NewRecordingBroadCastReceiver broadcastReceiver;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.button_start_recording).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecorderService();
            }
        });

        broadcastReceiver = new NewRecordingBroadCastReceiver(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NEW_RECORD_ADDED);

        registerReceiver(broadcastReceiver, filter);
    }

    private void startRecorderService() {
        Log.d("TAG", "Starting AudioRecorderService...");
        Intent intent = new Intent(MainActivity.this,
                AudioRecorderService.class
        );

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    permissions,
                    PERMISSION_RECORD_AUDIO
            );
        } else {
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_RECORD_AUDIO && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            startRecorderService();
        }
    }

    @Override
    public void onNewRecordingAddedCallback() {
        Log.d(TAG, "onNewRecordingAddedCallback() called");
        //todo: add recycler view update
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}