package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import ua.od.acros.dualsimtrafficcounter.events.NewOutgoingCallEvent;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class OutgoingCallReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (intent.getAction() != null && (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL) && getResultData() != null && !prefs.getBoolean(Constants.PREF_OTHER[24], true))) {
            Toast.makeText(context, "Outgoing call started", Toast.LENGTH_LONG).show();
            if (!CustomApplication.isMyServiceRunning(CallLoggerService.class)) {
                Intent i = new Intent(context, CallLoggerService.class);
                i.setAction(intent.getAction());
                i.putExtra(Intent.EXTRA_PHONE_NUMBER, getResultData());
                i.setFlags(intent.getFlags());
                context.startService(i);
            } else
                EventBus.getDefault().post(new NewOutgoingCallEvent(getResultData()));
        }
    }
}
