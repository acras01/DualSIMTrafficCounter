package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class ManualSimDialog extends AppCompatActivity {

    private static boolean mIsActive;
    private AlertDialog mDialog;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIsActive = true;
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

        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.attention)
                .setMessage(R.string.sim_not_defined)
                .setPositiveButton(android.R.string.ok, (dialog, id) -> dialog.cancel())
                .create();

        if (!this.isFinishing()) {
            mDialog.show();
        }
    }

    public static boolean isActive() {
        return mIsActive;
    }

    @Override
    protected final void onResume() {
        super.onResume();
        mIsActive = true;
    }

    @Override
    protected final void onPause() {
        super.onPause();
        mIsActive = false;
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();
        mIsActive = false;
        if (mDialog != null && mDialog.isShowing())
            mDialog.dismiss();
    }
}
