package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceViewHolder;
import androidx.appcompat.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.widget.TextView;

public class TwoLineEditTextPreference extends EditTextPreference {

    private final AppCompatEditText mEditText;

    public TwoLineEditTextPreference(Context context) {
        this(context, null);
    }

    public TwoLineEditTextPreference(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.preference.R.attr.editTextPreferenceStyle);
    }

    public TwoLineEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TwoLineEditTextPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mEditText = new AppCompatEditText(context, attrs);
        mEditText.setId(android.R.id.edit);
    }

    public AppCompatEditText getEditText() {
        return mEditText;
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
