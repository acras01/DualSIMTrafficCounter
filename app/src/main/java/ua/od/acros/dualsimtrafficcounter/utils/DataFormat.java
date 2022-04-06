package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.Context;

import java.util.Locale;

import ua.od.acros.dualsimtrafficcounter.R;

public class DataFormat {

    private final static double KB = 1024.0;
    private final static double MB = KB * KB;
    private final static double GB = KB * MB;

    public static String formatData(Context mContext, long data) {
        if (Math.abs(data) < MB)
            return (String.format(Locale.getDefault(), "%.2f", data / KB)) + mContext.getString(R.string.kb);
        else if (Math.abs(data) > GB)
            return (String.format(Locale.getDefault(), "%.2f", data / GB)) + mContext.getString(R.string.gb);
        else
            return (String.format(Locale.getDefault(), "%.2f", data / MB)) + mContext.getString(R.string.mb);
    }

    public static long getFormatLong(String data, int value) {
        long res = 0;
        double dData = Double.valueOf(data);
        switch (value) {
            case 0:
                res = (long) (dData * KB);
                break;
            case 1:
                res = (long) (dData * MB);
                break;
            case 2:
                res = (long) (dData * GB);
                break;
    }
    return res;
}

    public static long getRoundLong(long data, String round, String value) {
        double div = Double.valueOf(round);
        switch (value) {
            case "0":
                div = div * KB;
                break;
            case "1":
                div = div * MB;
                break;
            case "2":
                div = div * KB;
                break;
        }
        return (long) (Math.ceil(data / div) * div);
    }

    public static String formatCallDuration(Context context, long millis) {
        long seconds = (long) Math.ceil(millis / 1000);
        if (seconds < 60) {
            return String.format(context.getResources().getString(R.string.seconds), seconds);
        } else {
            long minutes = seconds / 60;
            seconds -= minutes * 60;
            return String.format(context.getResources().getString(R.string.minutes_seconds), minutes, seconds);
        }
    }

    public static long getDuration(String duration, int spinner) {
        long res = 0;
        long dData = Long.valueOf(duration);
        switch (spinner) {
            case 0:
                res = dData * Constants.SECOND;
                break;
            case 1:
                res = dData * Constants.MINUTE;
                break;
        }
        return res;
    }
}
