package ua.od.acros.dualsimtrafficcounter.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.MainActivity;
import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.CallsWidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class CallsInfoWidget extends AppWidgetProvider {

    private ArrayList<String> mIMSI;

    @Override
    public final void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        //super.onUpdate(context, appWidgetManager, appWidgetIds);
        updateWidget(context, appWidgetManager, appWidgetIds, readData(context));
    }

    @Override
    public final void onReceive(Context context, Intent intent) {
        //super.onReceive(context, intent);
        String action = intent.getAction();
        int[] widgetIds = intent.getIntArrayExtra(Constants.WIDGET_IDS);
        if (action != null) {
            if (action.equals(AppWidgetManager.ACTION_APPWIDGET_DELETED) && intent.getExtras() != null) {
                final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                    CustomApplication.deleteWidgetPreferenceFile(new int[]{appWidgetId}, Constants.CALLS_TAG);
            } else if (action.equals(Constants.CALLS_BROADCAST_ACTION) && widgetIds != null)
                updateWidget(context, AppWidgetManager.getInstance(context), widgetIds, readData(context));
        }
    }

    private Bundle readData(Context context) {
        Bundle bundle = new Bundle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        CustomDatabaseHelper dbHelper = CustomDatabaseHelper.getInstance(context);
        ContentValues dataMap;
        boolean emptyDB;
        if (prefs.getBoolean(Constants.PREF_OTHER[45], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(context);
            emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.CALLS + "_" +
                    mIMSI.get(Constants.SIM1), false);
            if (!emptyDB) {
                dataMap = CustomDatabaseHelper.readCallsDataForSim(dbHelper, mIMSI.get(0));
                bundle.putLong(Constants.CALLS1, (long) dataMap.get("calls"));
            } else
                bundle.putLong(Constants.CALLS1, 0L);
            if (mIMSI.size() >= 2) {
                emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.CALLS + "_" +
                        mIMSI.get(Constants.SIM2), false);
                if (!emptyDB) {
                    dataMap = CustomDatabaseHelper.readCallsDataForSim(dbHelper, mIMSI.get(1));
                    bundle.putLong(Constants.CALLS2, (long) dataMap.get("calls"));
                } else
                    bundle.putLong(Constants.CALLS2, 0L);
            }
            if (mIMSI.size() == 3) {
                emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.CALLS + "_" +
                        mIMSI.get(Constants.SIM3), false);
                if (!emptyDB) {
                    dataMap = CustomDatabaseHelper.readCallsDataForSim(dbHelper, mIMSI.get(2));
                    bundle.putLong(Constants.CALLS3, (long) dataMap.get("calls"));
                } else
                    bundle.putLong(Constants.CALLS3, 0L);
            }
        } else {
            emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.CALLS, true);
            if (!emptyDB) {
                dataMap = CustomDatabaseHelper.readCallsData(dbHelper);
                bundle.putLong(Constants.CALLS1, (long) dataMap.get(Constants.CALLS1));
                bundle.putLong(Constants.CALLS2, (long) dataMap.get(Constants.CALLS2));
                bundle.putLong(Constants.CALLS3, (long) dataMap.get(Constants.CALLS3));
            } else {
                bundle.putLong(Constants.CALLS1, 0L);
                bundle.putLong(Constants.CALLS2, 0L);
                bundle.putLong(Constants.CALLS3, 0L);
            }
        }
        return bundle;
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] ids, Bundle bundle) {
        for (int i : ids) {
            SharedPreferences prefs = context.getSharedPreferences(i + Constants.CALLS_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences prefsSIM = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getAll().size() == 0) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[1], true); //Show mNames
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[2], true); //Show icons
                edit.putString(Constants.PREF_WIDGET_CALLS[3], "none"); //SIM1 icon
                edit.putString(Constants.PREF_WIDGET_CALLS[4], "none"); //SIM2 icon
                edit.putString(Constants.PREF_WIDGET_CALLS[5], "none"); //SIM3 icon
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[6], false); //SIM1 user icon
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[7], false); //SIM2 user icon
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[8], false); //SIM3 user icon
                edit.putString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE); //Icon size
                edit.putString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE); //Font size
                edit.putInt(Constants.PREF_WIDGET_CALLS[11], Color.WHITE); //Text color
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[12], true); //Use background
                edit.putInt(Constants.PREF_WIDGET_CALLS[13], Color.TRANSPARENT); //Background color
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[14], true); //Show divider
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[15], true); //Show SIM1
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[16], true); //Show SIM2
                edit.putBoolean(Constants.PREF_WIDGET_CALLS[17], true); //Show SIM3
                edit.putString(Constants.PREF_WIDGET_CALLS[18], "1"); //Show remaining
                edit.putInt(Constants.PREF_WIDGET_CALLS[19], Color.WHITE); // Total Text color
                edit.apply();
            }
            String[] operatorNames = new String[]{MobileUtils.getName(context, Constants.PREF_SIM1[5], Constants.PREF_SIM1[6], Constants.SIM1),
                    MobileUtils.getName(context, Constants.PREF_SIM2[5], Constants.PREF_SIM2[6], Constants.SIM2),
                    MobileUtils.getName(context, Constants.PREF_SIM3[5], Constants.PREF_SIM3[6], Constants.SIM3)};
            Intent settIntent = new Intent(context, CallsWidgetConfigActivity.class);
            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
            settIntent.putExtras(extras);
            settIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent settPIntent = PendingIntent.getActivity(context, i, settIntent, 0);

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Constants.CALLS_TAP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, 0);

            int dim = Integer.parseInt(Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_CALLS[9], Constants.ICON_SIZE)));

            String sizestr = prefs.getString(Constants.PREF_WIDGET_CALLS[10], Constants.TEXT_SIZE);

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.calls_info_widget);

            //SIM1
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true)) {
                String text;
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_CALLS[18], "1")).equals("0")) {
                    String limit = prefsSIM.getString(Constants.PREF_SIM1_CALLS[1], "");
                    if (limit != null && !limit.equals("")) {
                        long lim = Long.valueOf(limit) * Constants.MINUTE;
                        long rest = lim - bundle.getLong(Constants.CALLS1, 0);
                        if (rest < 0)
                            rest = 0;
                        text = DataFormat.formatCallDuration(context, rest);
                    } else
                        text = context.getString(R.string.not_set);
                } else
                    text = "-" + DataFormat.formatCallDuration(context, bundle.getLong(Constants.CALLS1, 0));
                updateViews.setTextViewText(R.id.totSIM1, text);
                updateViews.setViewVisibility(R.id.operSIM1, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true)) {
                    updateViews.setViewVisibility(R.id.operSIM1, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM1, operatorNames[0]);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_CALLS[6], false))
                        Picasso.get()
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_CALLS[3], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});
                    else
                        Picasso.get()
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_CALLS[3], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo1, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo1, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, settPIntent);
                }
                if (sizestr != null && !sizestr.equals("")) {
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[19], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout1, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.simLayout1, View.GONE);

            //SIM2
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[16], true)) {
                String text;
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_CALLS[18], "1")).equals("0")) {
                    String limit = prefsSIM.getString(Constants.PREF_SIM2_CALLS[1], "");
                    if (limit != null && !limit.equals("")) {
                        long lim = Long.valueOf(limit) * Constants.MINUTE;
                        long rest = lim - bundle.getLong(Constants.CALLS2, 0);
                        if (rest < 0)
                            rest = 0;
                        text = DataFormat.formatCallDuration(context, rest);
                    } else
                        text = context.getString(R.string.not_set);
                } else
                    text = "-" + DataFormat.formatCallDuration(context, bundle.getLong(Constants.CALLS2, 0));
                updateViews.setTextViewText(R.id.totSIM2, text);
                updateViews.setViewVisibility(R.id.operSIM2, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true)) {
                    updateViews.setViewVisibility(R.id.operSIM2, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM2, operatorNames[1]);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_CALLS[7], false))
                        Picasso.get()
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_CALLS[4], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});
                    else
                        Picasso.get()
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_CALLS[4], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo2, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo2, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, settPIntent);
                }
                if (sizestr != null && !sizestr.equals("")) {
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[19], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout2, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true)) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[14], true))
                        updateViews.setViewVisibility(R.id.stub1, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub1, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub1, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout2, View.GONE);
                updateViews.setViewVisibility(R.id.stub1, View.GONE);
            }

            //SIM3
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[17], true)) {
                String text;
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_CALLS[18], "1")).equals("0")) {
                    String limit = prefsSIM.getString(Constants.PREF_SIM3_CALLS[1], "");
                    if (limit != null && !limit.equals("")) {
                        long lim = Long.valueOf(limit) * Constants.MINUTE;
                        long rest = lim - bundle.getLong(Constants.CALLS3, 0);
                        if (rest < 0)
                            rest = 0;
                        text = DataFormat.formatCallDuration(context, rest);
                    } else
                        text = context.getString(R.string.not_set);
                } else
                    text = "-" + DataFormat.formatCallDuration(context, bundle.getLong(Constants.CALLS3, 0));
                updateViews.setTextViewText(R.id.totSIM1, text);
                updateViews.setViewVisibility(R.id.operSIM3, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[1], true)) {
                    updateViews.setViewVisibility(R.id.operSIM3, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM3, operatorNames[2]);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[2], true)) {

                    if (!prefs.getBoolean(Constants.PREF_WIDGET_CALLS[8], false))
                        Picasso.get()
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_CALLS[5], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});
                    else
                        Picasso.get()
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_CALLS[5], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo3, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo3, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, settPIntent);
                }
                if (sizestr != null && !sizestr.equals("")) {
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.totSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[19], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.operSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_CALLS[11], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout3, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[15], true) ||
                        prefs.getBoolean(Constants.PREF_WIDGET_CALLS[16], true)) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[14], true))
                        updateViews.setViewVisibility(R.id.stub2, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub2, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub2, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout3, View.GONE);
                updateViews.setViewVisibility(R.id.stub2, View.GONE);
            }

            //BACKGROUND
            if (prefs.getBoolean(Constants.PREF_WIDGET_CALLS[12], true)) {
                updateViews.setInt(R.id.background, "setColorFilter", prefs.getInt(Constants.PREF_WIDGET_CALLS[13], ContextCompat.getColor(context, R.color.background)));
                updateViews.setViewVisibility(R.id.background, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.background, View.GONE);

            //UPDATE
            appWidgetManager.updateAppWidget(i, updateViews);
        }
    }

    @Override
    public final void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it
        CustomApplication.deleteWidgetPreferenceFile(appWidgetIds, Constants.CALLS_TAG);
        //super.onDeleted(context, appWidgetIds);
    }

    @Override
    public final void onDisabled(Context context) {
        Picasso.get().shutdown();
        //super.onDisabled(context);
    }
}

