package uk.co.cookiecalm;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.net.URI;

public final class Policy {
    private static final Set<String> REJECT = new HashSet<>(Arrays.asList(
        "reject all", "reject all cookies", "reject optional cookies", "reject non-essential cookies",
        "decline all", "decline optional cookies", "deny all", "refuse all", "only necessary", "necessary only",
        "necessary cookies only", "essential cookies only", "use necessary cookies only",
        "use essential cookies only", "accept necessary cookies only", "accept only necessary cookies", "accept essential cookies only", "accept only essential cookies",
        "only accept essential cookies", "only accept necessary cookies",
        "continue without accepting", "continue without agreeing", "tout refuser", "refuser tout",
        "alle ablehnen", "nur notwendige cookies", "rechazar todo", "rechazar todas", "solo cookies necesarias"
    ));
    private static final Set<String> NEWSLETTER_CLOSE = new HashSet<>(Arrays.asList(
        "close", "close dialog", "close form", "close popup", "close pop-up", "close modal",
        "close newsletter", "dismiss", "no thanks", "no thank you", "×", "✕", "✖", "x"
    ));
    public static String normalize(String s) {
        return java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFKC)
            .toLowerCase(Locale.ROOT).replace('\u2019', '\'')
            .replaceAll("[‐‑–—]", "-").replaceAll("\\s+", " ").trim().replaceAll("[.!]$", "");
    }
    public static boolean reject(String label) { return REJECT.contains(normalize(label)); }
    public static boolean newsletterClose(String label) { return NEWSLETTER_CLOSE.contains(normalize(label)); }
    public static boolean newsletterContext(String text, boolean hasEmailField) {
        String value = normalize(text);
        if (value.length() > 10000 || value.matches("(?s).*(?:\\bcookies?\\b|\\bcheckout\\b|\\bsign in\\b|\\blog in\\b|\\bcontact (?:us|support)\\b|\\bpassword\\b|\\bcredit card\\b|\\bbilling\\b|\\bpayment\\b|\\bsubscribe to continue\\b|\\bcontinue reading\\b).*")) return false;
        boolean explicitMarketing = value.matches("(?s).*(?:\\bnewsletter\\b|\\bmailing list\\b|\\bemail (?:updates|offers|marketing|news)\\b).*");
        boolean signupWithEmail = hasEmailField && value.matches("(?s).*(?:\\bsubscribe\\b|\\bsign up\\b|\\bjoin\\b).*");
        return explicitMarketing || signupWithEmail;
    }
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
