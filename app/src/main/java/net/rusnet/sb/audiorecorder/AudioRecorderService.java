package net.rusnet.sb.audiorecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class AudioRecorderService extends Service {

    private static final String CHANEL_ID = "Channel_1";
    private static final int NOTIFICATION_ID = 1;
    private static final String ACTION_PAUSE = "PauseRecording";
    private static final String ACTION_RESUME = "ResumeRecording";
    private static final String ACTION_STOP = "StopRecording";

    private RemoteViews mRemoteViews;

    private RecordingStatus mRecordingStatus;
    private int mCurrentTime;

    private String mFileName;
    private MediaRecorder mMediaRecorder;
    private Timer mTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mRecordingStatus = RecordingStatus.RECORDING;
        mCurrentTime = 0;

        createNotificationChannel();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && !TextUtils.isEmpty(intent.getAction())) {
            switch (intent.getAction()) {
                case ACTION_PAUSE:
                    mMediaRecorder.pause();
                    stopRecordingTimer();

                    mRecordingStatus = RecordingStatus.PAUSED;
                    updateNotification(mCurrentTime);

                    return START_NOT_STICKY;

                case ACTION_RESUME:
                    mMediaRecorder.resume();
                    startRecordingTimer();

                    mRecordingStatus = RecordingStatus.RECORDING;
                    updateNotification(mCurrentTime);

                    return START_NOT_STICKY;

                case ACTION_STOP:
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();
                    mMediaRecorder.release();
                    stopRecordingTimer();

                    stopSelf();

                    Intent newRecordingIntent = new Intent();
                    newRecordingIntent.setAction(MainActivity.ACTION_NEW_RECORD_ADDED);
                    sendBroadcast(newRecordingIntent);

                    return START_NOT_STICKY;
            }
        }

        startRecording();
        startRecordingTimer();
        startForeground(NOTIFICATION_ID, createNotification(mCurrentTime));
        return START_NOT_STICKY;

    }

    private void startRecordingTimer() {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                mCurrentTime++;
                updateNotification(mCurrentTime);
            }

        }, 0, 1000);
    }

    private void stopRecordingTimer() {
        mTimer.cancel();
        mTimer.purge();
    }

    private void startRecording() {
        mFileName = this.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath();

        Date date = new Date();
        String recordingDate = "/" + date.getTime() + ".3gp";
        mFileName += recordingDate;

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mMediaRecorder.setOutputFile(mFileName);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void updateNotification(int time) {
        Notification notification = createNotification(time);

        NotificationManagerCompat notificationManagerCompat =
                NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(NOTIFICATION_ID, notification);
    }

    private Notification createNotification(int time) {
        mRemoteViews = new RemoteViews(
                getPackageName(),
                R.layout.recording_notification);

        setupPauseAndResumeButton();
        setupStopButton();
        updateRecordingTimeTextView(String.valueOf(time));

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
                mRemoteViews.setImageViewResource(R.id.button_pause_resume, R.drawable.ic_play_arrow_black);
                break;
            case RECORDING:
                pauseOrResumeIntent.setAction(ACTION_PAUSE);
                mRemoteViews.setImageViewResource(R.id.button_pause_resume, R.drawable.ic_pause_black);
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

