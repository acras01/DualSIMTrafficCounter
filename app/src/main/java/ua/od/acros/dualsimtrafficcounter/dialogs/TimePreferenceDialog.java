package ua.od.acros.dualsimtrafficcounter.dialogs;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.TimePicker;

import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;

public class TimePreferenceDialog extends PreferenceDialogFragmentCompat implements DialogPreference.TargetFragment {

    private TimePicker mTimePicker = null;

    public static TimePreferenceDialog newInstance(Preference preference) {
        TimePreferenceDialog fragment = new TimePreferenceDialog();
        Bundle bundle = new Bundle(1);
        bundle.putString("key", preference.getKey());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    protected final View onCreateDialogView(Context context) {
        mTimePicker = new TimePicker(context);
        return (mTimePicker);
    }

    @Override
    protected final void onBindDialogView(View v) {
        super.onBindDialogView(v);
        if (!DateFormat.is24HourFormat(getContext()))
            mTimePicker.setIs24HourView(false);
        else
            mTimePicker.setIs24HourView(true);
        TimePreference pref = (TimePreference) getPreference();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mTimePicker.setHour(pref.mHour);
            mTimePicker.setMinute(pref.mMinute);
        } else {
            mTimePicker.setCurrentHour(pref.mHour);
            mTimePicker.setCurrentMinute(pref.mMinute);
        }
    }

    @Override
    public final void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            TimePreference pref = (TimePreference) getPreference();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pref.mHour = mTimePicker.getHour();
                pref.mMinute = mTimePicker.getMinute();
            } else {
                pref.mHour = mTimePicker.getCurrentHour();
                pref.mMinute = mTimePicker.getCurrentMinute();
            }
            String value = TimePreference.timeToString(pref.mHour, pref.mMinute);
            if (pref.callChangeListener(value)) pref.persistStringValue(value);
        }
    }

    @Override
    public final Preference findPreference(CharSequence charSequence) {
        return getPreference();
    }
}