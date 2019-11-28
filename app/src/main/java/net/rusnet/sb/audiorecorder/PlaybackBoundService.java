package net.rusnet.sb.audiorecorder;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class PlaybackBoundService extends Service {

    public static final String TAG = "TAG";//todo: remove all logs

    public static final int MSG_PLAY = 1;
    public static final int MSG_STOP = 2;
    public static final int MSG_NEXT = 3;
    public static final int MSG_PREV = 4;

    private final Messenger mMessenger = new Messenger(new IncomingHandler());
    private List<File> mFiles;
    private int mCurrentFilePosition;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private void updateFiles() {
        File root = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        mFiles = Arrays.asList(root.listFiles());
    }

    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PLAY:
                    updateFiles();
                    mCurrentFilePosition = msg.arg1;
                    Log.d(TAG, "MSG_PLAY");
                    Log.d(TAG, "Playing recording number: " + mCurrentFilePosition);
                    break;
                case MSG_STOP:
                    Log.d(TAG, "MSG_STOP");
                    break;
                case MSG_NEXT:
                    mCurrentFilePosition++;
                    if (mCurrentFilePosition >= mFiles.size()) mCurrentFilePosition = 0;
                    replyToMainActivity(msg.replyTo);
                    Log.d(TAG, "MSG_NEXT");
                    Log.d(TAG, "Playing recording number: " + mCurrentFilePosition);
                    break;
                case MSG_PREV:
                    mCurrentFilePosition--;
                    if (mCurrentFilePosition < 0) mCurrentFilePosition = mFiles.size() - 1;
                    replyToMainActivity(msg.replyTo);
                    Log.d(TAG, "MSG_PREV");
                    Log.d(TAG, "Playing recording number: " + mCurrentFilePosition);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void replyToMainActivity(Messenger replyTo) {
        Message message = Message.obtain(null, MainActivity.MSG_UPDATE_CURRENT_FILE, mCurrentFilePosition, 0);
        try {
            replyTo.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
