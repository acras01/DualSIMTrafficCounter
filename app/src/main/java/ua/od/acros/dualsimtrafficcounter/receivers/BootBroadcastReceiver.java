package ua.od.acros.dualsimtrafficcounter.receivers;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (intent.getAction() != null &&
                (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)
                        || intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON")
                        || intent.getAction().equals("com.htc.intent.action.QUICKBOOT_POWERON"))) {
            //start CountService
            if (!prefs.getBoolean(Constants.PREF_OTHER[5], false) && !prefs.getBoolean(Constants.PREF_OTHER[47], false))
                context.startService(new Intent(context, TrafficCountService.class));
            else {
                //Update widgets
                int[] ids = CustomApplication.getWidgetIds(Constants.TRAFFIC);
                if (ids.length != 0) {
                    Intent i = new Intent(Constants.TRAFFIC_BROADCAST_ACTION);
                    i.putExtra(Constants.WIDGET_IDS, ids);
                    context.sendBroadcast(i);
                }
            }
            //start CallLoggerService
            if (!prefs.getBoolean(Constants.PREF_OTHER[24], false))
                context.startService(new Intent(context, CallLoggerService.class));
            else {
                //Update widgets
                int[] ids = CustomApplication.getWidgetIds(Constants.CALLS);
                if (ids.length != 0) {
                    Intent i = new Intent(Constants.CALLS_BROADCAST_ACTION);
                    i.putExtra(Constants.WIDGET_IDS, ids);
                    context.sendBroadcast(i);
                }
            }

            //start WatchDogService
        /*if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
            context.startService(new Intent(context, WatchDogService.class));*/

            //start FloatingWindow
        /*if (prefs.getBoolean(Constants.PREF_OTHER[32], false) &&
                ((prefs.getBoolean(Constants.PREF_OTHER[41], false) && MobileUtils.isMobileDataActive(context)) ||
                        !prefs.getBoolean(Constants.PREF_OTHER[41], false)))
            StandOutWindow.show(context, FloatingWindowService.class, prefs.getInt(Constants.PREF_OTHER[38], StandOutWindow.DEFAULT_ID));*/

        /*CustomApplication.setOnOffAlarms(context);
        CustomApplication.setCallResetAlarm(context);*/
        }
    }
}
