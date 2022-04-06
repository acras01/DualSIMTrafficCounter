package ua.od.acros.dualsimtrafficcounter.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoLineListPreference extends androidx.preference.ListPreference {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TwoLineListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TwoLineListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoLineListPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView textView = (TextView) holder.findViewById(android.R.id.title);
        if (textView != null) {
            textView.setSingleLine(false);
        }
    }
}
