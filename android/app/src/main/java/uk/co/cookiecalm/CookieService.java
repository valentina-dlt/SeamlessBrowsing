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
import java.util.Locale;
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
        handler.postDelayed(this::scan, 700);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getPackageName() == null ||
            !browsers.contains(event.getPackageName().toString()) ||
            !prefs().getBoolean("enabled", true) || pending) return;
        pending = true;
        handler.postDelayed(() -> { pending = false; scan(); }, 350);
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

    private String hint(AccessibilityNodeInfo node) {
        CharSequence value = node.getHintText();
        return value == null ? "" : value.toString();
    }

    private String address(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) {
            String id = node.getViewIdResourceName();
            if (id == null) continue;
            String lower = id.toLowerCase(Locale.ROOT);
            boolean addressField = lower.contains("url_bar") || lower.contains("urlbar") ||
                lower.contains("location_bar") || lower.contains("locationbar");
            if (addressField) {
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

    private boolean hasEmailField(List<AccessibilityNodeInfo> group) {
        for (AccessibilityNodeInfo item : group) {
            if (!item.isEditable()) continue;
            String details = (hint(item) + " " + text(item) + " " +
                String.valueOf(item.getViewIdResourceName())).toLowerCase(Locale.ROOT);
            if (details.contains("email") || details.contains("e-mail")) return true;
        }
        return false;
    }

    private boolean hasEnteredFormText(List<AccessibilityNodeInfo> group) {
        for (AccessibilityNodeInfo item : group) {
            if (!item.isEditable()) continue;
            CharSequence value = item.getText();
            if (value != null && value.length() > 0) return true;
        }
        return false;
    }

    private boolean hasSensitiveForm(List<AccessibilityNodeInfo> group) {
        for (AccessibilityNodeInfo item : group) {
            String details = (hint(item) + " " + text(item) + " " +
                String.valueOf(item.getViewIdResourceName())).toLowerCase(Locale.ROOT);
            if (details.contains("password") || details.contains("credit card") ||
                details.contains("card number") || details.contains("billing")) return true;
        }
        return false;
    }

    private boolean newsletterDialog(AccessibilityNodeInfo closeControl) {
        AccessibilityNodeInfo parent = closeControl.getParent();
        for (int depth = 0; parent != null && depth < 14; depth++, parent = parent.getParent()) {
            List<AccessibilityNodeInfo> group = new ArrayList<>();
            flatten(parent, group, 0);
            if (group.size() > 300) break;
            StringBuilder context = new StringBuilder();
            for (AccessibilityNodeInfo item : group) {
                context.append(text(item)).append(' ').append(hint(item)).append(' ');
            }
            if (context.length() > 10000) break;
            if (Policy.newsletterContext(context.toString(), hasEmailField(group)) &&
                !hasEnteredFormText(group) && !hasSensitiveForm(group)) return true;
        }
        return false;
    }

    private boolean dismissNewsletter(List<AccessibilityNodeInfo> nodes, String host) {
        if (!prefs().getBoolean("closeNewsletters", true)) return false;
        for (AccessibilityNodeInfo labelNode : nodes) {
            if (!labelNode.isVisibleToUser()) continue;
            String label = text(labelNode);
            String id = String.valueOf(labelNode.getViewIdResourceName()).toLowerCase(Locale.ROOT);
            boolean closeLabel = Policy.newsletterClose(label) ||
                (id.contains("klaviyo") && id.contains("close"));
            if (!closeLabel) continue;

            AccessibilityNodeInfo target = labelNode;
            for (int depth = 0; target != null && depth < 5; depth++, target = target.getParent()) {
                if (target.isVisibleToUser() && target.isEnabled() && target.isClickable() &&
                    !String.valueOf(target.getClassName()).toLowerCase(Locale.ROOT).endsWith("link") &&
                    newsletterDialog(target)) {
                    lastClick = SystemClock.elapsedRealtime();
                    attempts++;
                    if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        prefs().edit().putInt("clicks", prefs().getInt("clicks", 0) + 1).apply();
                        report("Closed an email signup popup" + (host.isEmpty() ? "" : " on " + host), host);
                    } else {
                        report("Found the email popup close button, but Android would not press it", host);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private void scan() {
        if (!prefs().getBoolean("enabled", true)) {
            report("Automatic popup handling is off", "");
            return;
        }
        if (SystemClock.elapsedRealtime() - lastClick < 1200) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null ||
            !browsers.contains(root.getPackageName().toString())) {
            report("Waiting for a Chrome or Samsung Internet page", "");
            return;
        }

        List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        flatten(root, nodes, 0);

        // Private tabs and text the user has started entering are deliberately excluded.
        for (AccessibilityNodeInfo n : nodes) {
            String t = text(n).toLowerCase(Locale.ROOT);
            if (n.isFocused() && n.isEditable() && t.length() > 0) {
                report("Waiting while you type", "");
                return;
            }
            if (t.contains("incognito") || t.equals("secret mode") || t.contains("private browsing")) {
                report("Private browsing is paused", "");
                return;
            }
        }

        String host = address(nodes);
        Set<String> paused = prefs().getStringSet("paused", new HashSet<>());
        if (host.isEmpty() && !paused.isEmpty()) {
            report("Can't verify the site to respect a pause; no popup was touched", "");
            return;
        }
        if (!host.isEmpty() && paused.contains(host)) {
            report("Paused on " + host, host);
            return;
        }
        if (!host.equals(previousHost)) { previousHost = host; attempts = 0; }
        if (attempts >= 3) {
            report("No popup action succeeded after 3 tries" + (host.isEmpty() ? "" : " on " + host), host);
            return;
        }

        if (dismissNewsletter(nodes, host)) return;

        boolean sawChoice = false;
        for (AccessibilityNodeInfo labelNode : nodes) {
            if (!labelNode.isVisibleToUser() || !Policy.reject(text(labelNode))) continue;
            sawChoice = true;

            AccessibilityNodeInfo target = labelNode;
            for (int targetDepth = 0; target != null && targetDepth < 5; targetDepth++, target = target.getParent()) {
                String cls = String.valueOf(target.getClassName()).toLowerCase(Locale.ROOT);
                if (!target.isVisibleToUser() || !target.isEnabled() || !target.isClickable() ||
                    cls.endsWith("link")) continue;

                AccessibilityNodeInfo parent = target.getParent();
                for (int depth = 0; parent != null && depth < 12; depth++, parent = parent.getParent()) {
                    List<AccessibilityNodeInfo> group = new ArrayList<>();
                    flatten(parent, group, 0);
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
                    if (target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        prefs().edit().putInt("clicks", prefs().getInt("clicks", 0) + 1).apply();
                        report("Pressed the cookie rejection button" + (host.isEmpty() ? "" : " on " + host), host);
                    } else {
                        report("Found the rejection button, but Android would not press it", host);
                    }
                    return;
                }
            }
        }
        if (!host.isEmpty()) report(sawChoice
            ? "Found a rejection label, but not inside a clear cookie dialog on " + host
            : "No accessible Reject all button found on " + host, host);
        else report(sawChoice
            ? "Found a rejection label, but not inside a clear cookie dialog"
            : "No accessible Reject all button or email popup close control was found", "");
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
