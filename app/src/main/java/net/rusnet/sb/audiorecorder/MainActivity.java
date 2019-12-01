package net.rusnet.sb.audiorecorder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
    private static final int NO_FILE_SELECTED = -1;

    private NewRecordingBroadCastReceiver broadcastReceiver;

    private RecyclerView mFileListRecycler;
    private FileAdapter mAdapter;

    private TextView mRecordingName;
    private ImageButton mPlayImageButton;
    private Button mStartRecordingButton;
    private List<File> mFiles;
    private int mSelectedFilePosition = NO_FILE_SELECTED;

    private Messenger mServiceMessenger = null;
    private boolean mBound;
    private boolean mIsServicePlaying;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mServiceMessenger = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            mServiceMessenger = null;
            mBound = false;
        }
    };

    public static final int MSG_UPDATE_CURRENT_FILE = 10;
    public static final int MSG_UPDATE_PLAY_PAUSE_BUTTON = 20;
    private Messenger mClientMessenger = new Messenger(new IncomingHandlerMainActivity());

    class IncomingHandlerMainActivity extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_CURRENT_FILE:
                    mSelectedFilePosition = msg.arg1;
                    mRecordingName.setText(mFiles.get(mSelectedFilePosition).getName());
                    break;
                case MSG_UPDATE_PLAY_PAUSE_BUTTON:
                    mStartRecordingButton.setEnabled(true);
                    mIsServicePlaying = false;
                    mPlayImageButton.setImageResource(R.drawable.ic_play_arrow_black);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        initBroadCastReceiver();

        initRecyclerView();

        updateRecyclerContent();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(MainActivity.this, PlaybackBoundService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initViews() {
        mStartRecordingButton = findViewById(R.id.button_start_recording);
        mStartRecordingButton.setOnClickListener(new View.OnClickListener() {
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
                if (mSelectedFilePosition == NO_FILE_SELECTED) {
                    Toast.makeText(MainActivity.this, R.string.toast_select_a_recording, Toast.LENGTH_SHORT).show();
                } else {
                    if (!mIsServicePlaying) {
                        mStartRecordingButton.setEnabled(false);
                        mIsServicePlaying = true;
                        mPlayImageButton.setImageResource(R.drawable.ic_stop_black);
                        sendMessageToPlaybackService(PlaybackBoundService.MSG_PLAY, mSelectedFilePosition);
                    } else {
                        mStartRecordingButton.setEnabled(true);
                        mIsServicePlaying = false;
                        mPlayImageButton.setImageResource(R.drawable.ic_play_arrow_black);
                        sendMessageToPlaybackService(PlaybackBoundService.MSG_STOP);
                    }
                }
            }
        });

        findViewById(R.id.button_play_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound) {
                    if (mIsServicePlaying) {
                        sendMessageToPlaybackService(PlaybackBoundService.MSG_NEXT);
                    } else {
                        mSelectedFilePosition++;
                        if (mSelectedFilePosition >= mFiles.size()) mSelectedFilePosition = 0;
                        mRecordingName.setText(mFiles.get(mSelectedFilePosition).getName());
                    }
                }
            }
        });
        findViewById(R.id.button_play_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBound) {
                    if (mIsServicePlaying) {
                        sendMessageToPlaybackService(PlaybackBoundService.MSG_PREV);
                    } else {
                        mSelectedFilePosition--;
                        if (mSelectedFilePosition < 0) mSelectedFilePosition = mFiles.size() - 1;
                        mRecordingName.setText(mFiles.get(mSelectedFilePosition).getName());
                    }
                }
            }
        });
    }

    private void sendMessageToPlaybackService(int message) {
        sendMessageToPlaybackService(message, 0);
    }

    private void sendMessageToPlaybackService(int message, int position) {
        Message msg = Message.obtain(null, message, position, 0);
        msg.replyTo = mClientMessenger;
        try {
            mServiceMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updateRecyclerContent() {
        File root = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        mFiles = Arrays.asList(root.listFiles());
        mAdapter.setFiles(mFiles);
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
        updateRecyclerContent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onItemClick(int filePosition) {
        mSelectedFilePosition = filePosition;
        mRecordingName.setText(mFiles.get(mSelectedFilePosition).getName());
        if (mIsServicePlaying) {
            mPlayImageButton.callOnClick();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
    }
}