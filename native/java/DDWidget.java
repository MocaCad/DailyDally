package com.mocacad.dailydally;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;

/** Draws both home-screen widgets from the last snapshot the app saved. */
public final class DDWidget {
    static final String PREFS = "dailydally_widget";

    private DDWidget() {}

    static void refreshAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        for (int id : m.getAppWidgetIds(new ComponentName(c, DDWidgetLarge.class))) render(c, m, id, true);
        for (int id : m.getAppWidgetIds(new ComponentName(c, DDWidgetSmall.class))) render(c, m, id, false);
    }

    static void render(Context c, AppWidgetManager m, int id, boolean large) {
        JSONObject d;
        try {
            d = new JSONObject(c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("data", "{}"));
        } catch (Exception e) {
            d = new JSONObject();
        }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String pFrom = d.optString("pFrom", ""), pTo = d.optString("pTo", "");
        boolean done = today.equals(d.optString("last", ""));
        boolean paused = !pFrom.isEmpty() && !pTo.isEmpty() && today.compareTo(pFrom) >= 0 && today.compareTo(pTo) <= 0;
        boolean friday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY;

        String status;
        int color;
        if (d.length() == 0) { status = "Open DailyDally to start"; color = 0xFFFCE42A; }
        else if (paused) { status = "💤 Paused for exams"; color = 0xFF60A5FA; }
        else if (done) { status = "✓ Streak safe today"; color = 0xFF58CC02; }
        else if (friday) { status = "❄ Rest day, streak frozen"; color = 0xFF60A5FA; }
        else { status = "⚠ Streak at risk! Do a lesson"; color = 0xFFFF4B4B; }

        RemoteViews v = new RemoteViews(c.getPackageName(), large ? R.layout.widget_large : R.layout.widget_small);
        v.setTextViewText(R.id.w_streak, "🔥 " + d.optInt("streak", 0));
        v.setTextViewText(R.id.w_status, status);
        v.setTextColor(R.id.w_status, color);
        if (large) {
            int mins = d.optInt("mins", 30);
            v.setTextViewText(R.id.w_unit, d.optString("unit", "Unit 1: Python, by hand"));
            v.setTextViewText(R.id.w_next, "Lesson " + d.optInt("nextNum", 1) + ": " + d.optString("next", "Run your first Python program"));
            v.setTextViewText(R.id.w_mins, mins + " min · +" + mins + " XP");
            v.setProgressBar(R.id.w_bar, 100, d.optInt("pct", 0), false);
            v.setTextViewText(R.id.w_pct, d.optInt("done", 0) + "/48 lessons");
            v.setTextViewText(R.id.w_xp, "⚡ " + d.optInt("xp", 0) + " XP · Lv " + d.optInt("level", 1));
            v.setTextViewText(R.id.w_cta, done ? "Keep going ›" : "Start lesson ›");
        }
        Intent open = c.getPackageManager().getLaunchIntentForPackage(c.getPackageName());
        if (open != null) {
            PendingIntent pi = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            v.setOnClickPendingIntent(R.id.w_root, pi);
        }
        m.updateAppWidget(id, v);
    }
}
