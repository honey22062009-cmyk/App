package com.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Standalone test-APK browser window, using only platform Java classes. Never ships. */
public class BrowserFixtureActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        EditText address = new EditText(this);
        address.setId(com.example.test.R.id.qa_browser_url_bar);
        address.setContentDescription("Web address");
        String value = getIntent().getStringExtra("address");
        address.setText(value == null ? "https://example.com" : value);
        layout.addView(address);
        TextView privateLabel = new TextView(this);
        privateLabel.setText("New private tab · You've gone incognito");
        privateLabel.setTextSize(24);
        layout.addView(privateLabel);
        TextView page = new TextView(this);
        page.setText("Ordinary page copy can mention porn or sex without closing this browser.");
        layout.addView(page);
        setContentView(layout);
        layout.setFocusableInTouchMode(true);
        layout.requestFocus();
    }
}
