package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.CompoundButton;

public class CustomSwitch implements CompoundButton.OnCheckedChangeListener {

    private final SharedPreferences mPrefs;
    private SwitchCompat mSwitch;
    private final String mKey;

    public CustomSwitch(Context context, SwitchCompat swtch, String key) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mKey = key;
        setSwitch(swtch);
    }

    public final void setSwitch(SwitchCompat swtch) {
        if (mSwitch == swtch)
            return;
        if (mSwitch != null)
            mSwitch.setOnCheckedChangeListener(null);
        mSwitch = swtch;
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setChecked(isSwitchOn());
    }

    public final void onCheckedChanged(CompoundButton view, boolean isChecked) {
        Editor editor = mPrefs.edit();
        editor.putBoolean(mKey, isChecked)
                .apply();
    }

    public final boolean isSwitchOn() {
        return mPrefs.getBoolean(mKey, true);
    }

    public final void resume() {
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.setChecked(isSwitchOn());
    }

    public final void pause() {
        mSwitch.setOnCheckedChangeListener(null);
    }

    public final void setChecked(boolean checked) {
        mSwitch.setChecked(checked);
    }
}

