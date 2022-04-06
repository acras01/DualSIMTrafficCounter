package ua.od.acros.dualsimtrafficcounter.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomSwitch;

public class SettingsFragment extends Fragment implements View.OnClickListener {

    private SharedPreferences mPrefs;
    private CustomSwitch mSwitch;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        mSwitch = new CustomSwitch(context, new SwitchCompat(context), Constants.PREF_OTHER[3]);
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Objects.requireNonNull(getActivity()).setTitle(R.string.action_settings);
        View view = inflater.inflate(R.layout.settings_fragment, container, false);
        RelativeLayout calls = view.findViewById(R.id.calls_layout);
        if (!mPrefs.getBoolean(Constants.PREF_OTHER[25], false))
            calls.setVisibility(View.GONE);
        RelativeLayout widgets = view.findViewById(R.id.widgets_layout);
        if (CustomApplication.getWidgetIds(Constants.TRAFFIC).length == 0 &&
                CustomApplication.getWidgetIds(Constants.CALLS).length == 0)
            widgets.setVisibility(View.GONE);
        mSwitch.setSwitch(view.findViewById(R.id.notif_sw));
        view.findViewById(R.id.notif_touch_layout).setOnClickListener(this);
        view.findViewById(R.id.traff_layout).setOnClickListener(this);
        calls.setOnClickListener(this);
        view.findViewById(R.id.operator_layout).setOnClickListener(this);
        view.findViewById(R.id.other_layout).setOnClickListener(this);
        widgets.setOnClickListener(this);
        return view;
    }

    @Override
    public final void onResume() {
        super.onResume();
        mSwitch.resume();
        ((Toolbar) Objects.requireNonNull(getActivity()).findViewById(R.id.toolbar)).setTitle(R.string.action_settings);
    }

    @Override
    public final void onPause() {
        super.onPause();
        mSwitch.pause();
    }

    @Override
    public final void onClick(View view) {
        switch (view.getId()) {
            case R.id.notif_touch_layout:
                mSwitch.setChecked(true);
                ((SettingsActivity) Objects.requireNonNull(getActivity())).replaceFragments(SoundFragment.class);
                break;
            case R.id.traff_layout:
                ((SettingsActivity) Objects.requireNonNull(getActivity())).replaceFragments(TrafficLimitFragment.class);
                break;
            case R.id.calls_layout:
                ((SettingsActivity) Objects.requireNonNull(getActivity())).replaceFragments(CallsLimitFragment.class);
                break;
            case R.id.operator_layout:
                ((SettingsActivity) Objects.requireNonNull(getActivity())).replaceFragments(OperatorFragment.class);
                break;
            case R.id.widgets_layout:
                ((SettingsActivity) Objects.requireNonNull(getActivity())).replaceFragments(WidgetsFragment.class);
                break;
            case R.id.other_layout:
                ((SettingsActivity) Objects.requireNonNull(getActivity())).replaceFragments(OtherFragment.class);
                break;
        }
    }
}
