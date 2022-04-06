package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoLineCheckPreference extends androidx.preference.CheckBoxPreference {

    public TwoLineCheckPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TwoLineCheckPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TwoLineCheckPreference(Context context) {
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
