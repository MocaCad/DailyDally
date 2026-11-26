package com.mocacad.dailydally;

import android.content.Context;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/** Links the web app and the home-screen widgets. */
@CapacitorPlugin(name = "WidgetBridge")
public class WidgetBridge extends Plugin {
    /** Set when the widget's ▶ button opened the app; the web app reads it once. */
    static volatile boolean pendingStart = false;

    @PluginMethod
    public void update(PluginCall call) {
        String data = call.getString("data", "{}");
        Context ctx = getContext();
        ctx.getSharedPreferences(DDWidget.PREFS, Context.MODE_PRIVATE).edit().putString("data", data).apply();
        DDWidget.refreshAll(ctx);
        call.resolve();
    }

    @PluginMethod
    public void consumeStart(PluginCall call) {
        JSObject result = new JSObject();
        result.put("start", pendingStart);
        pendingStart = false;
        call.resolve(result);
    }
}
