package uk.co.cookiecalm;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;
import java.util.HashSet;
import java.util.Set;

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
