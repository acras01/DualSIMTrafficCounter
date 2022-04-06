package ua.od.acros.dualsimtrafficcounter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.activities.SettingsActivity;
import ua.od.acros.dualsimtrafficcounter.dialogs.CustomDialog;
import ua.od.acros.dualsimtrafficcounter.events.CustomDialogEvent;
import ua.od.acros.dualsimtrafficcounter.fragments.CallsFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.SetCallsDurationFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.SetTrafficUsageFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TestFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficForDateFragment;
import ua.od.acros.dualsimtrafficcounter.fragments.TrafficFragment;
import ua.od.acros.dualsimtrafficcounter.services.CallLoggerService;
import ua.od.acros.dualsimtrafficcounter.services.FloatingWindowService;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.services.WatchDogService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener, TrafficFragment.OnFragmentInteractionListener,
        TrafficForDateFragment.OnFragmentInteractionListener, TestFragment.OnFragmentInteractionListener,
        SetTrafficUsageFragment.OnFragmentInteractionListener, CallsFragment.OnFragmentInteractionListener,
        SetCallsDurationFragment.OnFragmentInteractionListener {

    private static final int REQUEST_CODE = 1981;
    private Context mContext;
    private SharedPreferences mPrefs;
    private static final String FIRST_RUN = "first_run";
    private static final String ANDROID_5_0 = "API21";
    private static final String EMAIL = "email";
    private static final String MTK = "mtk";
    private static final String XPOSED = "de.robv.android.xposed.installer";
    private boolean mNeedsReloadView = false;
    private boolean mNeedsRestart = false;
    private MenuItem mCallsItem;
    private NavigationView mNavigationView;
    private int mLastMenuItem;
    private String mAction;
    private Bundle mState;
    private Intent mStarterIntent;

    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStarterIntent = getIntent();
        mState = savedInstanceState;
        mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        if (savedInstanceState == null) {
            if (mPrefs.getBoolean(Constants.PREF_OTHER[29], true))
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_AUTO);
            else {
                if (Objects.requireNonNull(mPrefs.getString(Constants.PREF_OTHER[28], "1")).equals("0"))
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                else
                    getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
            // Now recreate for it to take effect
            recreate();
        }

        setContentView(R.layout.activity_main);

        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        if (drawer != null) {
            drawer.addDrawerListener(toggle);
        }
        toggle.syncState();

        mNavigationView = findViewById(R.id.nav_view);
        if (mNavigationView != null) {
            mNavigationView.setNavigationItemSelectedListener(this);
            //Prepare Navigation View Menu
            MenuItem mTestItem = mNavigationView.getMenu().findItem(R.id.nav_test);
            if (CustomApplication.isOldMtkDevice()) {
                mTestItem.setVisible(true);
                mTestItem.setEnabled(true);
            } else {
                mTestItem.setVisible(false);
                mTestItem.setEnabled(false);
            }

            mCallsItem = mNavigationView.getMenu().findItem(R.id.nav_calls_menu);
            if (mPrefs.getBoolean(Constants.PREF_OTHER[25], false)) {
                mCallsItem.setVisible(true);
                mCallsItem.setEnabled(true);
            } else {
                mCallsItem.setVisible(false);
                mCallsItem.setEnabled(false);
            }

            //set Version in Navigation View Header
            //View headerLayout = mNavigationView.findViewById(R.id.headerLayout);
            View headerLayout = mNavigationView.getHeaderView(0);
            TextView versionView = headerLayout.findViewById(R.id.versioninfo);
            String version;
            try {
                version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                version = getResources().getString(R.string.not_available);
            }
            versionView.setText(String.format(getResources().getString(R.string.app_version), version));
        }

        MobileUtils.getTelephonyManagerMethods(mContext);

        if (mPrefs.getBoolean(Constants.PREF_OTHER[32], false) &&
                ((mPrefs.getBoolean(Constants.PREF_OTHER[41], false) && MobileUtils.isMobileDataActive(mContext)) ||
                        (!mPrefs.getBoolean(Constants.PREF_OTHER[41], false) && !mPrefs.getBoolean(Constants.PREF_OTHER[47], false))))
            FloatingWindowService.showFloatingWindow(mContext, mPrefs);
        if (!CustomApplication.isMyServiceRunning(TrafficCountService.class))
            startService(new Intent(mContext, TrafficCountService.class));

        if (!CustomApplication.isMyServiceRunning(WatchDogService.class) && mPrefs.getBoolean(Constants.PREF_OTHER[4], true))
            startService(new Intent(mContext, WatchDogService.class));
        if (!CustomApplication.isPackageExisted(XPOSED)) {
            mPrefs.edit()
                    .putBoolean(Constants.PREF_OTHER[24], true)
                    .putBoolean(Constants.PREF_OTHER[25], false)
                    .apply();
        } else {
            //Update widgets
            int[] ids = CustomApplication.getWidgetIds(Constants.CALLS);
            if (ids.length != 0) {
                Intent i = new Intent(Constants.CALLS_BROADCAST_ACTION);
                i.putExtra(Constants.WIDGET_IDS, ids);
                sendBroadcast(i);
            }
        }

        /*if (!CustomApplication.isMyServiceRunning(mContext, CallLoggerService.class))
            startService(new Intent(mContext, CallLoggerService.class));*/

        mAction = getIntent().getAction();
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);
    }

    @Subscribe
    public final void onMessageEvent(CustomDialogEvent event) {
        FragmentManager fm = getSupportFragmentManager();
        if (CustomApplication.isOldMtkDevice()) {
            fm.beginTransaction()
                    .replace(R.id.content_frame, new TestFragment())
                    .commit();
            setItemChecked(R.id.nav_test, true);
        } else {
            if (!(getSupportFragmentManager().findFragmentById(R.id.content_frame) instanceof TrafficFragment)) {
                Fragment frg = fm.findFragmentByTag(Constants.TRAFFIC_TAG);
                if (frg == null)
                    frg = new TrafficFragment();
                fm.beginTransaction()
                        .replace(R.id.content_frame, frg, Constants.TRAFFIC_TAG)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                setItemChecked(R.id.nav_traffic, true);
            }
        }
    }

    @Override
    public final void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            FragmentManager fm = getSupportFragmentManager();
            Fragment fragment = fm.findFragmentById(R.id.content_frame);
            if (fragment instanceof TrafficFragment || (mAction != null && mAction.equals(Constants.CALLS_TAP)))
                finish();
            else {
                Fragment frg = fm.findFragmentByTag(Constants.TRAFFIC_TAG);
                if (frg == null)
                    frg = new TrafficFragment();
                fm.beginTransaction()
                        .replace(R.id.content_frame, frg, Constants.TRAFFIC_TAG)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                setItemChecked(R.id.nav_traffic, true);
            }
        }
    }

    private void showDialog(String key) {
        switch (key) {
            default:
                CustomDialog.newInstance(key).show(getSupportFragmentManager(), "dialog");
                break;
            case EMAIL:
                setItemChecked(mLastMenuItem, true);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.send_email)
                        .setMessage(R.string.why_email)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            Intent emailIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                            emailIntent.setType("text/plain");
                            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"acras1@gmail.com"});
                            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DualSim Traffic Counter");
                            ArrayList<Uri> uris = new ArrayList<>();
                            File dir = new File(String.valueOf(mContext.getFilesDir()));
                            String content = getString(R.string.body) + "\n";
                            //TelephonyMethods
                            String fileName = "telephony.txt";
                            File file = new File(dir, fileName);
                            try {
                                uris.add(Uri.fromFile(file));
                                InputStream is = openFileInput(fileName);
                                if (is != null) {
                                    InputStreamReader isr = new InputStreamReader(is);
                                    BufferedReader br = new BufferedReader(isr);
                                    String read;
                                    StringBuilder sb = new StringBuilder();
                                    while ((read = br.readLine()) != null ) {
                                        read += "\n";
                                        sb.append(read);
                                    }
                                    is.close();
                                    content += sb.toString();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //Active SIM
                            int sim = MobileUtils.getActiveSimForData(mContext);
                            fileName = "sim_log.txt";
                            content += "\n" + "Active SIM " + sim + "\n";
                            file = new File(dir, fileName);
                            try {
                                uris.add(Uri.fromFile(file));
                                InputStream is = openFileInput(fileName);
                                if (is != null) {
                                    InputStreamReader isr = new InputStreamReader(is);
                                    BufferedReader br = new BufferedReader(isr);
                                    String read;
                                    StringBuilder sb = new StringBuilder();
                                    while ((read = br.readLine()) != null ) {
                                        read += "\n";
                                        sb.append(read);
                                    }
                                    is.close();
                                    content += sb.toString();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //Operator Names
                            ArrayList<String> names = MobileUtils.getOperatorNames(mContext);
                            fileName = "name_log.txt";
                            content += "\n" + "Operator names " + names.toString() + "\n";
                            file = new File(dir, fileName);
                            try {
                                uris.add(Uri.fromFile(file));
                                InputStream is = openFileInput(fileName);
                                if (is != null) {
                                    InputStreamReader isr = new InputStreamReader(is);
                                    BufferedReader br = new BufferedReader(isr);
                                    String read;
                                    StringBuilder sb = new StringBuilder();
                                    while ((read = br.readLine()) != null ) {
                                        read += "\n";
                                        sb.append(read);
                                    }
                                    is.close();
                                    content += sb.toString();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //OperatorCodes
                            fileName = "code_log.txt";
                            file = new File(dir, fileName);
                            try {
                                uris.add(Uri.fromFile(file));
                                InputStream is = openFileInput(fileName);
                                if (is != null) {
                                    InputStreamReader isr = new InputStreamReader(is);
                                    BufferedReader br = new BufferedReader(isr);
                                    String read;
                                    StringBuilder sb = new StringBuilder();
                                    while ((read = br.readLine()) != null ) {
                                        read += "\n";
                                        sb.append(read);
                                    }
                                    is.close();
                                    content += sb.toString();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            emailIntent.putExtra(Intent.EXTRA_TEXT, content);
                            emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                            startActivity(Intent.createChooser(emailIntent, getString(R.string.choose_client)));
                            dialog.dismiss();
                        })
                        .show();
                break;
        }
    }

    @Override
    protected final void onPostResume() {
        super.onPostResume();
        FragmentManager fm = getSupportFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.content_frame);
        if (mNeedsRestart) {
            mNeedsRestart = false;
            finish();
            startActivity(mStarterIntent);
        } else if (mNeedsReloadView) {
            if (currentFragment instanceof TrafficFragment) {
                fm.beginTransaction()
                        .detach(currentFragment)
                        .attach(currentFragment)
                        .commit();
                setItemChecked(R.id.nav_traffic, true);
            }
        } else if (mPrefs.getBoolean(Constants.PREF_OTHER[9], true)) {
            mPrefs.edit()
                    .putBoolean(Constants.PREF_OTHER[9], false)
                    .apply();
            showDialog(FIRST_RUN);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                    !CustomApplication.hasRoot())
                showDialog(ANDROID_5_0);
            if (!CustomApplication.canToggleOn())
                showDialog(MTK);
        } else if (mAction != null && mState == null) {
            if (mAction.contains("dualsimtrafficcounter"))
                switch (mAction) {
                    case Constants.TRAFFIC_TAP:
                        fm.beginTransaction()
                                .replace(R.id.content_frame, new TrafficFragment(), Constants.TRAFFIC_TAG)
                                .addToBackStack(Constants.TRAFFIC_TAG)
                                .commit();
                        setItemChecked(R.id.nav_traffic, true);
                        break;
                    case Constants.CALLS_TAP:
                        fm.beginTransaction()
                                .replace(R.id.content_frame, new CallsFragment(), Constants.CALLS_TAG)
                                .addToBackStack(Constants.CALLS_TAG)
                                .commit();
                        setItemChecked(R.id.nav_calls, true);
                        break;
                }
            else {
                fm.beginTransaction()
                        .replace(R.id.content_frame, new TrafficFragment(), Constants.TRAFFIC_TAG)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                setItemChecked(R.id.nav_traffic, true);
            }
        } else if (CustomApplication.isPackageExisted(XPOSED) && (mLastMenuItem == R.id.nav_calls || mLastMenuItem == R.id.nav_set_duration) &&
                !mPrefs.getBoolean(Constants.PREF_OTHER[25], true)) {
            if (currentFragment instanceof CallsFragment) {
                fm.beginTransaction()
                        .remove(currentFragment)
                        .replace(R.id.content_frame, new TrafficFragment(), Constants.TRAFFIC_TAG)
                        .addToBackStack(Constants.TRAFFIC_TAG)
                        .commit();
                setItemChecked(R.id.nav_traffic, true);
            }
        } else if (mState == null) {
            fm.beginTransaction()
                    .replace(R.id.content_frame, new TrafficFragment(), Constants.TRAFFIC_TAG)
                    .addToBackStack(Constants.TRAFFIC_TAG)
                    .commit();
            setItemChecked(R.id.nav_traffic, true);
        }
    }

    @Override
    public final void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(Constants.PREF_OTHER[4])) {
            if (!sharedPreferences.getBoolean(key, false)) {
                stopService(new Intent(mContext, WatchDogService.class));
                mPrefs.edit().putBoolean(Constants.PREF_OTHER[6], true).apply();
            } else {
                startService(new Intent(mContext, WatchDogService.class));
                mPrefs.edit().putBoolean(Constants.PREF_OTHER[6], false).apply();
            }
        }
        if (key.equals(Constants.PREF_OTHER[7]))
            mNeedsReloadView = true;
        if (key.equals(Constants.PREF_OTHER[28]) || key.equals(Constants.PREF_OTHER[29]))
            mNeedsRestart = true;
        if (key.equals(Constants.PREF_OTHER[25])) {
            if (sharedPreferences.getBoolean(key, true)) {
                mCallsItem.setVisible(true);
                mCallsItem.setEnabled(true);
            } else {
                mCallsItem.setVisible(false);
                mCallsItem.setEnabled(false);
            }
        }
    }

    @Override
    public final boolean onNavigationItemSelected(@NonNull MenuItem item) {
        openFragment(item.getItemId());
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void openFragment(int itemId) {
        mAction = "";
        Fragment newFragment = null;
        FragmentManager fm = getSupportFragmentManager();
        String tag = "";
        switch (itemId) {
            case R.id.nav_traffic:
                mLastMenuItem = itemId;
                tag = Constants.TRAFFIC_TAG;
                break;
            case R.id.nav_calls:
                mLastMenuItem = itemId;
                tag = Constants.CALLS_TAG;
                break;
            case R.id.nav_traf_for_date:
                mLastMenuItem = itemId;
                newFragment = new TrafficForDateFragment();
                break;
            case R.id.nav_test:
                mLastMenuItem = itemId;
                newFragment = new TestFragment();
                break;
            case R.id.nav_set_usage:
                mLastMenuItem = itemId;
                newFragment = new SetTrafficUsageFragment();
                break;
            case R.id.nav_set_duration:
                mLastMenuItem = itemId;
                newFragment = new SetCallsDurationFragment();
                break;
            case R.id.nav_settings:
                setItemChecked(itemId, false);
                Intent i1 = new Intent(mContext, SettingsActivity.class);
                startActivityForResult(i1, REQUEST_CODE);
                break;
            case R.id.nav_email:
                setItemChecked(itemId, false);
                showDialog(EMAIL);
                break;
            case R.id.nav_4pda:
                setItemChecked(itemId, false);
                String url = "http://4pda.ru/forum/index.php?showtopic=699793";
                Intent i2 = new Intent(Intent.ACTION_VIEW);
                i2.setData(Uri.parse(url));
                startActivity(i2);
                break;
            case R.id.nav_exit:
                if (CustomApplication.isMyServiceRunning(WatchDogService.class))
                    stopService(new Intent(mContext, WatchDogService.class));
                if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
                    stopService(new Intent(mContext, TrafficCountService.class));
                mPrefs.edit()
                        .putBoolean(Constants.PREF_OTHER[5], true)
                        .apply();
                if (CustomApplication.isMyServiceRunning(CallLoggerService.class))
                    stopService(new Intent(mContext, CallLoggerService.class));
                FloatingWindowService.closeFloatingWindow(mContext, mPrefs);
                finish();
                break;
        }
        if (newFragment != null) {
            fm.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.content_frame, newFragment)
                    .commit();
            setItemChecked(itemId, true);
        } else if (!tag.equals("")) {
            newFragment = fm.findFragmentByTag(tag);
            if (newFragment == null) {
                if (tag.equals(Constants.TRAFFIC_TAG))
                    newFragment = new TrafficFragment();
                else
                    newFragment = new CallsFragment();
            }
            fm.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .replace(R.id.content_frame, newFragment, tag)
                    .addToBackStack(tag)
                    .commit();
        }

    }

    @Override
    protected final void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
            setItemChecked(mLastMenuItem, true);
    }

    private void setItemChecked(int id, boolean checked) {
        mNavigationView.setCheckedItem(id);
        mNavigationView.getMenu().findItem(id).setChecked(checked);
        if (id != R.id.nav_settings && id != R.id.nav_email && id != R.id.nav_4pda)
            mLastMenuItem = id;
    }

    @Override
    protected final void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().unregister(this);
        /*if (!CustomApplication.isMyServiceRunning(TrafficCountService.class) &&
                !CustomApplication.isMyServiceRunning(CallLoggerService.class))
            CustomDatabaseHelper.deleteInstance();*/
    }

    @Override
    public void onTrafficFragmentInteraction(Uri uri) {
        // Do stuff
    }

    @Override
    public void onTrafficForDateFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onTestFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onSetUsageFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onCallsFragmentInteraction(Uri uri) {
        // Do different stuff
    }

    @Override
    public void onSetDurationFragmentInteraction(Uri uri) {
        // Do different stuff
    }
}
