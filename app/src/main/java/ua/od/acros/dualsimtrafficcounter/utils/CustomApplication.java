package ua.od.acros.dualsimtrafficcounter.utils;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.stericson.RootShell.RootShell;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.joda.time.DateTime;
import org.joda.time.DateTimeComparator;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.receivers.OnOffReceiver;
import ua.od.acros.dualsimtrafficcounter.receivers.ResetReceiver;
import ua.od.acros.dualsimtrafficcounter.widgets.CallsInfoWidget;
import ua.od.acros.dualsimtrafficcounter.widgets.TrafficInfoWidget;

@ReportsCrashes(mailTo = "acras1@gmail.com",
        customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME,
                ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.BUILD, ReportField.BRAND,
                ReportField.SETTINGS_GLOBAL, ReportField.SETTINGS_SYSTEM,
                ReportField.STACK_TRACE, ReportField.LOGCAT, ReportField.SHARED_PREFERENCES },
        logcatArguments = { "-t", "300", "MyAppTag:V", "System.err:V", "AndroidRuntime:V", "*:S" },
        mode = ReportingInteractionMode.DIALOG,
        resDialogText = R.string.crash_toast_text,
        resDialogOkToast = R.string.crash_toast_text_ok)
public class CustomApplication extends Application {

    private static WeakReference<Context> mWeakReference;
    private static Boolean mIsOldMtkDevice = null;
    private static boolean mCanToggleOff;
    private static Boolean mHasRoot = null;
    private static Boolean mHasGeminiSupport = null;
    private static boolean mIsActivityVisible;
    private static Intent mSettingsIntent;
    private static boolean mIsDataUsageAvailable = true;
    private static boolean mCanToggleOn;

        /*static {
        SharedPreferences prefs = MyApplication.getAppContext().getSharedPreferences(Constants.APP_PREFERENCES, Context.MODE_PRIVATE);
        if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
        else {
            if (prefs.getBoolean(Constants.PREF_OTHER[28], false))
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }*/

