package ua.od.acros.dualsimtrafficcounter.preferences;

import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public abstract class PreferenceFragmentCompatFix extends PreferenceFragmentCompat {

    private static final String FRAGMENT_DIALOG_TAG = "android.support.v7.preference.PreferenceFragment.DIALOG";

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        if (this.getFragmentManager() != null && this.getFragmentManager().findFragmentByTag(FRAGMENT_DIALOG_TAG) == null) {
            DialogFragment f = null;
            if (preference instanceof TwoLineEditTextPreference) {
                f = EditTextPreferenceDialogFragmentCompatFix.newInstance(preference.getKey());
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
            if (f != null) {
                f.setTargetFragment(this, 0);
                f.show(this.getFragmentManager(), FRAGMENT_DIALOG_TAG);
            }
        }
    }
}
