package az.baku.managesystemback.mail;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EmailAddressParser {

    public static final String DEFAULT_SENDER_NAME = "Site Builder";

    private static final Pattern NAMED_ADDRESS = Pattern.compile("^\\s*(.*?)\\s*<([^<>]+)>\\s*$");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@<>]+@[^\\s@<>]+$");

    private EmailAddressParser() {
    }

    public static SenderAddress parse(String rawAddress) {
        if (rawAddress == null || rawAddress.isBlank()) {
            throw invalidAddress();
        }

        String value = rawAddress.trim();
        Matcher matcher = NAMED_ADDRESS.matcher(value);

        String name;
        String email;

        if (matcher.matches()) {
            name = matcher.group(1).trim();
            email = matcher.group(2).trim();
        } else {
            name = "";
            email = value;
        }

        if (!EMAIL.matcher(email).matches()) {
            throw invalidAddress();
        }

        if (name.isEmpty()) {
            name = DEFAULT_SENDER_NAME;
        }

        return new SenderAddress(name, email);
    }

    private static IllegalArgumentException invalidAddress() {
        return new IllegalArgumentException(
                "Invalid AUTH_EMAIL_FROM value. Expected 'Name <email>' or 'email'."
        );
    }
}
