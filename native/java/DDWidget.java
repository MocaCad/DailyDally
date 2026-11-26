package com.mocacad.dailydally;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeMap;
import org.json.JSONArray;
import org.json.JSONObject;

/** Draws the three home-screen widgets from the last snapshot the app saved. */
public final class DDWidget {
    static final String PREFS = "dailydally_widget";
    private static final int WHITE = 0xFFF2F1EC, GREY = 0xFF9AA0AB, ORANGE = 0xFFF5A524, RED = 0xFFFF6B5B,
            GREEN = 0xFF58CC02, BLUE = 0xFF60A5FA, YELLOW = 0xFFFCE42A, DARK = 0xFF1A1700;
    private static final int[] LABELS = {R.id.l0, R.id.l1, R.id.l2, R.id.l3, R.id.l4, R.id.l5, R.id.l6};
    private static final int[] CIRCLES = {R.id.c0, R.id.c1, R.id.c2, R.id.c3, R.id.c4, R.id.c5, R.id.c6};
    private static final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private DDWidget() {}

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : m.getAppWidgetIds(new ComponentName(c, DDWidgetLarge.class))) renderUpcoming(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, DDWidgetWeek.class))) renderWeek(c, m, id);
        for (int id : m.getAppWidgetIds(new ComponentName(c, DDWidgetSmall.class))) renderSmall(c, m, id);
    }

    /* ---------- shared ---------- */
    private static JSONObject data(Context c) {
        try {
            return new JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", "{}"));
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private static String iso(Calendar cal) { return ISO.format(cal.getTime()); }

    private static String label(String isoDate, String pattern) {
        try {
            Date d = ISO.parse(isoDate);
            return new SimpleDateFormat(pattern, Locale.getDefault()).format(d);
        } catch (Exception e) {
            return isoDate;
        }
    }

    private static String dayAfter(String isoDate) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(ISO.parse(isoDate));
            cal.add(Calendar.DAY_OF_MONTH, 1);
            return iso(cal);
        } catch (Exception e) {
            return isoDate;
        }
    }

    static PendingIntent openApp(Context c, boolean startLesson) {
        Intent i = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (i == null) return null;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (startLesson) {
            i.setAction("com.mocacad.dailydally.START_LESSON");
            i.putExtra("dd_start", true);
        }
        return PendingIntent.getActivity(c, startLesson ? 1 : 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static final class Day {
        String today, tomorrow;
        boolean friday, paused, done, empty;
        int streak;
        String pTo;
    }

    private static Day day(JSONObject d) {
        Day s = new Day();
        Calendar cal = Calendar.getInstance();
        s.today = iso(cal);
        s.friday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;
        cal.add(Calendar.DAY_OF_MONTH, 1);
        s.tomorrow = iso(cal);
        String pFrom = d.optString("pFrom", "");
        s.pTo = d.optString("pTo", "");
        s.paused = !pFrom.isEmpty() && !s.pTo.isEmpty() && s.today.compareTo(pFrom) >= 0 && s.today.compareTo(s.pTo) <= 0;
        s.done = s.today.equals(d.optString("last", ""));
        s.streak = d.optInt("streak", 0);
        s.empty = d.length() == 0;
        return s;
    }

    private static String statusText(Day s) {
        if (s.empty) return "Open DailyDally to start";
        if (s.paused) return "💤 Paused for exams";
        if (s.done) return "✓ Streak safe today";
        if (s.friday) return "❄ Rest day, streak frozen";
        return "⚠ Streak at risk! Do a lesson";
    }

    private static int statusColor(Day s) {
        if (s.empty) return YELLOW;
        if (s.paused || (s.friday && !s.done)) return BLUE;
        return s.done ? GREEN : RED;
    }

    /* ---------- Upcoming list (large) ---------- */
    static void renderUpcoming(Context c, AppWidgetManager m, int id) {
        JSONObject d = data(c);
        Day s = day(d);
        String pkg = c.getPackageName();
        RemoteViews v = new RemoteViews(pkg, R.layout.widget_upcoming);
        v.setTextViewText(R.id.u_streak, "🔥 " + s.streak);
        v.removeAllViews(R.id.u_list);

        List<JSONObject> overdue = new ArrayList<>(), today = new ArrayList<>();
        TreeMap<String, List<JSONObject>> later = new TreeMap<>();
        JSONArray rows = d.optJSONArray("rows");
        if (rows != null) {
            for (int i = 0; i < rows.length(); i++) {
                JSONObject r = rows.optJSONObject(i);
                if (r == null) continue;
                String due = r.optString("d", s.today);
                if (r.optBoolean("done", false)) {
                    if (s.today.equals(r.optString("on", ""))) today.add(r);
                } else if (due.compareTo(s.today) < 0) {
                    overdue.add(r);
                } else if (due.equals(s.today)) {
                    today.add(r);
                } else {
                    if (!later.containsKey(due)) later.put(due, new ArrayList<JSONObject>());
                    later.get(due).add(r);
                }
            }
        }
        boolean todayHasOpen = false;
        for (JSONObject r : today) if (!r.optBoolean("done", false)) todayHasOpen = true;
        // Nothing planned today but the streak still needs a lesson: pull the next one forward.
        if (!todayHasOpen && !s.done && !s.paused && !s.friday && overdue.isEmpty() && !later.isEmpty()) {
            String first = later.firstKey();
            List<JSONObject> l = later.get(first);
            today.add(l.remove(0));
            if (l.isEmpty()) later.remove(first);
        }

        int budget = 7;
        if (s.empty) {
            addNote(c, v, "Open DailyDally once to load your path.");
        } else {
            if (s.paused) budget = addHeader(c, v, "💤 Paused until " + label(dayAfter(s.pTo), "EEE, d MMM"), "", GREY, budget);
            if (!overdue.isEmpty()) {
                budget = addHeader(c, v, "Overdue", "Catch up", WHITE, budget);
                for (JSONObject r : overdue) budget = addItem(c, v, r, true, budget);
            }
            budget = addHeader(c, v, label(s.today, "EEEE, d MMM") + "  •  Today", "", today.isEmpty() ? GREY : WHITE, budget);
            for (JSONObject r : today) budget = addItem(c, v, r, false, budget);
            if (today.isEmpty() && s.friday && !s.paused && budget > 0) { addNote(c, v, "❄ Rest day. Your streak is frozen."); budget--; }
            for (String date : later.keySet()) {
                String title = date.equals(s.tomorrow) ? label(date, "EEEE, d MMM") + "  •  Tomorrow" : label(date, "EEEE, d MMM");
                budget = addHeader(c, v, title, "", WHITE, budget);
                for (JSONObject r : later.get(date)) budget = addItem(c, v, r, false, budget);
                if (budget <= 0) break;
            }
        }
        PendingIntent open = openApp(c, false), start = openApp(c, true);
        if (open != null) v.setOnClickPendingIntent(R.id.u_root, open);
        if (start != null) v.setOnClickPendingIntent(R.id.u_play, start);
        m.updateAppWidget(id, v);
    }

    private static int addHeader(Context c, RemoteViews v, String title, String action, int color, int budget) {
        if (budget <= 0) return budget;
        RemoteViews h = new RemoteViews(c.getPackageName(), R.layout.dd_row_header);
        h.setTextViewText(R.id.h_title, title);
        h.setTextColor(R.id.h_title, color);
        h.setTextViewText(R.id.h_action, action);
        v.addView(R.id.u_list, h);
        return budget - 1;
    }

    private static int addItem(Context c, RemoteViews v, JSONObject r, boolean overdue, int budget) {
        if (budget <= 0) return budget;
        boolean done = r.optBoolean("done", false);
        int mins = r.optInt("m", 30);
        RemoteViews it = new RemoteViews(c.getPackageName(), R.layout.dd_row_item);
        it.setTextViewText(R.id.i_title, r.optString("t", "Lesson"));
        it.setTextColor(R.id.i_title, done ? GREY : WHITE);
        String sub = overdue ? "⏱ " + label(r.optString("d", ""), "d MMM") + " · " + mins + " min" : "⏱ " + mins + " min · +" + mins + " XP";
        it.setTextViewText(R.id.i_sub, done ? "✓ Done · +" + mins + " XP" : sub);
        it.setTextColor(R.id.i_sub, done ? GREEN : overdue ? RED : ORANGE);
        it.setTextViewText(R.id.i_tag, "Unit " + r.optInt("u", 1) + " #");
        if (done) {
            it.setInt(R.id.i_check, "setBackgroundResource", R.drawable.dd_check_done);
            it.setTextViewText(R.id.i_check, "✓");
        }
        PendingIntent pi = openApp(c, !done);
        if (pi != null) it.setOnClickPendingIntent(R.id.i_root, pi);
        v.addView(R.id.u_list, it);
        return budget - 1;
    }

    private static void addNote(Context c, RemoteViews v, String text) {
        RemoteViews n = new RemoteViews(c.getPackageName(), R.layout.dd_row_note);
        n.setTextViewText(R.id.n_text, text);
        v.addView(R.id.u_list, n);
    }

    /* ---------- Week strip ---------- */
    static void renderWeek(Context c, AppWidgetManager m, int id) {
        JSONObject d = data(c);
        Day s = day(d);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_week);
        Set<String> active = new HashSet<>();
        JSONArray a = d.optJSONArray("active");
        if (a != null) for (int i = 0; i < a.length(); i++) active.add(a.optString(i));
        String pFrom = d.optString("pFrom", ""), pTo = d.optString("pTo", "");

        Calendar cal = Calendar.getInstance();
        int back = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7; // days since Monday
        cal.add(Calendar.DAY_OF_MONTH, -back);
        for (int i = 0; i < 7; i++) {
            String dIso = iso(cal);
            boolean isToday = dIso.equals(s.today), past = dIso.compareTo(s.today) < 0;
            boolean freeze = i == 4 || (!pFrom.isEmpty() && dIso.compareTo(pFrom) >= 0 && dIso.compareTo(pTo) <= 0);
            int bg, fg = WHITE;
            if (active.contains(dIso)) { bg = R.drawable.dd_day_done; fg = DARK; }
            else if (isToday) bg = R.drawable.dd_day_today;
            else if (past && freeze) bg = R.drawable.dd_day_freeze;
            else if (past) { bg = R.drawable.dd_day_past; fg = GREY; }
            else bg = R.drawable.dd_day_future;
            v.setInt(CIRCLES[i], "setBackgroundResource", bg);
            v.setTextViewText(CIRCLES[i], String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
            v.setTextColor(CIRCLES[i], fg);
            v.setTextColor(LABELS[i], isToday ? WHITE : GREY);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        String line;
        JSONObject next = null;
        JSONArray rows = d.optJSONArray("rows");
        if (rows != null) for (int i = 0; i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r != null && !r.optBoolean("done", false)) { next = r; break; }
        }
        if (s.empty) line = "Today  |  Open DailyDally";
        else if (s.paused) line = "Today  |  Exams, path paused 💤";
        else if (next == null) line = "Today  |  Path complete 🏆";
        else line = "Today  |  " + next.optString("t", "Next lesson") + " — " + next.optInt("m", 30) + " min";
        v.setTextViewText(R.id.w_line, line);
        v.setTextViewText(R.id.w_status, "🔥 " + s.streak + "   " + statusText(s));
        v.setTextColor(R.id.w_status, statusColor(s));
        PendingIntent open = openApp(c, !s.done && !s.paused && next != null);
        if (open != null) v.setOnClickPendingIntent(R.id.w_root, open);
        m.updateAppWidget(id, v);
    }

    /* ---------- Streak (small) ---------- */
    static void renderSmall(Context c, AppWidgetManager m, int id) {
        JSONObject d = data(c);
        Day s = day(d);
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_small);
        v.setTextViewText(R.id.s_streak, "🔥 " + s.streak);
        v.setTextViewText(R.id.s_status, statusText(s));
        v.setTextColor(R.id.s_status, statusColor(s));
        PendingIntent open = openApp(c, false);
        if (open != null) v.setOnClickPendingIntent(R.id.s_root, open);
        m.updateAppWidget(id, v);
    }
}
