package ua.od.acros.dualsimtrafficcounter.settings;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.Toolbar;
import android.text.InputFilter;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.TimePreferenceDialog;
import ua.od.acros.dualsimtrafficcounter.preferences.PreferenceFragmentCompatFix;
import ua.od.acros.dualsimtrafficcounter.preferences.TimePreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineCheckPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineEditTextPreference;
import ua.od.acros.dualsimtrafficcounter.preferences.TwoLineListPreference;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DateUtils;
import ua.od.acros.dualsimtrafficcounter.utils.InputFilterMinMax;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class CallsLimitFragment extends PreferenceFragmentCompatFix implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceChangeListener {

    private TwoLineEditTextPreference limit1, limit2, limit3,
            day1, day2, day3, round1, round2, round3;
    private TwoLineListPreference period1, period2, period3, opValue1, opValue2, opValue3;
    private TimePreference time1, time2, time3;
    private static SharedPreferences mPrefs;
    private boolean mIsAttached = false;
    private Context mContext;
    private static ArrayList<String> mIMSI = null;
    private static int mSimQuantity;

    @Override
    public final void onCreatePreferences(Bundle bundle, String s) {

        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mSimQuantity = mPrefs.getInt(Constants.PREF_OTHER[55], 1);

        if (mIMSI == null)
            mIMSI = MobileUtils.getSimIds(mContext);
        if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false))
            CustomApplication.loadCallsPreferences(mIMSI);

        addPreferencesFromResource(R.xml.calls_settings);

        limit1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[1]);
        limit2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[1]);
        limit3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[1]);
        period1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1_CALLS[2]);
        period2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2_CALLS[2]);
        period3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3_CALLS[2]);
        round1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[3]);
        round2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[3]);
        round3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[3]);
        time1 = (TimePreference) findPreference(Constants.PREF_SIM1_CALLS[4]);
        time2 = (TimePreference) findPreference(Constants.PREF_SIM2_CALLS[4]);
        time3 = (TimePreference) findPreference(Constants.PREF_SIM3_CALLS[4]);
        day1 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM1_CALLS[5]);
        day2 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM2_CALLS[5]);
        day3 = (TwoLineEditTextPreference) findPreference(Constants.PREF_SIM3_CALLS[5]);
        opValue1 = (TwoLineListPreference) findPreference(Constants.PREF_SIM1_CALLS[6]);
        opValue2 = (TwoLineListPreference) findPreference(Constants.PREF_SIM2_CALLS[6]);
        opValue3 = (TwoLineListPreference) findPreference(Constants.PREF_SIM3_CALLS[6]);

        PreferenceScreen sim2 = (PreferenceScreen) getPreferenceScreen().findPreference("calls_sim2");
        PreferenceScreen sim3 = (PreferenceScreen) getPreferenceScreen().findPreference("calls_sim3");

        TwoLineCheckPreference save = (TwoLineCheckPreference) findPreference(Constants.PREF_OTHER[45]);
        if (save != null && (mIMSI == null || mIMSI.size() != mSimQuantity || mIMSI.contains(null)))
            save.setEnabled(false);

        if (mSimQuantity == 1) {
            getPreferenceScreen().removePreference(sim2);
            getPreferenceScreen().removePreference(sim3);
        }
        if (mSimQuantity == 2) {
            getPreferenceScreen().removePreference(sim3);
        }
        if (mIsAttached)
            updateSummary();

        day1.setOnPreferenceChangeListener(this);
        day2.setOnPreferenceChangeListener(this);
        day3.setOnPreferenceChangeListener(this);

        day1.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});
        day2.getEditText().setFilters(new InputFilter[]{new InputFilterMinMax(1, 31)});

        if (getArguments() != null) {
            String sim = getArguments().getString("sim");
            SettingsActivity.openPreferenceScreen(this, (PreferenceScreen) getPreferenceScreen().findPreference(sim));
        }
    }

    @Override
    public final void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;
        if (preference instanceof TimePreference) {
            dialogFragment = TimePreferenceDialog.newInstance(preference);
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
        }
        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    private void updateSummary() {
        if (limit1 != null)
            try {
            limit1.setSummary(String.format(getResources().getString(R.string.minutes), Integer.valueOf(limit1.getText())));
            } catch (Exception e) {

            }
        if (period1 != null) {
            period1.setSummary(period1.getEntry());
            if (period1.getValue().equals("0") && day1 != null)
                day1.setEnabled(false);
            if ((period1.getValue().equals("1") || period1.getValue().equals("2")) && day1 != null) {
                day1.setEnabled(true);
                if (period1.getValue().equals("1"))
                    day1.setTitle(getResources().getString(R.string.day));
                else
                    day1.setTitle(getResources().getString(R.string.day_in_period));
            }
        }
        if (day1 != null && day1.isEnabled())
            day1.setSummary(day1.getText());
        if (round1 != null)
            round1.setSummary(String.format(getResources().getString(R.string.seconds), Integer.valueOf(round1.getText())));
        if (time1 != null)
            time1.setSummary(mPrefs.getString(Constants.PREF_SIM1_CALLS[4], "00:00"));
        if (opValue1 != null)
            opValue1.setSummary(opValue1.getEntry());

        if (mSimQuantity >= 2) {
            if (limit2 != null)
                try {
                    limit2.setSummary(String.format(getResources().getString(R.string.minutes), Integer.valueOf(limit2.getText())));
                } catch (Exception e) {

                }
            if (period2 != null) {
                period2.setSummary(period2.getEntry());
                if (period2.getValue().equals("0") && day2 != null)
                    day2.setEnabled(false);
                if ((period2.getValue().equals("1") || period2.getValue().equals("2")) && day2 != null) {
                    day2.setEnabled(true);
                    if (period2.getValue().equals("1"))
                        day2.setTitle(getResources().getString(R.string.day));
                    else
                        day2.setTitle(getResources().getString(R.string.day_in_period));
                }
            }
            if (day2 != null && day2.isEnabled())
                day2.setSummary(day2.getText());
            if (round2 != null)
                round2.setSummary(String.format(getResources().getString(R.string.seconds), Integer.valueOf(round2.getText())));
            if (time2 != null)
                time2.setSummary(mPrefs.getString(Constants.PREF_SIM2_CALLS[4], "00:00"));
            if (opValue2 != null)
                opValue2.setSummary(opValue2.getEntry());
        }

        if (mSimQuantity == 3) {
            if (limit3 != null)
                try {
                    limit3.setSummary(String.format(getResources().getString(R.string.minutes), Integer.valueOf(limit3.getText())));
                } catch (Exception e) {

                }
            if (period3 != null) {
                period3.setSummary(period3.getEntry());
                if (period3.getValue().equals("0") && day3 != null)
                    day3.setEnabled(false);
                if ((period3.getValue().equals("1") || period3.getValue().equals("2")) && day3 != null) {
                    day3.setEnabled(true);
                    if (period3.getValue().equals("1"))
                        day3.setTitle(getResources().getString(R.string.day));
                    else
                        day3.setTitle(getResources().getString(R.string.day_in_period));
                }
            }
            if (day3 != null && day3.isEnabled())
                day3.setSummary(day3.getText());
            if (round3 != null)
                round3.setSummary(String.format(getResources().getString(R.string.seconds), Integer.valueOf(round3.getText())));
            if (time3 != null)
                time3.setSummary(mPrefs.getString(Constants.PREF_SIM3_CALLS[4], "00:00"));
            if (opValue3 != null)
                opValue3.setSummary(opValue3.getEntry());
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(R.string.calls_limit_title);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public final void onPause() {
        super.onPause();
        mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public final void onAttach(Activity activity) {
        super.onAttach(activity);
        mIsAttached = true;
    }

    @Override
    public final void onDetach() {
        super.onDetach();
        mIsAttached = false;
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (mIsAttached)
            updateSummary();
        if (key.equals(Constants.PREF_OTHER[45])) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(mContext);
            if (sharedPreferences.getBoolean(key, false)) {
                for (int i = 0; i < mSimQuantity; i++) {
                    new SaveTask().execute(i);
                }
            } else
                new DeleteTask().execute();
        }
        if (!CustomApplication.isMyServiceRunning(CallLoggerService.class) &&
                (key.equals(Constants.PREF_SIM1_CALLS[2]) || key.equals(Constants.PREF_SIM1_CALLS[4]) || key.equals(Constants.PREF_SIM1_CALLS[5]) ||
                key.equals(Constants.PREF_SIM2_CALLS[2]) || key.equals(Constants.PREF_SIM2_CALLS[4]) || key.equals(Constants.PREF_SIM2_CALLS[5]) ||
                key.equals(Constants.PREF_SIM3_CALLS[2]) || key.equals(Constants.PREF_SIM3_CALLS[4]) || key.equals(Constants.PREF_SIM3_CALLS[5]))) {
            checkIfResetNeeded();
        }
        if (sharedPreferences.getBoolean(Constants.PREF_OTHER[45], false)) {
            int sim = Constants.DISABLED;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM1_CALLS)).contains(key))
                sim = Constants.SIM1;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM2_CALLS)).contains(key))
                sim = Constants.SIM2;
            if (new ArrayList<>(Arrays.asList(Constants.PREF_SIM3_CALLS)).contains(key))
                sim = Constants.SIM3;
            if (sim >= 0) {
                if (mIMSI == null)
                    mIMSI = MobileUtils.getSimIds(mContext);
                Map prefs = sharedPreferences.getAll();
                Object o = prefs.get(key);
                SharedPreferences.Editor editor = mContext.getSharedPreferences(Constants.CALLS + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
                CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
                editor.apply();
            }
        }
    }

    @Override
    public final boolean onPreferenceChange(Preference preference, Object o) {
        switch (preference.getKey()) {
            case "calls_day1":
            case "calls_day2":
            case "calls_day3":
                String input = o.toString();
                if (input.matches("[0-9]+") && (Integer.valueOf(input) >= 1 && Integer.valueOf(input) <= 31))
                    return true;
                break;
        }
        Toast.makeText(getActivity(), R.string.check_input, Toast.LENGTH_LONG).show();
        return false;
    }

    private static class SaveTask extends AsyncTask<Integer, Void, Boolean> {

        @Override
        protected final Boolean doInBackground(Integer... params) {
            Context ctx = CustomApplication.getAppContext();
            Map<String, ?> prefs = mPrefs.getAll();
            String[] keys = new String[Constants.PREF_SIM_CALLS.length];
            int sim = params[0];
            switch (sim) {
                case Constants.SIM1:
                    keys = Constants.PREF_SIM1_CALLS;
                    break;
                case Constants.SIM2:
                    keys = Constants.PREF_SIM2_CALLS;
                    break;
                case Constants.SIM3:
                    keys = Constants.PREF_SIM3_CALLS;
                    break;
            }
            SharedPreferences.Editor editor = ctx.getSharedPreferences(Constants.CALLS + "_" + mIMSI.get(sim), Context.MODE_PRIVATE).edit();
            Set<String> keySet = prefs.keySet();
            ArrayList<String> simKeys = new ArrayList<>(Arrays.asList(keys));
            for (String key : keySet) {
                if (simKeys.contains(key)) {
                    Object o = prefs.get(key);
                    CustomApplication.putObject(editor, key.substring(0, key.length() - 1), o);
                }
            }
            CustomApplication.putObject(editor, "stub", null);
            editor.apply();
            CustomDatabaseHelper dbHelper = CustomDatabaseHelper.getInstance(ctx);
            String[] list = new String[]{"black", "white"};
            for (String name : list) {
                if (CustomDatabaseHelper.isTableEmpty(dbHelper, name + "_" + mIMSI.get(sim), false))
                    CustomDatabaseHelper.writeList(sim, CustomDatabaseHelper.readList(sim, dbHelper, null, name), dbHelper, mIMSI, name);
            }
            return true;
        }

        @Override
        protected final void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(CustomApplication.getAppContext(), R.string.saved, Toast.LENGTH_LONG).show();
        }
    }

    private static class DeleteTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected final Boolean doInBackground(Void... params) {
            CustomDatabaseHelper dbHelper = CustomDatabaseHelper.getInstance(CustomApplication.getAppContext());
            CustomDatabaseHelper.deleteListTables(dbHelper, mIMSI);
            CustomDatabaseHelper.deleteDataTable(dbHelper, mIMSI, Constants.CALLS);
            CustomApplication.deletePreferenceFile(mSimQuantity, Constants.CALLS);
            return true;
        }

        @Override
        protected final void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(CustomApplication.getAppContext(), R.string.deleted, Toast.LENGTH_LONG).show();
        }
    }

    private void checkIfResetNeeded() {
        String[] simPref = new String[]{Constants.PREF_SIM1_CALLS[2], Constants.PREF_SIM1_CALLS[4],
                Constants.PREF_SIM1_CALLS[5], Constants.PREF_SIM1_CALLS[8]};
        LocalDateTime resetTime1 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM1_CALLS[8], "1970-01-01 00:00"));
        LocalDateTime nowDate = DateTime.now().toLocalDateTime();
        if (nowDate.compareTo(resetTime1) >= 0) {
            resetTime1 = DateUtils.setResetDate(mPrefs, simPref);
            if (resetTime1 != null) {
                mPrefs.edit()
                        .putString(Constants.PREF_SIM1_CALLS[8], resetTime1.toString(Constants.DATE_TIME_FORMATTER))
                        .apply();
            }
        }
        if (mSimQuantity >= 2) {
            simPref = new String[]{Constants.PREF_SIM2_CALLS[2], Constants.PREF_SIM2_CALLS[4],
                    Constants.PREF_SIM2_CALLS[5], Constants.PREF_SIM2_CALLS[8]};
            LocalDateTime resetTime2 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM2_CALLS[8], "1970-01-01 00:00"));
            if (nowDate.compareTo(resetTime2) >= 0) {
                resetTime2 = DateUtils.setResetDate(mPrefs, simPref);
                if (resetTime2 != null) {
                    mPrefs.edit()
                            .putString(Constants.PREF_SIM2_CALLS[8], resetTime2.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                }
            }
        }
        if (mSimQuantity == 3) {
            simPref = new String[]{Constants.PREF_SIM3_CALLS[2], Constants.PREF_SIM3_CALLS[4],
                    Constants.PREF_SIM3_CALLS[5], Constants.PREF_SIM3_CALLS[8]};
            LocalDateTime resetTime3 = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(mPrefs.getString(Constants.PREF_SIM3_CALLS[8], "1970-01-01 00:00"));
            if (nowDate.compareTo(resetTime3) >= 0) {
                resetTime3 = DateUtils.setResetDate(mPrefs, simPref);
                if (resetTime3 != null) {
                    mPrefs.edit()
                            .putString(Constants.PREF_SIM3_CALLS[8], resetTime3.toString(Constants.DATE_TIME_FORMATTER))
                            .apply();
                }
            }
        }
    }
}