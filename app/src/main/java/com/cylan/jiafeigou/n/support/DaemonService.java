package com.cylan.jiafeigou.n.support;

import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cylan.jiafeigou.BuildConfig;
import com.cylan.jiafeigou.R;
import com.cylan.jiafeigou.n.engine.DataSourceService;
import com.cylan.jiafeigou.utils.PathGetter;
import com.cylan.publicApi.ProcessUtils;
import com.cylan.support.DswLog;
import com.cylan.utils.FileUtils;

import java.io.File;

public class DaemonService extends Service {
    private static final String TAG = DaemonService.class.getSimpleName();
    public static final String KEY_DAEMON_NAME = "daemonName";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     * <p/>
     * name Used to name the worker thread, important only for debugging.
     */
    public DaemonService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    protected void onHandleIntent() {
        try {
            final String daemonServiceName = this.getClass().getName();
            final String filename = "daemon_c";
            final String daemonPath = getFilesDir() + filename;
            final File destFile = new File(daemonPath);
            FileUtils.copyAssets2Sdcard(this, destFile, "daemon_c");
            new File(daemonPath).setExecutable(true);
            final String packageName = getPackageName();
            final String processName = ProcessUtils.myProcessName(this) + ":push";
            final String logPath = PathGetter.getWslogPath();
            new ProcessBuilder().command(daemonPath,
                    packageName,
                    processName,
                    daemonServiceName,
                    BuildConfig.DEBUG ? "1" : "0", logPath)
                    .start();
            Log.d(TAG, "daemonPath: " + daemonPath);
            Log.d(TAG, "packageName: " + packageName);
            Log.d(TAG, "processName: " + processName);
            Log.d(TAG, "logPath: " + logPath);
        } catch (Exception e) {
            DswLog.ex(e.toString());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService(new Intent(this, DataSourceService.class));
        try2startForeground();
        onHandleIntent();
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 尝试提升优先级
     */
    private void try2startForeground() {
        if (Build.VERSION.SDK_INT >= 18) {
            //start an inner service
            startForeground(SERVICE_ID, sendEmptyNotification(this));
            startService(new Intent(this, InnerAssistService.class));
        } else {
            startForeground(SERVICE_ID, new Notification());
        }
    }

    private static final int SERVICE_ID = 11111;

    private static Notification sendEmptyNotification(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setWhen(System.currentTimeMillis());
        mBuilder.setAutoCancel(false);
        return mBuilder.build();
    }

    //此作用基于 微信保活意见
    public static class InnerAssistService extends IntentService {
        public InnerAssistService() {
            this("");
        }

        /**
         * Creates an IntentService.  Invoked by your subclass's constructor.
         *
         * @param name Used to name the worker thread, important only for debugging.
         */
        public InnerAssistService(String name) {
            super(name);
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            startForeground(SERVICE_ID, sendEmptyNotification(this));
        }
    }

    public static class BootCompletedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

}