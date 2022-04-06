package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatRadioButton;
import android.view.View;
import android.widget.RadioGroup;

import org.greenrobot.eventbus.EventBus;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.events.SetSimEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class ChooseSimDialog extends AppCompatActivity {

    private int mSimID;
    private static boolean mIsActive;
    private AppCompatButton bOK;
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
        View view = View.inflate(this, R.layout.sim_dialog, null);
        String[] operatorNames = new String[]{MobileUtils.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};

        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        AppCompatRadioButton sim1rb = view.findViewById(R.id.sim1RB);
        sim1rb.setText(operatorNames[0]);
        AppCompatRadioButton sim2rb = view.findViewById(R.id.sim2RB);
        sim2rb.setText(operatorNames[1]);
        AppCompatRadioButton sim3rb = view.findViewById(R.id.sim3RB);
        sim3rb.setText(operatorNames[2]);
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
        if (simQuantity == 1) {
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }
        if (simQuantity == 2)
            sim3rb.setEnabled(false);
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ||
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && !CustomApplication.isOldMtkDevice())) {
            sim1rb.setEnabled(false);
            sim2rb.setEnabled(false);
            sim3rb.setEnabled(false);
        }*/
        final ColorStateList[] textColor = new ColorStateList[]{ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorAccent))};
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            bOK.setEnabled(true);
            bOK.setTextColor(textColor[0]);
            switch (checkedId) {
                case R.id.sim1RB:
                    mSimID = Constants.SIM1;
                    break;
                case R.id.sim2RB:
                    mSimID = Constants.SIM2;
                    break;
                case R.id.sim3RB:
                    mSimID = Constants.SIM3;
                    break;
            }
        });
        mDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(view)
                .setTitle(R.string.attention)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        mDialog.setOnShowListener(dialogInterface -> {
            bOK = (AppCompatButton) mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            textColor[0] = bOK.getTextColors();
            bOK.setEnabled(false);
            bOK.setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            bOK.setOnClickListener(view1 -> {
                EventBus.getDefault().post(new SetSimEvent(mSimID, true));
                finish();
            });
        });
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