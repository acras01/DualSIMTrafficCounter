package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ResetReceiver extends BroadcastReceiver {

    private ArrayList<String> mIMSI = null;

    public ResetReceiver() {
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "counter:reset");
        }
        if (wl != null) {
            wl.acquire(10*60*1000L /*10 minutes*/);
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
        if (prefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            mIMSI = MobileUtils.getSimIds(context);
            CustomApplication.loadCallsPreferences(mIMSI);
            prefs = null;
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
        }

        LocalDateTime resetDate;
        String[] simPref = new String[]{Constants.PREF_SIM1_CALLS[2], Constants.PREF_SIM1_CALLS[4],
                Constants.PREF_SIM1_CALLS[5]};
        if (!prefs.getBoolean(Constants.PREF_SIM1_CALLS[9], false)) {
            resetDate = DateUtils.setResetDate(prefs, simPref);
            if (resetDate != null) {
                prefs.edit()
                        .putBoolean(Constants.PREF_SIM1_CALLS[9], true)
                        .putString(Constants.PREF_SIM1_CALLS[8], resetDate.toString(Constants.DATE_TIME_FORMATTER))
                        .apply();
            }
            if (mIMSI != null)
                saveSimSettings(context, prefs, 0);
        }
        if (simQuantity >= 2) {
            if (!prefs.getBoolean(Constants.PREF_SIM2_CALLS[9], false)) {
                simPref = new String[]{Constants.PREF_SIM2_CALLS[2], Constants.PREF_SIM2_CALLS[4],
                        Constants.PREF_SIM2_CALLS[5]};
                resetDate = DateUtils.setResetDate(prefs, simPref);
                if (resetDate != null) {
                    prefs.edit()
                            .putBoolean(Constants.PREF_SIM2_CALLS[9], true)
                            .putString(Constants.PREF_SIM2_CALLS[8], resetDate.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                }
                if (mIMSI != null)
                    saveSimSettings(context, prefs, 1);
            }
        }
        if (simQuantity == 3) {
            if (!prefs.getBoolean(Constants.PREF_SIM3_CALLS[9], false)) {
                simPref = new String[]{Constants.PREF_SIM3_CALLS[2], Constants.PREF_SIM3_CALLS[4],
                        Constants.PREF_SIM3_CALLS[5]};
                resetDate = DateUtils.setResetDate(prefs, simPref);
                if (resetDate != null) {
                    prefs.edit()
                            .putBoolean(Constants.PREF_SIM3_CALLS[9], true)
                            .putString(Constants.PREF_SIM3_CALLS[8], resetDate.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                }
                if (mIMSI != null)
                    saveSimSettings(context, prefs, 2);
            }
        }
        if (wl.isHeld())
            wl.release();
    }

    private void saveSimSettings(Context context, SharedPreferences prefs, int sim) {
        Map<String, ?> prefsMap = prefs.getAll();
        String[] keys = new String[Constants.PREF_SIM_CALLS.length];
        switch (sim) {
            case Constants.SIM1:
                keys = Constants.PREF_SIM1_CALLS;
                break;
            case Constants.SIM2:
                keys = Constants.PREF_SIM2_CALLS;
                break;
            case Constants.SIM3:
                keys = Constants.PREF_SIM3_CALLS;
                break;
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(Constants.CALLS + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
        Set<String> keySet = prefsMap.keySet();
        ArrayList<String> simKeys = new ArrayList<>(Arrays.asList(keys));
        for (String key : keySet) {
            if (simKeys.contains(key)) {
                Object o = prefsMap.get(key);
                CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
            }
        }
        CustomApplication.putObject(editor, "stub", null);
        editor.apply();
    }
}
