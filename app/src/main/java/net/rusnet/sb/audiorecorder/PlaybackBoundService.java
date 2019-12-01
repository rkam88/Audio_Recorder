package net.rusnet.sb.audiorecorder;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class PlaybackBoundService extends Service {

    public static final int MSG_PLAY = 1;
    public static final int MSG_STOP = 2;
    public static final int MSG_NEXT = 3;
    public static final int MSG_PREV = 4;

    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private List<File> mFiles;
    private int mCurrentFilePosition;
    private MediaPlayer mMediaPlayer;
    private MediaPlayerStatus mMediaPlayerStatus;
    private Messenger mReplyToMessenger;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void updateFiles() {
        File root = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        mFiles = Arrays.asList(root.listFiles());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayerStatus = MediaPlayerStatus.IDLE;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mReplyToMessenger = null;
    }

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            mReplyToMessenger = msg.replyTo;
            switch (msg.what) {
                case MSG_PLAY:
                    updateFiles();
                    if (mCurrentFilePosition != msg.arg1) {
                        mMediaPlayer.reset();
                        mMediaPlayerStatus = MediaPlayerStatus.IDLE;
                    }
                    mCurrentFilePosition = msg.arg1;

                    if (mMediaPlayerStatus == MediaPlayerStatus.IDLE) {
                        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mMediaPlayerStatus = MediaPlayerStatus.PLAYBACK_COMPLETED;
                                updatePlayStopButtonInMainActivity(mReplyToMessenger);
                            }
                        });
                        prepareAndStartMediaPlayer();
                    }

                    mMediaPlayer.start();
                    mMediaPlayerStatus = MediaPlayerStatus.STARTED;

                    break;
                case MSG_STOP:
                    mMediaPlayer.stop();
                    try {
                        mMediaPlayer.prepare();
                        mMediaPlayerStatus = MediaPlayerStatus.PREPARED;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    break;
                case MSG_NEXT:
                    mCurrentFilePosition++;
                    if (mCurrentFilePosition >= mFiles.size()) mCurrentFilePosition = 0;
                    updateCurrentTrackInMainActivity(msg.replyTo);
                    updatePlayStopButtonInMainActivity(msg.replyTo);

                    mMediaPlayer.reset();
                    mMediaPlayerStatus = MediaPlayerStatus.IDLE;
                    prepareAndStartMediaPlayer();

                    break;
                case MSG_PREV:
                    mCurrentFilePosition--;
                    if (mCurrentFilePosition < 0) mCurrentFilePosition = mFiles.size() - 1;
                    updateCurrentTrackInMainActivity(msg.replyTo);
                    updatePlayStopButtonInMainActivity(msg.replyTo);

                    mMediaPlayer.reset();
                    mMediaPlayerStatus = MediaPlayerStatus.IDLE;
                    prepareAndStartMediaPlayer();

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void prepareAndStartMediaPlayer() {
        try {
            mMediaPlayer.setDataSource(mFiles.get(mCurrentFilePosition).getAbsolutePath());
            mMediaPlayer.prepare();
            mMediaPlayerStatus = MediaPlayerStatus.PREPARED;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateCurrentTrackInMainActivity(Messenger replyTo) {
        Message message = Message.obtain(null, MainActivity.MSG_UPDATE_CURRENT_FILE, mCurrentFilePosition, 0);
        try {
            replyTo.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void updatePlayStopButtonInMainActivity(Messenger replyTo) {
        Message message = Message.obtain(null, MainActivity.MSG_UPDATE_PLAY_PAUSE_BUTTON);
        try {
            replyTo.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


}

enum MediaPlayerStatus {
    IDLE,
    PLAYBACK_COMPLETED,
    STARTED,
    PREPARED
}

