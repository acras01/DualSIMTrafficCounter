package ua.od.acros.dualsimtrafficcounter.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.settings.CallsLimitFragment;
import ua.od.acros.dualsimtrafficcounter.settings.OperatorFragment;
import ua.od.acros.dualsimtrafficcounter.settings.OtherFragment;
import ua.od.acros.dualsimtrafficcounter.settings.SettingsFragment;
import ua.od.acros.dualsimtrafficcounter.settings.TrafficLimitFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomSwitch;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartScreenCallback {

    private static ActionBar mActionBar;
    private static String mTag, mAction;
    private static PreferenceFragmentCompat mFragment;
    private static WeakReference<Context> mContext;

    @SuppressLint("RestrictedApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAction = getIntent().getAction();
        mContext = new WeakReference<>(CustomApplication.getAppContext());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext.get());
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
        setContentView(R.layout.activity_settings);
        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDefaultDisplayHomeAsUpEnabled(true);
        }
        if (getIntent().getStringExtra("show") != null) {
            PreferenceFragmentCompat fragment = null;
            String sim = getIntent().getStringExtra("sim");
            switch (getIntent().getStringExtra("show")) {
                case Constants.TRAFFIC_TAG:
                    fragment = new TrafficLimitFragment();
                    break;
                case Constants.CALLS_TAG:
                    fragment = new CallsLimitFragment();
                    break;
            }
            if (fragment != null) {
                Bundle args = new Bundle();
                args.putString("sim", sim);
                fragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                        .commit();
            }
        } else
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home)
            onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_frame);
        if (mTag != null && (mTag.contains("sim") || mTag.equals("logo")
                || mTag.equals("float") || mTag.contains("visual")
                || mTag.contains("service"))) {
            mTag = "";
            if (mFragment instanceof TrafficLimitFragment)
                replaceFragments(TrafficLimitFragment.class);
            else if (mFragment instanceof CallsLimitFragment)
                replaceFragments(CallsLimitFragment.class);
            else if (mFragment instanceof OperatorFragment)
                replaceFragments(OperatorFragment.class);
            else if (mFragment instanceof OtherFragment)
                replaceFragments(OtherFragment.class);
        } else if (fragment instanceof SettingsFragment) {
            setResult(RESULT_OK, null);
            finish();
            if (mAction != null && mAction.equals(Constants.SETTINGS_TAP)) {
                Intent activityIntent = new Intent(mContext.get(), MainActivity.class);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(activityIntent);
            }
        }
        else
            replaceFragments(SettingsFragment.class);
    }

    public void replaceFragments(Class fragmentClass) {
        if (mActionBar != null) {
            mActionBar.setDisplayShowCustomEnabled(false);
            mActionBar.setSubtitle("");
        }
        Fragment fragment = null;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Insert the fragment by replacing any existing fragment
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        openPreferenceScreen(preferenceFragmentCompat, preferenceScreen);
        return true;
    }

    public static void openPreferenceScreen(PreferenceFragmentCompat preferenceFragmentCompat, PreferenceScreen preferenceScreen) {
        mTag = preferenceScreen.getKey();
        mFragment = preferenceFragmentCompat;
        preferenceFragmentCompat.setPreferenceScreen(preferenceScreen);
        mActionBar.setSubtitle(preferenceScreen.getTitle());
        if (mFragment instanceof OtherFragment && mTag.equals("float")) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
            mActionBar.setDisplayShowHomeEnabled(true);
            mActionBar.setDisplayShowCustomEnabled(true);
            mActionBar.setCustomView(R.layout.actionbar_switch);
            View custom = mActionBar.getCustomView();
            TextView tv = custom.findViewById(R.id.titleText);
            tv.setText(R.string.other_title);
            tv = custom.findViewById(R.id.subTitleText);
            tv.setText(R.string.floating_window);
            SwitchCompat actionBarSwitch = custom.findViewById(R.id.switchForActionBar);
            if (actionBarSwitch != null)
                OtherFragment.setSwitch(new CustomSwitch(mContext.get(), actionBarSwitch, Constants.PREF_OTHER[32]));
        } else if (mFragment instanceof TrafficLimitFragment)
            ((TrafficLimitFragment) mFragment).updateSummary();
    }
}