package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.events.MobileConnectionEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoConnectivityEvent;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.services.WatchDogService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ConnectivityChangeReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
                .putInt(Constants.PREF_OTHER[55], prefs.getBoolean(Constants.PREF_OTHER[13], true) ? MobileUtils.isMultiSim(context)
                        : Integer.valueOf(Objects.requireNonNull(prefs.getString(Constants.PREF_OTHER[14], "1"))))
                .apply();

        if (intent.getAction() != null && intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, Boolean.FALSE) ||
                    !MobileUtils.isMobileDataActive(context)) {
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new NoConnectivityEvent());
            } else {
                //start WatchDogService
                if (prefs.getBoolean(Constants.PREF_OTHER[4], true))
                    context.startService(new Intent(context, WatchDogService.class));
                if (!CustomApplication.isMyServiceRunning(TrafficCountService.class) &&
                        !prefs.getBoolean(Constants.PREF_OTHER[5], false)) {
                    Intent i = new Intent(context, TrafficCountService.class);
                    i.setAction(intent.getAction());
                    i.putExtras(Objects.requireNonNull(intent.getExtras()));
                    i.setFlags(intent.getFlags());
                    context.startService(i);
                } else if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    EventBus.getDefault().post(new MobileConnectionEvent());
            }
        }
    }
}