import uk.co.cookiecalm.Policy;
public final class PolicyTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        check(Policy.reject("Reject all")); check(Policy.reject("Essential cookies only"));
        check(Policy.reject("Accept essential cookies only")); check(Policy.reject("Accept only essential cookies"));
        check(!Policy.reject("Accept essential and optional cookies"));
        for (String label : new String[]{"Accept all", "Close", "OK", "Reject", "Buy now", "Reject all and pay"}) check(!Policy.reject(label));
        check(Policy.cookieContext("We use cookies")); check(!Policy.cookieContext("Invitations"));
        check(Policy.paidChoice("Subscribe to continue without tracking"));
        check(!Policy.paidChoice("Cookies help our site work"));
        check(Policy.host("example.com/path").equals("example.com"));
        for (String address : new String[]{"chrome://newtab", "not a domain", "javascript:alert(1)", "https://user@example.com"}) check(Policy.host(address).isEmpty());
        System.out.println("Android policy tests passed");
    }
}
