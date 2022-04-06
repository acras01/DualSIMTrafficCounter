package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.WindowManager;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.ListEvent;
import ua.od.acros.dualsimtrafficcounter.events.NoListEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class ChooseOperatorDialog extends AppCompatActivity {

    private AlertDialog mDialog;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = CustomApplication.getAppContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (savedInstanceState == null) {
            if (prefs.getBoolean(Constants.PREF_OTHER[29], true))
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_OTHER[28], "1")).equals("0"))
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }

        getWindow().setAttributes(new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT));

        final ArrayList<String> whiteList = getIntent().getStringArrayListExtra("whitelist");
        final ArrayList<String> blackList = getIntent().getStringArrayListExtra("blacklist");
        final Bundle bundle = getIntent().getBundleExtra("bundle");
        String number = bundle.getString("number");

        mDialog = new AlertDialog.Builder(this)
                .setTitle(number)
                .setCancelable(false)
                .setMessage(R.string.is_out_of_home_network)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    bundle.putStringArrayList("list", blackList);
                    bundle.putBoolean("black", true);
                    EventBus.getDefault().post(new ListEvent(bundle));
                    finish();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> {
                    bundle.putStringArrayList("list", whiteList);
                    bundle.putBoolean("black", false);
                    EventBus.getDefault().post(new ListEvent(bundle));
                    finish();
                })
                .setNeutralButton(android.R.string.cancel, (dialog, which) -> {
                    EventBus.getDefault().post(new NoListEvent());
                    finish();
                })
                .create();
        if (mDialog.getWindow() != null)
            mDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        if(!this.isFinishing()){
            mDialog.show();
        }
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}