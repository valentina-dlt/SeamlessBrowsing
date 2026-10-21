package uk.co.cookiecalm;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CookieService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Set<String> browsers = new HashSet<>(Arrays.asList("com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.sec.android.app.sbrowser"));
    private long lastClick;
    private boolean pending;
    private String previousHost = "";
    private int attempts;
    private SharedPreferences prefs() { return getSharedPreferences("cookiecalm", MODE_PRIVATE); }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null || !browsers.contains(event.getPackageName().toString()) || !prefs().getBoolean("enabled", true) || pending) return;
        pending = true;
        handler.postDelayed(() -> { pending = false; scan(); }, 700);
    }
    private void flatten(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out, int depth) {
        if (node == null || depth > 35 || out.size() >= 1200) return;
        out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) flatten(node.getChild(i), out, depth + 1);
    }
    private String text(AccessibilityNodeInfo node) {
        CharSequence value = node.getText();
        if (value == null || value.length() == 0) value = node.getContentDescription();
        return value == null ? "" : value.toString();
    }
    private String address(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            String id = node.getViewIdResourceName();
            if (id != null && (id.endsWith(":id/url_bar") || id.endsWith(":id/location_bar_edit_text") || id.endsWith(":id/location_bar_text"))) return Policy.host(text(node));
        }
        return "";
    }
    private void scan() {
        if (!prefs().getBoolean("enabled", true) || SystemClock.elapsedRealtime() - lastClick < 3000) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null || !browsers.contains(root.getPackageName().toString())) return;
        List<AccessibilityNodeInfo> nodes = new ArrayList<>(); flatten(root, nodes, 0);
        // Private tabs and focused text fields are deliberately excluded.
        for (AccessibilityNodeInfo n : nodes) {
            if (n.isFocused() && n.isEditable()) return;
            String t = text(n).toLowerCase(java.util.Locale.ROOT);
            if (t.contains("incognito") || t.equals("secret mode") || t.contains("private browsing")) return;
        }
        String host = address(nodes);
        if (host.isEmpty()) return;
        prefs().edit().putString("lastHost", host).apply();
        if (prefs().getStringSet("paused", new HashSet<>()).contains(host)) return;
        if (!host.equals(previousHost)) { previousHost = host; attempts = 0; }
        if (attempts >= 3) return;
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser() || !node.isEnabled() || !node.isClickable() || !Policy.reject(text(node))) continue;
            // Only actual buttons, never ambiguous clickable text or links.
            if (!String.valueOf(node.getClassName()).endsWith("Button")) continue;
            AccessibilityNodeInfo parent = node.getParent();
            for (int depth = 0; parent != null && depth < 5; depth++, parent = parent.getParent()) {
                String cls = String.valueOf(parent.getClassName());
                if (cls.contains("WebView")) break;
                List<AccessibilityNodeInfo> group = new ArrayList<>(); flatten(parent, group, 0);
                if (group.size() > 100) break;
                StringBuilder context = new StringBuilder(); boolean editable = false;
                for (AccessibilityNodeInfo item : group) { context.append(text(item)).append(' '); editable |= item.isEditable(); }
                if (context.length() > 6000 || editable) break;
                if (Policy.cookieContext(context.toString()) && !Policy.paidChoice(context.toString())) {
                    lastClick = SystemClock.elapsedRealtime(); attempts++;
                    if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        prefs().edit().putInt("clicks", prefs().getInt("clicks", 0) + 1).apply();
                    }
                    return;
                }
            }
        }
    }
    @Override public void onInterrupt() { handler.removeCallbacksAndMessages(null); pending = false; }
    @Override public void onDestroy() { onInterrupt(); super.onDestroy(); }
}
