package ua.od.acros.dualsimtrafficcounter.activities;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.acra.ACRA;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.dialogs.SetSizeDialog;
import ua.od.acros.dualsimtrafficcounter.dialogs.ShowSimDialog;
import ua.od.acros.dualsimtrafficcounter.fragments.IconsListFragment;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;
import yuku.ambilwarna.AmbilWarnaDialog;

import static ua.od.acros.dualsimtrafficcounter.utils.CustomApplication.getRealPathFromURI;
import static ua.od.acros.dualsimtrafficcounter.utils.CustomApplication.getStringArray;
import static ua.od.acros.dualsimtrafficcounter.utils.CustomApplication.onOff;
import static ua.od.acros.dualsimtrafficcounter.utils.CustomApplication.setOnClickListenerWithChild;

public class TrafficWidgetConfigActivity extends AppCompatActivity implements IconsListFragment.OnCompleteListener,
        CompoundButton.OnCheckedChangeListener, View.OnClickListener,
        SetSizeDialog.TextSizeDialogListener, ShowSimDialog.ShowSimDialogClosedListener {

    private static final int SELECT_PHOTO = 101;
    private int mWidgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Intent mResultValueIntent;
    private ImageView tiv, biv, logo1, logo2, logo3, tiv1, tiv2, tiv3;
    private TextView infoSum, namesSum, iconsSum, logoSum1, logoSum2,
            logoSum3, textSizeSum, iconsSizeSum, speedSum, backSum,
            speedTextSum, speedIconsSum, showSimSum, divSum, activesum, daynightSum, remainSum, rxtxSum, minusSum;
    private RelativeLayout simLogoL, speedFontL, speedArrowsL, showSimL, backColorL, logoL1, logoL2, logoL3,
            remainL, rxtxL, minusL;
    private AppCompatCheckBox minus;
    private SharedPreferences.Editor mEdit;
    private int mTextColor, mTextColor1, mTextColor2, mTextColor3, mBackColor;
    private final int KEY_TEXT = 0;
    private final int KEY_ICON = 1;
    private final int KEY_TEXT_S = 2;
    private final int KEY_ICON_S = 3;
    private int mDim, remainSel, infoSel, rxtxSel;
    private String mUserPickedImage;
    private boolean[] mSim;
    private Context mContext;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = CustomApplication.getAppContext();

        /*if (!CustomApplication.isMyServiceRunning(mContext, TrafficCountService.class))
            startService(new Intent(this, TrafficCountService.class));*/

        mDim = (int) getResources().getDimension(R.dimen.logo_size);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mWidgetID = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mWidgetID == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        SharedPreferences prefsWidget = getSharedPreferences(String.valueOf(mWidgetID) + Constants.TRAFFIC_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        if (icicle == null) {
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
        int simQuantity = prefs.getInt(Constants.PREF_OTHER[55], 1);

        mEdit = prefsWidget.edit();
        if (prefsWidget.getAll().size() == 0) {
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true);//Show mNames
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[2], "0");//Show full/short info
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[3], false);//Show speed
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true);//Show sim icons
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], "none");//SIM1 icon
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], "none");//SIM2 icon
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], "none");//SIM3 icon
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false);//SIM1 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false);//SIM2 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false);//SIM3 user icon
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE);//Icon size
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE);//Font size
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);//Text color
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true);//Use background
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);//Background color
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE);//Speed text size
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE);//Speed arrows size
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true);//show sim1
            if (simQuantity >= 2)
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true);//show sim2
            else
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], false);
            if (simQuantity == 3)
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true);//Show sim3
            else
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], false);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true);//Show divider
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false);//Show only active SIM
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false);//Show day/night icons
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[24], "1");//Show remaining
            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[25], "0");//Show RX/TX
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true);//Show over-limit traffic
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(mContext, android.R.color.holo_green_dark));//TX Text color
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(mContext, android.R.color.holo_orange_dark));//RX Text color
            mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[29], Color.WHITE);//Total Text color
            mEdit.apply();
        }

        mSim = new boolean[]{prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true),
                prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true), prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true)};

        mTextColor = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);
        mTextColor1 = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(mContext, android.R.color.holo_green_dark));
        mTextColor2 = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(mContext, android.R.color.holo_orange_dark));
        mTextColor3 = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[29], Color.WHITE);
        mBackColor = prefsWidget.getInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);

        mResultValueIntent = new Intent();
        mResultValueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetID);

        setResult(RESULT_CANCELED, mResultValueIntent);

        setContentView(R.layout.traffic_info_widget_configure);
        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        AppCompatCheckBox names = findViewById(R.id.names);
        namesSum = findViewById(R.id.names_summary);
        if (names != null) {
            names.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true));
            names.setOnCheckedChangeListener(this);
            if (names.isChecked())
                namesSum.setText(R.string.on);
            else
                namesSum.setText(R.string.off);
        }

        AppCompatCheckBox icons = findViewById(R.id.icons);
        iconsSum = findViewById(R.id.icons_summary);
        logoL1 = findViewById(R.id.logoLayout1);
        logoL2 = findViewById(R.id.logoLayout2);
        logoL3 = findViewById(R.id.logoLayout3);
        simLogoL = findViewById(R.id.simLogoSize);
        if (icons != null) {
            icons.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true));
            icons.setOnCheckedChangeListener(this);
            onOff(logoL1, icons.isChecked());
            onOff(logoL2, simQuantity >= 2 && icons.isChecked());
            onOff(logoL3, simQuantity == 3 && icons.isChecked());
            onOff(simLogoL, icons.isChecked());
            if (icons.isChecked())
                iconsSum.setText(R.string.on);
            else
                iconsSum.setText(R.string.off);
        }

        AppCompatCheckBox speed = findViewById(R.id.speed);
        speedSum = findViewById(R.id.speed_summary);
        speedFontL = findViewById(R.id.speedFontSize);
        speedArrowsL = findViewById(R.id.speedArrowsSize);
        if (speed != null) {
            speed.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], true));
            speed.setOnCheckedChangeListener(this);
            onOff(speedFontL, speed.isChecked());
            onOff(speedArrowsL, speed.isChecked());
            if (speed.isChecked())
                speedSum.setText(R.string.on);
            else
                speedSum.setText(R.string.off);
        }

        AppCompatCheckBox back = findViewById(R.id.useBack);
        backSum = findViewById(R.id.back_summary);
        backColorL = findViewById(R.id.backColorLayout);
        if (back != null) {
            back.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true));
            back.setOnCheckedChangeListener(this);
            onOff(backColorL, back.isChecked());
            if (back.isChecked())
                backSum.setText(R.string.on);
            else
                backSum.setText(R.string.off);
        }

        AppCompatCheckBox div = findViewById(R.id.divider);
        divSum = findViewById(R.id.divider_summary);
        if (div != null) {
            div.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true));
            div.setOnCheckedChangeListener(this);
            if (div.isChecked())
                divSum.setText(R.string.on);
            else
                divSum.setText(R.string.off);
        }

        AppCompatCheckBox active = findViewById(R.id.activesim);
        activesum = findViewById(R.id.activesim_summary);
        showSimL = findViewById(R.id.showSim);
        if (active != null) {
            active.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false));
            active.setOnCheckedChangeListener(this);
            onOff(showSimL, !active.isChecked());
            if (active.isChecked())
                activesum.setText(R.string.on);
            else
                activesum.setText(R.string.off);
        }

        AppCompatCheckBox daynight = findViewById(R.id.daynight_icons);
        daynightSum = findViewById(R.id.daynight_icons_summary);
        if (daynight != null) {
            daynight.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false));
            daynight.setOnCheckedChangeListener(this);
            if (daynight.isChecked())
                daynightSum.setText(R.string.on);
            else
                daynightSum.setText(R.string.off);
        }

        rxtxL = findViewById(R.id.rxtx_layout);
        rxtxSel = Integer.valueOf(Objects.requireNonNull(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[25], "0")));
        boolean rxtxState = rxtxSel == 0;
        rxtxSum = findViewById(R.id.rx_tx_summary);
        if (rxtxState)
            rxtxSum.setText(R.string.show_rx_tx_sum);
        else
            rxtxSum.setText(R.string.show_used_left);

        remainL = findViewById(R.id.remain_layout);
        remainSum = findViewById(R.id.remain_data_summary);
        remainSel = Integer.valueOf(Objects.requireNonNull(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[24], "1")));
        if (remainSel == 0)
            remainSum.setText(R.string.remain);
        else
            remainSum.setText(R.string.used);

        minus = findViewById(R.id.minus);
        minusL = findViewById(R.id.minus_layout);
        minusSum = findViewById(R.id.minus_summary);
        if (minus != null) {
            minus.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true));
            minus.setOnCheckedChangeListener(this);
            if (minus.isChecked())
                minusSum.setText(R.string.on);
            else
                minusSum.setText(R.string.off);
        }

        RelativeLayout infoL = findViewById(R.id.info_layout);
        infoSum = findViewById(R.id.info_summary);
        infoSel = Integer.valueOf(Objects.requireNonNull(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")));
        boolean infoState = infoSel == 0;
        if (infoL != null) {
            setOnClickListenerWithChild(infoL, this);
            onOff(rxtxL, infoState);
            if (infoState)
                infoSum.setText(R.string.all);
            else
                infoSum.setText(R.string.only_total);
        }

        onOff(remainL, !rxtxState && infoState);
        onOff(minusL, !rxtxState && infoState);

        RelativeLayout simFontL = findViewById(R.id.simFontSize);

        textSizeSum = findViewById(R.id.textSizeSum);
        textSizeSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE));

        iconsSizeSum = findViewById(R.id.iconSizeSum);
        iconsSizeSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE));

        speedTextSum = findViewById(R.id.speedTextSizeSum);
        speedTextSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE));

        speedIconsSum = findViewById(R.id.speedIconsSizeSum);
        speedIconsSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE));

        showSimSum = findViewById(R.id.simChooseSum);
        String sum = "";
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true))
            sum = "SIM1";
        if (simQuantity >= 2 && prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true))
            if (sum.equals(""))
                sum = "SIM2";
            else
                sum += ", SIM2";
        if (simQuantity == 3 && prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true))
            if (sum.equals(""))
                sum = "SIM3";
            else
                sum += ", SIM3";
        showSimSum.setText(sum);

        tiv = findViewById(R.id.textColorPreview);
        tiv1 = findViewById(R.id.textColorPreview1);
        tiv2 = findViewById(R.id.textColorPreview2);
        tiv3 = findViewById(R.id.textColorPreview3);
        biv = findViewById(R.id.backColorPreview);
        tiv.setBackgroundColor(mTextColor);
        tiv1.setBackgroundColor(mTextColor1);
        tiv2.setBackgroundColor(mTextColor2);
        tiv3.setBackgroundColor(mTextColor3);
        biv.setBackgroundColor(mBackColor);

        logo1 = findViewById(R.id.logoPreview1);
        logo2 = findViewById(R.id.logoPreview2);
        logo3 = findViewById(R.id.logoPreview3);
        logoSum1 = findViewById(R.id.logoSum1);
        logoSum2 = findViewById(R.id.logoSum2);
        logoSum3 = findViewById(R.id.logoSum3);

        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false)) {
            loadImageFromFile(new File(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[5], "")), mDim, logo1);
            logoSum1.setText(getResources().getString(R.string.userpick));
        } else
            loadImageFromResource(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[5],
                    "none"), "drawable", mContext.getPackageName()), mDim, logo1);
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false)) {
            loadImageFromFile(new File(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[6], "")), mDim, logo2);
            logoSum2.setText(getResources().getString(R.string.userpick));
        } else
            loadImageFromResource(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[6],
                    "none"), "drawable", mContext.getPackageName()), mDim, logo2);
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false)) {
            loadImageFromFile(new File(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[7], "")), mDim, logo3);
            logoSum3.setText(getResources().getString(R.string.userpick));
        } else
            loadImageFromResource(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[7],
                    "none"), "drawable", mContext.getPackageName()), mDim, logo3);

        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        for (int i = 0; i < list.length; i++) {
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[5], "none")))
                logoSum1.setText(list[i]);
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[6], "none")))
                logoSum2.setText(list[i]);
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[7], "none")))
                logoSum3.setText(list[i]);
        }

        tiv.setOnClickListener(this);
        tiv1.setOnClickListener(this);
        tiv2.setOnClickListener(this);
        tiv3.setOnClickListener(this);
        biv.setOnClickListener(this);
        logo1.setOnClickListener(this);
        logo2.setOnClickListener(this);
        logo3.setOnClickListener(this);
        setOnClickListenerWithChild(simFontL, this);
        setOnClickListenerWithChild(simLogoL, this);
        setOnClickListenerWithChild(speedFontL, this);
        setOnClickListenerWithChild(speedArrowsL, this);
        setOnClickListenerWithChild(showSimL, this);
        setOnClickListenerWithChild(rxtxL, this);
        setOnClickListenerWithChild(remainL, this);
        backColorL.setOnClickListener(this);
    }

    private void loadImageFromFile(File file, int dim, ImageView dest) {
        Picasso.get()
                .load(file)
                .resize(dim, dim)
                .centerInside()
                .error(R.drawable.none)
                .into(dest);
    }

    private void loadImageFromResource(int id, int dim, ImageView dest) {
        Picasso.get()
                .load(id)
                .resize(dim, dim)
                .centerInside()
                .error(R.drawable.none)
                .into(dest);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.widget_config_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            if (item.getItemId() == R.id.save) {
                mEdit.apply();
                Intent intent = new Intent(Constants.TRAFFIC_BROADCAST_ACTION);
                intent.putExtra(Constants.WIDGET_IDS, new int[]{mWidgetID});
                sendBroadcast(intent);
                setResult(RESULT_OK, mResultValueIntent);
                finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ACRA.getErrorReporter().handleException(e);
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialog(View view) {
        DialogFragment dialog = null;
        AlertDialog.Builder ldb = new AlertDialog.Builder(this);
        ArrayAdapter<String> adapter;
        int selection = -1;
        String[] array = null;
        DialogInterface.OnClickListener myClickListener = (dialog1, which) -> {
            ListView lv = ((AlertDialog) dialog1).getListView();
            String[] array1 = getStringArray(lv.getAdapter());
            boolean isChecked = lv.getCheckedItemPosition() == 0;
            if (Arrays.equals(array1, getResources().getStringArray(R.array.remain))) {
                remainSel = lv.getCheckedItemPosition();
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[24], isChecked ? "0" : "1");
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[25], isChecked ? "1" : "0");
                if (isChecked)
                    remainSum.setText(R.string.remain);
                else
                    remainSum.setText(R.string.used);
            } else if (Arrays.equals(array1, getResources().getStringArray(R.array.fullinfo))) {
                infoSel = lv.getCheckedItemPosition();
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[2], isChecked ? "0" : "1");
                if (isChecked)
                    infoSum.setText(R.string.all);
                else
                    infoSum.setText(R.string.only_total);
                SharedPreferences prefsWidget = getSharedPreferences(String.valueOf(mWidgetID) + Constants.TRAFFIC_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
                boolean rxtxChecked = Objects.requireNonNull(prefsWidget.getString(Constants.PREF_WIDGET_TRAFFIC[25], "0")).equals("0");
                onOff(remainL, !isChecked);
                onOff(rxtxL, isChecked);
                onOff(minusL, isChecked && !rxtxChecked);
                if (!isChecked) {
                    mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], isChecked);
                    minus.setChecked(isChecked);
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[25], isChecked ? "0" : "1");
                } else {
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[24], isChecked ? "1" : "0");
                }
            } else if (Arrays.equals(array1, getResources().getStringArray(R.array.rxtx))) {
                rxtxSel = lv.getCheckedItemPosition();
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[24], isChecked ? "1" : "0");
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[25], isChecked ? "0" : "1");
                if (isChecked) {
                    minus.setChecked(!isChecked);
                    mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], !isChecked);
                }
                onOff(remainL, !isChecked);
                onOff(minusL, !isChecked);
                if (isChecked)
                    rxtxSum.setText(R.string.show_rx_tx_sum);
                else
                    rxtxSum.setText(R.string.show_used_left);
            }
            dialog1.dismiss();
        };
        switch (view.getId()) {
            case R.id.remain_data_summary:
            case R.id.remaintv:
            case R.id.remain_layout:
                array = getResources().getStringArray(R.array.remain);
                ldb.setTitle(R.string.show_remaining_widget);
                selection = remainSel;
                break;
            case R.id.info_summary:
            case R.id.infotv:
            case R.id.info_layout:
                array = getResources().getStringArray(R.array.fullinfo);
                ldb.setTitle(R.string.show_full_text_on_widget);
                selection = infoSel;
                break;
            case R.id.rx_tx_summary:
            case R.id.rx_txtv:
            case R.id.rxtx_layout:
                array = getResources().getStringArray(R.array.rxtx);
                ldb.setTitle(R.string.show_rx_tx);
                selection = rxtxSel;
                break;
            case R.id.logoPreview1:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_TRAFFIC[5]);
                break;
            case R.id.logoPreview2:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_TRAFFIC[6]);
                break;
            case R.id.logoPreview3:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_TRAFFIC[7]);
                break;
            case R.id.simFontSize:
            case R.id.textSize:
            case R.id.textSizeSum:
                dialog = SetSizeDialog.newInstance(textSizeSum.getText().toString(),
                        KEY_TEXT, Constants.TRAFFIC_TAG);
                break;
            case R.id.simLogoSize:
            case R.id.iconSize:
            case R.id.iconSizeSum:
                dialog = SetSizeDialog.newInstance(iconsSizeSum.getText().toString(),
                        KEY_ICON, Constants.TRAFFIC_TAG);
                break;
            case R.id.speedFontSize:
            case R.id.speedTextSize:
            case R.id.speedTextSizeSum:
                dialog = SetSizeDialog.newInstance(speedTextSum.getText().toString(),
                        KEY_TEXT_S, Constants.TRAFFIC_TAG);
                break;
            case R.id.speedArrowsSize:
            case R.id.speedIconsSize:
            case R.id.speedIconsSizeSum:
                dialog = SetSizeDialog.newInstance(speedIconsSum.getText().toString(),
                        KEY_ICON_S, Constants.TRAFFIC_TAG);
                break;
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
                dialog = ShowSimDialog.newInstance(Constants.TRAFFIC_TAG, mSim);
                break;
        }
        if (dialog != null) {
            dialog.show(getSupportFragmentManager(), "dialog");
        } else if (array != null) {
            adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, array);
            ldb.setSingleChoiceItems(adapter, selection, myClickListener);
            ldb.setNegativeButton(android.R.string.cancel, null);
            ldb.create();
            ldb.show();
        }
    }

    @Override
    public void onFinishEditDialog(String inputText, int dialog, String activity) {
        if (activity.equals(Constants.TRAFFIC_TAG) && !inputText.equals("")) {
            switch (dialog) {
                case KEY_TEXT:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[12], inputText);
                    textSizeSum.setText(inputText);
                    break;
                case KEY_ICON:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[11], inputText);
                    iconsSizeSum.setText(inputText);
                    break;
                case KEY_TEXT_S:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[16], inputText);
                    speedTextSum.setText(inputText);
                    break;
                case KEY_ICON_S:
                    mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[17], inputText);
                    speedIconsSum.setText(inputText);
                    break;
            }
        }
    }

    @Override
    public void onComplete(int position, String logo) {
        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        if (position < list.length - 1) {
            mUserPickedImage = "";
            int sim = Constants.DISABLED;
            switch (logo) {
                case "logo1":
                    sim = Constants.SIM1;
                    break;
                case "logo2":
                    sim = Constants.SIM2;
                    break;
                case "logo3":
                    sim = Constants.SIM3;
                    break;
            }
            String opLogo;
            if (listitems[position].equals("auto"))
                opLogo = MobileUtils.getLogoFromCode(mContext, sim);
            else
                opLogo = listitems[position];
            int resourceId = getResources().getIdentifier(opLogo, "drawable", mContext.getPackageName());
            if (logo.equals(Constants.PREF_WIDGET_TRAFFIC[5])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false);
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], opLogo);
                loadImageFromResource(resourceId, mDim, logo1);
                logoSum1.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_TRAFFIC[6])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false);
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], opLogo);
                loadImageFromResource(resourceId, mDim, logo2);
                logoSum2.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_TRAFFIC[7])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false);
                mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], opLogo);
                loadImageFromResource(resourceId, mDim, logo3);
                logoSum3.setText(list[position]);
            }
        } else {
            mUserPickedImage = logo;
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent, SELECT_PHOTO);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == RESULT_OK){
                    Uri selectedImage = imageReturnedIntent.getData();
                    if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[5])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], true);
                        String path = getRealPathFromURI(selectedImage);
                        if (path != null) {
                            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[5], path);
                            loadImageFromFile(new File(path), mDim, logo1);
                        }
                        logoSum1.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[6])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], true);
                        String path = getRealPathFromURI(selectedImage);
                        if (path != null) {
                            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[6], path);
                            loadImageFromFile(new File(path), mDim, logo2);
                        }
                        logoSum2.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_TRAFFIC[7])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], true);
                        String path = getRealPathFromURI(selectedImage);
                        if (path != null) {
                            mEdit.putString(Constants.PREF_WIDGET_TRAFFIC[7], path);
                            loadImageFromFile(new File(path), mDim, logo3);
                        }
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    }
                    mUserPickedImage = "";
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.names:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[1], isChecked);
                if (isChecked)
                    namesSum.setText(R.string.on);
                else
                    namesSum.setText(R.string.off);
                break;
            case R.id.speed:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[3], isChecked);
                onOff(speedFontL, isChecked);
                onOff(speedArrowsL, isChecked);
                if (isChecked) {
                    speedSum.setText(R.string.on);
                    speedFontL.setOnClickListener(this);
                    speedArrowsL.setOnClickListener(this);
                }
                else
                    speedSum.setText(R.string.off);
                break;
            case R.id.divider:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[21], isChecked);
                if (isChecked)
                    divSum.setText(R.string.on);
                else
                    divSum.setText(R.string.off);
                break;
            case R.id.useBack:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[14], isChecked);
                if (isChecked)
                    backSum.setText(R.string.on);
                else
                    backSum.setText(R.string.off);
                onOff(backColorL, isChecked);
                break;
            case R.id.icons:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[4], isChecked);
                onOff(logoL1, isChecked);
                onOff(logoL2, isChecked);
                onOff(logoL3, isChecked);
                onOff(simLogoL, isChecked);
                if (isChecked) {
                    iconsSum.setText(R.string.on);
                    simLogoL.setOnClickListener(this);
                    logoL1.setOnClickListener(this);
                    logoL2.setOnClickListener(this);
                    logoL3.setOnClickListener(this);
                }
                else
                    iconsSum.setText(R.string.off);
                break;
            case R.id.activesim:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[22], isChecked);
                onOff(showSimL, !isChecked);
                if (isChecked)
                    activesum.setText(R.string.on);
                else {
                    activesum.setText(R.string.off);
                    showSimL.setOnClickListener(this);
                }
                break;
            case R.id.daynight_icons:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[23], isChecked);
                if (isChecked)
                    daynightSum.setText(R.string.on);
                else
                    daynightSum.setText(R.string.off);
                break;
            case R.id.minus:
                mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], isChecked);
                if (isChecked)
                    minusSum.setText(R.string.on);
                else
                    minusSum.setText(R.string.off);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        AmbilWarnaDialog ambilWarnaDialog = null;
        switch (v.getId()) {
            case R.id.textColorPreview:
                ambilWarnaDialog = new AmbilWarnaDialog(this, mTextColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[13], color);
                        tiv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.textColorPreview1:
                ambilWarnaDialog = new AmbilWarnaDialog(this, mTextColor1, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[27], color);
                        tiv1.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.textColorPreview2:
                ambilWarnaDialog = new AmbilWarnaDialog(this, mTextColor2, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[28], color);
                        tiv2.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.textColorPreview3:
                ambilWarnaDialog = new AmbilWarnaDialog(this, mTextColor3, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[29], color);
                        tiv3.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.backColorPreview:
                ambilWarnaDialog = new AmbilWarnaDialog(this, mBackColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_TRAFFIC[15], color);
                        biv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.logoPreview1:
            case R.id.logoPreview2:
            case R.id.logoPreview3:
            case R.id.simFontSize:
            case R.id.simLogoSize:
            case R.id.speedFontSize:
            case R.id.speedArrowsSize:
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
            case R.id.textSize:
            case R.id.textSizeSum:
            case R.id.iconSize:
            case R.id.iconSizeSum:
            case R.id.speedIconsSize:
            case R.id.speedIconsSizeSum:
            case R.id.speedTextSize:
            case R.id.speedTextSizeSum:
            case R.id.remain_data_summary:
            case R.id.remaintv:
            case R.id.remain_layout:
            case R.id.info_summary:
            case R.id.infotv:
            case R.id.info_layout:
            case R.id.rx_tx_summary:
            case R.id.rx_txtv:
            case R.id.rxtx_layout:
                showDialog(v);
                break;
        }
        if (ambilWarnaDialog != null)
            ambilWarnaDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void OnDialogClosed(String activity, boolean[] sim) {
        if (activity.equals(Constants.TRAFFIC_TAG)) {
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[18], sim[0]);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], sim[1]);
            mEdit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], sim[2]);
            mSim = sim;
            String sum = "";
            if (sim[0])
                sum = "SIM1";
            if (sim[1])
                if (sum.equals(""))
                    sum = "SIM2";
                else
                    sum += ", SIM2";
            if (sim[2])
                if (sum.equals(""))
                    sum = "SIM3";
                else
                    sum += ", SIM3";
            showSimSum.setText(sum);
        }
    }
}
