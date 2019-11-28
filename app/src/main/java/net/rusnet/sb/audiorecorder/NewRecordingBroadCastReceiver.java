package net.rusnet.sb.audiorecorder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NewRecordingBroadCastReceiver extends BroadcastReceiver {

    public interface NewRecordingListener {
        void onNewRecordingAddedCallback();
    }

    private NewRecordingListener mListener;

    public NewRecordingBroadCastReceiver(NewRecordingListener listener) {
        mListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        mListener.onNewRecordingAddedCallback();
    }
}
