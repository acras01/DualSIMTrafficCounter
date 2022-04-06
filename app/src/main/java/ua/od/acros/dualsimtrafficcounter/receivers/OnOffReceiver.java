package ua.od.acros.dualsimtrafficcounter.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import org.acra.ACRA;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileOutputStream;

import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class OnOffReceiver extends BroadcastReceiver {

    @Override
    public final void onReceive(Context context, Intent intent) {

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "counter:onoff");
        }
        if (wl != null) {
            wl.acquire(10*60*1000L /*10 minutes*/);
        }

        try {
            int sim = intent.getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
            boolean action = intent.getBooleanExtra(Constants.ON_OFF, true);
            MobileUtils.toggleMobileDataConnection(action, context, sim);
            File dir = new File(String.valueOf(context.getFilesDir()));
            String fileName = "onoff.txt";
            File file = new File(dir, fileName);
            FileOutputStream os = new FileOutputStream(file, true);
            String out = DateTime.now().toLocalDateTime().toString() + " " + sim + " " + action + "\n";
            os.write(out.getBytes());
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        if (wl != null && wl.isHeld())
            wl.release();
    }
}
