package ua.od.acros.dualsimtrafficcounter.utils;

import android.text.InputFilter;
import android.text.Spanned;

import org.acra.ACRA;

public class InputFilterMinMax implements InputFilter {

    private final int minValue;
    private final int maxValue;

    public InputFilterMinMax(int min, int max) {
        this.minValue = min;
        this.maxValue = max;
    }

    public InputFilterMinMax(String min, String max) {
        this.minValue = Integer.parseInt(min);
        this.maxValue = Integer.parseInt(max);
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        try {
            int input = Integer.parseInt(dest.toString() + source.toString());
            if (isInRange(minValue, maxValue, input))
                return null;
        } catch (NumberFormatException nfe) {
            ACRA.getErrorReporter().handleException(nfe);
        }
        return "";
    }

    private boolean isInRange(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }
}
