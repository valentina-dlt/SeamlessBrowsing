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
    private final Set<String> browsers = new HashSet<>(Arrays.asList(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.sec.android.app.sbrowser"));
    private long lastClick;
    private boolean pending;
    private String previousHost = "";
    private int attempts;
    private SharedPreferences prefs() { return getSharedPreferences("cookiecalm", MODE_PRIVATE); }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        // Scan immediately when the service is enabled, even if Chrome is already open.
        handler.postDelayed(this::scan, 700);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null ||
            !browsers.contains(event.getPackageName().toString()) ||
            !prefs().getBoolean("enabled", true) || pending) return;
        pending = true;
        handler.postDelayed(() -> { pending = false; scan(); }, 500);
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
            if (id == null) continue;
            String lower = id.toLowerCase(java.util.Locale.ROOT);
            if (lower.endsWith(":id/url_bar") || lower.endsWith(":id/url_bar_text") ||
                lower.contains(":id/location_bar")) {
                String host = Policy.host(text(node));
                if (!host.isEmpty()) return host;
            }
        }
        return "";
    }

    private void report(String status, String host) {
        SharedPreferences.Editor edit = prefs().edit().putString("lastStatus", status);
        if (host != null && !host.isEmpty()) edit.putString("lastHost", host);
        edit.apply();
    }

    private void scan() {
        if (!prefs().getBoolean("enabled", true)) {
            report("Automatic rejection is off", "");
            return;
        }
        if (SystemClock.elapsedRealtime() - lastClick < 2500) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null ||
            !browsers.contains(root.getPackageName().toString())) return;

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        flatten(root, nodes, 0);

        // Private tabs and focused text fields are deliberately excluded.
        for (AccessibilityNodeInfo n : nodes) {
            if (n.isFocused() && n.isEditable()) {
                report("Waiting while you type", "");
                return;
            }
            String t = text(n).toLowerCase(java.util.Locale.ROOT);
            if (t.contains("incognito") || t.equals("secret mode") || t.contains("private browsing")) {
                report("Private browsing is paused", "");
                return;
            }
        }

        String host = address(nodes);
        if (host.isEmpty()) {
            report("Chrome page is open, but its address was not readable", "");
            return;
        }
        report("Checking " + host, host);
        if (prefs().getStringSet("paused", new HashSet<>()).contains(host)) {
            report("Paused on " + host, host);
            return;
        }
        if (!host.equals(previousHost)) { previousHost = host; attempts = 0; }
        if (attempts >= 3) {
            report("No rejection click succeeded after 3 tries on " + host, host);
            return;
        }

        boolean sawChoice = false;
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser() || !node.isEnabled() || !node.isClickable() ||
                !Policy.reject(text(node))) continue;

            // Chrome may expose webpage controls as generic Views. The exact opt-out
            // label plus nearby cookie context is the safety boundary; links are excluded.
            String cls = String.valueOf(node.getClassName()).toLowerCase(java.util.Locale.ROOT);
            if (cls.endsWith("link")) continue;
            sawChoice = true;

            AccessibilityNodeInfo parent = node.getParent();
            for (int depth = 0; parent != null && depth < 12; depth++, parent = parent.getParent()) {
                List<AccessibilityNodeInfo> group = new ArrayList<>();
                flatten(parent, group, 0);
                // Ignore page-sized subtrees; continue only while the local dialog is bounded.
                if (group.size() > 300) break;
                StringBuilder context = new StringBuilder();
                boolean editable = false;
                for (AccessibilityNodeInfo item : group) {
                    context.append(text(item)).append(' ');
                    editable |= item.isEditable();
                }
                if (context.length() > 12000 || editable) break;
                String contextText = context.toString();
                if (!Policy.cookieContext(contextText) || Policy.paidChoice(contextText)) continue;

                lastClick = SystemClock.elapsedRealtime();
                attempts++;
                if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    prefs().edit().putInt("clicks", prefs().getInt("clicks", 0) + 1).apply();
                    report("Pressed the cookie rejection button on " + host, host);
                } else {
                    report("Found the rejection button, but Android would not press it", host);
                }
                return;
            }
        }
        report(sawChoice
            ? "Found a rejection label, but not inside a clear cookie dialog"
            : "No accessible Reject all button found on " + host, host);
    }

    @Override public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
        pending = false;
    }
    @Override public void onDestroy() {
        onInterrupt();
        super.onDestroy();
    }
}
