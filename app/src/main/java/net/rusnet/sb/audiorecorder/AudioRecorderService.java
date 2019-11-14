package net.rusnet.sb.audiorecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AudioRecorderService extends Service {

    private static final String TAG = "TAG";

    private static final String CHANEL_ID = "Channel_1";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_PAUSE = "PauseRecording";
    private static final String ACTION_RESUME = "ResumeRecording";
    private static final String ACTION_STOP = "StopRecording";

    private RemoteViews mRemoteViews;

    private RecordingStatus mRecordingStatus;
    private String currentTime;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRecordingStatus = RecordingStatus.RECORDING;
        currentTime = "0";

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
            switch (intent.getAction()) {
                case ACTION_PAUSE:
                    //todo: pause recording
                    mRecordingStatus = RecordingStatus.PAUSED;
                    updateNotification(currentTime);

                    Log.d(TAG, "onStartCommand: launched with ACTION_PAUSE");
                    return START_NOT_STICKY;

                case ACTION_RESUME:
                    //todo: resume recording
                    mRecordingStatus = RecordingStatus.RECORDING;
                    updateNotification(currentTime);

                    Log.d(TAG, "onStartCommand: launched with ACTION_RESUME");
                    return START_NOT_STICKY;

                case ACTION_STOP:
                    //todo: stop recording
                    Log.d(TAG, "onStartCommand: launched with ACTION_STOP");
                    stopSelf();
                    return START_NOT_STICKY;
            }
        }

        startForeground(NOTIFICATION_ID, createNotification(currentTime));

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();


        //todo: check later
        Log.d(TAG, "onDestroy() called");
    }

    private void updateNotification(String time) {
        Notification notification = createNotification(time);

        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification(String time) {
        mRemoteViews = new RemoteViews(
                getPackageName(),
                R.layout.recording_notification);

        setupPauseAndResumeButton();
        setupStopButton();
        updateRecordingTimeTextView(time);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this,
                CHANEL_ID
        );
        builder.setContent(mRemoteViews)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(false)
                .setOngoing(true);

        return builder.build();
    }

    private void updateRecordingTimeTextView(String time) {
        mRemoteViews.setTextViewText(R.id.recording_time, getString(R.string.recording_time, time));
    }

    private void setupStopButton() {
        Intent stopIntent = new Intent(this, AudioRecorderService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mRemoteViews.setOnClickPendingIntent(
                R.id.button_stop,
                stopPendingIntent
        );
    }

    private void setupPauseAndResumeButton() {
        Intent pauseOrResumeIntent = new Intent(this, AudioRecorderService.class);
        switch (mRecordingStatus) {
            case PAUSED:
                pauseOrResumeIntent.setAction(ACTION_RESUME);
                mRemoteViews.setImageViewResource(R.id.button_pause_resume, R.drawable.ic_pause_black);
                break;
            case RECORDING:
                pauseOrResumeIntent.setAction(ACTION_PAUSE);
                mRemoteViews.setImageViewResource(R.id.button_pause_resume, R.drawable.ic_play_arrow_black);
                break;
        }
        PendingIntent pauseOrResumePendingIntent = PendingIntent.getService(
                this,
                0,
                pauseOrResumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mRemoteViews.setOnClickPendingIntent(
                R.id.button_pause_resume,
                pauseOrResumePendingIntent);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANEL_ID,
                    "Channel name",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Channel description");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

}

enum RecordingStatus {
    RECORDING,
    PAUSED
}

