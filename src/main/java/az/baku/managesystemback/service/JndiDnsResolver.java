package az.baku.managesystemback.service;

import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * JNDI-based DNS resolver. Uses the JDK DNS provider, so no extra dependency is
 * required.
 *
 * @author Fatima Sultanova
 */
@Component
public class JndiDnsResolver implements DnsResolver {

    private static final int INITIAL_TIMEOUT_MS = 3000;
    private static final int RETRIES = 1;

    @Override
    public List<String> lookupTxt(String hostname) {
        return lookup(hostname, "TXT");
    }

    @Override
    public List<String> lookupCname(String hostname) {
        return lookup(hostname, "CNAME");
    }

    @Override
    public List<String> lookupA(String hostname) {
        return lookup(hostname, "A");
    }

    private List<String> lookup(String hostname, String type) {
        Hashtable<String, String> environment = new Hashtable<>();
        environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        environment.put("com.sun.jndi.dns.timeout.initial", String.valueOf(INITIAL_TIMEOUT_MS));
        environment.put("com.sun.jndi.dns.timeout.retries", String.valueOf(RETRIES));

        DirContext context = null;
        try {
            context = new InitialDirContext(environment);
            Attributes attributes = context.getAttributes(hostname, new String[]{type});
            Attribute attribute = attributes.get(type);

            if (attribute == null) {
                return List.of();
            }

            List<String> values = new ArrayList<>();
            NamingEnumeration<?> enumeration = attribute.getAll();
            while (enumeration.hasMore()) {
                Object value = enumeration.next();
                if (value != null) {
                    values.add(String.valueOf(value));
                }
            }
            return values;
        } catch (NameNotFoundException ex) {
            return List.of();
        } catch (NamingException ex) {
            throw new DnsLookupException("DNS lookup failed for type " + type + ".", ex);
        } finally {
            closeQuietly(context);
        }
    }

    private void closeQuietly(DirContext context) {
        if (context == null) {
            return;
        }

        try {
            context.close();
        } catch (NamingException ignored) {
            // Nothing to do.
        }
    }
}
