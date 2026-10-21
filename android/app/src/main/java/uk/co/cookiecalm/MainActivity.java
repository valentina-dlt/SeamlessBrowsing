package uk.co.cookiecalm;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;
import java.util.HashSet;
import java.util.Set;
import java.text.DateFormat;
import java.util.Date;

public final class MainActivity extends Activity {
    private LinearLayout layout;
    private SharedPreferences prefs() { return getSharedPreferences("cookiecalm", MODE_PRIVATE); }
    private void text(String value, int size) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setPadding(0, 16, 0, 16); layout.addView(v);
    }
    private void button(String value, View.OnClickListener action) {
        Button b = new Button(this); b.setText(value); b.setOnClickListener(action); layout.addView(b);
    }
    private boolean serviceEnabled() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        for (AccessibilityServiceInfo info : manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (info.getResolveInfo() != null && info.getResolveInfo().serviceInfo != null &&
                getPackageName().equals(info.getResolveInfo().serviceInfo.packageName) &&
                info.getResolveInfo().serviceInfo.name.endsWith("CookieService")) return true;
        }
        return false;
    }
    @Override public void onCreate(Bundle state) { super.onCreate(state); }
    @Override public void onResume() { super.onResume(); render(); }
    private void render() {
        ScrollView scroll = new ScrollView(this);
        layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 70, 40, 40); scroll.addView(layout); setContentView(scroll);
        text("Cookie Calm", 30);
        text("Android Chrome does not run Chrome extensions. Cookie Calm uses Accessibility to find and press a clearly labeled cookie rejection button or email signup popup close button in the visible browser screen.", 17);
        text("No browser text or history is sent anywhere. The app has no Internet permission. Disable its Accessibility service in Android Settings at any time.", 16);

        Switch enabled = new Switch(this);
        enabled.setText("Automatic popup handling");
        enabled.setChecked(prefs().getBoolean("enabled", true));
        enabled.setOnCheckedChangeListener((v, on) -> {
            prefs().edit().putBoolean("enabled", on).apply();
            prefs().edit().putString("lastStatus", on ? "Open a browser page to check it" : "Automatic popup handling is off").apply();
        });
        layout.addView(enabled);

        Switch newsletters = new Switch(this);
        newsletters.setText("Close email signup popups");
        newsletters.setChecked(prefs().getBoolean("closeNewsletters", true));
        newsletters.setOnCheckedChangeListener((v, on) -> {
            prefs().edit().putBoolean("closeNewsletters", on).apply();
            prefs().edit().putString("lastStatus", on ? "Email popup closing is on" : "Email popup closing is off").apply();
        });
        layout.addView(newsletters);

        button("Open Accessibility settings", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        if (serviceEnabled()) {
            text("Accessibility status: On", 16);
        } else {
            text("Accessibility status: Off. Open Installed apps → Cookie Calm and turn it on. If Android blocks the switch, open App info → ⋮ → Allow restricted settings, then return here.", 16);
        }

        String host = prefs().getString("lastHost", "");
        String status = prefs().getString("lastStatus", "Open Chrome and load a page to check it.");
        text("Status: " + status, 16);

        int eventCount = prefs().getInt("diag_event_count", 0);
        int scanCount = prefs().getInt("diag_scan_count", 0);
        long eventAt = prefs().getLong("diag_event_time", 0);
        long scanAt = prefs().getLong("diag_scan_time", 0);
        String event = eventCount == 0 ? "No browser accessibility events seen yet" :
            prefs().getString("diag_event_package", "unknown") + " (type " +
            prefs().getString("diag_event_type", "?") + ", " +
            DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(eventAt)) + ")";
        String scan = scanCount == 0 ? "No scan yet" :
            scanCount + " scans; last root " + prefs().getString("diag_root_package", "unknown") +
            ", " + prefs().getString("diag_node_count", "0") + " nodes at " +
            DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(scanAt));
        String diagnosticText = "Browser event: " + event +
            "\\nScan: " + scan +
            "\\nCookie choice label: " + prefs().getString("diag_reject_label", "not checked") +
            "\\nEmail close label: " + prefs().getString("diag_close_label", "not checked") +
            "\\nDecision: " + prefs().getString("diag_decision", "no candidate decision recorded") +
            "\\nClick: " + prefs().getString("diag_click_result", "not attempted") +
            "\\nResult: " + prefs().getString("diag_result", "none");
        text("On-device diagnostics (no page text is saved)", 18);
        text(diagnosticText, 14);
        button("Copy diagnostics", v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("Cookie Calm diagnostics", diagnosticText));
                Toast.makeText(this, "Diagnostics copied", Toast.LENGTH_SHORT).show();
            }
        });
        if (!host.isEmpty()) {
            Set<String> paused = new HashSet<>(prefs().getStringSet("paused", new HashSet<>()));
            text("Most recently observed site: " + host, 16);
            button((paused.contains(host) ? "Resume on " : "Pause on ") + host, v -> {
                if (!paused.remove(host)) paused.add(host);
                prefs().edit().putStringSet("paused", paused).apply(); render();
            });
        }
        text("Popup actions recorded: " + prefs().getInt("clicks", 0) +
            "\nA recorded click does not prove the site honoured the choice.", 15);
        text("Experimental: private tabs, unclear cookie choices, payment walls, and popups without accessible labels remain untouched. Chrome desktop preferences sync separately.", 15);
    }
}
