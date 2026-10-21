package uk.co.cookiecalm;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.net.URI;

public final class Policy {
    private static final Set<String> REJECT = new HashSet<>(Arrays.asList(
        "reject all", "reject all cookies", "reject optional cookies", "reject non-essential cookies",
        "decline all", "decline optional cookies", "deny all", "refuse all", "only necessary",
        "necessary cookies only", "essential cookies only", "use necessary cookies only",
        "use essential cookies only", "accept necessary cookies only", "accept only necessary cookies", "accept essential cookies only", "accept only essential cookies",
        "only accept essential cookies", "only accept necessary cookies",
        "continue without accepting", "continue without agreeing", "tout refuser", "refuser tout",
        "alle ablehnen", "nur notwendige cookies", "rechazar todo", "rechazar todas", "solo cookies necesarias"
    ));
    public static String normalize(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT).replace('\u2019', '\'').replaceAll("\\s+", " ").trim().replaceAll("[.!]$", "");
    }
    public static boolean reject(String label) { return REJECT.contains(normalize(label)); }
    public static boolean cookieContext(String text) {
        return text.toLowerCase(Locale.ROOT).matches("(?s).*\\bcookies?\\b.*") || text.toLowerCase(Locale.ROOT).contains("consent preferences");
    }
    public static boolean paidChoice(String text) {
        return text.toLowerCase(Locale.ROOT).matches("(?s).*(subscribe|pay|purchase).{0,45}(reject|refuse|without|tracking).*") ||
            text.toLowerCase(Locale.ROOT).matches("(?s).*(reject|refuse).{0,45}(subscribe|pay|purchase).*");
    }
    public static String host(String address) {
        try {
            String input = address.trim();
            if (!input.contains("://")) input = "https://" + input;
            URI uri = new URI(input);
            String host = uri.getHost();
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || host == null || !host.contains(".") || uri.getUserInfo() != null) return "";
            return host.toLowerCase(Locale.ROOT);
        } catch (Exception ignored) { return ""; }
    }
    private Policy() {}
}
