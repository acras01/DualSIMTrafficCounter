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
import ua.od.acros.dualsimtrafficcounter.activities.TrafficWidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.services.TrafficCountService;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;
import ua.od.acros.dualsimtrafficcounter.utils.CustomDatabaseHelper;
import ua.od.acros.dualsimtrafficcounter.utils.DataFormat;
import ua.od.acros.dualsimtrafficcounter.utils.MobileUtils;

public class TrafficInfoWidget extends AppWidgetProvider {

    private ArrayList<String> mIMSI;

    @Override
    public final void onUpdate(Context context, AppWidgetManager widgetManager, int[] widgetId) {
        //super.onUpdate(context, widgetManager, widgetId);
        updateWidget(context, widgetManager, widgetId, readData(context));
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
                    CustomApplication.deleteWidgetPreferenceFile(new int[]{appWidgetId}, Constants.TRAFFIC_TAG);
            } else if (action.equals(Constants.TRAFFIC_BROADCAST_ACTION) && widgetIds != null)
                updateWidget(context, AppWidgetManager.getInstance(context), widgetIds, Objects.requireNonNull(intent.getExtras()));
        }
    }

    private Bundle readData(Context context) {
        Bundle bundle = new Bundle();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        CustomDatabaseHelper dbHelper = CustomDatabaseHelper.getInstance(context);
        ContentValues dataMap;
        boolean emptyDB;
        int activeSIM;
        if (CustomApplication.isMyServiceRunning(TrafficCountService.class))
            activeSIM = TrafficCountService.getActiveSIM();
        else
            activeSIM = prefs.getInt(Constants.PREF_OTHER[46], Constants.SIM1);
        bundle.putInt(Constants.SIM_ACTIVE, activeSIM);
        if (prefs.getBoolean(Constants.PREF_OTHER[44], false)) {
            if (mIMSI == null)
                mIMSI = MobileUtils.getSimIds(context);
            emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.TRAFFIC + "_" +
                    mIMSI.get(Constants.SIM1), false);
            if (!emptyDB) {
                dataMap = CustomDatabaseHelper.readTrafficDataForSim(dbHelper, mIMSI.get(0));
                bundle.putLong(Constants.SIM1RX, (long) dataMap.get("rx"));
                bundle.putLong(Constants.SIM1TX, (long) dataMap.get("tx"));
                bundle.putLong(Constants.TOTAL1, (long) dataMap.get("total"));
                bundle.putLong(Constants.SIM1RX_N, (long) dataMap.get("rx_n"));
                bundle.putLong(Constants.SIM1TX_N, (long) dataMap.get("tx_n"));
                bundle.putLong(Constants.TOTAL1_N, (long) dataMap.get("total_n"));
            } else {
                bundle.putLong(Constants.SIM1RX, 0L);
                bundle.putLong(Constants.SIM1TX, 0L);
                bundle.putLong(Constants.TOTAL1, 0L);
                bundle.putLong(Constants.SIM1RX_N, 0L);
                bundle.putLong(Constants.SIM1TX_N, 0L);
                bundle.putLong(Constants.TOTAL1_N, 0L);
            }
            if (mIMSI.size() >= 2) {
                emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.TRAFFIC + "_" +
                        mIMSI.get(Constants.SIM2), false);
                if (!emptyDB) {
                    dataMap = CustomDatabaseHelper.readTrafficDataForSim(dbHelper, mIMSI.get(1));
                    bundle.putLong(Constants.SIM2RX, (long) dataMap.get("rx"));
                    bundle.putLong(Constants.SIM2TX, (long) dataMap.get("tx"));
                    bundle.putLong(Constants.TOTAL2, (long) dataMap.get("total"));
                    bundle.putLong(Constants.SIM2RX_N, (long) dataMap.get("rx_n"));
                    bundle.putLong(Constants.SIM2TX_N, (long) dataMap.get("tx_n"));
                    bundle.putLong(Constants.TOTAL2_N, (long) dataMap.get("total_n"));
                } else {
                    bundle.putLong(Constants.SIM2RX, 0L);
                    bundle.putLong(Constants.SIM2TX, 0L);
                    bundle.putLong(Constants.TOTAL2, 0L);
                    bundle.putLong(Constants.SIM2RX_N, 0L);
                    bundle.putLong(Constants.SIM2TX_N, 0L);
                    bundle.putLong(Constants.TOTAL2_N, 0L);
                }
            }
            if (mIMSI.size() >= 3) {
                emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.TRAFFIC + "_" +
                        mIMSI.get(Constants.SIM3), false);
                if (!emptyDB) {
                    dataMap = CustomDatabaseHelper.readTrafficDataForSim(dbHelper, mIMSI.get(2));
                    bundle.putLong(Constants.SIM3RX, (long) dataMap.get("rx"));
                    bundle.putLong(Constants.SIM3TX, (long) dataMap.get("tx"));
                    bundle.putLong(Constants.TOTAL3, (long) dataMap.get("total"));
                    bundle.putLong(Constants.SIM3RX_N, (long) dataMap.get("rx_n"));
                    bundle.putLong(Constants.SIM3TX_N, (long) dataMap.get("tx_n"));
                    bundle.putLong(Constants.TOTAL3_N, (long) dataMap.get("total_n"));
                } else {
                    bundle.putLong(Constants.SIM3RX, 0L);
                    bundle.putLong(Constants.SIM3TX, 0L);
                    bundle.putLong(Constants.TOTAL3, 0L);
                    bundle.putLong(Constants.SIM3RX_N, 0L);
                    bundle.putLong(Constants.SIM3TX_N, 0L);
                    bundle.putLong(Constants.TOTAL3_N, 0L);
                }
            }
        } else {
            emptyDB = CustomDatabaseHelper.isTableEmpty(dbHelper, Constants.CALLS, true);
            if (!emptyDB) {
                dataMap = CustomDatabaseHelper.readTrafficData(CustomDatabaseHelper.getInstance(context));
                bundle.putLong(Constants.SIM1RX, (long) dataMap.get(Constants.SIM1RX));
                bundle.putLong(Constants.SIM2RX, (long) dataMap.get(Constants.SIM2RX));
                bundle.putLong(Constants.SIM3RX, (long) dataMap.get(Constants.SIM3RX));
                bundle.putLong(Constants.SIM1TX, (long) dataMap.get(Constants.SIM1TX));
                bundle.putLong(Constants.SIM2TX, (long) dataMap.get(Constants.SIM2TX));
                bundle.putLong(Constants.SIM3TX, (long) dataMap.get(Constants.SIM3TX));
                bundle.putLong(Constants.TOTAL1, (long) dataMap.get(Constants.TOTAL1));
                bundle.putLong(Constants.TOTAL2, (long) dataMap.get(Constants.TOTAL2));
                bundle.putLong(Constants.TOTAL3, (long) dataMap.get(Constants.TOTAL3));
                bundle.putLong(Constants.SIM1RX_N, (long) dataMap.get(Constants.SIM1RX_N));
                bundle.putLong(Constants.SIM2RX_N, (long) dataMap.get(Constants.SIM2RX_N));
                bundle.putLong(Constants.SIM3RX_N, (long) dataMap.get(Constants.SIM3RX_N));
                bundle.putLong(Constants.SIM1TX_N, (long) dataMap.get(Constants.SIM1TX_N));
                bundle.putLong(Constants.SIM2TX_N, (long) dataMap.get(Constants.SIM2TX_N));
                bundle.putLong(Constants.SIM3TX_N, (long) dataMap.get(Constants.SIM3TX_N));
                bundle.putLong(Constants.TOTAL1_N, (long) dataMap.get(Constants.TOTAL1_N));
                bundle.putLong(Constants.TOTAL2_N, (long) dataMap.get(Constants.TOTAL2_N));
                bundle.putLong(Constants.TOTAL3_N, (long) dataMap.get(Constants.TOTAL3_N));
            } else {
                bundle.putLong(Constants.SIM1RX, 0L);
                bundle.putLong(Constants.SIM2RX, 0L);
                bundle.putLong(Constants.SIM3RX, 0L);
                bundle.putLong(Constants.SIM1TX, 0L);
                bundle.putLong(Constants.SIM2TX, 0L);
                bundle.putLong(Constants.SIM3TX, 0L);
                bundle.putLong(Constants.TOTAL1, 0L);
                bundle.putLong(Constants.TOTAL2, 0L);
                bundle.putLong(Constants.TOTAL3, 0L);
                bundle.putLong(Constants.SIM1RX_N, 0L);
                bundle.putLong(Constants.SIM2RX_N, 0L);
                bundle.putLong(Constants.SIM3RX_N, 0L);
                bundle.putLong(Constants.SIM1TX_N, 0L);
                bundle.putLong(Constants.SIM2TX_N, 0L);
                bundle.putLong(Constants.SIM3TX_N, 0L);
                bundle.putLong(Constants.TOTAL1_N, 0L);
                bundle.putLong(Constants.TOTAL2_N, 0L);
                bundle.putLong(Constants.TOTAL3_N, 0L);
            }
        }
        return bundle;
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int[] ids, Bundle bundle) {
        if (bundle.size() <= 1)
            bundle = readData(context);
        for (int i : ids) {
            SharedPreferences prefs = context.getSharedPreferences(i + Constants.TRAFFIC_TAG + Constants.WIDGET_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences prefsSIM = PreferenceManager.getDefaultSharedPreferences(context);
            if (prefs.getAll().size() == 0) {
                SharedPreferences.Editor edit = prefs.edit();
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true);//Show mNames
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[2], "0");//Show full/short info
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[3], false);//Show speed
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true);//Show sim icons
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[5], "none");//SIM1 icon
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[6], "none");//SIM2 icon
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[7], "none");//SIM3 icon
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false);//SIM1 user icon
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false);//SIM2 user icon
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false);//SIM3 user icon
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE);//Icon size
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE);//Font size
                edit.putInt(Constants.PREF_WIDGET_TRAFFIC[13], Color.WHITE);//Text color
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true);//Use background
                edit.putInt(Constants.PREF_WIDGET_TRAFFIC[15], Color.TRANSPARENT);//Background color
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE);//Speed text size
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE);//Speed arrows size
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true);//show sim1
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true);//show sim2
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true);//Show sim3
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true);//Show divider
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false);//Show only active SIM
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false);//Show day/night icons
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[24], "1");//Show remaining
                edit.putString(Constants.PREF_WIDGET_TRAFFIC[25], "0");//Show RX/TX
                edit.putBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true);//Show over-limit traffic
                edit.putInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(context, android.R.color.holo_green_dark));//TX Text color
                edit.putInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(context, android.R.color.holo_orange_dark));//RX Text color
                edit.putInt(Constants.PREF_WIDGET_TRAFFIC[29], Color.WHITE);//Total Text color
                edit.apply();
            }

            Intent settIntent = new Intent(context, TrafficWidgetConfigActivity.class);
            Bundle extras = new Bundle();
            extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, i);
            settIntent.putExtras(extras);
            settIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent settPIntent = PendingIntent.getActivity(context, i, settIntent, 0);

            Intent intent = new Intent(context, MainActivity.class);
            intent.setAction(Constants.TRAFFIC_TAP);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, i, intent, 0);

            int dim = Integer.parseInt(Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[11], Constants.ICON_SIZE)));
            int dims = Integer.parseInt(Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[17], Constants.ICON_SIZE)));

            String sizestr = prefs.getString(Constants.PREF_WIDGET_TRAFFIC[12], Constants.TEXT_SIZE);
            String sizestrs = prefs.getString(Constants.PREF_WIDGET_TRAFFIC[16], Constants.TEXT_SIZE);

            RemoteViews updateViews = new RemoteViews(context.getPackageName(), R.layout.traffic_info_widget);
            boolean[] isNight =  CustomApplication.getIsNightState();

            //SIM1
            if ((prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true) && !prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false))
                    || (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false) && bundle.getInt(Constants.SIM_ACTIVE) == Constants.SIM1)) {
                String text;
                String limit = isNight[0] ? prefsSIM.getString(Constants.PREF_SIM1[18], "") : prefsSIM.getString(Constants.PREF_SIM1[1], "");
                String round = isNight[0] ? prefsSIM.getString(Constants.PREF_SIM1[22], "") : prefsSIM.getString(Constants.PREF_SIM1[4], "0");
                int value;
                if (Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM1[2], "")).equals(""))
                    value = 0;
                else
                    value = isNight[0] ? Integer.valueOf(Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM1[19], ""))) :
                            Integer.valueOf(Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM1[2], "")));
                float valuer;
                long lim = 0;
                if (limit != null && !limit.equals("")) {
                    if (round != null) {
                        valuer = 1 - Float.valueOf(round) / 100;
                        lim = (long) (valuer * DataFormat.getFormatLong(limit, value));
                    }
                }
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")).equals("0")) {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[25], "1")).equals("0"))
                        text = DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.TOTAL1_N, 0) :
                                bundle.getLong(Constants.TOTAL1, 0));
                    else {
                        if (lim == 0)
                            text = context.getString(R.string.not_set);
                        else
                            text = DataFormat.formatData(context, lim);
                    }
                } else {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[24], "1")).equals("0")) {
                        long tot = isNight[0] ? (lim - bundle.getLong(Constants.TOTAL1_N, 0)) :
                                (lim - bundle.getLong(Constants.TOTAL1, 0));
                        if (tot < 0)
                            tot = 0;
                        text = DataFormat.formatData(context, tot);
                    } else
                        text = "-" + DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.TOTAL1_N, 0) :
                                bundle.getLong(Constants.TOTAL1, 0));
                }
                updateViews.setTextViewText(R.id.totSIM1, text);
                updateViews.setViewVisibility(R.id.txSIM1, View.GONE);
                updateViews.setViewVisibility(R.id.rxSIM1, View.GONE);
                updateViews.setViewVisibility(R.id.vert11, View.GONE);
                updateViews.setViewVisibility(R.id.vert12, View.GONE);
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")).equals("0")) {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[25], "0")).equals("0")) {
                        updateViews.setInt(R.id.totSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[29], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setInt(R.id.txSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(context, android.R.color.holo_green_dark)));
                        updateViews.setInt(R.id.rxSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(context, android.R.color.holo_orange_dark)));
                        updateViews.setTextViewText(R.id.txSIM1, DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.SIM1TX_N, 0) :
                                bundle.getLong(Constants.SIM1TX, 0)));
                        updateViews.setTextViewText(R.id.rxSIM1, DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.SIM1RX_N, 0) :
                                bundle.getLong(Constants.SIM1RX, 0)));
                    } else {
                        updateViews.setInt(R.id.txSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setInt(R.id.rxSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setTextViewText(R.id.txSIM1, DataFormat.formatData(context, isNight[0] ? bundle.getLong(Constants.TOTAL1_N, 0) :
                                bundle.getLong(Constants.TOTAL1, 0)));
                        if (lim == 0)
                            updateViews.setTextViewText(R.id.rxSIM1, context.getString(R.string.not_available));
                        else {
                            long rest = isNight[0] ? (lim - bundle.getLong(Constants.TOTAL1_N, 0)) :
                                    (lim - bundle.getLong(Constants.TOTAL1, 0));
                            if (rest < 0) {
                                if (!prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true))
                                    rest = 0;
                                updateViews.setInt(R.id.totSIM1, "setTextColor", ContextCompat.getColor(context, android.R.color.holo_red_dark));
                            } else
                                updateViews.setInt(R.id.totSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[29], ContextCompat.getColor(context, R.color.widget_text)));
                            updateViews.setTextViewText(R.id.rxSIM1, DataFormat.formatData(context, rest));
                        }
                    }
                    updateViews.setViewVisibility(R.id.txSIM1, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.rxSIM1, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert11, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert12, View.VISIBLE);
                }

                String title1;
                updateViews.setViewVisibility(R.id.operSIM1, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true)) {
                    if (bundle.getString(Constants.OPERATOR1, "").equals(""))
                        title1 = "SIM1";
                    else
                        title1 = bundle.getString(Constants.OPERATOR1, "");
                    updateViews.setViewVisibility(R.id.operSIM1, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM1, title1);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[8], false))
                        Picasso.get()
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[5], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});
                    else
                        Picasso.get()
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[5], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo1, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM1, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo1, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM1, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM1, settPIntent);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false) && prefsSIM.getBoolean(Constants.PREF_SIM1[17], false)) {
                    if (!isNight[0])
                        Picasso.get()
                                .load(R.drawable.day)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1_n, new int[]{i});
                    else
                        Picasso.get()
                                .load(R.drawable.night)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo1_n, new int[]{i});
                    updateViews.setViewVisibility(R.id.logo1_n, View.VISIBLE);
                } else
                    updateViews.setViewVisibility(R.id.logo1_n, View.GONE);
                if (sizestr != null && !sizestr.equals("")) {
                    updateViews.setFloat(R.id.txSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.rxSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.txSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.rxSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.totSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM1, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.operSIM1, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout1, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], true)) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true))
                        updateViews.setViewVisibility(R.id.stub1, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub1, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub1, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout1, View.GONE);
                updateViews.setViewVisibility(R.id.stub1, View.GONE);
            }

            //SIM2
            if ((prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true) && !prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false))
                    || (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false) && bundle.getInt(Constants.SIM_ACTIVE) == Constants.SIM2)) {
                String text;
                String limit = isNight[1] ? prefsSIM.getString(Constants.PREF_SIM2[18], "") : prefsSIM.getString(Constants.PREF_SIM2[1], "");
                String round = isNight[1] ? prefsSIM.getString(Constants.PREF_SIM2[22], "") : prefsSIM.getString(Constants.PREF_SIM2[4], "0");
                int value;
                if (Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM2[2], "")).equals(""))
                    value = 0;
                else
                    value = isNight[1] ? Integer.valueOf(Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM2[19], ""))) :
                            Integer.valueOf(Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM2[2], "")));
                float valuer;
                long lim = 0;
                if (limit != null && !limit.equals("")) {
                    if (round != null) {
                        valuer = 1 - Float.valueOf(round) / 100;
                        lim = (long) (valuer * DataFormat.getFormatLong(limit, value));
                    }
                }
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")).equals("0")) {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[25], "1")).equals("0"))
                        text = DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.TOTAL2_N, 0) :
                                bundle.getLong(Constants.TOTAL2, 0));
                    else {
                        if (lim == 0)
                            text = context.getString(R.string.not_set);
                        else
                            text = DataFormat.formatData(context, lim);
                    }
                } else {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[24], "1")).equals("0")) {
                        long tot = isNight[1] ? (lim - bundle.getLong(Constants.TOTAL2_N, 0)) :
                                (lim - bundle.getLong(Constants.TOTAL2, 0));
                        if (tot < 0)
                            tot = 0;
                        text = DataFormat.formatData(context, tot);
                    } else
                        text = "-" + DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.TOTAL2_N, 0) :
                                bundle.getLong(Constants.TOTAL2, 0));
                }
                updateViews.setTextViewText(R.id.totSIM2, text);
                updateViews.setViewVisibility(R.id.txSIM2, View.GONE);
                updateViews.setViewVisibility(R.id.rxSIM2, View.GONE);
                updateViews.setViewVisibility(R.id.vert21, View.GONE);
                updateViews.setViewVisibility(R.id.vert22, View.GONE);
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")).equals("0")) {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[25], "0")).equals("0")) {
                        updateViews.setInt(R.id.totSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[29], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setInt(R.id.txSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(context, android.R.color.holo_green_dark)));
                        updateViews.setInt(R.id.rxSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(context, android.R.color.holo_orange_dark)));
                        updateViews.setTextViewText(R.id.txSIM2, DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.SIM2TX_N, 0) :
                                bundle.getLong(Constants.SIM2TX, 0)));
                        updateViews.setTextViewText(R.id.rxSIM2, DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.SIM2RX_N, 0) :
                                bundle.getLong(Constants.SIM2RX, 0)));
                    } else {
                        updateViews.setInt(R.id.txSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setInt(R.id.rxSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setTextViewText(R.id.txSIM2, DataFormat.formatData(context, isNight[1] ? bundle.getLong(Constants.TOTAL2_N, 0) :
                                bundle.getLong(Constants.TOTAL2, 0)));
                        if (lim == 0)
                            updateViews.setTextViewText(R.id.rxSIM2, context.getString(R.string.not_available));
                        else {
                            long rest = isNight[1] ? (lim - bundle.getLong(Constants.TOTAL2_N, 0)) :
                                    (lim - bundle.getLong(Constants.TOTAL2, 0));
                            if (rest < 0) {
                                if (!prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true))
                                    rest = 0;
                                updateViews.setInt(R.id.totSIM2, "setTextColor", ContextCompat.getColor(context, android.R.color.holo_red_dark));
                            } else
                                updateViews.setInt(R.id.totSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[29], ContextCompat.getColor(context, R.color.widget_text)));
                            updateViews.setTextViewText(R.id.rxSIM2, DataFormat.formatData(context, rest));
                        }
                    }
                    updateViews.setViewVisibility(R.id.txSIM2, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.rxSIM2, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert21, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert22, View.VISIBLE);
                }
                String title2;
                updateViews.setViewVisibility(R.id.operSIM2, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true)) {
                    if (bundle.getString(Constants.OPERATOR2, "").equals(""))
                        title2 = "SIM2";
                    else
                        title2 = bundle.getString(Constants.OPERATOR2);
                    updateViews.setViewVisibility(R.id.operSIM2, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM2, title2);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[9], false))
                        Picasso.get()
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[6], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});
                    else
                        Picasso.get()
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[6], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo2, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM2, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo2, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM2, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM2, settPIntent);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false) && prefsSIM.getBoolean(Constants.PREF_SIM2[17], false)) {
                    if (!isNight[1])
                        Picasso.get()
                                .load(R.drawable.day)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2_n, new int[]{i});
                    else
                        Picasso.get()
                                .load(R.drawable.night)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo2_n, new int[]{i});
                updateViews.setViewVisibility(R.id.logo2_n, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.logo2_n, View.GONE);
            if (sizestr != null && !sizestr.equals("")) {
                    updateViews.setFloat(R.id.txSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.rxSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.txSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.rxSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.totSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM2, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.operSIM2, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout2, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], true) ||
                        (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true) && !prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false))) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true))
                        updateViews.setViewVisibility(R.id.stub2, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub2, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub2, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout2, View.GONE);
                updateViews.setViewVisibility(R.id.stub2, View.GONE);
            }

            //SIM3
            if ((prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[20], true) && !prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false))
                    || (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false) && bundle.getInt(Constants.SIM_ACTIVE) == Constants.SIM3)) {
                String text;
                String limit = isNight[2] ? prefsSIM.getString(Constants.PREF_SIM3[18], "") : prefsSIM.getString(Constants.PREF_SIM3[1], "");
                String round = isNight[2] ? prefsSIM.getString(Constants.PREF_SIM3[22], "") : prefsSIM.getString(Constants.PREF_SIM3[4], "0");
                int value;
                if (Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM3[2], "")).equals(""))
                    value = 0;
                else
                    value = isNight[2] ? Integer.valueOf(Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM3[19], ""))) :
                            Integer.valueOf(Objects.requireNonNull(prefsSIM.getString(Constants.PREF_SIM3[2], "")));
                float valuer;
                long lim;
                if (!Objects.requireNonNull(limit).equals("")) {
                    valuer = 1 - Float.valueOf(Objects.requireNonNull(round)) / 100;
                    lim = (long) (valuer * DataFormat.getFormatLong(limit, value));
                } else
                    lim = 0;
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")).equals("0")) {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[25], "1")).equals("0"))
                        text = DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.TOTAL3_N, 0) :
                                bundle.getLong(Constants.TOTAL3, 0));
                    else {
                        if (lim == 0)
                            text = context.getString(R.string.not_set);
                        else
                            text = DataFormat.formatData(context, lim);
                    }
                } else {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[24], "1")).equals("0")) {
                        long tot = isNight[2] ? (lim - bundle.getLong(Constants.TOTAL3_N, 0)) :
                                (lim - bundle.getLong(Constants.TOTAL3, 0));
                        if (tot < 0)
                            tot = 0;
                        text = DataFormat.formatData(context, tot);
                    } else
                        text = "-" + DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.TOTAL3_N, 0) :
                                bundle.getLong(Constants.TOTAL3, 0));
                }
                updateViews.setTextViewText(R.id.totSIM3, text);
                updateViews.setViewVisibility(R.id.txSIM3, View.GONE);
                updateViews.setViewVisibility(R.id.rxSIM3, View.GONE);
                updateViews.setViewVisibility(R.id.vert31, View.GONE);
                updateViews.setViewVisibility(R.id.vert32, View.GONE);
                if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[2], "0")).equals("0")) {
                    if (Objects.requireNonNull(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[25], "0")).equals("0")) {
                        updateViews.setInt(R.id.totSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[29], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setInt(R.id.txSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, android.R.color.holo_green_dark)));
                        updateViews.setInt(R.id.rxSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, android.R.color.holo_orange_dark)));
                        updateViews.setTextViewText(R.id.txSIM3, DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.SIM3TX_N, 0) :
                                bundle.getLong(Constants.SIM3TX, 0)));
                        updateViews.setTextViewText(R.id.rxSIM3, DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.SIM3RX_N, 0) :
                                bundle.getLong(Constants.SIM3RX, 0)));
                    } else {
                        updateViews.setInt(R.id.txSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[27], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setInt(R.id.rxSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[28], ContextCompat.getColor(context, R.color.widget_text)));
                        updateViews.setTextViewText(R.id.txSIM3, DataFormat.formatData(context, isNight[2] ? bundle.getLong(Constants.TOTAL3_N, 0) :
                                bundle.getLong(Constants.TOTAL3, 0)));
                        if (lim == 0)
                            updateViews.setTextViewText(R.id.rxSIM3, context.getString(R.string.not_available));
                        else {
                            long rest = isNight[2] ? (lim - bundle.getLong(Constants.TOTAL3_N, 0)) :
                                    (lim - bundle.getLong(Constants.TOTAL3, 0));
                            if (rest < 0) {
                                if (!prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[26], true))
                                    rest = 0;
                                updateViews.setInt(R.id.totSIM3, "setTextColor", ContextCompat.getColor(context, android.R.color.holo_red_dark));
                            } else
                                updateViews.setInt(R.id.totSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[29], ContextCompat.getColor(context, R.color.widget_text)));
                            updateViews.setTextViewText(R.id.rxSIM3, DataFormat.formatData(context, rest));
                        }

                    }
                    updateViews.setViewVisibility(R.id.txSIM3, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.rxSIM3, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert31, View.VISIBLE);
                    updateViews.setViewVisibility(R.id.vert32, View.VISIBLE);
                }
                String title3;
                updateViews.setViewVisibility(R.id.operSIM3, View.GONE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[1], true)) {
                    if (bundle.getString(Constants.OPERATOR3, "").equals(""))
                        title3 = "SIM3";
                    else
                        title3 = bundle.getString(Constants.OPERATOR3);
                    updateViews.setViewVisibility(R.id.operSIM3, View.VISIBLE);
                    updateViews.setTextViewText(R.id.operSIM3, title3);
                }

                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[4], true)) {
                    if (!prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[10], false))
                        Picasso.get()
                                .load(context.getResources().getIdentifier(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[7], "none"), "drawable", context.getPackageName()))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});
                    else
                        Picasso.get()
                                .load(new File(prefs.getString(Constants.PREF_WIDGET_TRAFFIC[7], "")))
                                .resize(dim, dim)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3, new int[]{i});

                    updateViews.setViewVisibility(R.id.logo3, View.VISIBLE);
                    updateViews.setOnClickPendingIntent(R.id.logo3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM3, pendingIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, pendingIntent);
                } else {
                    updateViews.setViewVisibility(R.id.logo3, View.GONE);
                    updateViews.setOnClickPendingIntent(R.id.operSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.txSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.rxSIM3, settPIntent);
                    updateViews.setOnClickPendingIntent(R.id.totSIM3, settPIntent);
                }
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[23], false) && prefsSIM.getBoolean(Constants.PREF_SIM3[17], false)) {
                    if (!isNight[2])
                        Picasso.get()
                                .load(R.drawable.day)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3_n, new int[]{i});
                    else
                        Picasso.get()
                                .load(R.drawable.night)
                                .resize(dim / 3, dim / 3)
                                .centerInside()
                                .error(R.drawable.none)
                                .into(updateViews, R.id.logo3_n, new int[]{i});
                    updateViews.setViewVisibility(R.id.logo3_n, View.VISIBLE);
                } else
                    updateViews.setViewVisibility(R.id.logo3_n, View.GONE);
                if (!Objects.requireNonNull(sizestr).equals("") && !Objects.requireNonNull(sizestrs).equals("")) {
                    updateViews.setFloat(R.id.txSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.rxSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", Float.parseFloat(sizestr));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", Float.parseFloat(sizestr));
                } else {
                    updateViews.setFloat(R.id.txSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.rxSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.totSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.operSIM3, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.operSIM3, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.simLayout3, View.VISIBLE);
                if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], true) || ((prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[18], true) ||
                        prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[19], true)) && !prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[22], false))) {
                    if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[21], true))
                        updateViews.setViewVisibility(R.id.stub3, View.VISIBLE);
                    else
                        updateViews.setViewVisibility(R.id.stub3, View.GONE);
                } else
                    updateViews.setViewVisibility(R.id.stub3, View.GONE);
            } else {
                updateViews.setViewVisibility(R.id.simLayout3, View.GONE);
                updateViews.setViewVisibility(R.id.stub3, View.GONE);
            }

            //SPEED
            if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[3], false)) {
                updateViews.setViewVisibility(R.id.speedLayout, View.VISIBLE);
                String speedRX = String.format(context.getResources().getString(R.string.speed),
                        DataFormat.formatData(context, bundle.getLong(Constants.SPEEDRX, 0L)));
                String speedTX = String.format(context.getResources().getString(R.string.speed),
                        DataFormat.formatData(context, bundle.getLong(Constants.SPEEDTX, 0L)));

                updateViews.setTextViewText(R.id.tvSpeedRX, speedRX);
                updateViews.setTextViewText(R.id.tvSpeedTX, speedTX);
                updateViews.setOnClickPendingIntent(R.id.tvSpeedRX, settPIntent);
                updateViews.setOnClickPendingIntent(R.id.tvSpeedTX, settPIntent);
                updateViews.setOnClickPendingIntent(R.id.ivRX, settPIntent);
                updateViews.setOnClickPendingIntent(R.id.ivTX, settPIntent);

                Picasso.get()
                        .load(R.drawable.rx_arrow)
                        .resize(dims, dims)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(updateViews, R.id.ivRX, new int[]{i});
                Picasso.get()
                        .load(R.drawable.tx_arrow)
                        .resize(dims, dims)
                        .centerInside()
                        .error(R.drawable.none)
                        .into(updateViews, R.id.ivTX, new int[]{i});

                if (!sizestr.equals("") && !sizestrs.equals("")) {
                    updateViews.setFloat(R.id.tvSpeedRX, "setTextSize", Float.parseFloat(sizestrs));
                    updateViews.setFloat(R.id.tvSpeedTX, "setTextSize", Float.parseFloat(sizestrs));
                } else {
                    updateViews.setFloat(R.id.tvSpeedRX, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                    updateViews.setFloat(R.id.tvSpeedTX, "setTextSize", context.getResources().getDimension(R.dimen.widget_text_size));
                }
                updateViews.setInt(R.id.tvSpeedRX, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setInt(R.id.tvSpeedTX, "setTextColor", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[13], ContextCompat.getColor(context, R.color.widget_text)));
                updateViews.setViewVisibility(R.id.speedLayout, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.speedLayout, View.GONE);

            //BACKGROUND
            if (prefs.getBoolean(Constants.PREF_WIDGET_TRAFFIC[14], true)) {
                updateViews.setInt(R.id.background, "setColorFilter", prefs.getInt(Constants.PREF_WIDGET_TRAFFIC[15], ContextCompat.getColor(context, R.color.background)));
                updateViews.setViewVisibility(R.id.background, View.VISIBLE);
            } else
                updateViews.setViewVisibility(R.id.background, View.GONE);

            //UPDATE
            appWidgetManager.updateAppWidget(i, updateViews);
        }
    }

    @Override
    public final void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        CustomApplication.deleteWidgetPreferenceFile(appWidgetIds, Constants.TRAFFIC_TAG);
        //super.onDeleted(context, appWidgetIds);
    }

    @Override
    public final void onDisabled(Context context) {
        Picasso.get().shutdown();
        //super.onDisabled(context);
    }
}

