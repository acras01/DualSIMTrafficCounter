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

public class CallsWidgetConfigActivity extends AppCompatActivity implements IconsListFragment.OnCompleteListener,
        View.OnClickListener, CompoundButton.OnCheckedChangeListener, SetSizeDialog.TextSizeDialogListener,
        ShowSimDialog.ShowSimDialogClosedListener {

    private static final int SELECT_PHOTO = 101;
    private final int KEY_TEXT = 0, KEY_ICON = 1;
    private int mDim;
    private int mWidgetID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences.Editor mEdit;
    private Context mContext;
    private int mTextColor, mTextColor1, mBackColor, remainSel;
    private Intent mResultValueIntent;
    private TextView namesSum, iconsSum, divSum, backSum, textSizeSum, iconsSizeSum, logoSum1, logoSum2, logoSum3, showSimSum, remainSum;
    private RelativeLayout logoL1;
    private RelativeLayout logoL2;
    private RelativeLayout logoL3;
    private RelativeLayout simLogoL;
    private RelativeLayout backColorL;
    private ImageView tiv, tiv1, biv, logo1, logo2, logo3;
    private String mUserPickedImage;
    private boolean[] mSim;

    public CallsWidgetConfigActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = CustomApplication.getAppContext();

        /*if (!CustomApplication.isMyServiceRunning(mContext, CallLoggerService.class))
            startService(new Intent(mContext, CallLoggerService.class));*/

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

        SharedPreferences prefsWidget = getSharedPreferences(String.valueOf(mWidgetID) + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
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
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[1], true); //Show mNames
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[2], true); //Show icons
            mEdit.putString(Constants.PREF_WIDGET_CALLS[3], "none"); //SIM1 icon
            mEdit.putString(Constants.PREF_WIDGET_CALLS[4], "none"); //SIM2 icon
            mEdit.putString(Constants.PREF_WIDGET_CALLS[5], "none"); //SIM3 icon
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[6], false); //SIM1 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[7], false); //SIM2 user icon
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[8], false); //SIM3 user icon
            mEdit.putString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE); //Icon size
            mEdit.putString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE); //Font size
            mEdit.putInt(Constants.PREF_WIDGET_CALLS[11], Color.WHITE); //Text color
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[12], true); //Use background
            mEdit.putInt(Constants.PREF_WIDGET_CALLS[13], Color.TRANSPARENT); //Background color
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[14], true); //Show divider
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[15], true); //Show SIM1
            if (simQuantity >= 2)
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[16], true); //Show SIM2
            else
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[16], false);
            if (simQuantity == 3)
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[17], true); //Show SIM3
            else
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[17], false);
            mEdit.putString(Constants.PREF_WIDGET_CALLS[18], "1"); //Show remaining
            mEdit.putInt(Constants.PREF_WIDGET_CALLS[19], Color.WHITE); //Total Text color
            mEdit.apply();
        }

        mSim = new boolean[]{prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[15], true),
                prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[16], true), prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[17], true)};

        mTextColor = prefsWidget.getInt(Constants.PREF_WIDGET_CALLS[11], Color.WHITE);
        mTextColor1 = prefsWidget.getInt(Constants.PREF_WIDGET_CALLS[19], Color.WHITE);
        mBackColor = prefsWidget.getInt(Constants.PREF_WIDGET_CALLS[13], Color.TRANSPARENT);

        mResultValueIntent = new Intent();
        mResultValueIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetID);

        setResult(RESULT_CANCELED, mResultValueIntent);

        setContentView(R.layout.calls_info_widget_configure);
        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        AppCompatCheckBox names = findViewById(R.id.names);
        names.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[1], true));
        AppCompatCheckBox icons = findViewById(R.id.icons);
        icons.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[2], true));
        AppCompatCheckBox back = findViewById(R.id.useBack);
        back.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[12], true));
        AppCompatCheckBox div = findViewById(R.id.divider);
        div.setChecked(prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[14], true));

        namesSum = findViewById(R.id.names_summary);
        if (names.isChecked())
            namesSum.setText(R.string.on);
        else
            namesSum.setText(R.string.off);
        iconsSum = findViewById(R.id.icons_summary);
        if (icons.isChecked())
            iconsSum.setText(R.string.on);
        else
            iconsSum.setText(R.string.off);
        divSum = findViewById(R.id.divider_summary);
        if (div.isChecked())
            divSum.setText(R.string.on);
        else
            divSum.setText(R.string.off);
        backSum = findViewById(R.id.back_summary);
        if (back.isChecked())
            backSum.setText(R.string.on);
        else
            backSum.setText(R.string.off);

        RelativeLayout remainL = findViewById(R.id.remain_layout);
        if (remainL != null) {
            remainSum = findViewById(R.id.remain_calls_summary);
            remainSel = Integer.valueOf(Objects.requireNonNull(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[18], "1")));
            if (remainSel == 0)
                remainSum.setText(R.string.remain);
            else
                remainSum.setText(R.string.used);
        }


        logoL1 = findViewById(R.id.logoLayout1);
        logoL2 = findViewById(R.id.logoLayout2);
        logoL3 = findViewById(R.id.logoLayout3);

        RelativeLayout simFontL = findViewById(R.id.simFontSize);
        simLogoL = findViewById(R.id.simLogoSize);
        RelativeLayout showSimL = findViewById(R.id.showSim);
        backColorL = findViewById(R.id.backColorLayout);

        onOff(logoL1, icons.isChecked());
        onOff(logoL2, simQuantity >= 2 && icons.isChecked());
        onOff(logoL3, simQuantity == 3 && icons.isChecked());
        onOff(backColorL, back.isChecked());

        showSimSum = findViewById(R.id.simChooseSum);
        String sum = "";
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[15], true))
            sum = "SIM1";
        if (simQuantity >= 2 && prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[16], true))
            if (sum.equals(""))
                sum = "SIM2";
            else
                sum += ", SIM2";
        if (simQuantity == 3 && prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[17], true))
            if (sum.equals(""))
                sum = "SIM3";
            else
                sum += ", SIM3";
        showSimSum.setText(sum);

        textSizeSum = findViewById(R.id.textSizeSum);
        textSizeSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE));

        iconsSizeSum = findViewById(R.id.iconSizeSum);
        iconsSizeSum.setText(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE));

        names.setOnCheckedChangeListener(this);
        icons.setOnCheckedChangeListener(this);
        back.setOnCheckedChangeListener(this);

        tiv = findViewById(R.id.textColorPreview);
        tiv1 = findViewById(R.id.textColorPreview1);
        biv = findViewById(R.id.backColorPreview);
        tiv.setBackgroundColor(mTextColor);
        tiv1.setBackgroundColor(mTextColor1);
        biv.setBackgroundColor(mBackColor);

        logo1 = findViewById(R.id.logoPreview1);
        logo2 = findViewById(R.id.logoPreview2);
        logo3 = findViewById(R.id.logoPreview3);
        logoSum1 = findViewById(R.id.logoSum1);
        logoSum2 = findViewById(R.id.logoSum2);
        logoSum3 = findViewById(R.id.logoSum3);

        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[6], false)) {
            loadImageFromFile(new File(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[3], "")), mDim, logo1);
            logoSum1.setText(getResources().getString(R.string.userpick));
        } else
            loadImageFromResource(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[3],
                    "none"), "drawable", mContext.getPackageName()), mDim, logo1);
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[7], false)) {
            loadImageFromFile(new File(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[4], "")), mDim, logo2);
            logoSum2.setText(getResources().getString(R.string.userpick));
        } else
            loadImageFromResource(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[4],
                    "none"), "drawable", mContext.getPackageName()), mDim, logo2);
        if (prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[8], false)) {
            loadImageFromFile(new File(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[5], "")), mDim, logo3);
            logoSum3.setText(getResources().getString(R.string.userpick));
        } else
            loadImageFromResource(getResources().getIdentifier(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[5],
                    "none"), "drawable", mContext.getPackageName()), mDim, logo3);

        String[] listitems = getResources().getStringArray(R.array.icons_values);
        String[] list = getResources().getStringArray(R.array.icons);
        for (int i = 0; i < list.length; i++) {
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[6], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[3], "none")))
                logoSum1.setText(list[i]);
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[7], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[4], "none")))
                logoSum2.setText(list[i]);
            if (!prefsWidget.getBoolean(Constants.PREF_WIDGET_CALLS[8], false) && listitems[i].equals(prefsWidget.getString(Constants.PREF_WIDGET_CALLS[5], "none")))
                logoSum3.setText(list[i]);
        }

        tiv.setOnClickListener(this);
        tiv1.setOnClickListener(this);
        biv.setOnClickListener(this);
        logo1.setOnClickListener(this);
        logo2.setOnClickListener(this);
        logo3.setOnClickListener(this);
        setOnClickListenerWithChild(simFontL, this);
        setOnClickListenerWithChild(simLogoL, this);
        setOnClickListenerWithChild(showSimL, this);
        if (remainL != null) {
            setOnClickListenerWithChild(remainL, this);
        }
        backColorL.setOnClickListener(this);
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
                Intent intent = new Intent(Constants.CALLS_BROADCAST_ACTION);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
            if (logo.equals(Constants.PREF_WIDGET_CALLS[3])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[6], false);
                mEdit.putString(Constants.PREF_WIDGET_CALLS[3], opLogo);
                loadImageFromResource(resourceId, mDim, logo1);
                logoSum1.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_CALLS[4])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[7], false);
                mEdit.putString(Constants.PREF_WIDGET_CALLS[4], opLogo);
                loadImageFromResource(resourceId, mDim, logo2);
                logoSum2.setText(list[position]);
            } else if (logo.equals(Constants.PREF_WIDGET_CALLS[5])) {
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[8], false);
                mEdit.putString(Constants.PREF_WIDGET_CALLS[5], opLogo);
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
                    if (mUserPickedImage.equals(Constants.PREF_WIDGET_CALLS[5])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[8], true);
                        String path = getRealPathFromURI(selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_CALLS[5], path);
                        loadImageFromFile(new File(path), mDim, logo1);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_CALLS[6])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[9], true);
                        String path = getRealPathFromURI(selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_CALLS[6], path);
                        loadImageFromFile(new File(path), mDim, logo2);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    } else if (mUserPickedImage.equals(Constants.PREF_WIDGET_CALLS[7])) {
                        mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[10], true);
                        String path = getRealPathFromURI(selectedImage);
                        mEdit.putString(Constants.PREF_WIDGET_CALLS[7], path);
                        loadImageFromFile(new File(path), mDim, logo3);
                        logoSum3.setText(getResources().getString(R.string.userpick));
                    }
                    mUserPickedImage = "";
                }
                break;
        }
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
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.names:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[1], isChecked);
                if (isChecked)
                    namesSum.setText(R.string.on);
                else
                    namesSum.setText(R.string.off);
                break;
            case R.id.divider:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[14], isChecked);
                if (isChecked)
                    divSum.setText(R.string.on);
                else
                    divSum.setText(R.string.off);
                break;
            case R.id.useBack:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[12], isChecked);
                if (isChecked)
                    backSum.setText(R.string.on);
                else
                    backSum.setText(R.string.off);
                onOff(backColorL, isChecked);
                break;
            case R.id.icons:
                mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[2], isChecked);
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
        }
    }

    @Override
    public void onClick(View v) {
        AmbilWarnaDialog dialog = null;
        switch (v.getId()) {
            case R.id.textColorPreview:
                dialog = new AmbilWarnaDialog(this, mTextColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_CALLS[11], color);
                        tiv.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.textColorPreview1:
                dialog = new AmbilWarnaDialog(this, mTextColor1, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_CALLS[19], color);
                        tiv1.setBackgroundColor(color);
                    }
                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                break;
            case R.id.backColorPreview:
                dialog = new AmbilWarnaDialog(this, mBackColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        mEdit.putInt(Constants.PREF_WIDGET_CALLS[13], color);
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
            case R.id.textSize:
            case R.id.textSizeSum:
            case R.id.iconSize:
            case R.id.iconSizeSum:
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
            case R.id.remain_calls_summary:
            case R.id.remaintv:
            case R.id.remain_layout:
                showDialog(v);
                break;
        }
        if (dialog != null)
            dialog.show();
    }

    private void showDialog(View view) {
        DialogFragment dialog = null;
        AlertDialog.Builder ldb = new AlertDialog.Builder(this);
        ArrayAdapter<String> adapter = null;
        int selection = -1;
        DialogInterface.OnClickListener myClickListener = (dialog1, which) -> {
            ListView lv = ((AlertDialog) dialog1).getListView();
            String[] array = getStringArray(lv.getAdapter());
            boolean isChecked = lv.getCheckedItemPosition() == 0;
            if (Arrays.equals(array, getResources().getStringArray(R.array.remain))) {
                remainSel = lv.getCheckedItemPosition();
                mEdit.putString(Constants.PREF_WIDGET_CALLS[18], isChecked ? "0" : "1");
                if (isChecked)
                    remainSum.setText(R.string.remain);
                else
                    remainSum.setText(R.string.used);
            }
            dialog1.dismiss();
        };
        switch (view.getId()) {
            case R.id.remain_calls_summary:
            case R.id.remaintv:
            case R.id.remain_layout:
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice,
                        getResources().getStringArray(R.array.remain));
                ldb.setTitle(R.string.show_remaining_calls_widget);
                selection = remainSel;
                break;
            case R.id.logoPreview1:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_CALLS[3]);
                break;
            case R.id.logoPreview2:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_CALLS[4]);
                break;
            case R.id.logoPreview3:
                dialog = IconsListFragment.newInstance(Constants.PREF_WIDGET_CALLS[5]);
                break;
            case R.id.simFontSize:
            case R.id.textSize:
            case R.id.textSizeSum:
                dialog = SetSizeDialog.newInstance(textSizeSum.getText().toString(),
                        KEY_TEXT, Constants.CALLS_TAG);
                break;
            case R.id.simLogoSize:
            case R.id.iconSize:
            case R.id.iconSizeSum:
                dialog = SetSizeDialog.newInstance(iconsSizeSum.getText().toString(),
                        KEY_ICON, Constants.CALLS_TAG);
                break;
            case R.id.showSim:
            case R.id.simChoose:
            case R.id.simChooseSum:
                dialog = ShowSimDialog.newInstance(Constants.CALLS_TAG, mSim);
                break;
        }
        if (dialog != null) {
            dialog.show(getSupportFragmentManager(), "dialog");
        } else if (adapter != null) {
            ldb.setSingleChoiceItems(adapter, selection, myClickListener);
            ldb.setNegativeButton(android.R.string.cancel, null);
            ldb.create();
            ldb.show();
        }
    }

    public void onFinishEditDialog(String inputText, int dialog, String activity) {
        if (activity.equals(Constants.CALLS_TAG) && !inputText.equals("")) {
            switch (dialog) {
                case KEY_TEXT:
                    mEdit.putString(Constants.PREF_WIDGET_CALLS[10], inputText);
                    textSizeSum.setText(inputText);
                    break;
                case KEY_ICON:
                    mEdit.putString(Constants.PREF_WIDGET_CALLS[9], inputText);
                    iconsSizeSum.setText(inputText);
                    break;
            }
        }
    }

    @Override
    public void OnDialogClosed(String activity, boolean[] sim) {
        if (activity.equals(Constants.CALLS_TAG)) {
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[15], sim[0]);
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[16], sim[1]);
            mEdit.putBoolean(Constants.PREF_WIDGET_CALLS[17], sim[2]);
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

