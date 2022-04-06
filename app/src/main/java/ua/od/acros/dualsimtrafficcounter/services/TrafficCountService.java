package ua.od.acros.dualsimtrafficcounter.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import android.util.SparseArray;
import android.widget.Toast;

import org.acra.ACRA;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDateTime;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseActionDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ChooseSimDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ManualSimDialog;
import ua.od.acros.dualsimtrafficcounter.events.ActionTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.events.PostNotificationEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetSimEvent;
import ua.od.acros.dualsimtrafficcounter.events.SetTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.events.TipTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.settings.TrafficLimitFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.CustomNotification;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import wei.mark.standout.StandOutWindow;


public class TrafficCountService extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static WeakReference<Context> mContext;
    private static long mLastUpdateTime;
    private static long mStartRX = 0;
    private static long mStartTX = 0;
    private static long mReceived1 = 0;
    private static long mTransmitted1 = 0;
    private static long mReceived2 = 0;
    private static long mTransmitted2 = 0;
    private static long mReceived3 = 0;
    private static long mTransmitted3 = 0;
    private static boolean mIsSIM1OverLimit = false;
    private static boolean mIsSIM2OverLimit = false;
    private static boolean mIsSIM3OverLimit = false;
    private static boolean mIsNight1 = false;
    private static boolean mIsNight2 = false;
    private static boolean mIsNight3 = false;
    private static int mSimQuantity;
    private static int mActiveSIM = Constants.DISABLED;
    private static int mLastActiveSIM = Constants.DISABLED;
    private static LocalDateTime mNowDate;// mLastDate, mResetTime1, mResetTime2, mResetTime3;
    private static ContentValues mTrafficData;
    private static CustomDatabaseHelper mDbHelper;
    private static ScheduledExecutorService mTaskExecutor = null;
    private static ScheduledFuture<?> mTaskResult = null;
    private static SharedPreferences mPrefs;
    private static String[] mOperatorNames = new String[3];
    private static boolean mHasActionChosen1, mHasActionChosen2, mHasActionChosen3,
            mHasPreLimitNotificationShown1, mHasPreLimitNotificationShown2, mHasPreLimitNotificationShown3,
            mSIM1ContinueOverLimit, mSIM2ContinueOverLimit, mSIM3ContinueOverLimit,
            mIsResetNeeded3, mIsResetNeeded2, mIsResetNeeded1;
    private static long[] mLimits = new long[3];
    private static Handler mHandler;
    private static boolean[] mFlashPreOverLimit;
    private static ArrayList<String> mIMSI = null;
    private static SparseArray<Long[]> mRxTx = null;
    private static ArrayList<String> mUids = null;
    private static CountDownTimer mTimer = null;
    private static Service mService = null;
    private static UidObserver mUidObserver;
    private static boolean mDoNotStopService = false;

    public TrafficCountService() {
    }

    @Override
    public final IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static int getActiveSIM() {
        return mActiveSIM;
    }

    private static class UidObserver extends ContentObserver {
        UidObserver() {
            super(null);
        }

        @Override
        public final void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public final void onChange(boolean selfChange, Uri uri) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            startNewTimerTask(Constants.COUNT);
        }

        @Override
        public final boolean deliverSelfNotifications() {
            return true;
        }
    }

    @Override
    public final void onCreate() {
        super.onCreate();

        mService = this;
        Context ctx = CustomApplication.getAppContext();
        mContext = new WeakReference<Context>(ctx);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        mSimQuantity = mPrefs.getInt(Constants.PREF_OTHER[55], 1);

        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            mIMSI = MobileUtils.getSimIds(ctx);
            CustomApplication.loadTrafficPreferences(mIMSI);
            mPrefs = null;
            mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        mPrefs.registerOnSharedPreferenceChangeListener(this);

        // cancel if already existed
        if (mTaskExecutor != null) {
            if (mTaskResult != null)
                mTaskResult.cancel(true);
            mTaskExecutor.shutdown();
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        } else {
            // recreate new
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    @Subscribe
    public final void onMessageEvent(MobileConnectionEvent event) {
        Context ctx = mContext.get();
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        if (mTimer != null)
            mTimer.cancel();
        if (mSimQuantity == 1) {
            mActiveSIM = Constants.SIM1;
            startNewTimerTask(Constants.COUNT);
        } else {
            if (mPrefs.getBoolean(Constants.PREF_OTHER[43], true)) {
                mActiveSIM = MobileUtils.getActiveSimForData(ctx);
                if (mActiveSIM != Constants.DISABLED)
                    startNewTimerTask(Constants.COUNT);
                else {
                    Intent dialogIntent = new Intent(ctx, ManualSimDialog.class);
                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    if (!ManualSimDialog.isActive())
                        ctx.startActivity(dialogIntent);
                }
            } else {
                Intent dialogIntent = new Intent(ctx, ChooseSimDialog.class);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!ChooseSimDialog.isActive())
                    ctx.startActivity(dialogIntent);
            }
        }
    }

    @Subscribe
    public final void onMessageEvent(SetSimEvent event) {
        if (event.action != null) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            mActiveSIM = event.sim;
            startNewTimerTask(Constants.COUNT);
        }
    }

    @Subscribe
    public final void onMessageEvent(PostNotificationEvent event) {
        updateNotification(mActiveSIM);
    }

    @Subscribe
    public final void onMessageEvent(NoConnectivityEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }

        mLastActiveSIM = mActiveSIM;

        if (mPrefs.getBoolean(Constants.PREF_SIM1[14], true) && mLastActiveSIM == Constants.SIM1)
            mTrafficData.put(Constants.TOTAL1, DataFormat.getRoundLong((long) mTrafficData.get(Constants.TOTAL1),
                    mPrefs.getString(Constants.PREF_SIM1[15], "1"), Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM1[16], "0"))));

        if (mPrefs.getBoolean(Constants.PREF_SIM2[14], true) && mLastActiveSIM == Constants.SIM2)
            mTrafficData.put(Constants.TOTAL2, DataFormat.getRoundLong((long) mTrafficData.get(Constants.TOTAL2),
                    mPrefs.getString(Constants.PREF_SIM2[15], "1"), Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM2[16], "0"))));

        if (mPrefs.getBoolean(Constants.PREF_SIM3[14], true) && mLastActiveSIM == Constants.SIM3)
            mTrafficData.put(Constants.TOTAL3, DataFormat.getRoundLong((long) mTrafficData.get(Constants.TOTAL3),
                    mPrefs.getString(Constants.PREF_SIM3[15], "1"), Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM3[16], "0"))));

        writeTrafficDataToDatabase(mLastActiveSIM);
        sendDataBroadcast(0L, 0L);
        boolean floatingWindow = mPrefs.getBoolean(Constants.PREF_OTHER[32], false);
        boolean alwaysShow = !mPrefs.getBoolean(Constants.PREF_OTHER[41], false) && !mPrefs.getBoolean(Constants.PREF_OTHER[47], false);
        boolean bool = floatingWindow && !alwaysShow;
        if (bool)
            FloatingWindowService.closeFloatingWindow(mContext.get(), mPrefs);
        if (!mDoNotStopService && mPrefs.getBoolean(Constants.PREF_OTHER[47], false)) {
            mTimer.start();
        }
    }

    @Subscribe
    public final void onMessageEvent(ActionTrafficEvent event) {
        final Context ctx = mContext.get();
        int sim = event.sim;
        switch (sim) {
            case Constants.SIM1:
                mHasActionChosen1 = true;
                break;
            case Constants.SIM2:
                mHasActionChosen2 = true;
                break;
            case Constants.SIM3:
                mHasActionChosen3 = true;
                break;
        }
        mPrefs.edit()
                .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                .apply();
        try {
            switch (event.action) {
                case Constants.CHANGE_ACTION:
                    if (!mIsSIM2OverLimit && sim == Constants.SIM1)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM2);
                    else if (!mIsSIM3OverLimit && sim == Constants.SIM1)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM3);
                    else if (!mIsSIM1OverLimit && sim == Constants.SIM2)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    else if (!mIsSIM3OverLimit && sim == Constants.SIM2)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM3);
                    else if (!mIsSIM1OverLimit && sim == Constants.SIM3)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    else if (!mIsSIM2OverLimit && sim == Constants.SIM3)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM2);
                    else
                        startNewTimerTask(Constants.CHECK);
                    break;
                case Constants.SETTINGS_ACTION:
                    startActivity(CustomApplication.getSettingsIntent());
                    break;
                case Constants.LIMIT_ACTION:
                    Intent i = new Intent(ctx, SettingsActivity.class);
                    i.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, TrafficLimitFragment.class.getName());
                    i.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    i.putExtra(Constants.SIM_ACTIVE, sim);
                    startActivity(i);
                    startNewTimerTask(Constants.CHECK);
                    break;
                case Constants.CONTINUE_ACTION:
                    if (!MobileUtils.isMobileDataActive(ctx)) {
                        if (CustomApplication.canToggleOn())
                            MobileUtils.toggleMobileDataConnection(true, ctx, sim);
                        else
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(ctx, R.string.enable_mobile_data, Toast.LENGTH_LONG).show();
                                }
                            });
                    }
                    switch (sim) {
                        case Constants.SIM1:
                            mSIM1ContinueOverLimit = true;
                            break;
                        case Constants.SIM2:
                            mSIM2ContinueOverLimit = true;
                            break;
                        case Constants.SIM3:
                            mSIM3ContinueOverLimit = true;
                            break;
                    }
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                            .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                            .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                            .apply();
                    break;
                case Constants.OFF_ACTION:
                    MobileUtils.toggleMobileDataConnection(false, ctx, mActiveSIM);
                    if (CustomApplication.canToggleOn())
                        startNewTimerTask(Constants.CHECK);
                    else {
                        switch (mActiveSIM) {
                            case Constants.SIM1:
                                mSIM1ContinueOverLimit = true;
                                break;
                            case Constants.SIM2:
                                mSIM2ContinueOverLimit = true;
                                break;
                            case Constants.SIM3:
                                mSIM3ContinueOverLimit = true;
                                break;
                        }
                        mPrefs.edit()
                                .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                .apply();
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }

    }

    @Subscribe(sticky = true)
    public final void onMessageEvent(SetTrafficEvent event) {
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        CustomApplication.sleep(1000);
        if (mTrafficData == null)
            readTrafficDataFromDatabase();
        mNowDate = DateTime.now().toLocalDateTime();
        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
        int sim = event.sim;
        switch (sim) {
            case Constants.SIM1:
                mReceived1 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted1 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight1) {
                    mTrafficData.put(Constants.SIM1RX_N, mReceived1);
                    mTrafficData.put(Constants.SIM1TX_N, mTransmitted1);
                    mTrafficData.put(Constants.TOTAL1_N, mReceived1 + mTransmitted1);
                } else {
                    mTrafficData.put(Constants.SIM1RX, mReceived1);
                    mTrafficData.put(Constants.SIM1TX, mTransmitted1);
                    mTrafficData.put(Constants.TOTAL1, mReceived1 + mTransmitted1);
                }
                break;
            case Constants.SIM2:
                mReceived2 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted2 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight2) {
                    mTrafficData.put(Constants.SIM2RX_N, mReceived2);
                    mTrafficData.put(Constants.SIM2TX_N, mTransmitted2);
                    mTrafficData.put(Constants.TOTAL2_N, mReceived2 + mTransmitted2);
                } else {
                    mTrafficData.put(Constants.SIM2RX, mReceived2);
                    mTrafficData.put(Constants.SIM2TX, mTransmitted2);
                    mTrafficData.put(Constants.TOTAL2, mReceived2 + mTransmitted2);
                }
                break;
            case Constants.SIM3:
                mReceived3 = DataFormat.getFormatLong(event.rx, event.rxv);
                mTransmitted3 = DataFormat.getFormatLong(event.tx, event.txv);
                if (mIsNight3) {
                    mTrafficData.put(Constants.SIM3RX_N, mReceived3);
                    mTrafficData.put(Constants.SIM3TX_N, mTransmitted3);
                    mTrafficData.put(Constants.TOTAL3_N, mReceived3 + mTransmitted3);
                } else {
                    mTrafficData.put(Constants.SIM3RX, mReceived3);
                    mTrafficData.put(Constants.SIM3TX, mTransmitted3);
                    mTrafficData.put(Constants.TOTAL3, mReceived3 + mTransmitted3);
                }
                break;
        }
        writeTrafficDataToDatabase(sim);
        if (CustomApplication.isScreenOn())
            updateNotification(sim);
        if ((CustomApplication.isActivityVisible() || CustomApplication.getWidgetIds(Constants.TRAFFIC).length != 0) &&
                CustomApplication.isScreenOn())
            sendDataBroadcast(0L, 0L);
        EventBus.getDefault().removeStickyEvent(event);
        if(!MobileUtils.isMobileDataActive(mContext.get()))
            mTimer.start();
    }

    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {
        final Context ctx = mContext.get();
        boolean mobile = MobileUtils.isMobileDataActive(ctx);
        if ((mPrefs.getBoolean(Constants.PREF_OTHER[47], false) && !mobile) ||
                mPrefs.getBoolean(Constants.PREF_OTHER[5], false))
            mService.stopSelf();
        else {
            mHandler = new Handler();

            mTimer = new CountDownTimer(10000, 10000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    //stop WatchDogService
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[4], true))
                        ctx.stopService(new Intent(ctx, WatchDogService.class));
                    mService.stopSelf();
                }
            };

            mUidObserver = new UidObserver();
            getContentResolver().registerContentObserver(Constants.UID_URI, true, mUidObserver);

            mDbHelper = CustomDatabaseHelper.getInstance(ctx);
            mTrafficData = new ContentValues();
            readTrafficDataFromDatabase();
            if (mTrafficData.get(Constants.LAST_DATE).equals("")) {
                LocalDateTime dateTime = DateTime.now().toLocalDateTime();
                mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
                mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
            }
            mActiveSIM = Constants.DISABLED;
            mLastActiveSIM = mPrefs.getInt(Constants.PREF_OTHER[46], Constants.DISABLED);

            mIsNight1 = CustomApplication.getIsNightState()[0];
            mIsNight2 = CustomApplication.getIsNightState()[1];
            mIsNight3 = CustomApplication.getIsNightState()[2];

            mOperatorNames = new String[]{MobileUtils.getName(ctx, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                    MobileUtils.getName(ctx, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                    MobileUtils.getName(ctx, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
            mFlashPreOverLimit = new boolean[]{false, false, false};

            sendDataBroadcast(0L, 0L);

            if (!EventBus.getDefault().isRegistered(this))
                EventBus.getDefault().register(this);

            startForeground(Constants.STARTED_ID, buildNotification(mLastActiveSIM));

            if (mobile) {
                mHasPreLimitNotificationShown1 = false;
                mHasPreLimitNotificationShown2 = false;
                mHasPreLimitNotificationShown3 = false;

                mSIM1ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM1[27], false);
                mSIM2ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM2[27], false);
                mSIM3ContinueOverLimit = mPrefs.getBoolean(Constants.PREF_SIM3[27], false);

                mHasActionChosen1 = mPrefs.getBoolean(Constants.PREF_SIM1[28], false);
                mHasActionChosen2 = mPrefs.getBoolean(Constants.PREF_SIM2[28], false);
                mHasActionChosen3 = mPrefs.getBoolean(Constants.PREF_SIM3[28], false);

                mIsResetNeeded1 = mPrefs.getBoolean(Constants.PREF_SIM1[25], false);
                //if (mIsResetNeeded1)
                //    mResetTime1 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM1[26], "1970-01-01 00:00"));
                mIsResetNeeded2 = mPrefs.getBoolean(Constants.PREF_SIM2[25], false);
                //if (mIsResetNeeded2)
                //    mResetTime2 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM2[26], "1970-01-01 00:00"));
                mIsResetNeeded3 = mPrefs.getBoolean(Constants.PREF_SIM3[25], false);
                //if (mIsResetNeeded3)
                //    mResetTime3 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM3[26], "1970-01-01 00:00"));

                mLimits = CustomApplication.getTrafficSimLimitsValues();

                // schedule task
                if (mSimQuantity == 1) {
                    mActiveSIM = Constants.SIM1;
                    startNewTimerTask(Constants.COUNT);
                } else {
                    if (mPrefs.getBoolean(Constants.PREF_OTHER[43], true)) {
                        mActiveSIM = MobileUtils.getActiveSimForData(ctx);
                        if (mActiveSIM != Constants.DISABLED)
                            startNewTimerTask(Constants.COUNT);
                        else {
                            Intent dialogIntent = new Intent(ctx, ManualSimDialog.class);
                            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (!ManualSimDialog.isActive())
                                ctx.startActivity(dialogIntent);
                        }
                    } else {
                        Intent dialogIntent = new Intent(ctx, ChooseSimDialog.class);
                        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        if (!ChooseSimDialog.isActive())
                            ctx.startActivity(dialogIntent);
                    }
                }
            }

            mPrefs.edit()
                    .putBoolean(Constants.PREF_OTHER[48], true)
                    .apply();
        }
        return START_STICKY;
    }

    private void readTrafficDataFromDatabase() {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext.get());
            ContentValues cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(0));
            mTrafficData.put(Constants.SIM1RX, (long) cv.get("rx"));
            mTrafficData.put(Constants.SIM1TX, (long) cv.get("tx"));
            mTrafficData.put(Constants.TOTAL1, (long) cv.get("total"));
            mTrafficData.put(Constants.SIM1RX_N, (long) cv.get("rx_n"));
            mTrafficData.put(Constants.SIM1TX_N, (long) cv.get("tx_n"));
            mTrafficData.put(Constants.TOTAL1_N, (long) cv.get("total_n"));
            mTrafficData.put(Constants.PERIOD1, (int) cv.get("period"));
            mTrafficData.put(Constants.SIM2RX, 0L);
            mTrafficData.put(Constants.SIM3RX, 0L);
            mTrafficData.put(Constants.SIM2TX, 0L);
            mTrafficData.put(Constants.SIM3TX, 0L);
            mTrafficData.put(Constants.TOTAL2, 0L);
            mTrafficData.put(Constants.TOTAL3, 0L);
            mTrafficData.put(Constants.SIM2RX_N, 0L);
            mTrafficData.put(Constants.SIM3RX_N, 0L);
            mTrafficData.put(Constants.SIM2TX_N, 0L);
            mTrafficData.put(Constants.SIM3TX_N, 0L);
            mTrafficData.put(Constants.TOTAL2_N, 0L);
            mTrafficData.put(Constants.TOTAL3_N, 0L);
            mTrafficData.put(Constants.LAST_TIME, (String) cv.get(Constants.LAST_TIME));
            mTrafficData.put(Constants.LAST_DATE, (String) cv.get(Constants.LAST_DATE));
            if (mSimQuantity >= 2) {
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(1));
                mTrafficData.put(Constants.SIM2RX, (long) cv.get("rx"));
                mTrafficData.put(Constants.SIM2TX, (long) cv.get("tx"));
                mTrafficData.put(Constants.TOTAL2, (long) cv.get("total"));
                mTrafficData.put(Constants.SIM2RX_N, (long) cv.get("rx_n"));
                mTrafficData.put(Constants.SIM2TX_N, (long) cv.get("tx_n"));
                mTrafficData.put(Constants.TOTAL2_N, (long) cv.get("total_n"));
                mTrafficData.put(Constants.PERIOD2, (int) cv.get("period"));
            }
            if (mSimQuantity == 3) {
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(2));
                mTrafficData.put(Constants.SIM3RX, (long) cv.get("rx"));
                mTrafficData.put(Constants.SIM3TX, (long) cv.get("tx"));
                mTrafficData.put(Constants.TOTAL3, (long) cv.get("total"));
                mTrafficData.put(Constants.SIM3RX_N, (long) cv.get("rx_n"));
                mTrafficData.put(Constants.SIM3TX_N, (long) cv.get("tx_n"));
                mTrafficData.put(Constants.TOTAL3_N, (long) cv.get("total_n"));
                mTrafficData.put(Constants.PERIOD3, (int) cv.get("period"));
            }
        } else
            mTrafficData = CustomDatabaseHelper.readTrafficData(mDbHelper);
        if (mTrafficData.get(Constants.LAST_DATE).equals("")) {
            LocalDateTime dateTime = DateTime.now().toLocalDateTime();
            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
        }
    }

    private static Long[] getBytesForUid(int uid) {
        File dir = new File("/proc/uid_stat/");
        String[] children = dir.list();
        if (children == null || !Arrays.asList(children).contains(String.valueOf(uid)))
            return new Long[] {0L, 0L};
        File uidFileDir = new File("/proc/uid_stat/" + uid);
        File uidActualFileReceived = new File(uidFileDir, "tcp_rcv");
        File uidActualFileSent = new File(uidFileDir, "tcp_snd");
        String textReceived = "0";
        String textSent = "0";
        try {
            BufferedReader brReceived = new BufferedReader(new FileReader(uidActualFileReceived));
            BufferedReader brSent = new BufferedReader(new FileReader(uidActualFileSent));
            String receivedLine;
            String sentLine;
            if ((receivedLine = brReceived.readLine()) != null)
                textReceived = receivedLine;
            if ((sentLine = brSent.readLine()) != null)
                textSent = sentLine;
        } catch (IOException e) {
        }
        return new Long[] {Long.valueOf(textReceived), Long.valueOf(textSent)};
    }

    private static void startNewTimerTask(int task) {
        TimerTask tTask = null;
        if (task == Constants.COUNT) {
            mDoNotStopService = false;
            mStartRX = TrafficStats.getMobileRxBytes();
            mStartTX = TrafficStats.getMobileTxBytes();
            String[] prefs = new String[Constants.PREF_SIM_DATA.length];
            mUids = null;
            switch (mActiveSIM) {
                case Constants.SIM1:
                    tTask = new CountTimerTask1();
                    prefs = Constants.PREF_SIM1;
                    break;
                case Constants.SIM2:
                    tTask = new CountTimerTask2();
                    prefs = Constants.PREF_SIM2;
                    break;
                case Constants.SIM3:
                    tTask = new CountTimerTask3();
                    prefs = Constants.PREF_SIM3;
                    break;
            }
            if (mPrefs.getBoolean(prefs[33], false)) {
                mUids = CustomDatabaseHelper.readList(mActiveSIM, mDbHelper, mIMSI, "uid");
                mRxTx = new SparseArray<>();
                for (String uid : mUids) {
                    int id = Integer.valueOf(uid);
                    Long[] rxtx = getBytesForUid(id);
                    mRxTx.put(id, rxtx);
                }
            }
            mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[46], mActiveSIM)
                    .apply();
            updateNotification(mActiveSIM);
            boolean floatingWindow = mPrefs.getBoolean(Constants.PREF_OTHER[32], false);
            boolean alwaysShow = !mPrefs.getBoolean(Constants.PREF_OTHER[41], false) && !mPrefs.getBoolean(Constants.PREF_OTHER[47], false);
            boolean bool = floatingWindow && !alwaysShow;
            if (bool)
                FloatingWindowService.showFloatingWindow(mContext.get(), mPrefs);
        } else
            tTask = new CheckTimerTask();
        if (mTaskResult == null || mTaskResult.isCancelled())
            mTaskExecutor = Executors.newSingleThreadScheduledExecutor();
        if (tTask != null) {
            mTaskResult = mTaskExecutor.scheduleAtFixedRate(tTask, 0, Constants.NOTIFY_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Context ctx = mContext.get();
        if (key.equals(Constants.PREF_SIM_DATA[33]) || key.equals(Constants.PREF_SIM1[33]) ||
                key.equals(Constants.PREF_SIM2[33]) || key.equals(Constants.PREF_SIM3[33])) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            startNewTimerTask(Constants.COUNT);
        }
        if (key.equals(Constants.PREF_OTHER[15]) || key.equals(Constants.PREF_SIM1[23]) ||
                key.equals(Constants.PREF_SIM2[23]) || key.equals(Constants.PREF_SIM3[23]))
            updateNotification(mActiveSIM);
        if (key.equals(Constants.PREF_OTHER[44])) {
            writeTrafficDataToDatabase(mActiveSIM);
            readTrafficDataFromDatabase();
        }
        if (key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2]) || key.equals(Constants.PREF_SIM1[3])
                || key.equals(Constants.PREF_SIM1[4]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10])) {
            mSIM1ContinueOverLimit = false;
            mHasActionChosen1 = false;
            mPrefs.edit()
                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                    .apply();
        }
        if (key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2]) || key.equals(Constants.PREF_SIM2[3])
                || key.equals(Constants.PREF_SIM2[4]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]))  {
            mSIM2ContinueOverLimit = false;
            mHasActionChosen2 = false;
            mPrefs.edit()
                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                    .apply();
        }
        if (key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2]) || key.equals(Constants.PREF_SIM3[3])
                || key.equals(Constants.PREF_SIM3[4]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10]))  {
            mSIM3ContinueOverLimit = false;
            mHasActionChosen3 = false;
            mPrefs.edit()
                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                    .apply();
        }
        if (key.equals(Constants.PREF_SIM1[3]) || key.equals(Constants.PREF_SIM1[9]) || key.equals(Constants.PREF_SIM1[10]) ||
                key.equals(Constants.PREF_SIM2[3]) || key.equals(Constants.PREF_SIM2[9]) || key.equals(Constants.PREF_SIM2[10]) ||
                key.equals(Constants.PREF_SIM3[3]) || key.equals(Constants.PREF_SIM3[9]) || key.equals(Constants.PREF_SIM3[10])) {
            checkIfResetNeeded(true);
        }
        if (key.equals(Constants.PREF_SIM1[1]) || key.equals(Constants.PREF_SIM1[2]) || key.equals(Constants.PREF_SIM1[4]) ||
                key.equals(Constants.PREF_SIM1[29]) || key.equals(Constants.PREF_SIM1[30]) ||
                key.equals(Constants.PREF_SIM2[1]) || key.equals(Constants.PREF_SIM2[2]) || key.equals(Constants.PREF_SIM2[4]) ||
                key.equals(Constants.PREF_SIM2[29]) || key.equals(Constants.PREF_SIM2[30]) ||
                key.equals(Constants.PREF_SIM3[1]) || key.equals(Constants.PREF_SIM3[2]) || key.equals(Constants.PREF_SIM3[4]) ||
                key.equals(Constants.PREF_SIM3[29]) || key.equals(Constants.PREF_SIM3[30])) {
            mLimits = CustomApplication.getTrafficSimLimitsValues();
            mHasPreLimitNotificationShown1 = false;
            mHasPreLimitNotificationShown2 = false;
            mHasPreLimitNotificationShown3 = false;
            mFlashPreOverLimit = new boolean[] {false, false, false};

        }
        if (key.equals(Constants.PREF_SIM1[5]) || key.equals(Constants.PREF_SIM1[6]))
            mOperatorNames[0] = MobileUtils.getName(ctx, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1);
        if (key.equals(Constants.PREF_SIM2[5]) || key.equals(Constants.PREF_SIM2[6]))
            mOperatorNames[1] = MobileUtils.getName(ctx, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2);
        if (key.equals(Constants.PREF_SIM3[5]) || key.equals(Constants.PREF_SIM3[6]))
            mOperatorNames[2] = MobileUtils.getName(ctx, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3);
        if (key.equals(Constants.PREF_OTHER[24]) && sharedPreferences.getBoolean(key, false)) {
            new CountDownTimer(2000, 2000) {
                @Override
                public void onTick(long millisUntilFinished) {

                }

                @Override
                public void onFinish() {
                    updateNotification(mActiveSIM);
                }
            }.start();
        }
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[44], false)) {
            int sim = Constants.DISABLED;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM1)).contains(key))
                sim = Constants.SIM1;
            else if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM2)).contains(key))
                sim = Constants.SIM2;
            else if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM3)).contains(key))
                sim = Constants.SIM3;
            if (sim >= 0 && sim < mIMSI.size()) {
                Map prefs = sharedPreferences.getAll();
                Object o = prefs.get(key);
                key = key.substring(0, key.length() - 1);
                SharedPreferences.Editor editor = getSharedPreferences(Constants.TRAFFIC + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
                boolean operator = key.equals(Constants.PREF_SIM_DATA[5]) ||
                        key.equals(Constants.PREF_SIM_DATA[6]);
                if (!operator)
                    CustomApplication.putObject(editor, key, o);
                editor.apply();
            }
        }
    }

    private static void checkIfResetNeeded(boolean settingsChanged) {
        String[] simPref = new String[]{Constants.PREF_SIM1[3], Constants.PREF_SIM1[9], Constants.PREF_SIM1[10]};
        LocalDateTime resetTime1 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM1[26], "1970-01-01 00:00")));
        if (mNowDate == null)
            mNowDate = DateTime.now().toLocalDateTime();
        if (mNowDate.compareTo(resetTime1) >= 0 || settingsChanged) {
            resetTime1 = DateUtils.setResetDate(mPrefs, simPref);
            if (resetTime1 != null) {
                mPrefs.edit()
                        .putString(Constants.PREF_SIM1[26], resetTime1.toString(Constants.DATE_TIME_FORMATTER))
                        .apply();
                if (!settingsChanged) {
                    mIsResetNeeded1 = true;
                    mPrefs.edit()
                            .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                            .apply();
                }
            }
        }
        if (mSimQuantity >= 2) {
            simPref = new String[]{Constants.PREF_SIM2[3], Constants.PREF_SIM2[9], Constants.PREF_SIM2[10]};
            LocalDateTime resetTime2 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM2[26], "1970-01-01 00:00")));
            if (mNowDate.compareTo(resetTime2) >= 0 || settingsChanged) {
                resetTime2 = DateUtils.setResetDate(mPrefs, simPref);
                if (resetTime2 != null) {
                    mPrefs.edit()
                            .putString(Constants.PREF_SIM2[26], resetTime2.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                    if (!settingsChanged) {
                        mIsResetNeeded2 = true;
                        mPrefs.edit()
                                .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                .apply();
                    }
                }
            }
        }
        if (mSimQuantity == 3) {
            simPref = new String[]{Constants.PREF_SIM3[3], Constants.PREF_SIM3[9], Constants.PREF_SIM3[10]};
            LocalDateTime resetTime3 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM3[26], "1970-01-01 00:00")));
            if (mNowDate.compareTo(resetTime3) >= 0 || settingsChanged) {
                resetTime3 = DateUtils.setResetDate(mPrefs, simPref);
                if (resetTime3 != null) {
                    mPrefs.edit()
                            .putString(Constants.PREF_SIM3[26], resetTime3.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                    if (!settingsChanged) {
                        mIsResetNeeded3 = true;
                        mPrefs.edit()
                                .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                .apply();
                    }
                }
            }
        }
    }

    private static class CheckTimerTask extends TimerTask {

        @Override
        public final void run() {
            final Context ctx = mContext.get();
            EventBus.getDefault().post(new TipTrafficEvent());
            LocalDateTime dt = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.get(Constants.LAST_DATE));

            long tot1 = mIsNight1 ? (long) mTrafficData.get(Constants.TOTAL1_N) : (long) mTrafficData.get(Constants.TOTAL1);
            long tot2 = mIsNight2 ? (long) mTrafficData.get(Constants.TOTAL2_N) : (long) mTrafficData.get(Constants.TOTAL2);
            long tot3 = mIsNight3 ? (long) mTrafficData.get(Constants.TOTAL3_N) : (long) mTrafficData.get(Constants.TOTAL3);
            try {
                if (mPrefs.getBoolean(Constants.PREF_SIM1[31], false) && mIsSIM1OverLimit &&
                        (DateUtils.isNextDayOrMonth(dt, Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM1[3], "")))
                        || (tot1 <= mLimits[0] && (mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        || (!mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) && !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    startNewTimerTask(Constants.COUNT);
                }
                if (mPrefs.getBoolean(Constants.PREF_SIM2[31], false) && mIsSIM2OverLimit &&
                        (DateUtils.isNextDayOrMonth(dt, Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM2[3], "")))
                        || (tot2 <= mLimits[1] && (mPrefs.getBoolean(Constants.PREF_SIM2[8], false)
                        || (!mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) && !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM2);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    startNewTimerTask(Constants.COUNT);
                }
                if (mPrefs.getBoolean(Constants.PREF_SIM3[31], false) && mIsSIM3OverLimit &&
                        (DateUtils.isNextDayOrMonth(dt, Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM3[3], "")))
                        || (tot3 <= mLimits[2] && (mPrefs.getBoolean(Constants.PREF_SIM3[8], false)
                        || (!mPrefs.getBoolean(Constants.PREF_SIM1[8], false)
                        && !mPrefs.getBoolean(Constants.PREF_SIM2[8], false) && !mPrefs.getBoolean(Constants.PREF_SIM3[8], false)))))) {
                    MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM3);
                    if (mTaskResult != null) {
                        mTaskResult.cancel(false);
                        mTaskExecutor.shutdown();
                    }
                    startNewTimerTask(Constants.COUNT);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private static class CountTimerTask1 extends TimerTask {

        @Override
        public final void run() {
            try {
                if (MobileUtils.isMobileDataActive(mContext.get())
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mTrafficData
                    //begin
                    mTrafficData.put(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                    mTrafficData.put(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    mTrafficData.put(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    mTrafficData.put(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                    mTrafficData.put(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    mTrafficData.put(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    mTrafficData.put(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                    mTrafficData.put(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    mTrafficData.put(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    mTrafficData.put(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                    mTrafficData.put(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    mTrafficData.put(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    mTrafficData.put(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                    mTrafficData.put(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    mTrafficData.put(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    mTrafficData.put(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                    mTrafficData.put(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                    mTrafficData.put(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                    mTrafficData.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                    mTrafficData.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                    //end

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.get(Constants.LAST_DATE));
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    mIsNight1 = CustomApplication.getIsNightState()[0];
                    mIsNight2 = CustomApplication.getIsNightState()[1];
                    mIsNight3 = CustomApplication.getIsNightState()[2];

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[44], false) && mIMSI != null) ?
                            CustomDatabaseHelper.isTableEmpty(mDbHelper, Constants.TRAFFIC + "_" + mIMSI.get(Constants.SIM1), false) :
                            CustomDatabaseHelper.isTableEmpty(mDbHelper, Constants.TRAFFIC, true);

                    if (emptyDB) {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                        mTrafficData.put(Constants.LAST_RX, 0L);
                        mTrafficData.put(Constants.LAST_TX, 0L);
                        mTrafficData.put(Constants.LAST_TIME, "");
                        mTrafficData.put(Constants.LAST_DATE, "");
                        writeTrafficDataToDatabase(Constants.DISABLED);
                    } else if (mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
                        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
                        if (mIsResetNeeded1) {
                            mTrafficData.put(Constants.SIM1RX, 0L);
                            mTrafficData.put(Constants.SIM1TX, 0L);
                            mTrafficData.put(Constants.TOTAL1, 0L);
                            mTrafficData.put(Constants.SIM1RX_N, 0L);
                            mTrafficData.put(Constants.SIM1TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL1_N, 0L);
                            rx = tx = mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mHasActionChosen1 = false;
                            mFlashPreOverLimit[Constants.SIM1] = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM1);
                            writeTrafficDataToDatabase(Constants.SIM1);
                        }
                        if (mIsResetNeeded2) {
                            mTrafficData.put(Constants.SIM2RX, 0L);
                            mTrafficData.put(Constants.SIM2TX, 0L);
                            mTrafficData.put(Constants.TOTAL2, 0L);
                            mTrafficData.put(Constants.SIM2RX_N, 0L);
                            mTrafficData.put(Constants.SIM2TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL2_N, 0L);
                            if (!mIsNight1) {
                                rx = (long) mTrafficData.get(Constants.SIM1RX);
                                tx = (long) mTrafficData.get(Constants.SIM1TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL1);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM1RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM1TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL1_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mHasActionChosen2 = false;
                            mFlashPreOverLimit[Constants.SIM2] = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM2);
                            writeTrafficDataToDatabase(Constants.SIM2);
                        }
                        if (mIsResetNeeded3) {
                            mTrafficData.put(Constants.SIM3RX, 0L);
                            mTrafficData.put(Constants.SIM3TX, 0L);
                            mTrafficData.put(Constants.TOTAL3, 0L);
                            mTrafficData.put(Constants.SIM3RX_N, 0L);
                            mTrafficData.put(Constants.SIM3TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL3_N, 0L);
                            if (!mIsNight1) {
                                rx = (long) mTrafficData.get(Constants.SIM1RX);
                                tx = (long) mTrafficData.get(Constants.SIM1TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL1);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM1RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM1TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL1_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mHasActionChosen3 = false;
                            mFlashPreOverLimit[Constants.SIM3] = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM3);
                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight1) {
                            rx = (long) mTrafficData.get(Constants.SIM1RX);
                            tx = (long) mTrafficData.get(Constants.SIM1TX);
                            tot = (long) mTrafficData.get(Constants.TOTAL1);
                        } else {
                            rx = (long) mTrafficData.get(Constants.SIM1RX_N);
                            tx = (long) mTrafficData.get(Constants.SIM1TX_N);
                            tot = (long) mTrafficData.get(Constants.TOTAL1_N);
                        }
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    if (mUids != null) {
                        for (String uid : mUids) {
                            int id = Integer.valueOf(uid);
                            Long[] rxtx = mRxTx.get(id);
                            Long[] rxtx_new = getBytesForUid(id);
                            if (rxtx != null) {
                                uidRx += rxtx_new[0] - rxtx[0];
                                uidTx += rxtx_new[1] - rxtx[1];
                                mRxTx.remove(id);
                                mRxTx.put(id, rxtx_new);
                            }
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    if ((tot <= mLimits[0]) || mSIM1ContinueOverLimit) {
                        rx += diffrx;
                        tx += difftx;
                        if (mPrefs.getBoolean(Constants.PREF_SIM1[32], false))
                            tot = rx;
                        else
                            tot = tx + rx;
                        mIsSIM1OverLimit = false;
                        if (mPrefs.getBoolean(Constants.PREF_SIM1[29], false) && !mHasPreLimitNotificationShown1 && !mHasActionChosen1) {
                            int left = (int) (100 * (1.0 - (double) tot / (double) mLimits[0]));
                            if (left < Integer.valueOf(Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM1[30], "0")))) {
                                mHasPreLimitNotificationShown1 = true;
                                mFlashPreOverLimit[Constants.SIM1] = true;
                                vibrate(1000);
                                showPreLimitToast(Constants.SIM1);
                            }
                        }
                    } else if (!mHasActionChosen1) {
                        mIsSIM1OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            pushAlertNotification(mActiveSIM);
                        makePreCheckActions(mActiveSIM);
                    }

                    if (!mIsSIM1OverLimit) {
                        if (!mIsNight1) {
                            mTrafficData.put(Constants.SIM1RX, rx);
                            mTrafficData.put(Constants.SIM1TX, tx);
                            mTrafficData.put(Constants.TOTAL1, tot);
                        } else {
                            mTrafficData.put(Constants.SIM1RX_N, rx);
                            mTrafficData.put(Constants.SIM1TX_N, tx);
                            mTrafficData.put(Constants.TOTAL1_N, tot);
                        }
                        postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);
                    }

                    if ((CustomApplication.isActivityVisible() ||
                            CustomApplication.getWidgetIds(Constants.TRAFFIC).length != 0 ||
                                    mPrefs.getBoolean(Constants.PREF_OTHER[32], false)) && CustomApplication.isScreenOn())
                        sendDataBroadcast(speedRX, speedTX);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private static class CountTimerTask2 extends TimerTask {

        @Override
        public final void run() {
            try {
                if (MobileUtils.isMobileDataActive(mContext.get())
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mTrafficData
                    //begin
                    mTrafficData.put(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                    mTrafficData.put(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    mTrafficData.put(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    mTrafficData.put(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                    mTrafficData.put(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    mTrafficData.put(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    mTrafficData.put(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                    mTrafficData.put(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    mTrafficData.put(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    mTrafficData.put(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                    mTrafficData.put(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    mTrafficData.put(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    mTrafficData.put(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                    mTrafficData.put(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    mTrafficData.put(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    mTrafficData.put(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                    mTrafficData.put(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                    mTrafficData.put(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                    mTrafficData.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                    mTrafficData.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                    //end

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.get(Constants.LAST_DATE));
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    mIsNight1 = CustomApplication.getIsNightState()[0];
                    mIsNight2 = CustomApplication.getIsNightState()[1];
                    mIsNight3 = CustomApplication.getIsNightState()[2];

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[44], false) && mIMSI != null) ?
                            CustomDatabaseHelper.isTableEmpty(mDbHelper, Constants.TRAFFIC + "_" + mIMSI.get(Constants.SIM2), false) :
                            CustomDatabaseHelper.isTableEmpty(mDbHelper, Constants.TRAFFIC, true);

                    if (emptyDB) {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                        mTrafficData.put(Constants.LAST_RX, 0L);
                        mTrafficData.put(Constants.LAST_TX, 0L);
                        mTrafficData.put(Constants.LAST_TIME, "");
                        mTrafficData.put(Constants.LAST_DATE, "");
                        writeTrafficDataToDatabase(Constants.DISABLED);
                    } else if (mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
                        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
                        if (mIsResetNeeded1) {
                            mTrafficData.put(Constants.SIM1RX, 0L);
                            mTrafficData.put(Constants.SIM1TX, 0L);
                            mTrafficData.put(Constants.TOTAL1, 0L);
                            mTrafficData.put(Constants.SIM1RX_N, 0L);
                            mTrafficData.put(Constants.SIM1TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL1_N, 0L);
                            if (!mIsNight2) {
                                rx = (long) mTrafficData.get(Constants.SIM2RX);
                                tx = (long) mTrafficData.get(Constants.SIM2TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL2);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM2RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM2TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL2_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mHasActionChosen1 = false;
                            mFlashPreOverLimit[Constants.SIM1] = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM1);
                            writeTrafficDataToDatabase(Constants.SIM1);
                        }
                        if (mIsResetNeeded2) {
                            mTrafficData.put(Constants.SIM2RX, 0L);
                            mTrafficData.put(Constants.SIM2TX, 0L);
                            mTrafficData.put(Constants.TOTAL2, 0L);
                            mTrafficData.put(Constants.SIM2RX_N, 0L);
                            mTrafficData.put(Constants.SIM2TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL2_N, 0L);
                            rx = tx = mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mHasActionChosen2 = false;
                            mFlashPreOverLimit[Constants.SIM2] = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM2);
                            writeTrafficDataToDatabase(Constants.SIM2);
                        }
                        if (mIsResetNeeded3) {
                            mTrafficData.put(Constants.SIM3RX, 0L);
                            mTrafficData.put(Constants.SIM3TX, 0L);
                            mTrafficData.put(Constants.TOTAL3, 0L);
                            mTrafficData.put(Constants.SIM3RX_N, 0L);
                            mTrafficData.put(Constants.SIM3TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL3_N, 0L);
                            if (!mIsNight2) {
                                rx = (long) mTrafficData.get(Constants.SIM2RX);
                                tx = (long) mTrafficData.get(Constants.SIM2TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL2);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM2RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM2TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL2_N);
                            }
                            mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mHasActionChosen3 = false;
                            mFlashPreOverLimit[Constants.SIM3] = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM3);
                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight2) {
                            rx = (long) mTrafficData.get(Constants.SIM2RX);
                            tx = (long) mTrafficData.get(Constants.SIM2TX);
                            tot = (long) mTrafficData.get(Constants.TOTAL2);
                        } else {
                            rx = (long) mTrafficData.get(Constants.SIM2RX_N);
                            tx = (long) mTrafficData.get(Constants.SIM2TX_N);
                            tot = (long) mTrafficData.get(Constants.TOTAL2_N);
                        }
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    if (mUids != null) {
                        for (String uid : mUids) {
                            int id = Integer.valueOf(uid);
                            Long[] rxtx = mRxTx.get(id);
                            Long[] rxtx_new = getBytesForUid(id);
                            if (rxtx != null) {
                                uidRx += rxtx_new[0] - rxtx[0];
                                uidTx += rxtx_new[1] - rxtx[1];
                                mRxTx.remove(id);
                                mRxTx.put(id, rxtx_new);
                            }
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    if ((tot <= mLimits[1]) || mSIM2ContinueOverLimit) {
                        rx += diffrx;
                        tx += difftx;
                        if (mPrefs.getBoolean(Constants.PREF_SIM2[32], false))
                            tot = rx;
                        else
                            tot = tx + rx;
                        mIsSIM2OverLimit = false;
                        if (mPrefs.getBoolean(Constants.PREF_SIM2[29], false) && !mHasPreLimitNotificationShown2 && !mHasActionChosen2) {
                            int left = (int) (100 * (1.0 - (double) tot / (double) mLimits[1]));
                            if (left < Integer.valueOf(Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM2[30], "0")))) {
                                mHasPreLimitNotificationShown2 = true;
                                mFlashPreOverLimit[Constants.SIM2] = true;
                                vibrate(1000);
                                showPreLimitToast(Constants.SIM2);
                            }
                        }
                    } else if (!mHasActionChosen2) {
                        mIsSIM2OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            pushAlertNotification(mActiveSIM);
                        makePreCheckActions(mActiveSIM);
                    }

                    if (!mIsSIM2OverLimit) {
                        if (!mIsNight2) {
                            mTrafficData.put(Constants.SIM2RX, rx);
                            mTrafficData.put(Constants.SIM2TX, tx);
                            mTrafficData.put(Constants.TOTAL2, tot);
                        } else {
                            mTrafficData.put(Constants.SIM2RX_N, rx);
                            mTrafficData.put(Constants.SIM2TX_N, tx);
                            mTrafficData.put(Constants.TOTAL2_N, tot);
                        }
                        postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);
                    }

                    if ((CustomApplication.isActivityVisible() ||
                            CustomApplication.getWidgetIds(Constants.TRAFFIC).length != 0 ||
                            mPrefs.getBoolean(Constants.PREF_OTHER[32], false)) && CustomApplication.isScreenOn())
                        sendDataBroadcast(speedRX, speedTX);
                }
            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private static class CountTimerTask3 extends TimerTask {

        @Override
        public final void run() {
            try {
                if (MobileUtils.isMobileDataActive(mContext.get())
                        && (mTaskResult != null && !mTaskResult.isCancelled())) {

                    long speedRX;
                    long speedTX;

                    //avoid NPE by refreshing mTrafficData
                    //begin
                    mTrafficData.put(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                    mTrafficData.put(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    mTrafficData.put(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    mTrafficData.put(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                    mTrafficData.put(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    mTrafficData.put(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    mTrafficData.put(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                    mTrafficData.put(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    mTrafficData.put(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    mTrafficData.put(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                    mTrafficData.put(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    mTrafficData.put(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    mTrafficData.put(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                    mTrafficData.put(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    mTrafficData.put(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    mTrafficData.put(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                    mTrafficData.put(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                    mTrafficData.put(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                    mTrafficData.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
                    mTrafficData.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
                    //end

                    long timeDelta = SystemClock.elapsedRealtime() - mLastUpdateTime;
                    if (timeDelta < 1) {
                        // Can't div by 0 so make sure the value displayed is minimal
                        timeDelta = Long.MAX_VALUE;
                    }
                    mLastUpdateTime = SystemClock.elapsedRealtime();

                    long rx = 0;
                    long tx = 0;
                    long tot = 0;

                    LocalDateTime lastDate = Constants.DATE_FORMATTER.parseLocalDateTime((String) mTrafficData.get(Constants.LAST_DATE));
                    mNowDate = DateTime.now().toLocalDateTime();
                    if (mNowDate.getDayOfYear() != lastDate.getDayOfYear())
                        checkIfResetNeeded(false);

                    mIsNight1 = CustomApplication.getIsNightState()[0];
                    mIsNight2 = CustomApplication.getIsNightState()[1];
                    mIsNight3 = CustomApplication.getIsNightState()[2];

                    boolean emptyDB = (mPrefs.getBoolean(Constants.PREF_OTHER[44], false) && mIMSI != null) ?
                            CustomDatabaseHelper.isTableEmpty(mDbHelper, Constants.TRAFFIC + "_" + mIMSI.get(Constants.SIM3), false) :
                            CustomDatabaseHelper.isTableEmpty(mDbHelper, Constants.TRAFFIC, true);

                    if (emptyDB) {
                        mTrafficData.put(Constants.SIM1RX, 0L);
                        mTrafficData.put(Constants.SIM2RX, 0L);
                        mTrafficData.put(Constants.SIM3RX, 0L);
                        mTrafficData.put(Constants.SIM1TX, 0L);
                        mTrafficData.put(Constants.SIM2TX, 0L);
                        mTrafficData.put(Constants.SIM3TX, 0L);
                        mTrafficData.put(Constants.TOTAL1, 0L);
                        mTrafficData.put(Constants.TOTAL2, 0L);
                        mTrafficData.put(Constants.TOTAL3, 0L);
                        mTrafficData.put(Constants.SIM1RX_N, 0L);
                        mTrafficData.put(Constants.SIM2RX_N, 0L);
                        mTrafficData.put(Constants.SIM3RX_N, 0L);
                        mTrafficData.put(Constants.SIM1TX_N, 0L);
                        mTrafficData.put(Constants.SIM2TX_N, 0L);
                        mTrafficData.put(Constants.SIM3TX_N, 0L);
                        mTrafficData.put(Constants.TOTAL1_N, 0L);
                        mTrafficData.put(Constants.TOTAL2_N, 0L);
                        mTrafficData.put(Constants.TOTAL3_N, 0L);
                        mTrafficData.put(Constants.LAST_RX, 0L);
                        mTrafficData.put(Constants.LAST_TX, 0L);
                        mTrafficData.put(Constants.LAST_TIME, "");
                        mTrafficData.put(Constants.LAST_DATE, "");
                        writeTrafficDataToDatabase(Constants.DISABLED);
                    } else if (mIsResetNeeded1 || mIsResetNeeded2 || mIsResetNeeded3) {
                        mTrafficData.put(Constants.LAST_TIME, mNowDate.toString(Constants.TIME_FORMATTER));
                        mTrafficData.put(Constants.LAST_DATE, mNowDate.toString(Constants.DATE_FORMATTER));
                        if (mIsResetNeeded1) {
                            mTrafficData.put(Constants.SIM1RX, 0L);
                            mTrafficData.put(Constants.SIM1TX, 0L);
                            mTrafficData.put(Constants.TOTAL1, 0L);
                            mTrafficData.put(Constants.SIM1RX_N, 0L);
                            mTrafficData.put(Constants.SIM1TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL1_N, 0L);
                            if (!mIsNight3) {
                                rx = (long) mTrafficData.get(Constants.SIM3RX);
                                tx = (long) mTrafficData.get(Constants.SIM3TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL3);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM3RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM3TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL3_N);
                            }
                            mReceived1 = mTransmitted1 = 0;
                            mIsResetNeeded1 = false;
                            mHasActionChosen1 = false;
                            mFlashPreOverLimit[Constants.SIM1] = false;
                            mSIM1ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM1[25], mIsResetNeeded1)
                                    .putString(Constants.PREF_SIM1[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM1);
                            writeTrafficDataToDatabase(Constants.SIM1);
                        }
                        if (mIsResetNeeded2) {
                            mTrafficData.put(Constants.SIM2RX, 0L);
                            mTrafficData.put(Constants.SIM2TX, 0L);
                            mTrafficData.put(Constants.TOTAL2, 0L);
                            mTrafficData.put(Constants.SIM2RX_N, 0L);
                            mTrafficData.put(Constants.SIM2TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL2_N, 0L);
                            if (!mIsNight3) {
                                rx = (long) mTrafficData.get(Constants.SIM3RX);
                                tx = (long) mTrafficData.get(Constants.SIM3TX);
                                tot = (long) mTrafficData.get(Constants.TOTAL3);
                            } else {
                                rx = (long) mTrafficData.get(Constants.SIM3RX_N);
                                tx = (long) mTrafficData.get(Constants.SIM3TX_N);
                                tot = (long) mTrafficData.get(Constants.TOTAL3_N);
                            }
                            mReceived2 = mTransmitted2 = 0;
                            mIsResetNeeded2 = false;
                            mHasActionChosen2 = false;
                            mFlashPreOverLimit[Constants.SIM2] = false;
                            mSIM2ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM2[25], mIsResetNeeded2)
                                    .putString(Constants.PREF_SIM2[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM2);
                            writeTrafficDataToDatabase(Constants.SIM2);
                        }
                        if (mIsResetNeeded3) {
                            mTrafficData.put(Constants.SIM3RX, 0L);
                            mTrafficData.put(Constants.SIM3TX, 0L);
                            mTrafficData.put(Constants.TOTAL3, 0L);
                            mTrafficData.put(Constants.SIM3RX_N, 0L);
                            mTrafficData.put(Constants.SIM3TX_N, 0L);
                            mTrafficData.put(Constants.TOTAL3_N, 0L);
                            rx = tx = mReceived3 = mTransmitted3 = 0;
                            mIsResetNeeded3 = false;
                            mHasActionChosen3 = false;
                            mFlashPreOverLimit[Constants.SIM3] = false;
                            mSIM3ContinueOverLimit = false;
                            mPrefs.edit()
                                    .putBoolean(Constants.PREF_SIM3[25], mIsResetNeeded3)
                                    .putString(Constants.PREF_SIM3[24], mNowDate.toString(Constants.DATE_TIME_FORMATTER))
                                    .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                                    .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                                    .apply();
                            if (mPrefs.getBoolean(Constants.PREF_OTHER[30], false))
                                pushResetNotification(Constants.SIM3);
                            writeTrafficDataToDatabase(Constants.SIM3);
                        }
                    } else {
                        if (!mIsNight2) {
                            rx = (long) mTrafficData.get(Constants.SIM3RX);
                            tx = (long) mTrafficData.get(Constants.SIM3TX);
                            tot = (long) mTrafficData.get(Constants.TOTAL3);
                        } else {
                            rx = (long) mTrafficData.get(Constants.SIM3RX_N);
                            tx = (long) mTrafficData.get(Constants.SIM3TX_N);
                            tot = (long) mTrafficData.get(Constants.TOTAL3_N);
                        }
                    }

                    long uidRx = 0;
                    long uidTx = 0;

                    if (mUids != null) {
                        for (String uid : mUids) {
                            int id = Integer.valueOf(uid);
                            Long[] rxtx = mRxTx.get(id);
                            Long[] rxtx_new = getBytesForUid(id);
                            if (rxtx != null) {
                                uidRx += rxtx_new[0] - rxtx[0];
                                uidTx += rxtx_new[1] - rxtx[1];
                                mRxTx.remove(id);
                                mRxTx.put(id, rxtx_new);
                            }
                        }
                    }

                    long diffrx = TrafficStats.getMobileRxBytes() - mStartRX - uidRx;
                    long difftx = TrafficStats.getMobileTxBytes() - mStartTX - uidTx;

                    if (diffrx < 0)
                        diffrx = 0;
                    if (difftx < 0)
                        difftx = 0;

                    speedRX = (long) (diffrx / (timeDelta / 1000F));
                    speedTX = (long) (difftx / (timeDelta / 1000F));

                    mStartRX = TrafficStats.getMobileRxBytes();
                    mStartTX = TrafficStats.getMobileTxBytes();

                    if ((tot <= mLimits[2]) || mSIM3ContinueOverLimit) {
                        rx += diffrx;
                        tx += difftx;
                        if (mPrefs.getBoolean(Constants.PREF_SIM3[32], false))
                            tot = rx;
                        else
                            tot = tx + rx;
                        mIsSIM3OverLimit = false;
                        if (mPrefs.getBoolean(Constants.PREF_SIM3[29], false) && !mHasPreLimitNotificationShown3 && !mHasActionChosen3) {
                            int left = (int) (100 * (1.0 - (double) tot / (double) mLimits[2]));
                            if (left < Integer.valueOf(Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM3[30], "0")))) {
                                mHasPreLimitNotificationShown3 = true;
                                mFlashPreOverLimit[Constants.SIM3] = true;
                                vibrate(1000);
                                showPreLimitToast(Constants.SIM3);
                            }
                        }
                    } else if (!mHasActionChosen3) {
                        mIsSIM3OverLimit = true;
                        if (mPrefs.getBoolean(Constants.PREF_OTHER[3], false))
                            pushAlertNotification(mActiveSIM);
                        makePreCheckActions(mActiveSIM);
                    }

                    if (!mIsSIM3OverLimit) {
                        if (!mIsNight3) {
                            mTrafficData.put(Constants.SIM3RX, rx);
                            mTrafficData.put(Constants.SIM3TX, tx);
                            mTrafficData.put(Constants.TOTAL3, tot);
                        } else {
                            mTrafficData.put(Constants.SIM3RX_N, rx);
                            mTrafficData.put(Constants.SIM3TX_N, tx);
                            mTrafficData.put(Constants.TOTAL3_N, tot);
                        }
                        postDataChanges(mActiveSIM, diffrx, difftx, emptyDB);
                    }

                    if ((CustomApplication.isActivityVisible() ||
                            CustomApplication.getWidgetIds(Constants.TRAFFIC).length != 0 ||
                            mPrefs.getBoolean(Constants.PREF_OTHER[32], false)) && CustomApplication.isScreenOn())
                        sendDataBroadcast(speedRX, speedTX);
                }

            } catch (Exception e) {
                e.printStackTrace();
                ACRA.getErrorReporter().handleException(e);
            }
        }
    }

    private static void vibrate(int i) {
        Vibrator v = (Vibrator) mContext.get().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(i);
        }
    }

    private static void postDataChanges(int sim, long diffrx, long difftx, boolean emptyDB) {
        mTrafficData.put(Constants.LAST_RX, TrafficStats.getMobileRxBytes());
        mTrafficData.put(Constants.LAST_TX, TrafficStats.getMobileTxBytes());
        LocalDateTime dateTime = DateTime.now().toLocalDateTime();
        final long MB = 1024 * 1024;
        if ((diffrx + difftx > MB) || dateTime.get(DateTimeFieldType.secondOfMinute()) == 59
                || emptyDB) {
            mTrafficData.put(Constants.LAST_TIME, dateTime.toString(Constants.TIME_FORMATTER));
            mTrafficData.put(Constants.LAST_DATE, dateTime.toString(Constants.DATE_FORMATTER));
            writeTrafficDataToDatabase(sim);
            if (CustomApplication.isScreenOn())
                updateNotification(sim);
        }
    }

    private static void showPreLimitToast(final int sim) {
        mHandler.post(() -> Toast.makeText(mContext.get(), String.format(mContext.get().getResources().getString(R.string.pre_limit), mOperatorNames[sim]), Toast.LENGTH_LONG).show());
    }

    private static void pushResetNotification(int simID) {
        Context ctx = mContext.get();
        Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_alert);
        String text = "";
        int id;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[15], false)) {
            String[] pref = new String[Constants.PREF_SIM1.length];
            switch (simID) {
                case Constants.SIM1:
                    pref = Constants.PREF_SIM1;
                    text = mOperatorNames[0];
                    break;
                case Constants.SIM2:
                    pref = Constants.PREF_SIM2;
                    text = mOperatorNames[1];
                    break;
                case Constants.SIM3:
                    pref = Constants.PREF_SIM3;
                    text = mOperatorNames[2];
                    break;
            }
            if (Objects.requireNonNull(mPrefs.getString(pref[23], "none")).equals("auto"))
                id = ctx.getResources().getIdentifier("logo_" + MobileUtils.getLogoFromCode(ctx, simID), "drawable", ctx.getPackageName());
            else
                id = ctx.getResources().getIdentifier(mPrefs.getString(pref[23], "none"), "drawable", ctx.getPackageName());
        } else
            id = R.drawable.ic_launcher_small;
        text = String.format(ctx.getResources().getString(R.string.data_reset), text);
        Intent notificationIntent = new Intent(ctx, MainActivity.class);
        notificationIntent.setAction("traffic");
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                .setContentIntent(contentIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(id)
                .setLargeIcon(bm)
                .setContentTitle(ctx.getResources().getString(R.string.notification_title))
                .setContentText(text);
        if (nm != null) {
            nm.notify(simID + 1977, builder.build());
        }
    }

    private static void writeTrafficDataToDatabase(int sim) {
        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext.get());
            ContentValues cv = new ContentValues();
            switch (sim) {
                case Constants.SIM1:
                    putDataForSim(cv, 1);
                    break;
                case Constants.SIM2:
                    putDataForSim(cv, 2);
                    break;
                case Constants.SIM3:
                    putDataForSim(cv, 3);
                    break;
                case Constants.DISABLED:
                    for (int i = 0; i < mIMSI.size(); i++) {
                        putDataForSim(cv, i + 1);
                        CustomDatabaseHelper.writeData(cv, mDbHelper, Constants.TRAFFIC + "_" + mIMSI.get(i));
                    }
            }
            if (mActiveSIM != Constants.DISABLED)
                CustomDatabaseHelper.writeData(cv, mDbHelper, Constants.TRAFFIC + "_" + mIMSI.get(sim));
        } else
            CustomDatabaseHelper.writeData(mTrafficData, mDbHelper, Constants.TRAFFIC);
    }

    private static void putDataForSim(ContentValues cv, int i) {
        cv.put("rx", (long) mTrafficData.get("sim" + i + "rx"));
        cv.put("tx", (long) mTrafficData.get("sim" + i + "tx"));
        cv.put("total", (long) mTrafficData.get("total" + i));
        cv.put("rx_n", (long) mTrafficData.get("sim" + i + "rx_n"));
        cv.put("tx_n", (long) mTrafficData.get("sim" + i + "tx_n"));
        cv.put("total_n", (long) mTrafficData.get("total" + i + "_n"));
        cv.put("period", (int) mTrafficData.get("period" + i));
        cv.put(Constants.LAST_TIME, (String) mTrafficData.get(Constants.LAST_TIME));
        cv.put(Constants.LAST_DATE, (String) mTrafficData.get(Constants.LAST_DATE));
    }

    private static void sendDataBroadcast(long speedRX, long speedTX) {
        int sim;
        Context ctx = mContext.get();
        if (mActiveSIM == Constants.DISABLED)
            sim = mLastActiveSIM;
        else
            sim = mActiveSIM;
        if (sim >= 0) {
            try {
                Intent intent = new Intent(Constants.TRAFFIC_BROADCAST_ACTION);
                intent.putExtra(Constants.WIDGET_IDS, CustomApplication.getWidgetIds(Constants.TRAFFIC));
                intent.putExtra(Constants.SPEEDRX, speedRX);
                intent.putExtra(Constants.SPEEDTX, speedTX);
                intent.putExtra(Constants.SIM1RX, (long) mTrafficData.get(Constants.SIM1RX));
                intent.putExtra(Constants.SIM1TX, (long) mTrafficData.get(Constants.SIM1TX));
                intent.putExtra(Constants.TOTAL1, (long) mTrafficData.get(Constants.TOTAL1));
                intent.putExtra(Constants.SIM1RX_N, (long) mTrafficData.get(Constants.SIM1RX_N));
                intent.putExtra(Constants.SIM1TX_N, (long) mTrafficData.get(Constants.SIM1TX_N));
                intent.putExtra(Constants.TOTAL1_N, (long) mTrafficData.get(Constants.TOTAL1_N));
                if (mSimQuantity >= 2) {
                    intent.putExtra(Constants.SIM2RX, (long) mTrafficData.get(Constants.SIM2RX));
                    intent.putExtra(Constants.SIM2TX, (long) mTrafficData.get(Constants.SIM2TX));
                    intent.putExtra(Constants.TOTAL2, (long) mTrafficData.get(Constants.TOTAL2));
                    intent.putExtra(Constants.SIM2RX_N, (long) mTrafficData.get(Constants.SIM2RX_N));
                    intent.putExtra(Constants.SIM2TX_N, (long) mTrafficData.get(Constants.SIM2TX_N));
                    intent.putExtra(Constants.TOTAL2_N, (long) mTrafficData.get(Constants.TOTAL2_N));
                }
                if (mSimQuantity == 3) {
                    intent.putExtra(Constants.SIM3RX, (long) mTrafficData.get(Constants.SIM3RX));
                    intent.putExtra(Constants.SIM3TX, (long) mTrafficData.get(Constants.SIM3TX));
                    intent.putExtra(Constants.TOTAL3, (long) mTrafficData.get(Constants.TOTAL3));
                    intent.putExtra(Constants.SIM3RX_N, (long) mTrafficData.get(Constants.SIM3RX_N));
                    intent.putExtra(Constants.SIM3TX_N, (long) mTrafficData.get(Constants.SIM3TX_N));
                    intent.putExtra(Constants.TOTAL3_N, (long) mTrafficData.get(Constants.TOTAL3_N));
                }
                intent.putExtra(Constants.SIM_ACTIVE, sim);
                intent.putExtra(Constants.OPERATOR1, mOperatorNames[0]);
                intent.putExtra(Constants.OPERATOR2, mOperatorNames[1]);
                intent.putExtra(Constants.OPERATOR3, mOperatorNames[2]);
                ctx.sendBroadcast(intent);
                if (mPrefs.getBoolean(Constants.PREF_OTHER[32], false) && sim != Constants.DISABLED) {
                    long total = 0;
                    int limit = 0;
                    switch (sim) {
                        case Constants.SIM1:
                            if (mIsNight1)
                                total = (long) mTrafficData.get(Constants.TOTAL1_N);
                            else
                                total = (long) mTrafficData.get(Constants.TOTAL1);
                            if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM1[1], "")).equals(""))
                                limit = -1;
                            break;
                        case Constants.SIM2:
                            if (mIsNight2)
                                total = (long) mTrafficData.get(Constants.TOTAL2_N);
                            else
                                total = (long) mTrafficData.get(Constants.TOTAL2);
                            if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM2[1], "")).equals(""))
                                limit = -1;
                            break;
                        case Constants.SIM3:
                            if (mIsNight3)
                                total = (long) mTrafficData.get(Constants.TOTAL3_N);
                            else
                                total = (long) mTrafficData.get(Constants.TOTAL3);
                            if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_SIM3[1], "")).equals(""))
                                limit = -1;
                            break;
                    }
                    if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[39], "1")).equals("0")) {
                        if (limit < 0)
                            total = -1;
                        else
                            total = mLimits[sim] - total;
                /*if (total < 0)
                    total = 0;*/
                    }
                    Bundle bundle = new Bundle();
                    bundle.putLong("total", total);
                    bundle.putBoolean("flash", mFlashPreOverLimit[sim]);
                    bundle.putLong(Constants.SPEEDRX, speedRX);
                    bundle.putLong(Constants.SPEEDTX, speedTX);
                    if (!CustomApplication.isMyServiceRunning(FloatingWindowService.class))
                        FloatingWindowService.showFloatingWindow(ctx, mPrefs);
                    StandOutWindow.sendData(ctx, FloatingWindowService.class,
                            mPrefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID), Constants.FLOATING_WINDOW, bundle, null, -1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void updateNotification(int sim) {
        NotificationManager nm = (NotificationManager) mContext.get().getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(Constants.STARTED_ID, buildNotification(sim));
        }
    }

    private static Notification buildNotification(int sim) {
        Context ctx = mContext.get();
        if (sim == Constants.DISABLED)
            sim = mLastActiveSIM;
        String traffic = "";
        long tot1, tot2 = 0, tot3 = 0;
        if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[19], "1")).equals("0")) {
            tot1 = mIsNight1 ? mLimits[0] - (long) mTrafficData.get(Constants.TOTAL1_N) : mLimits[0] - (long) mTrafficData.get(Constants.TOTAL1);
            if (tot1 < 0)
                tot1 = 0;
            if (mSimQuantity >= 2) {
                tot2 = mIsNight2 ? mLimits[1] - (long) mTrafficData.get(Constants.TOTAL2_N) : mLimits[1] - (long) mTrafficData.get(Constants.TOTAL2);
                if (tot2 < 0)
                    tot2 = 0;
            }
            if (mSimQuantity == 3) {
                tot3 = mIsNight3 ? mLimits[2] - (long) mTrafficData.get(Constants.TOTAL3_N) : mLimits[2] - (long) mTrafficData.get(Constants.TOTAL3);
                if (tot3 < 0)
                    tot3 = 0;
            }
        } else {
            tot1 = mIsNight1 ? (long) mTrafficData.get(Constants.TOTAL1_N) : (long) mTrafficData.get(Constants.TOTAL1);
            tot2 = mIsNight2 ? (long) mTrafficData.get(Constants.TOTAL2_N) : (long) mTrafficData.get(Constants.TOTAL2);
            tot3 = mIsNight3 ? (long) mTrafficData.get(Constants.TOTAL3_N) : (long) mTrafficData.get(Constants.TOTAL3);
        }
        if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[16], "0")).equals("0")) {
            if (mLimits[0] != Long.MAX_VALUE)
                traffic = DataFormat.formatData(ctx, tot1);
            else
                traffic = ctx.getString(R.string.not_set);
            if (mSimQuantity >= 2)
                if (mLimits[1] != Long.MAX_VALUE)
                    traffic += "  ||  " + DataFormat.formatData(ctx, tot2);
                else
                    traffic += "  ||  " + ctx.getString(R.string.not_set);
            if (mSimQuantity == 3)
                if (mLimits[2] != Long.MAX_VALUE)
                    traffic += "  ||  " + DataFormat.formatData(ctx, tot3);
                else
                    traffic += "  ||  " + ctx.getString(R.string.not_set);
        } else {
            switch (sim) {
                case Constants.SIM1:
                    if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[19], "1")).equals("0"))
                        if (mLimits[0] != Long.MAX_VALUE)
                            traffic = String.format(ctx.getString(R.string.traffic_rest), mOperatorNames[0],
                                    DataFormat.formatData(ctx, tot1));
                        else
                            traffic = String.format(ctx.getString(R.string.traffic_rest), mOperatorNames[0],
                                    ctx.getString(R.string.not_set));
                    else
                        traffic = mOperatorNames[0] + ": " +
                            DataFormat.formatData(ctx, tot1);
                    break;
                case Constants.SIM2:
                    if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[19], "1")).equals("0"))
                        if (mLimits[1] != Long.MAX_VALUE)
                            traffic = String.format(ctx.getString(R.string.traffic_rest), mOperatorNames[1],
                                    DataFormat.formatData(ctx, tot2));
                        else
                            traffic = String.format(ctx.getString(R.string.traffic_rest), mOperatorNames[1],
                                    ctx.getString(R.string.not_set));
                    else
                        traffic = mOperatorNames[1] + ": " +
                                DataFormat.formatData(ctx, tot2);
                    break;
                case Constants.SIM3:
                    if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[19], "1")).equals("0"))
                        if (mLimits[2] != Long.MAX_VALUE)
                            traffic = String.format(ctx.getString(R.string.traffic_rest), mOperatorNames[2],
                                    DataFormat.formatData(ctx, tot3));
                        else
                            traffic = String.format(ctx.getString(R.string.traffic_rest), mOperatorNames[2],
                                    ctx.getString(R.string.not_set));
                    else
                        traffic = mOperatorNames[2] + ": " +
                            DataFormat.formatData(ctx, tot3);
                    break;
            }
        }
        String calls = "";
        ContentValues cv;
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(ctx);
            cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(0));
            tot1 = (long) cv.get("calls");
            if (mSimQuantity >= 2) {
                cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(1));
                tot2 = (long) cv.get("calls");
            }
            if (mSimQuantity == 3) {
                cv = CustomDatabaseHelper.readCallsDataForSim(mDbHelper, mIMSI.get(2));
                tot3 = (long) cv.get("calls");
            }
        } else {
            cv = CustomDatabaseHelper.readCallsData(mDbHelper);
            tot1 = (long) cv.get(Constants.CALLS1);
            tot2 = (long) cv.get(Constants.CALLS2);
            tot3 = (long) cv.get(Constants.CALLS3);
        }
        int[] limits = CustomApplication.getCallsSimLimitsValues(true);
        if (limits[0] != Long.MAX_VALUE)
            calls += DataFormat.formatCallDuration(ctx, tot1);
        else
            calls += ctx.getString(R.string.not_set);
        if (mSimQuantity >= 2)
            if (limits[1] != Long.MAX_VALUE)
                calls += "  ||  " + DataFormat.formatCallDuration(ctx, tot2);
            else
                calls += "  ||  " + ctx.getString(R.string.not_set);
        if (mSimQuantity == 3)
            if (limits[2] != Long.MAX_VALUE)
                calls += "  ||  " + DataFormat.formatCallDuration(ctx, tot3);
            else
                calls += "  ||  " + ctx.getString(R.string.not_set);

        return CustomNotification.getNotification(ctx, traffic, calls);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private static void makePreCheckActions(int sim) {
        Context ctx = mContext.get();

        String[] keys = new String[Constants.PREF_SIM_DATA.length];
        switch (sim) {
            case Constants.SIM1:
                keys = Constants.PREF_SIM1;
                break;
            case Constants.SIM2:
                keys = Constants.PREF_SIM2;
                break;
            case Constants.SIM3:
                keys = Constants.PREF_SIM3;
                break;
        }

        boolean choice = false;

        //check if can change sim
        if (mPrefs.getBoolean(Constants.PREF_OTHER[10], false)) {
            //if all sim overlimit - push action dialog
            if (mIsSIM1OverLimit && mIsSIM2OverLimit && mIsSIM3OverLimit) {
                mDoNotStopService = true;
                Intent dialogIntent = new Intent(ctx, ChooseActionDialog.class);
                dialogIntent.putExtra(Constants.SIM_ACTIVE, sim);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!ChooseActionDialog.isActive())
                    ctx.startActivity(dialogIntent);
            } else {
                //else try to change active sim
                try {
                    MobileUtils.toggleMobileDataConnection(false, ctx, Constants.DISABLED);
                    if (!mIsSIM2OverLimit && sim == Constants.SIM1 && mSimQuantity >= 2)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM2);
                    else if (!mIsSIM3OverLimit && sim == Constants.SIM1 && mSimQuantity == 3)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM3);
                    else if (!mIsSIM1OverLimit && sim == Constants.SIM2)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    else if (!mIsSIM3OverLimit && sim == Constants.SIM2 && mSimQuantity == 3)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM3);
                    else if (!mIsSIM1OverLimit && sim == Constants.SIM3)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM1);
                    else if (!mIsSIM2OverLimit && sim == Constants.SIM3)
                        MobileUtils.toggleMobileDataConnection(true, ctx, Constants.SIM2);
                    else
                        choice = true;
                } catch (Exception e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        } else {
            //if can't change sim - check if needs to disable data
            if (mPrefs.getBoolean(keys[7], false)) {
                try {
                    MobileUtils.toggleMobileDataConnection(false, ctx, Constants.DISABLED);
                } catch (Exception e) {
                    e.printStackTrace();
                    ACRA.getErrorReporter().handleException(e);
                }
                choice = true;
            }
            //check if needs to push action dialog
            if (!mPrefs.getBoolean(Constants.PREF_OTHER[51], false)) {
                mDoNotStopService = true;
                EventBus.getDefault().post(new ActionTrafficEvent(sim, Constants.CONTINUE_ACTION));
            } else {
                mDoNotStopService = true;
                Intent dialogIntent = new Intent(ctx, ChooseActionDialog.class);
                dialogIntent.putExtra(Constants.SIM_ACTIVE, sim);
                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (!ChooseActionDialog.isActive())
                    ctx.startActivity(dialogIntent);
            }
        }
        if (choice) {
            if (mTaskResult != null) {
                mTaskResult.cancel(false);
                mTaskExecutor.shutdown();
            }
            Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_disable);
            Intent notificationIntent = new Intent(ctx, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_disable_small)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setLargeIcon(bm)
                    .setWhen(System.currentTimeMillis())
                    .setContentTitle(ctx.getString(R.string.service_stopped_title));
            if (nm != null) {
                nm.notify(Constants.STARTED_ID, builder.build());
            }

            EventBus.getDefault().post(new TipTrafficEvent());

            startNewTimerTask(Constants.CHECK);
        }
    }

    private static void pushAlertNotification(int sim) {
        Context ctx = mContext.get();
        Intent notificationIntent;
        if ((mPrefs.getBoolean(Constants.PREF_SIM1[7], true) && mIsSIM1OverLimit) ||
                (mPrefs.getBoolean(Constants.PREF_SIM2[7], true) && mIsSIM2OverLimit) ||
                (mPrefs.getBoolean(Constants.PREF_SIM2[7], true) && mIsSIM3OverLimit)) {
            notificationIntent = new Intent(ctx.getApplicationContext(), SettingsActivity.class);
            notificationIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, TrafficLimitFragment.class.getName());
            notificationIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
            notificationIntent.putExtra(Constants.SIM_ACTIVE, sim);
        } else {
            final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
            notificationIntent = new Intent(Intent.ACTION_MAIN);
            notificationIntent.setComponent(cn);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent pIntent = PendingIntent.getActivity(ctx.getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[2], false))
            builder.setDefaults(Notification.DEFAULT_VIBRATE);
        Bitmap bm = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_alert);
        String opName;
        if (sim == Constants.SIM1)
            opName = mOperatorNames[0];
        else if (sim == Constants.SIM2)
            opName = mOperatorNames[1];
        else
            opName = mOperatorNames[2];
        String txt;
        if ((sim == Constants.SIM1 && mPrefs.getBoolean(Constants.PREF_SIM1[7], true)) ||
                (sim == Constants.SIM2 && mPrefs.getBoolean(Constants.PREF_SIM2[7], true)) ||
                (sim == Constants.SIM3 && mPrefs.getBoolean(Constants.PREF_SIM3[7], true)))
            txt = ctx.getString(R.string.data_dis);
        else
            txt = ctx.getString(R.string.data_dis_tip);

        Notification n = builder
                .setContentIntent(pIntent)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_alert_small)
                .setLargeIcon(bm)
                .setTicker(ctx.getString(R.string.app_name))
                .setWhen(System.currentTimeMillis())
                .setContentTitle(txt)
                .setContentText(opName + ": " + ctx.getString(R.string.over_limit))
                .build();
        if (!Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[1], "")).equals("")) {
            n.sound = Uri.parse(mPrefs.getString(Constants.PREF_OTHER[1], ""));
            n.flags = Notification.FLAG_ONLY_ALERT_ONCE;
        }
        if (nm != null) {
            nm.notify(sim, n);
        }
    }

    @Override
    public final void onDestroy() {
        super.onDestroy();
        mPrefs.edit()
                .putBoolean(Constants.PREF_SIM1[27], mSIM1ContinueOverLimit)
                .putBoolean(Constants.PREF_SIM2[27], mSIM2ContinueOverLimit)
                .putBoolean(Constants.PREF_SIM3[27], mSIM3ContinueOverLimit)
                .putBoolean(Constants.PREF_SIM1[28], mHasActionChosen1)
                .putBoolean(Constants.PREF_SIM2[28], mHasActionChosen2)
                .putBoolean(Constants.PREF_SIM3[28], mHasActionChosen3)
                .putBoolean(Constants.PREF_OTHER[48], false)
                .apply();
        if (mActiveSIM != -1)
            mPrefs.edit()
                    .putInt(Constants.PREF_OTHER[46], mActiveSIM)
                    .apply();
        if (mTaskResult != null) {
            mTaskResult.cancel(false);
            mTaskExecutor.shutdown();
        }
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(Constants.STARTED_ID);
        }
        if (mTrafficData != null)
            writeTrafficDataToDatabase(mActiveSIM);
        /*if (mDbHelper != null && !CustomApplication.isMyServiceRunning(CallLoggerService.class))
            CustomDatabaseHelper.deleteInstance();*/
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        if (mUidObserver != null)
            getContentResolver().unregisterContentObserver(mUidObserver);
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
    }
}