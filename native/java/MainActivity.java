package com.mocacad.dailydally;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Lets the web app send streak/XP data to the home-screen widgets.
        registerPlugin(WidgetBridge.class);
        super.onCreate(savedInstanceState);
    }
}
