package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.appcompat.widget.SwitchCompat;
import android.view.View;
import android.widget.TextView;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomSwitch;

public class SoundFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_CODE_ALERT_RINGTONE = 142;
    private Context mContext;
    private SharedPreferences mPrefs;
    private CustomSwitch mSwitch = null;

    @Override
    public final void onCreatePreferences(Bundle bundle, String s) {
        mContext = Objects.requireNonNull(getActivity()).getApplicationContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        addPreferencesFromResource(R.xml.notification_settings);
        ActionBar actionBar = ((AppCompatActivity)getActivity()).getSupportActionBar();
        SwitchCompat actionBarSwitch = null;
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.actionbar_switch);
            View custom = actionBar.getCustomView();
            TextView tv = custom.findViewById(R.id.titleText);
            tv.setText(R.string.use_notification_title);
            tv = custom.findViewById(R.id.subTitleText);
            tv.setVisibility(View.GONE);
            actionBarSwitch = custom.findViewById(R.id.switchForActionBar);
        }
        if (actionBarSwitch != null)
            mSwitch = new CustomSwitch(getActivity(), actionBarSwitch, Constants.PREF_OTHER[3]);
        updateSettings();
    }

    private void updateSettings() {
        int count = getPreferenceScreen().getPreferenceCount();
        for (int i = 0; i < count; ++i) {
            androidx.preference.Preference pref = getPreferenceScreen().getPreference(i);
            pref.setEnabled(mSwitch.isSwitchOn());
            if (pref.getKey().equals(Constants.PREF_OTHER[1])) {
                Ringtone ringtone = RingtoneManager.getRingtone(mContext, Uri.parse(getRingtonePreferenceValue()));
                pref.setSummary(ringtone.getTitle(mContext));
            }
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        if (mSwitch != null)
            mSwitch.resume();
        updateSettings();
    }

    @Override
    public final void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
        if (mSwitch != null)
            mSwitch.pause();
    }

    @Override
    public final boolean onPreferenceTreeClick(Preference preference) {
        if (preference.getKey().equals("ringtone")) {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
            String existingValue = getRingtonePreferenceValue();
            if (existingValue != null) {
                if (existingValue.length() == 0) {
                    // Select "Silent"
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                } else {
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                }
            } else {
                // No ringtone has been selected, set to the default
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
            }

            startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
            return true;
        } else {
            return super.onPreferenceTreeClick(preference);
        }
    }

    @Override
    public final void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ALERT_RINGTONE && data != null) {
            Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if (ringtone != null) {
                setRingtonePreferenceValue(ringtone.toString());
            } else {
                // "Silent" was selected
                setRingtonePreferenceValue("");
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    private void setRingtonePreferenceValue(String ringtonePreferenceValue) {
        mPrefs.edit()
                .putString(Constants.PREF_OTHER[1], ringtonePreferenceValue)
                .apply();
    }

    private String getRingtonePreferenceValue() {
        return mPrefs.getString(Constants.PREF_OTHER[1], "");
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[3]))
            updateSettings();
    }
}
