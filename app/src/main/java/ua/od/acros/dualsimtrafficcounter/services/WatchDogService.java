package ua.od.acros.dualsimtrafficcounter.services;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class WatchDogService extends Service{

    private SharedPreferences mPrefs;
    private Context mContext;
    private CustomDatabaseHelper mDbHelper;
    private Timer mTimer;
    private boolean mIsFirstRun, mInCall;
    private ArrayList<String> mIMSI;
    private long mInterval;

    public WatchDogService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mInterval = Long.parseLong(Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[8], "1"))) * 60 * 1000;
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        // cancel if already existed
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = new Timer();
        } else {
            // recreate new
            mTimer = new Timer();
        }
        final TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (tm != null) {
            tm.listen(new PhoneStateListener() {

                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                        switch (state) {
                            case TelephonyManager.CALL_STATE_OFFHOOK:
                                mInCall = true;
                                break;
                            case TelephonyManager.CALL_STATE_IDLE:
                                mInCall = false;
                                break;
                        }
                }
            }, PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mIsFirstRun = true;
        // schedule task
        mTimer.scheduleAtFixedRate(new WatchDogTask(), 0, mInterval);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
        mTimer.purge();
        mPrefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
    }

    private class WatchDogTask extends TimerTask {
        @Override
        public void run() {
            if (mIsFirstRun) {
                CustomApplication.sleep(mInterval);
                mIsFirstRun = false;
            }
            ContentValues cv;
            String lastUpdate;
            LocalDateTime now = DateTime.now().toLocalDateTime(), last = null;
            DateTimeFormatter fmt = DateTimeFormat.forPattern(Constants.DATE_FORMAT + " " + Constants.TIME_FORMAT + ":ss");
            if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
                LocalDateTime t1 = null, t2 = null, t3 = null;
                if (mIMSI == null)
                    mIMSI = MobileUtils.getSimIds(mContext);
                cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(0));
                lastUpdate = cv.get(Constants.LAST_DATE) + " " + cv.get(Constants.LAST_TIME);
                try {
                    t1 = fmt.parseLocalDateTime(lastUpdate);
                } catch (Exception e) {
                }
                if (mIMSI.size() >= 2) {
                    cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(1));
                    lastUpdate = cv.get(Constants.LAST_DATE) + " " + cv.get(Constants.LAST_TIME);
                    try {
                    t2 = fmt.parseLocalDateTime(lastUpdate);
                    } catch (Exception e) {
                    }
                }
                if (mIMSI.size() >= 3) {
                    cv = CustomDatabaseHelper.readTrafficDataForSim(mDbHelper, mIMSI.get(1));
                    lastUpdate = cv.get(Constants.LAST_DATE) + " " + cv.get(Constants.LAST_TIME);
                    try {
                        t3 = fmt.parseLocalDateTime(lastUpdate);
                    } catch (Exception e) {
                    }
                }
                if (t1 != null && t2 != null) {
                    if (t1.isAfter(t2))
                        last = t1;
                    else
                        last= t2;
                } else if (t2 != null)
                    last = t2;
                else if (t1 != null)
                    last = t1;
                if (t3 != null && last != null) {
                    if (t3.isAfter(last))
                        last = t3;
                } else if (t3 != null)
                    last = t3;
                if (last == null)
                    last = now;
            } else {
                cv = CustomDatabaseHelper.readTrafficData(mDbHelper);
                if (cv.get(Constants.LAST_DATE).equals("")) {
                    cv.put(Constants.LAST_TIME, now.toString(Constants.TIME_FORMATTER));
                    cv.put(Constants.LAST_DATE, now.toString(Constants.DATE_FORMATTER));
                }
                lastUpdate = cv.get(Constants.LAST_DATE) + " " + cv.get(Constants.LAST_TIME);
                last = fmt.parseLocalDateTime(lastUpdate);
            }
            if ((now.toDateTime().getMillis() - last.toDateTime().getMillis()) > 61 * 1000 &&
                    (MobileUtils.isMobileDataActive(mContext) &&
                            MobileUtils.getActiveSimForData(mContext) > Constants.DISABLED) &&
                    !mPrefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                stopService(new Intent(mContext, TrafficCountService.class));
                startService(new Intent(mContext, TrafficCountService.class));
            }
            if (CustomApplication.isMyServiceRunning(CallLoggerService.class) && !mInCall)
                stopService(new Intent(mContext, CallLoggerService.class));
        }
    }
}
