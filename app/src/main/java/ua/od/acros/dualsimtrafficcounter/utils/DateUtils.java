package ua.od.acros.dualsimtrafficcounter.utils;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;

import java.util.Objects;

import static java.lang.Integer.parseInt;

public class DateUtils {

    public static boolean isNextDayOrMonth(LocalDateTime date, String period) {
        LocalDateTime now = new DateTime().withTimeAtStartOfDay().toLocalDateTime();
        switch (period) {
            case "0":
                return now.getDayOfYear() != date.getDayOfYear();
            case "1":
                return now.getMonthOfYear() != date.getMonthOfYear();
            default:
                return false;
        }
    }

/*    @Nullable
    public static DataResetObject getResetDate(SharedPreferences preferences, String[] simPref) {
        LocalDateTime now = new DateTime().toLocalDateTime();
        int delta = parseInt(preferences.getString(simPref[2], "1"));
        LocalDateTime last;
        String date = preferences.getString(simPref[3], "");
        if (!date.equals(""))
            last = Constants.DATE_TIME_FORMATTER.parseLocalDateTime(date);
        else
            last = Constants.DATE_FORMATTER.parseLocalDateTime("1970-01-01");
        String[] time = preferences.getString(simPref[1], "00:00").split(":");
        last = last.withTime(Integer.valueOf(time[0]), Integer.valueOf(time[1]), 0, 0);
        switch (preferences.getString(simPref[0], "")) {
            case "0":
                delta = 1;
                break;
            case "1":
                if (delta >= 28)
                    switch (now.getMonthOfYear()) {
                        case 2:
                            if (now.year().isLeap())
                                delta = 29;
                            else
                                delta = 28;
                            break;
                        case 4:
                        case 6:
                        case 9:
                        case 11:
                            if (delta == 31)
                                delta = 30;
                            break;
                    }
                break;
        }
        int diff = Days.daysBetween(last, now).getDays();
        if (preferences.getString(simPref[0], "").equals("1")) {
            int month = now.getMonthOfYear();
            int daysInMonth = 31;
            switch (last.getMonthOfYear()) {
                case 2:
                    if (last.year().isLeap())
                        daysInMonth = 29;
                    else
                        daysInMonth = 28;
                    break;
                case 4:
                case 6:
                case 9:
                case 11:
                    daysInMonth = 30;
                    break;
            }
            int year = now.getYear();
            if (now.getDayOfMonth() > delta && diff < daysInMonth) {
                month += 1;
                if (month > 12) {
                    month = 1;
                    year += 1;
                }
            }
            date = year + "-" + month + "-" + delta;
            return new DataResetObject(0, Constants.DATE_TIME_FORMATTER.parseLocalDateTime(date + " " + preferences.getString(simPref[1], "00:00")));
        } else {
            int period = 0;
            if (preferences.getString(simPref[0], "").equals("2"))
                period = diff;
            if (diff >= delta) {
                if (preferences.getString(simPref[0], "").equals("2"))
                    period = diff - delta;
                return new DataResetObject(period, Constants.DATE_TIME_FORMATTER.parseLocalDateTime(now.toString(Constants.DATE_FORMATTER) + " " + preferences.getString(simPref[1], "00:00")));
            } else
                return null;
        }
    } */

    @Nullable
    public static LocalDateTime setResetDate(SharedPreferences preferences, String[] simPref) {
        String time = preferences.getString(simPref[1], "00:00");
        LocalDateTime now;
        if (time != null) {
            now = new LocalDateTime()
                    .withHourOfDay(Integer.valueOf(time.split(":")[0]))
                    .withMinuteOfHour(Integer.valueOf(time.split(":")[1]));
            int delta = parseInt(Objects.requireNonNull(preferences.getString(simPref[2], "1")));
            switch (Objects.requireNonNull(preferences.getString(simPref[0], ""))) {
                case "0":
                    delta = 1;
                    return now.plusDays(delta);
                case "1":
                    if (delta >= now.getDayOfMonth())
                        return now.withDayOfMonth(delta);
                    else
                        return now.plusMonths(1).withDayOfMonth(delta);
                case "2":
                    return now.plusDays(delta);
            }
        }
        return null;
    }

    /*public static long getInterval(SharedPreferences sharedPreferences, int sim) {
        long interval;
        DateTime now = new DateTime().withTimeAtStartOfDay();
        String[] pref = new String[Constants.PREF_SIM1.length];
        switch (sim) {
            case Constants.SIM1:
                pref = Constants.PREF_SIM1;
                break;
            case Constants.SIM2:
                pref = Constants.PREF_SIM2;
                break;
            case Constants.SIM3:
                pref = Constants.PREF_SIM3;
                break;
        }
        switch (sharedPreferences.getString(pref[3], "")) {
            default:
            case "0":
                interval = AlarmManager.INTERVAL_DAY;
                break;
            case "1":
                switch (now.getMonthOfYear()) {
                    case 2:
                        if (now.year().isLeap())
                            interval = 29;
                        else
                            interval = 28;
                        break;
                    case 4:
                    case 6:
                    case 9:
                    case 11:
                        interval = 30;
                        break;
                    default:
                        interval = 31;
                }
                interval *= AlarmManager.INTERVAL_DAY;
                break;
            case "2":
                interval = parseInt(sharedPreferences.getString(pref[10], "1")) * AlarmManager.INTERVAL_DAY;
                break;
        }
        return interval;
    }*/
}
