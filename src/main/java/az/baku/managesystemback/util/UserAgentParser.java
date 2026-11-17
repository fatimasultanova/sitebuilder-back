package az.baku.managesystemback.util;

import java.util.Locale;

public final class UserAgentParser {

    private UserAgentParser() {
    }

    public static UserAgentInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return new UserAgentInfo(null, null);
        }

        String value = userAgent.toLowerCase(Locale.ROOT);
        return new UserAgentInfo(detectBrowser(value), detectOs(value));
    }

    private static String detectBrowser(String value) {
        if (value.contains("edg/") || value.contains("edge/")) {
            return "Edge";
        }
        if (value.contains("opr/") || value.contains("opera")) {
            return "Opera";
        }
        if (value.contains("firefox/")) {
            return "Firefox";
        }
        if (value.contains("chrome/") || value.contains("chromium/")) {
            return "Chrome";
        }
        if (value.contains("safari/")) {
            return "Safari";
        }
        return null;
    }

    private static String detectOs(String value) {
        if (value.contains("iphone") || value.contains("ipad") || value.contains("ipod")) {
            return "iOS";
        }
        if (value.contains("android")) {
            return "Android";
        }
        if (value.contains("windows")) {
            return "Windows";
        }
        if (value.contains("mac os x") || value.contains("macintosh")) {
            return "macOS";
        }
        if (value.contains("linux")) {
            return "Linux";
        }
        return null;
    }

    public record UserAgentInfo(String browser, String os) {

        public String deviceLabel() {
            if (browser == null || os == null) {
                return null;
            }
            return browser + " on " + os;
        }
    }
}
