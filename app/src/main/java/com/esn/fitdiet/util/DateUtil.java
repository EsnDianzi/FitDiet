package com.esn.fitdiet.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Date helpers using yyyy-MM-dd keys. */
public final class DateUtil {

    private static final String FMT = "yyyy-MM-dd";

    private DateUtil() { }

    public static String today() {
        return format(new Date());
    }

    public static String toDate(long millis) {
        return format(new Date(millis));
    }

    public static String format(Date d) {
        return new SimpleDateFormat(FMT, Locale.CHINA).format(d);
    }

    public static String yesterday() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, -1);
        return format(c.getTime());
    }

    /** Returns the last N dates ending today, oldest first. */
    public static String[] lastDays(int n) {
        String[] out = new String[n];
        Calendar c = Calendar.getInstance();
        for (int i = n - 1; i >= 0; i--) {
            c.setTime(new Date());
            c.add(Calendar.DAY_OF_MONTH, -i);
            out[n - 1 - i] = format(c.getTime());
        }
        return out;
    }

    /** 本周一对应的 yyyy-MM-dd（中国周首日 = 周一）。 */
    public static String startOfWeek() {
        Calendar c = Calendar.getInstance();
        // Calendar.DAY_OF_WEEK: 周日=1, 周一=2, ..., 周六=7
        int dow = c.get(Calendar.DAY_OF_WEEK);
        int delta = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
        c.add(Calendar.DAY_OF_MONTH, -delta);
        return format(c.getTime());
    }
}
