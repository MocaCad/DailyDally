package com.mocacad.dailydally;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Lets the web app send progress to the home-screen widgets.
        registerPlugin(WidgetBridge.class);
        super.onCreate(savedInstanceState);
        checkStart(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkStart(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        DDWidget.refreshAll(this);
    }

    private void checkStart(Intent intent) {
        if (intent != null && intent.getBooleanExtra("dd_start", false)) {
            WidgetBridge.pendingStart = true;
            intent.removeExtra("dd_start");
        }
    }
}
