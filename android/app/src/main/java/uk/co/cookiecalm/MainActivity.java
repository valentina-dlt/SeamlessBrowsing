package uk.co.cookiecalm;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import java.util.HashSet;
import java.util.Set;

public final class MainActivity extends Activity {
    private LinearLayout layout;
    private SharedPreferences prefs() { return getSharedPreferences("cookiecalm", MODE_PRIVATE); }
    private void text(String value, int size) { TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setPadding(0, 16, 0, 16); layout.addView(v); }
    private void button(String value, View.OnClickListener action) { Button b = new Button(this); b.setText(value); b.setOnClickListener(action); layout.addView(b); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); }
    @Override public void onResume() { super.onResume(); render(); }
    private void render() {
        ScrollView scroll = new ScrollView(this); layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(40, 70, 40, 40); scroll.addView(layout); setContentView(scroll);
        text("Cookie Calm", 30);
        text("Reject optional cookies in Chrome and Samsung Internet.", 19);
        text("This companion uses Android Accessibility to read visible browser controls and press explicit cookie rejection buttons. It cannot handle every banner. It does not send screen text or browsing history anywhere, and has no Internet permission. You can disable access in Android Settings at any time.", 16);
        Switch enabled = new Switch(this); enabled.setText("Automatic rejection"); enabled.setChecked(prefs().getBoolean("enabled", true));
        enabled.setOnCheckedChangeListener((v, on) -> prefs().edit().putBoolean("enabled", on).apply()); layout.addView(enabled);
        button("Open Accessibility settings", v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        text("Choose Installed apps → Cookie Calm → Allow. Samsung may first require App info → menu → Allow restricted settings for a sideloaded APK.", 15);
        String host = prefs().getString("lastHost", "");
        if (!host.isEmpty()) {
            Set<String> paused = new HashSet<>(prefs().getStringSet("paused", new HashSet<>()));
            text("Most recently observed site: " + host, 16);
            button((paused.contains(host) ? "Resume on " : "Pause on ") + host, v -> {
                if (!paused.remove(host)) paused.add(host);
                prefs().edit().putStringSet("paused", paused).apply(); render();
            });
        }
        text("Rejection buttons pressed: " + prefs().getInt("clicks", 0) + "\nA click count does not prove that a site honoured your choice.", 15);
        text("Experimental: private tabs, ambiguous banners, pay-or-consent walls, and banners without accessible rejection buttons remain untouched. Phone preferences are local; Chrome desktop settings sync separately.", 15);
    }
}
