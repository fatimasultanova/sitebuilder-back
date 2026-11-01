package az.baku.managesystemback.mail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailMasker {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

    private EmailMasker() {
    }

    public static String mask(String email) {
        if (email == null || email.isBlank()) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex == email.length() - 1) {
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex + 1);

        if (localPart.length() == 1) {
            return "***@" + domain;
        }

        return localPart.charAt(0) + "***@" + domain;
    }

    public static String maskAll(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher matcher = EMAIL_PATTERN.matcher(value);
        StringBuilder masked = new StringBuilder();

        while (matcher.find()) {
            matcher.appendReplacement(masked, Matcher.quoteReplacement(mask(matcher.group())));
        }

        matcher.appendTail(masked);
        return masked.toString();
    }
}
