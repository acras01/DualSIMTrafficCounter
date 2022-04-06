package ua.od.acros.dualsimtrafficcounter.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.acra.ACRA;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.utils.BlackListAdapter;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.ListItem;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class BlackListActivity extends AppCompatActivity {

    private static int mKey;
    private static CustomDatabaseHelper mDbHelper;
    private static BlackListAdapter mAdapter;
    private static ArrayList<String> mList;
    private static WeakReference<ProgressBar> pb;
    private static SharedPreferences mPrefs;

    @SuppressLint("RestrictedApi")
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
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
        mDbHelper = CustomDatabaseHelper.getInstance(ctx);
        mKey = Integer.valueOf(Objects.requireNonNull(getIntent().getDataString()));
        String[] mOperatorNames = new String[]{MobileUtils.getName(ctx, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(ctx, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(ctx, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
        setContentView(R.layout.activity_recyclerview);
        ProgressBar pBar = findViewById(R.id.progressBar);
        pb = new WeakReference<>(pBar);
        if (pBar != null) {
            pBar.setVisibility(View.GONE);
        }
        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);
        ActionBar bar = getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDefaultDisplayHomeAsUpEnabled(true);
            bar.setSubtitle(mOperatorNames[mKey]);
            bar.setTitle(getString(R.string.black_list));
        }
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            List<ListItem> blackList = new ArrayList<>();
            mAdapter = new BlackListAdapter(blackList);
            recyclerView.setAdapter(mAdapter);
            new LoadContactsTask(recyclerView).execute();
        }
    }

    public final boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public final boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case R.id.save:
                    new SaveTask().execute();
                default:
                    finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return super.onOptionsItemSelected(item);
    }

    private static class SaveTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected final Boolean doInBackground(Void... params) {
            Context ctx = CustomApplication.getAppContext();
            ArrayList<String> list = mAdapter.getCheckedItems();
            for (Iterator<String> i = mList.iterator(); i.hasNext(); ) {
                if (list.contains(i.next())) {
                    i.remove();
                }
            }
            ArrayList<String> imsi = null;
            if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false))
                imsi = MobileUtils.getSimIds(ctx);
            CustomDatabaseHelper.writeList(mKey, mList, mDbHelper, imsi, "black");
            return true;
        }

        @Override
        protected final void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(CustomApplication.getAppContext(), R.string.saved, Toast.LENGTH_LONG).show();
        }
    }

    private static class LoadContactsTask extends AsyncTask<Void, Void, List<ListItem>> {

        final WeakReference<RecyclerView> rv;

        LoadContactsTask(RecyclerView rv) {
            this.rv = new WeakReference<>(rv);
        }

        @Override
        protected final void onPreExecute() {
            super.onPreExecute();
            pb.get().setVisibility(View.VISIBLE);
        }

        @Override
        protected final List<ListItem> doInBackground(Void... params) {
            ArrayList<String> imsi = null;
            Context ctx = CustomApplication.getAppContext();
            if (mPrefs.getBoolean(Constants.PREF_OTHER[45], true))
                imsi = MobileUtils.getSimIds(ctx);
            mList = CustomDatabaseHelper.readList(mKey, mDbHelper, imsi, "black");
            List<ListItem> blackList = new ArrayList<>();
            for (String number : mList)
                blackList.add(new ListItem(number, false));
            return blackList;
        }

        @Override
        protected final void onPostExecute(List<ListItem> result) {
            pb.get().setVisibility(View.GONE);
            if (result != null) {
                mAdapter.swapItems(result);
            }
        }
    }
}