    @Override
    public final void onCreate() {
        super.onCreate();
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        Context context = getApplicationContext();
        mWeakReference = new WeakReference<>(context);
        fixFolderPermissionsAsync();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        ArrayList imsi = MobileUtils.getSimIds(context);
        if (imsi.size() > 0 && preferences.getBoolean(Constants.PREF_OTHER[44], false))
            loadTrafficPreferences(imsi);
        if (imsi.size() > 0 && preferences.getBoolean(Constants.PREF_OTHER[45], false))
            loadCallsPreferences(imsi);


        SharedPreferences.Editor edit = preferences.edit();

        if (!preferences.contains(Constants.PREF_OTHER[55]))
            edit.putInt(Constants.PREF_OTHER[55], preferences.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                    : Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_OTHER[14], "1"))))
                    .apply();

        Map<String, ?> keys = preferences.getAll();
        if (keys.get(Constants.PREF_OTHER[28]) != null && Objects.requireNonNull(Objects.requireNonNull(keys.get(Constants.PREF_OTHER[28]))).getClass().equals(Boolean.class)) {
            if (preferences.getBoolean(Constants.PREF_OTHER[28], true))
                edit.remove(Constants.PREF_OTHER[28])
                        .putString(Constants.PREF_OTHER[28], "0");
            else
                edit.remove(Constants.PREF_OTHER[28])
                        .putString(Constants.PREF_OTHER[28], "1");
        }
        if (keys.get(Constants.PREF_OTHER[7]) != null && Objects.requireNonNull(keys.get(Constants.PREF_OTHER[7])).getClass().equals(Boolean.class)) {
            if (preferences.getBoolean(Constants.PREF_OTHER[7], true))
                edit.remove(Constants.PREF_OTHER[7])
                        .putString(Constants.PREF_OTHER[7], "0");
            else
                edit.remove(Constants.PREF_OTHER[7])
                        .putString(Constants.PREF_OTHER[7], "1");
        }
        if (keys.get(Constants.PREF_OTHER[19]) != null && Objects.requireNonNull(keys.get(Constants.PREF_OTHER[19])).getClass().equals(Boolean.class)) {
            if (preferences.getBoolean(Constants.PREF_OTHER[19], true))
                edit.remove(Constants.PREF_OTHER[19])
                        .putString(Constants.PREF_OTHER[19], "0");
            else
                edit.remove(Constants.PREF_OTHER[19])
                        .putString(Constants.PREF_OTHER[19], "1");
        }
        if (keys.get(Constants.PREF_OTHER[16]) != null && Objects.requireNonNull(keys.get(Constants.PREF_OTHER[16])).getClass().equals(Boolean.class)) {
            if (preferences.getBoolean(Constants.PREF_OTHER[16], true))
                edit.putString(Constants.PREF_OTHER[16], "0")
                        .remove(Constants.PREF_OTHER[16]);
            else
                edit.remove(Constants.PREF_OTHER[16])
                        .putString(Constants.PREF_OTHER[16], "1");
        }
        if (keys.get(Constants.PREF_OTHER[27]) != null && Objects.requireNonNull(keys.get(Constants.PREF_OTHER[27])).getClass().equals(Boolean.class)) {
            if (preferences.getBoolean(Constants.PREF_OTHER[27], true))
                edit.remove(Constants.PREF_OTHER[27])
                        .putString(Constants.PREF_OTHER[27], "0");
            else
                edit.remove(Constants.PREF_OTHER[27])
                        .putString(Constants.PREF_OTHER[27], "1");
        }
        if (keys.get(Constants.PREF_OTHER[39]) != null && Objects.requireNonNull(keys.get(Constants.PREF_OTHER[39])).getClass().equals(Boolean.class)) {
            if (preferences.getBoolean(Constants.PREF_OTHER[39], true))
                edit.remove(Constants.PREF_OTHER[39])
                        .putString(Constants.PREF_OTHER[39], "0");
            else
                edit.remove(Constants.PREF_OTHER[39])
                        .putString(Constants.PREF_OTHER[39], "1");
        }
        edit.apply();
        int[] ids = getWidgetIds(Constants.TRAFFIC);
        if (ids.length != 0) {
            SharedPreferences prefsWidget;
            for (int id : ids) {
                prefsWidget = getSharedPreferences(String.valueOf(id) + Constants.TRAFFIC_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
                keys = prefsWidget.getAll();
                edit = prefsWidget.edit();
                if (keys.get(Constants.PREF_WIDGET_TRAFFIC[2]) != null &&
                        Objects.requireNonNull(keys.get(Constants.PREF_WIDGET_TRAFFIC[2])).getClass().equals(Boolean.class)) {
                    if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[2], true))
                        edit.putString(Constants.PREF_WIDGET_TRAFFIC[2], "0")
                                .remove(Constants.PREF_WIDGET_TRAFFIC[2]);
                    else
                        edit.remove(Constants.PREF_WIDGET_TRAFFIC[2])
                                .putString(Constants.PREF_WIDGET_TRAFFIC[2], "1");
                }
                if (keys.get(Constants.PREF_WIDGET_TRAFFIC[24]) != null &&
                        Objects.requireNonNull(keys.get(Constants.PREF_WIDGET_TRAFFIC[24])).getClass().equals(Boolean.class)) {
                    if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[24], true))
                        edit.remove(Constants.PREF_WIDGET_TRAFFIC[24])
                                .putString(Constants.PREF_WIDGET_TRAFFIC[24], "0");
                    else
                        edit.remove(Constants.PREF_WIDGET_TRAFFIC[24])
                                .putString(Constants.PREF_WIDGET_TRAFFIC[24], "1");
                }
                if (keys.get(Constants.PREF_WIDGET_TRAFFIC[25]) != null &&
                        Objects.requireNonNull(keys.get(Constants.PREF_WIDGET_TRAFFIC[25])).getClass().equals(Boolean.class)) {
                    if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[25], true))
                        edit.remove(Constants.PREF_WIDGET_TRAFFIC[25])
                                .putString(Constants.PREF_WIDGET_TRAFFIC[25], "0");
                    else
                        edit.remove(Constants.PREF_WIDGET_TRAFFIC[25])
                                .putString(Constants.PREF_WIDGET_TRAFFIC[25], "1");
                    edit.apply();
                }
            }
        }
        ids = getWidgetIds(Constants.CALLS);
        if (ids.length != 0) {
            SharedPreferences prefsWidget;
            for (int id : ids) {
                prefsWidget = getSharedPreferences(String.valueOf(id) + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
                keys = prefsWidget.getAll();
                edit = prefsWidget.edit();
                if (keys.get(Constants.PREF_WIDGET_CALLS[18]) != null &&
                        keys.get(Constants.PREF_WIDGET_CALLS[18]).getClass().equals(Boolean.class)) {
                    if (prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[18], true))
                        edit.putString(Constants.PREF_WIDGET_CALLS[18], "0")
                                .remove(Constants.PREF_WIDGET_CALLS[18]);
                    else
                        edit.remove(Constants.PREF_WIDGET_CALLS[18])
                                .putString(Constants.PREF_WIDGET_CALLS[18], "1");
                    edit.apply();
                }
            }
        }

        //Check if Data Usage Fragment available
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity");
        mSettingsIntent = new Intent(Intent.ACTION_MAIN);
        mSettingsIntent.setComponent(cn);
        mSettingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (context.getPackageManager().queryIntentActivities(mSettingsIntent, PackageManager.MATCH_DEFAULT_ONLY).size() == 0)
            mIsDataUsageAvailable = false;
        //Check if can toggle mobile data
        mCanToggleOff = (isOldMtkDevice() && Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) ||
                (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && hasRoot());
        mCanToggleOn = isOldMtkDevice();
        //Store subids
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager sm = SubscriptionManager.from(context);
            List<SubscriptionInfo> sl = sm.getActiveSubscriptionInfoList();
            StringBuilder subInfo = new StringBuilder();
            for (SubscriptionInfo si : sl) {
                subInfo.append(si.getSubscriptionId()).append(";");
            }
            subInfo = new StringBuilder(subInfo.substring(0, subInfo.length() - 1));
            preferences.edit()
                    .putString(Constants.PREF_OTHER[56], subInfo.toString())
                    .apply();
        }
        setOnOffAlarms();
        setCallResetAlarm();
    }

    public static void setOnClickListenerWithChild(ViewGroup v, View.OnClickListener listener) {
        v.setOnClickListener(listener);
        for (int i = 0; i < v.getChildCount(); i++) {
            View child = v.getChildAt(i);
            if (child instanceof ViewGroup) {
                setOnClickListenerWithChild((ViewGroup) child, listener);
            } else {
                child.setOnClickListener(listener);
            }
        }
    }

    public static String[] getStringArray(ListAdapter adapter) {
        String[] a = new String[adapter.getCount()];
        for (int i = 0; i < a.length; i++)
            a[i] = adapter.getItem(i).toString();
        return a;
    }

    public static String getRealPathFromURI(Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = mWeakReference.get().getContentResolver().query(contentUri, proj, null, null, null);
            int column_index;
            if (cursor != null) {
                column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            } else
                return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public static void onOff(ViewGroup layout, boolean state) {
        layout.setEnabled(false);
        for (int i = 0; i < layout.getChildCount(); i++) {
            View child = layout.getChildAt(i);
            if (child instanceof ViewGroup) {
                onOff((ViewGroup) child, state);
            } else {
                child.setEnabled(state);
            }
        }
    }

    public static Intent getSettingsIntent() {
        return mSettingsIntent;
    }

    public static boolean isDataUsageAvailable() {
        return !mIsDataUsageAvailable;
    }

    public static Context getAppContext() {
        return mWeakReference.get();
    }

    public static boolean isActivityVisible() {
        return mIsActivityVisible;
    }

    public static void resumeActivity() {
        mIsActivityVisible = true;
    }

    public static void pauseActivity() {
        mIsActivityVisible = false;
    }

    public static boolean isScreenOn() {
        PowerManager pm = (PowerManager) mWeakReference.get().getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
                return pm.isInteractive();
            else
                return pm.isScreenOn();
        }
        return false;
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass) {
        try {
            ActivityManager manager = (ActivityManager) mWeakReference.get().getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                //List<ActivityManager.RunningServiceInfo> list = manager.getRunningServices(Integer.MAX_VALUE);
                for (ActivityManager.RunningServiceInfo serviceInfo : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (serviceClass.getName().equals(serviceInfo.service.getClassName()))
                        return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static boolean isPackageExisted(String targetPackage) {
        try {
            PackageInfo info = mWeakReference.get().getPackageManager().getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    // Supported MTK devices
    private static final Set<String> OLD_MTK_DEVICES = new HashSet<>(Arrays.asList(
            // Single-core SoC
            "mt6575",
            // Dual-core SoC
            "mt6572",
            "mt6577",
            "mt8377",
            // Quad-core SoC
            "mt6582",
            "mt6582m",
            "mt6589",
            "mt8389",
            // Octa-core SoC
            "mt6592"));

    public static boolean isOldMtkDevice() {
        if (mIsOldMtkDevice == null)
            mIsOldMtkDevice = (OLD_MTK_DEVICES.contains(Build.HARDWARE.toLowerCase()) ||
                    OLD_MTK_DEVICES.contains(System.getProperty("ro.mediatek.platform", "")) ||
                    OLD_MTK_DEVICES.contains(System.getProperty("ro.board.platform", ""))) &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
        return mIsOldMtkDevice;
    }

    public static boolean hasGeminiSupport() {
        if (mHasGeminiSupport == null)
            mHasGeminiSupport = Objects.requireNonNull(System.getProperty("ro.mediatek.gemini_support", "")).equals("true") &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1;
        return mHasGeminiSupport;
    }

    public static boolean hasRoot() {
        if (mHasRoot == null)
            //mHasRoot = RootShell.isRootAvailable() && RootShell.isAccessGiven();
            mHasRoot = RootShell.isAccessGiven();
        return mHasRoot;
    }

    public static void putObject(SharedPreferences.Editor editor, String key, Object o) {
        if (o == null)
            editor.putString(key, "null");
        else if (o instanceof String)
            editor.putString(key, (String) o);
        else if (o instanceof Boolean)
            editor.putBoolean(key, (boolean) o);
    }

    public static int[] getWidgetIds(String name) {
        Class c;
        Context context = mWeakReference.get();
        if (name.equals(Constants.CALLS))
            c = CallsInfoWidget.class;
        else
            c = TrafficInfoWidget.class;
        int[] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, c));
        if (ids.length == 0) {
            try {
                File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
                String[] children = dir.list();
                int i = 0;
                for (String aChildren : children) {
                    String[] str = aChildren.split("_");
                    if (str.length > 0 && str[1].equalsIgnoreCase(name) && str[2].equalsIgnoreCase("widget")) {
                        ids[i] = Integer.valueOf(aChildren.split("_")[0]);
                        i++;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ids;
    }

    public static void sleep(long time) {
        try {
            TimeUnit.MILLISECONDS.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static int[] getCallsSimLimitsValues(boolean min) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mWeakReference.get());
        int limit1, limit2, limit3;
        try {
            if (min)
                limit1 = Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM1_CALLS[1], ""))) * Constants.MINUTE;
            else
                limit1 = Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM1_CALLS[1], "")));
        } catch (Exception e) {
            limit1 = Integer.MAX_VALUE;
        }
        try {
            if (min)
                limit2 = Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM2_CALLS[1], ""))) * Constants.MINUTE;
            else
                limit2 = Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM2_CALLS[1], "")));
        } catch (Exception e) {
            limit2 = Integer.MAX_VALUE;
        }
        try {
            if (min)
                limit3 = Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM3_CALLS[1], ""))) * Constants.MINUTE;
            else
                limit3 = Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM3_CALLS[1], "")));
        } catch (Exception e) {
            limit3 = Integer.MAX_VALUE;
        }
        return new int[]{limit1, limit2, limit3};
    }

    public static long[] getTrafficSimLimitsValues() {
        boolean[] isNight = getIsNightState();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mWeakReference.get());
        String limit1 = isNight[0] ? preferences.getString(Constants.PREF_SIM1[18], "") : preferences.getString(Constants.PREF_SIM1[1], "");
        String limit2 = isNight[1] ? preferences.getString(Constants.PREF_SIM2[18], "") : preferences.getString(Constants.PREF_SIM2[1], "");
        String limit3 = isNight[2] ? preferences.getString(Constants.PREF_SIM3[18], "") : preferences.getString(Constants.PREF_SIM3[1], "");
        String round1 = isNight[0] ? preferences.getString(Constants.PREF_SIM1[22], "0") : preferences.getString(Constants.PREF_SIM1[4], "0");
        String round2 = isNight[1] ? preferences.getString(Constants.PREF_SIM2[22], "0") : preferences.getString(Constants.PREF_SIM2[4], "0");
        String round3 = isNight[2] ? preferences.getString(Constants.PREF_SIM3[22], "0") : preferences.getString(Constants.PREF_SIM3[4], "0");
        int value1;
        if (Objects.requireNonNull(preferences.getString(Constants.PREF_SIM1[2], "")).equals(""))
            value1 = 0;
        else
            value1 = isNight[0] ? Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM1[19], ""))) :
                    Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM1[2], "")));
        int value2;
        if (Objects.requireNonNull(preferences.getString(Constants.PREF_SIM2[2], "")).equals(""))
            value2 = 0;
        else
            value2 = isNight[1] ? Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM2[19], ""))) :
                    Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM2[2], "")));
        int value3;
        if (Objects.requireNonNull(preferences.getString(Constants.PREF_SIM3[2], "")).equals(""))
            value3 = 0;
        else
            value3 = isNight[2] ? Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM3[19], ""))) :
                    Integer.valueOf(Objects.requireNonNull(preferences.getString(Constants.PREF_SIM3[2], "")));
        float valuer1, valuer2, valuer3;
        long lim1, lim2, lim3;
        try {
            valuer1 = 1 - Float.valueOf(Objects.requireNonNull(round1)) / 100;
            lim1 = (long) (valuer1 * DataFormat.getFormatLong(limit1, value1));
        } catch (Exception e) {
            lim1 = Long.MAX_VALUE;
        }
        try {
            valuer2 = 1 - Float.valueOf(Objects.requireNonNull(round2)) / 100;
            lim2 = (long) (valuer2 * DataFormat.getFormatLong(limit2, value2));
        } catch (Exception e) {
            lim2 = Long.MAX_VALUE;
        }
        try {
            valuer3 = 1 - Float.valueOf(Objects.requireNonNull(round3)) / 100;
            lim3 = (long) (valuer3 * DataFormat.getFormatLong(limit3, value3));
        } catch (Exception e) {
            lim3 = Long.MAX_VALUE;
        }
        return new long[]{lim1, lim2, lim3};
    }

    public static boolean[] getIsNightState() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mWeakReference.get());
        DateTime now = new DateTime();
        boolean isNight1, isNight2, isNight3;
        if (preferences.getBoolean(Constants.PREF_SIM1[17], false)) {
            String time = preferences.getString(Constants.PREF_SIM1[20], "23:00");
            if (Objects.requireNonNull(time).equals("null"))
                time = "23:00";
            String timeON = now.toString(Constants.DATE_FORMATTER) + " " + time;
            time = preferences.getString(Constants.PREF_SIM1[21], "06:00");
            if (Objects.requireNonNull(time).equals("null"))
                time = "06:00";
            String timeOFF = now.toString(Constants.DATE_FORMATTER) + " " + time;
            isNight1 = DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
        } else
            isNight1 = false;
        if (preferences.getBoolean(Constants.PREF_SIM2[17], false)) {
            String time = preferences.getString(Constants.PREF_SIM2[20], "23:00");
            if (Objects.requireNonNull(time).equals("null"))
                time = "23:00";
            String timeON = now.toString(Constants.DATE_FORMATTER) + " " + time;
            time = preferences.getString(Constants.PREF_SIM2[21], "06:00");
            if (Objects.requireNonNull(time).equals("null"))
                time = "06:00";
            String timeOFF = now.toString(Constants.DATE_FORMATTER) + " " + time;
            isNight2 = DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
        } else
            isNight2 = false;
        if (preferences.getBoolean(Constants.PREF_SIM3[17], false)) {
            String time = preferences.getString(Constants.PREF_SIM3[20], "23:00");
            if (Objects.requireNonNull(time).equals("null"))
                time = "23:00";
            String timeON = now.toString(Constants.DATE_FORMATTER) + " " + time;
            time = preferences.getString(Constants.PREF_SIM3[21], "06:00");
            if (Objects.requireNonNull(time).equals("null"))
                time = "06:00";
            String timeOFF = now.toString(Constants.DATE_FORMATTER) + " " + time;
            isNight3 = DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeON)) >= 0 && DateTimeComparator.getInstance().compare(now, Constants.DATE_TIME_FORMATTER.parseDateTime(timeOFF)) <= 0;
        } else
            isNight3 = false;

        return new boolean[]{isNight1, isNight2, isNight3};
    }

    public static void deletePreferenceFile(int quantity, String name) {
        Context context = mWeakReference.get();
        File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
        String[] children = dir.list();
        for (String aChildren : children) {
            for (int j = 0; j < quantity; j++) {
                if (aChildren.contains(name))
                    context.getSharedPreferences(aChildren.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().apply();
            }
        }
        sleep(1000);
        for (String aChildren : children) {
            for (int j = 0; j < quantity; j++) {
                if (aChildren.contains(name))
                    new File(dir, aChildren).delete();
            }
        }
    }

    public static void deleteWidgetPreferenceFile(int[] ids, String name) {
        Context context = mWeakReference.get();
        File dir = new File(context.getFilesDir().getParent() + "/shared_prefs/");
        String[] children = dir.list();
        for (String aChildren : children) {
            for (int j : ids)
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + name + Constants.WIDGET_PREFERENCES))
                    context.getSharedPreferences(aChildren.replace(".xml", ""), Context.MODE_PRIVATE).edit().clear().apply();
        }
        sleep(1000);
        for (String aChildren : children) {
            for (int j : ids)
                if (aChildren.replace(".xml", "").equalsIgnoreCase(String.valueOf(j) + name + Constants.WIDGET_PREFERENCES))
                    if (new File(dir, aChildren).delete())
                        Toast.makeText(context, R.string.deleted, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean canToggleOff() {
        return mCanToggleOff;
    }

    public static boolean canToggleOn() {
        return mCanToggleOn;
    }

    private static void setOnOffAlarms() {
        Context context = mWeakReference.get();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        DateTime alarmTime;
        //Scheduled ON/OFF
        if (am != null && (!prefs.getString(Constants.PREF_SIM1[11], "0").equals("3") || !prefs.getString(Constants.PREF_SIM2[11], "0").equals("3")
                || !prefs.getString(Constants.PREF_SIM3[11], "0").equals("3"))) {
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("1")) {
                Intent i1Off = new Intent(context, OnOffReceiver.class);
                i1Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1Off.putExtra(Constants.ON_OFF, false);
                i1Off.setAction(Constants.ALARM_ACTION);
                final int SIM1_OFF = 100;
                PendingIntent pi1Off = PendingIntent.getBroadcast(context, SIM1_OFF, i1Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                    am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1Off);
            }
            if (prefs.getString(Constants.PREF_SIM1[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM1[11], "0").equals("2")) {
                Intent i1On = new Intent(context, OnOffReceiver.class);
                i1On.putExtra(Constants.SIM_ACTIVE, Constants.SIM1);
                i1On.putExtra(Constants.ON_OFF, true);
                i1On.setAction(Constants.ALARM_ACTION);
                final int SIM1_ON = 101;
                PendingIntent pi1On = PendingIntent.getBroadcast(context, SIM1_ON, i1On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM1[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi1On);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("1")) {
                Intent i2Off = new Intent(context, OnOffReceiver.class);
                i2Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2Off.putExtra(Constants.ON_OFF, false);
                i2Off.setAction(Constants.ALARM_ACTION);
                final int SIM2_OFF = 110;
                PendingIntent pi2Off = PendingIntent.getBroadcast(context, SIM2_OFF, i2Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2Off);
            }
            if (prefs.getString(Constants.PREF_SIM2[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM2[11], "0").equals("2")) {
                Intent i2On = new Intent(context, OnOffReceiver.class);
                i2On.putExtra(Constants.SIM_ACTIVE, Constants.SIM2);
                i2On.putExtra(Constants.ON_OFF, true);
                i2On.setAction(Constants.ALARM_ACTION);
                final int SIM2_ON = 111;
                PendingIntent pi2On = PendingIntent.getBroadcast(context, SIM2_ON, i2On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM2[13], "00:05").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi2On);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("1")) {
                Intent i3Off = new Intent(context, OnOffReceiver.class);
                i3Off.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3Off.putExtra(Constants.ON_OFF, false);
                i3Off.setAction(Constants.ALARM_ACTION);
                final int SIM3_OFF = 120;
                PendingIntent pi3Off = PendingIntent.getBroadcast(context, SIM3_OFF, i3Off, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:35").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[12], "23:55").split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3Off);
            }
            if (prefs.getString(Constants.PREF_SIM3[11], "0").equals("0") ||
                    prefs.getString(Constants.PREF_SIM3[11], "0").equals("2")) {
                Intent i3On = new Intent(context, OnOffReceiver.class);
                i3On.putExtra(Constants.SIM_ACTIVE, Constants.SIM3);
                i3On.putExtra(Constants.ON_OFF, true);
                i3On.setAction(Constants.ALARM_ACTION);
                final int SIM3_ON = 121;
                PendingIntent pi3On = PendingIntent.getBroadcast(context, SIM3_ON, i3On, 0);
                alarmTime = new DateTime().withHourOfDay(Integer.valueOf(prefs.getString(Constants.PREF_SIM3[13], "00:05").split(":")[0]))
                        .withMinuteOfHour(Integer.valueOf(Objects.requireNonNull(prefs.getString(Constants.PREF_SIM3[13], "00:05")).split(":")[1]))
                        .withSecondOfMinute(0);
                if (alarmTime.getMillis() < System.currentTimeMillis())
                    alarmTime = alarmTime.plusDays(1);
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, pi3On);
            }
        }
    }

    private static void setCallResetAlarm() {
        Context context = mWeakReference.get();
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        DateTime alarmTime = new DateTime().withTimeAtStartOfDay();
        //Calls reset
        if (prefs.getBoolean(Constants.PREF_OTHER[25], true)) {
            Intent iReset = new Intent(context, ResetReceiver.class);
            iReset.setAction(Constants.RESET_ACTION);
            final int RESET = 1981;
            PendingIntent piReset = PendingIntent.getBroadcast(context, RESET, iReset, 0);
            if (alarmTime.getMillis() < System.currentTimeMillis())
                alarmTime = alarmTime.plusDays(1);
            if (am != null) {
                am.setRepeating(AlarmManager.RTC_WAKEUP, alarmTime.getMillis(), AlarmManager.INTERVAL_DAY, piReset);
            }
        }
    }

    public static void loadTrafficPreferences(ArrayList imsi) {
        if (imsi != null && imsi.size() > 0) {
            Context context = mWeakReference.get();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int simQuantity = preferences.getInt(Constants.PREF_OTHER[55], 1);
            String path = context.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = preferences.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefsMap;
            String name = Constants.TRAFFIC + "_" + imsi.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefsMap = prefSim.getAll();
                if (prefsMap.size() != 0)
                    for (String key : prefsMap.keySet()) {
                        Object o = prefsMap.get(key);
                        key = key + 1;
                        putObject(editor, key, o);
                    }
                prefSim = null;
            }
            if (simQuantity >= 2) {
                name = Constants.TRAFFIC + "_" + imsi.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefsMap = prefSim.getAll();
                    if (prefsMap.size() != 0)
                        for (String key : prefsMap.keySet()) {
                            Object o = prefsMap.get(key);
                            key = key + 2;
                            putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            if (simQuantity == 3) {
                name = Constants.TRAFFIC + "_" + imsi.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefsMap = prefSim.getAll();
                    if (prefsMap.size() != 0)
                        for (String key : prefsMap.keySet()) {
                            Object o = prefsMap.get(key);
                            key = key + 3;
                            putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }
    }

    public static void loadCallsPreferences(ArrayList imsi) {
        if (imsi != null && imsi.size() > 0) {
            Context context = mWeakReference.get();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            int simQuantity = preferences.getInt(Constants.PREF_OTHER[55], 1);
            String path = context.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = preferences.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefs;
            String name = Constants.CALLS + "_" + imsi.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefs = prefSim.getAll();
                if (prefs.size() != 0)
                    for (String key : prefs.keySet()) {
                        Object o = prefs.get(key);
                        key = key + 1;
                        putObject(editor, key, o);
                    }
                prefSim = null;
            }
            if (simQuantity >= 2) {
                name = Constants.CALLS + "_" + imsi.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (String key : prefs.keySet()) {
                            Object o = prefs.get(key);
                            key = key + 2;
                            putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            if (simQuantity == 3) {
                name = Constants.CALLS + "_" + imsi.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = context.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (String key : prefs.keySet()) {
                            Object o = prefs.get(key);
                            key = key + 3;
                            putObject(editor, key, o);
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }
    }

    private static void fixFolderPermissionsAsync() {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mWeakReference.get().getFilesDir().setExecutable(true, false);
                mWeakReference.get().getFilesDir().setReadable(true, false);
                File filesPrefsFolder = new File(mWeakReference.get().getFilesDir().getAbsolutePath() + "/../shared_prefs");
                filesPrefsFolder.setExecutable(true, false);
                filesPrefsFolder.setReadable(true, false);
            }
        });
    }
}
