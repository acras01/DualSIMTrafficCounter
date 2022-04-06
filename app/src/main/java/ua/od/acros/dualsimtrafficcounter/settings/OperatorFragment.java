package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;


public class OperatorFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TwoLineCheckPreference auto1, auto2, auto3, showLogo;
    private TwoLineEditTextPreference name1, name2, name3;
    private TwoLineListPreference logo1, logo2, logo3;
    private SharedPreferences mPrefs;
    private boolean mIsAttached;
    private Context mContext;
    private ArrayList<String> mIMSI = null;

    @Override
    public final void onCreatePreferences(Bundle bundle, String s) {

        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        int simNumber = mPrefs.getInt(Constants.PREF_OTHER[55], 1);

        if (mPrefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            mIMSI = MobileUtils.getSimIds(mContext);
            String path = mContext.getFilesDir().getParent() + "/shared_prefs/";
            SharedPreferences.Editor editor = mPrefs.edit();
            SharedPreferences prefSim;
            Map<String, ?> prefs;
            String name = Constants.TRAFFIC + "_" + mIMSI.get(0);
            if (new File(path + name + ".xml").exists()) {
                prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                prefs = prefSim.getAll();
                if (prefs.size() != 0)
                    for (String key : prefs.keySet()) {
                        if (key.equals(Constants.PREF_SIM_DATA[5]) || key.equals(Constants.PREF_SIM_DATA[6])) {
                            Object o = prefs.get(key);
                            key = key + 1;
                            CustomApplication.putObject(editor, key, o);
                        }
                    }
                prefSim = null;
            }
            if (simNumber >= 2) {
                name = Constants.TRAFFIC + "_" + mIMSI.get(1);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (String key : prefs.keySet()) {
                            if (key.equals(Constants.PREF_SIM_DATA[5]) || key.equals(Constants.PREF_SIM_DATA[6])) {
                                Object o = prefs.get(key);
                                key = key + 2;
                                CustomApplication.putObject(editor, key, o);
                            }
                        }
                    prefSim = null;
                }
            }
            if (simNumber == 3) {
                name = Constants.TRAFFIC + "_" + mIMSI.get(2);
                if (new File(path + name + ".xml").exists()) {
                    prefSim = mContext.getSharedPreferences(name, Context.MODE_PRIVATE);
                    prefs = prefSim.getAll();
                    if (prefs.size() != 0)
                        for (String key : prefs.keySet()) {
                            if (key.equals(Constants.PREF_SIM_DATA[5]) || key.equals(Constants.PREF_SIM_DATA[6])) {
                                Object o = prefs.get(key);
                                key = key + 3;
                                CustomApplication.putObject(editor, key, o);
                            }
                        }
                    prefSim = null;
                }
            }
            editor.apply();
        }

        addPreferencesFromResource(R.xml.operator_settings);

        showLogo = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[15]);

        name1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1[6]);
        auto1 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM1[5]);
        logo1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1[23]);

        auto2 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM2[5]);
        name2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2[6]);
        logo2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2[23]);

        auto3 = (TwoLineCheckPreference) findPreference(Constants.PREF_SIM3[5]);
        name3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3[6]);
        logo3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3[23]);

        PreferenceScreen sim2 = (PreferenceScreen) getPreferenceScreen().findPreference("sim2");
        PreferenceScreen sim3 = (PreferenceScreen) getPreferenceScreen().findPreference("sim3");

        if (simNumber == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
            logo2.setEnabled(false);
            logo3.setEnabled(false);
        }
        if (simNumber == 2) {
            getPreferenceScreen().removePreference(sim3);
            logo3.setEnabled(false);
        }
        if (mIsAttached)
            updateSummary();
    }

    private void updateSummary() {
        if (auto1 != null && !auto1.isChecked())
            name1.setSummary(name1.getText());
        if (auto2 != null &&  !auto2.isChecked())
            name2.setSummary(name2.getText());
        if (auto3 != null &&  !auto3.isChecked())
            name3.setSummary(name3.getText());

        String[] listitems = getResources().getStringArray(R.array.logo_values);
        String[] list = getResources().getStringArray(R.array.logo);
        for (int i = 0; i < list.length; i++) {
            if (showLogo != null) {
                if (showLogo.isChecked() && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM1[23], "none")))
                    if (logo1 != null)
                        logo1.setSummary(list[i]);
                if (showLogo.isChecked() && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM2[23], "none")))
                    if (logo2 != null)
                        logo2.setSummary(list[i]);
                if (showLogo.isChecked() && listitems[i].equals(mPrefs.getString(Constants.PREF_SIM3[23], "none")))
                    if (logo3 != null)
                        logo3.setSummary(list[i]);
            }
        }
    }

    @Override
    public final void onAttach(Context activity) {
        super.onAttach(activity);
        mIsAttached = true;
    }

    @Override
    public final void onDetach() {
        super.onDetach();
        mIsAttached = false;
    }

    @Override
    public final void onResume() {
        super.onResume();
        ((Toolbar) Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar)).setTitle(R.string.name_title);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public final void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mIsAttached)
            updateSummary();
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[44], false)) {
            int sim = Constants.DISABLED;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM1)).contains(key))
                sim = Constants.SIM1;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM2)).contains(key))
                sim = Constants.SIM2;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM3)).contains(key))
                sim = Constants.SIM3;
            if (sim >= 0) {
                Map prefs = sharedPreferences.getAll();
                Object o = prefs.get(key);
                SharedPreferences.Editor editor = mContext.getSharedPreferences(Constants.TRAFFIC + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
                CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
                editor.apply();
            }
        }
    }
}