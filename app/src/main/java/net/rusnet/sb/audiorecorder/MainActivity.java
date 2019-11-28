package net.rusnet.sb.audiorecorder;

import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity
        extends AppCompatActivity
        implements NewRecordingBroadCastReceiver.NewRecordingListener,
        FileAdapter.OnItemClickListener {

    public static final int PERMISSION_RECORD_AUDIO = 0;
    public static final String ACTION_NEW_RECORD_ADDED = "net.rusnet.sb.audiorecorder.ACTION_NEW_RECORD_ADDED";

    private NewRecordingBroadCastReceiver broadcastReceiver;

    private static final String TAG = "MainActivity";

    private RecyclerView mFileListRecycler;
    private FileAdapter mAdapter;

    private TextView mRecordingName;
    private ImageButton mPlayImageButton;
    private File mSelectedFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        initBroadCastReceiver();

        initRecyclerView();

        updateRecyclerContent();

    }

    private void initViews() {
        findViewById(R.id.button_start_recording).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecorderService();
            }
        });

        mRecordingName = findViewById(R.id.text_view_recording_name);
        mPlayImageButton = findViewById(R.id.image_button_play_recording);
        mPlayImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedFile == null) {
                    Toast.makeText(MainActivity.this, R.string.toast_select_a_recording, Toast.LENGTH_SHORT).show();
                } else {
                    //todo: create and launch playback service
                    Toast.makeText(MainActivity.this, "Launched service with file: " + mSelectedFile.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateRecyclerContent() {
        File root = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        List<File> sdFiles = Arrays.asList(root.listFiles());
        mAdapter.setFiles(sdFiles);
        mAdapter.notifyDataSetChanged();
    }

    private void initRecyclerView() {
        mFileListRecycler = findViewById(R.id.recycler_file_list);
        mAdapter = new FileAdapter(new ArrayList<File>());
        mAdapter.setOnItemClickListener(this);
        mFileListRecycler.setAdapter(mAdapter);
    }

    private void initBroadCastReceiver() {
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
        updateRecyclerContent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onItemClick(File file) {
        mSelectedFile = file;
        mRecordingName.setText(file.getName());
        //todo: update current track in playback service (if launched)
    }
}