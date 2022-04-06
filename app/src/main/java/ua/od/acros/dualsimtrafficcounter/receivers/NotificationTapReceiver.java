package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class NotificationTapReceiver extends BroadcastReceiver {
    public NotificationTapReceiver() {
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        Intent activityIntent = null;
        String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case Constants.TRAFFIC_TAP:
                    activityIntent = new Intent(context, MainActivity.class);
                    activityIntent.setAction(Constants.TRAFFIC_TAP);
                    break;
                case Constants.CALLS_TAP:
                    activityIntent = new Intent(context, MainActivity.class);
                    activityIntent.setAction(Constants.CALLS_TAP);
                    break;
                case Constants.SETTINGS_TAP:
                    activityIntent = new Intent(context, SettingsActivity.class);
                    activityIntent.setAction(Constants.SETTINGS_TAP);
                    break;
                case Constants.HIDE:
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                    prefs.edit()
                            .putBoolean(Constants.PREF_OTHER[50], !prefs.getBoolean(Constants.PREF_OTHER[50], true))
                            .apply();
                    Intent i;
                    if (CustomApplication.isMyServiceRunning(TrafficCountService.class)) {
                        i = new Intent(context, TrafficCountService.class);
                        context.stopService(i);
                        context.startService(i);
                    } else if (CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                        i = new Intent(context, CallLoggerService.class);
                        context.stopService(i);
                        context.startService(i);
                    }
                    break;
            }
        }
        if (activityIntent != null) {
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(activityIntent);
            Intent closePanelIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(closePanelIntent);
        }
    }
}
