package com.mocacad.dailydally;

import android.content.Context;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Receives a JSON snapshot of progress from the web app and refreshes the widgets. */
@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridge extends Plugin {
    @PluginMethod
    public void update(PluginCall call) {
        String data = call.getString("data", "{}");
        Context ctx = getContext();
        ctx.getSharedPreferences(DDWidget.PREFS, Context.MODE_PRIVATE).edit().putString("data", data).apply();
        DDWidget.refreshAll(ctx);
        call.resolve();
    }
}
