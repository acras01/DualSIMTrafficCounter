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
import ua.od.acros.dualsimtrafficcounter.events.ActionTrafficEvent;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;

public class ChooseActionDialog extends AppCompatActivity {

    private String mAction = "";
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
        View view = View.inflate(this, R.layout.action_dialog, null);
        AppCompatRadioButton change = view.findViewById(R.id.actionchange);
        AppCompatRadioButton mobileData = view.findViewById(R.id.actionmobiledata);
        AppCompatRadioButton off = view.findViewById(R.id.actionoff);
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);
        if (!CustomApplication.canToggleOn() ||
                prefs.getBoolean(Constants.PREF_OTHER[10], false) || simQuantity == 1)
            change.setEnabled(false);
        if (CustomApplication.isDataUsageAvailable())
            mobileData.setEnabled(false);
        if (!CustomApplication.canToggleOff())
            off.setEnabled(false);
        mSimID = getIntent().getIntExtra(Constants.SIM_ACTIVE, Constants.DISABLED);
        RadioGroup radioGroup = view.findViewById(R.id.radioGroup);
        final ColorStateList[] textColor = new ColorStateList[] {ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorAccent))};
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            bOK.setEnabled(true);
            bOK.setTextColor(textColor[0]);
            switch (checkedId) {
                case R.id.actionmobiledata:
                    mAction = Constants.SETTINGS_ACTION;
                    break;
                case R.id.actionsettings:
                    mAction = Constants.LIMIT_ACTION;
                    break;
                case R.id.actionchange:
                    mAction = Constants.CHANGE_ACTION;
                    break;
                case R.id.actionoff:
                    mAction = Constants.OFF_ACTION;
                    break;
            }
        });
        if (CustomApplication.isDataUsageAvailable())
            view.findViewById(R.id.actionmobiledata).setEnabled(false);
        mDialog = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(view)
                .setTitle(R.string.attention)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
                    EventBus.getDefault().post(new ActionTrafficEvent(mSimID, Constants.CONTINUE_ACTION));
                    finish();
                })
                .create();

        mDialog.setOnShowListener(dialogInterface -> {
            bOK = (AppCompatButton) mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            textColor[0] = bOK.getTextColors();
            bOK.setEnabled(false);
            bOK.setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));
            bOK.setOnClickListener(view1 -> {
                EventBus.getDefault().post(new ActionTrafficEvent(mSimID, mAction));
                finish();
            });
        });
        if(!this.isFinishing()){
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