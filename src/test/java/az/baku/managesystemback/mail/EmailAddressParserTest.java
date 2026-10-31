package az.baku.managesystemback.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressParserTest {

    @Test
    void parsesNamedAddress() {
        SenderAddress address = EmailAddressParser.parse("Site Builder <sender@example.com>");

        assertThat(address.name()).isEqualTo("Site Builder");
        assertThat(address.email()).isEqualTo("sender@example.com");
    }

    @Test
    void parsesPlainAddressAndUsesDefaultName() {
        SenderAddress address = EmailAddressParser.parse("sender@example.com");

        assertThat(address.name()).isEqualTo("Site Builder");
        assertThat(address.email()).isEqualTo("sender@example.com");
    }

    @Test
    void rejectsInvalidAddress() {
        assertThatThrownBy(() -> EmailAddressParser.parse("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AUTH_EMAIL_FROM");
    }
}
