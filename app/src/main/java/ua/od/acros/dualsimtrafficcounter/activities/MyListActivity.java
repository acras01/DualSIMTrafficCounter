package ua.od.acros.dualsimtrafficcounter.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
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
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.ListItem;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import ua.od.acros.dualsimtrafficcounter.utils.MyListAdapter;

public class MyListActivity extends AppCompatActivity {


    private static int mKey;
    private static boolean mChoice;
    private static CustomDatabaseHelper mDbHelper;
    private static WeakReference<ProgressBar> pb;
    private static MyListAdapter mAdapter;
    private static SharedPreferences mPrefs;

    @SuppressLint("RestrictedApi")
    @Override
    protected final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context mContext = CustomApplication.getAppContext();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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
        mDbHelper = CustomDatabaseHelper.getInstance(mContext);
        mKey = Integer.valueOf(Objects.requireNonNull(getIntent().getDataString()));
        mChoice = mKey < 3;
        if (!mChoice)
            mKey = mKey - 3;
        String[] mOperatorNames = new String[]{MobileUtils.getName(mContext, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                MobileUtils.getName(mContext, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                MobileUtils.getName(mContext, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
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
            if (mChoice)
                bar.setTitle(getString(R.string.white_list));
            else
                bar.setTitle(getString(R.string.uid_list));
        }
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        if (recyclerView != null) {
            recyclerView.setHasFixedSize(true);
            List<ListItem> myList = new ArrayList<>();
            mAdapter = new MyListAdapter(myList);
            recyclerView.setAdapter(mAdapter);
            recyclerView.setHasFixedSize(true);
            recyclerView.setItemViewCacheSize(20);
            recyclerView.setDrawingCacheEnabled(true);
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView.setLayoutManager(layoutManager);
            new LoadTask(recyclerView).execute();
        }
    }

    private static List<ListItem> loadContactsFromDB(Context context, ArrayList<String> list) {
        List<ListItem> whiteList = new ArrayList<>();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        Cursor cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME}, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String number = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[\\s\\-()]", "");
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                Uri contactID = Uri.parse("contact_photo://" + cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)));
                whiteList.add(new ListItem(contactID, name, number, list.contains(number)));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return whiteList;
    }

    private static List<ListItem> loadAppUids(ArrayList<String> list) {
        List<ListItem> uidList = new ArrayList<>();
        Context ctx = CustomApplication.getAppContext();
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo pkg : packages) {
            String label = (String)((pkg != null) ? pm.getApplicationLabel(pkg) : ctx.getString(R.string.unknown));
            Uri icon = Uri.parse("app_icon://" + ((pkg != null) ? pkg.packageName : ctx.getString(R.string.unknown)));
            String uid = String.valueOf((pkg != null) ? pkg.uid : ctx.getString(R.string.unknown));
            uidList.add(new ListItem(icon, label, uid, list.contains(uid)));
        }
        return uidList;
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
            ArrayList<String> imsi = null;
            if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false))
                imsi = MobileUtils.getSimIds(CustomApplication.getAppContext());
            if (mChoice)
                CustomDatabaseHelper.writeList(mKey, mAdapter.getCheckedItems(), mDbHelper, imsi, "white");
            else
                CustomDatabaseHelper.writeList(mKey, mAdapter.getCheckedItems(), mDbHelper, imsi, "uid");
            return true;
        }

        @Override
        protected final void onPostExecute(Boolean result) {
            if (result)
                Toast.makeText(CustomApplication.getAppContext(), R.string.saved, Toast.LENGTH_LONG).show();
        }
    }

    private static class LoadTask extends AsyncTask<Void, Void, List<ListItem>> {

        final WeakReference<RecyclerView> rv;

        LoadTask(RecyclerView rv) {
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
            if (mPrefs.getBoolean(Constants.PREF_OTHER[45], false))
                imsi = MobileUtils.getSimIds(ctx);
            ArrayList<String> myList;
            List<ListItem> listItems;
            if (mChoice) {
                myList = CustomDatabaseHelper.readList(mKey, mDbHelper, imsi, "white");
                listItems = loadContactsFromDB(ctx, myList);
                List<String> numbers = new ArrayList<>();
                for (ListItem item : listItems)
                    numbers.add(item.getNumber());
                for (Iterator<String> i = myList.iterator(); i.hasNext(); ) {
                    if (numbers.contains(i.next())) {
                        i.remove();
                    }
                }
                for (Iterator<String> i = myList.iterator(); i.hasNext(); ) {
                    listItems.add(new ListItem(Uri.parse("contact_photo://" + ctx.getString(R.string.unknown)), ctx.getString(R.string.unknown), i.next(), true));
                    i.remove();
                }
            } else {
                myList = CustomDatabaseHelper.readList(mKey, mDbHelper, imsi, "uid");
                listItems = loadAppUids(myList);
            }
            return listItems;
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
