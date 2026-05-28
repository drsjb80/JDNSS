package edu.msudenver.cs.jdnss;

import lombok.NonNull;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class DnsNameCodec {
    private static final Logger logger = JDNSS.logger;

    private DnsNameCodec() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a domain string into the form needed for a response --
     * given a string "www.foobar.org" it is converted to the 3www6foobar3org0
     *
     * @param s the original String
     * @return the converted form in bytes
     */
    static byte[] convertString(@NonNull final String s) {
        if (s.equals("")) {
            failIllegalArgument("domain name must not be empty");
        }
        assert !s.equals("");

        // there's an extra byte needed both before and after
        byte[] a = new byte[s.length() + 2];
        int pointer = 0;

        String[] b = s.split("\\.");

        for (String foo : b) {
            int l = foo.length();
            a[pointer++] = (byte) l;

            byte[] c = foo.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(c, 0, a, pointer, l);
            pointer += l;
        }
        a[pointer] = 0;

        return a;
    }

    static Map.Entry<String, Integer> parseName(final int start, final byte[] buffer) {
        return parseName(start, buffer, new HashSet<>());
    }

    private static Map.Entry<String, Integer> parseName(final int start, final byte[] buffer,
                                                        final Set<Integer> visitedStarts) {
        logger.traceEntry(new ObjectMessage(start));

        if (start < 0 || start >= buffer.length || !visitedStarts.add(start)) {
            failIllegalName();
        }

        int current = start;
        StringBuilder name = new StringBuilder();

        while (true) {
            if (current >= buffer.length) {
                failIllegalName();
            }

            int length = buffer[current] & 0xff;

            // compression pointer labels are terminal per DNS name compression rules.
            if ((length & 0xc0) == 0xc0) {
                if (current + 1 >= buffer.length) {
                    failIllegalName();
                }

                int pointer = Utils.addThem(length & 0x3f, buffer[current + 1]);
                logger.trace(pointer);

                // preserve original safety rule: only allow backwards pointers.
                if (pointer >= start) {
                    failIllegalName();
                }

                String suffix = parseName(pointer, buffer, visitedStarts).getKey();
                if (name.length() > 0 && !suffix.isEmpty()) {
                    name.append(".");
                }
                name.append(suffix);
                current += 2;
                return Map.entry(name.toString(), current);
            }

            current++;
            length &= 0x3f;

            if (length == 0) {
                return Map.entry(name.toString(), current);
            }

            if (current + length > buffer.length) {
                failIllegalName();
            }

            if (name.length() > 0) {
                name.append(".");
            }

            for (int i = 0; i < length; i++) {
                char c = (char) buffer[current + i];
                logger.trace("Adding " + c);
                name.append(c);
            }
            current += length;
        }
    }

    private static void failIllegalName() {
        logger.warn("Illegal name");
        throw new IllegalArgumentException("Illegal DNS name encoding");
    }

    private static void failIllegalArgument(final String message) {
        logger.warn(message);
        throw new IllegalArgumentException(message);
    }
}