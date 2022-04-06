package ua.od.acros.dualsimtrafficcounter.settings;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

import ua.od.acros.dualsimtrafficcounter.R;
import ua.od.acros.dualsimtrafficcounter.activities.CallsWidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.activities.TrafficWidgetConfigActivity;
import ua.od.acros.dualsimtrafficcounter.utils.Constants;
import ua.od.acros.dualsimtrafficcounter.utils.CustomApplication;


public class WidgetsFragment extends Fragment implements View.OnClickListener {

    private Context mContext;
    private ArrayList<TextView> mTraffic, mCalls;

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = CustomApplication.getAppContext();
    }

    @Override
    public final View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Objects.requireNonNull(getActivity()).setTitle(R.string.widgets_title);
        View view = inflater.inflate(R.layout.widgets_fragment, container, false);
        LinearLayoutCompat widgets = view.findViewById(R.id.widgets_layout);
        LinearLayoutCompat.LayoutParams lp = new LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.MATCH_PARENT,
                LinearLayoutCompat.LayoutParams.WRAP_CONTENT);
        lp.setMargins(5, 50, 0, 0);
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getActivity().getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        TypedArray arr = getActivity().obtainStyledAttributes(typedValue.data, new int[]{
                android.R.attr.textColorPrimary});
        int primaryColor = arr.getColor(0, -1);
        int[] ids = CustomApplication.getWidgetIds(Constants.TRAFFIC);
        if (ids.length != 0) {
            mTraffic = new ArrayList<>();
            TextView traffic = new TextView(mContext);
            setAppearance(traffic);
            traffic.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            traffic.setText(R.string.traffic);
            traffic.setAllCaps(true);
            traffic.setLayoutParams(lp);
            widgets.addView(traffic);
            for (int id : ids) {
                TextView tv = new TextView(mContext);
                tv.setId(id);
                setAppearance(tv);
                tv.setTextColor(primaryColor);
                tv.setText(String.format(getString(R.string.widget), id));
                lp.setMargins(5, 50, 0, 0);
                tv.setLayoutParams(lp);
                tv.setOnClickListener(this);
                widgets.addView(tv);
                mTraffic.add(tv);
            }
        }
        ids = CustomApplication.getWidgetIds(Constants.CALLS);
        if (ids.length != 0) {
            mCalls = new ArrayList<>();
            TextView calls = new TextView(mContext);
            setAppearance(calls);
            calls.setAllCaps(true);
            calls.setTextColor(ContextCompat.getColor(mContext, R.color.colorAccent));
            calls.setText(R.string.calls);
            calls.setLayoutParams(lp);
            widgets.addView(calls);
            for (int id : ids) {
                TextView tv = new TextView(mContext);
                tv.setId(id);
                setAppearance(tv);
                tv.setTextColor(primaryColor);
                tv.setText(String.format(getString(R.string.widget), id));
                tv.setLayoutParams(lp);
                tv.setOnClickListener(this);
                widgets.addView(tv);
                mCalls.add(tv);
            }
        }
        arr.recycle();
        return view;
    }

    @Override
    public final void onResume() {
        super.onResume();
        ((Toolbar) getActivity().findViewById(R.id.toolbar)).setTitle(R.string.widgets_title);
    }

    @Override
    public final void onClick(View view) {
        if (view instanceof TextView) {
            Class activity = null;
            if (mTraffic != null && mTraffic.contains(view))
            activity = TrafficWidgetConfigActivity.class;
            else if (mCalls != null && mCalls.contains(view))
                activity = CallsWidgetConfigActivity.class;
            if (activity != null) {
                Intent settIntent = new Intent(mContext, activity);
                Bundle extras = new Bundle();
                extras.putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, view.getId());
                settIntent.putExtras(extras);
                settIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mContext.startActivity(settIntent);
            }
        }
    }

    private void setAppearance(TextView textView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            textView.setTextAppearance(android.R.style.TextAppearance_Medium);
        else
            textView.setTextAppearance(mContext, android.R.style.TextAppearance_Medium);
    }
}
