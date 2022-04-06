package ua.od.acros.dualsimtrafficcounter.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import androidx.preference.DialogPreference;
import android.util.AttributeSet;

import java.util.Locale;

public class TimePreference extends DialogPreference {

    public int mHour = 0;
    public int mMinute = 0;
    private static Locale mCurrentLocale;

    private static int parseHour(String value) {
        try {
            String[] time = value.split(":");
            return (Integer.parseInt(time[0]));
        } catch (Exception e) {
            return 0;
        }
    }

    private static int parseMinute(String value) {
        try {
            String[] time = value.split(":");
            return (Integer.parseInt(time[1]));
        } catch (Exception e) {
            return 0;
        }
    }

    public static String timeToString(int h, int m) {
        return String.format(mCurrentLocale, "%02d", h) + ":" + String.format(mCurrentLocale, "%02d", m);
    }

    public TimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (mCurrentLocale == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                mCurrentLocale = context.getResources().getConfiguration().getLocales().get(0);
            else
                mCurrentLocale = context.getResources().getConfiguration().locale;
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String value;
        if (restoreValue) {
            if (defaultValue == null)
                value = getPersistedString("00:00");
            else
                value = getPersistedString(defaultValue.toString());
        } else {
            value = defaultValue.toString();
        }
        mHour = parseHour(value);
        mMinute = parseMinute(value);
    }

    public void persistStringValue(String value) {
        persistString(value);
    }
}