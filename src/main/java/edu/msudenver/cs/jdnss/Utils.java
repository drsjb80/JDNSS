package edu.msudenver.cs.jdnss;

import lombok.NonNull;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

final class Utils {
    private static final Logger logger = JDNSS.logger;

    private Utils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Ignoring case, find the String in the Set that
     * matches the end of s, and is the longest that does so.
     */
    static String findLongest(@NonNull final Set<String> e, @NonNull final String s) {
        assert !e.isEmpty();
        assert !s.equals("");

        logger.traceEntry(s);

        String longest = null;

        for (String next : e) {
            logger.info(next);

            if (s.toLowerCase().endsWith(next.toLowerCase())) {
                if (longest == null || next.length() > longest.length()) {
                    longest = next;
                }
            }
        }

        logger.traceExit(longest);
        return longest;
    }

    /**
     * Returns a formatted nicely byte array
     *
     * @param buffer what to format
     * @return a String with eight bytes per line in the form
     * character(or dot if not a character) followed by
     * the an integer representation.
     */
    static String toString(byte[] buffer) {
        StringBuilder s = new StringBuilder();
        DecimalFormat df = new DecimalFormat("000");

        for (int i = 0; i < buffer.length; i++) {
            if (i % 10 == 0) {
                // if (i != 0 && i % 16 == 0) s += "\n";
                if (i != 0) {
                    s.append("\n");
                }
                s.append(df.format(i)).append(": ");
            }

            char c = (char) buffer[i];
            s.append((c >= 33 && c <= 126) ? c + " " : ". ");

            s.append(df.format(buffer[i] & 0xFF)).append(" ");
        }

        return s.toString();
    }

    /**
     * Get one byte from an integer
     *
     * @param from  the integer to retrive from
     * @param which which byte(1 = lowest, 4 = highest)
     * @return the requested byte
     */
    static byte getByte(int from, int which) {
        if (which < 1 || which > 4) {
            failIllegalArgument("which must be between 1 and 4");
        }
        assert which >= 1 && which <= 4;

        int shift = (which - 1) * 8;
        return (byte) ((from >>> shift) & 0xff);
    }

    /**
     * Get one byte from a long
     *
     * @param from  the integer to retrive from
     * @param which which byte(1 = lowest, 8 = highest)
     * @return the requested byte
     */
    private static byte getByte(long from, int which) {
        if (which < 1 || which > 8) {
            failIllegalArgument("which must be between 1 and 8");
        }
        assert which >= 1 && which <= 8;

        int shift = (which - 1) * 8;
        return (byte) ((from >>> shift) & 0xff);
    }

    /**
     * Get two bytes from an integer
     *
     * @param from  the integer to retrieve from
     * @param which which byte to start with
     * @return the requested byte array
     */
    static byte[] getTwoBytes(int from, int which) {
        if (which <= 1 || which > 4) {
            failIllegalArgument("which must be between 2 and 4");
        }
        assert which > 1 && which <= 4;

        byte[] ret = new byte[2];
        ret[0] = getByte(from, which);
        ret[1] = getByte(from, which - 1);
        return ret;
    }

    /**
     * Get all four bytes from an integer
     *
     * @param from the integer to retrive from
     * @return the requested byte array
     */
    static byte[] getBytes(int from) {
        byte[] ret = new byte[4];
        ret[0] = getByte(from, 4);
        ret[1] = getByte(from, 3);
        ret[2] = getByte(from, 2);
        ret[3] = getByte(from, 1);
        return ret;
    }

    /**
     * Get all eight bytes from a long
     *
     * @param from the integer to retrive from
     * @return the requested byte array
     */
    static byte[] getBytes(long from) {
        byte[] ret = new byte[8];
        ret[0] = getByte(from, 8);
        ret[1] = getByte(from, 7);
        ret[2] = getByte(from, 6);
        ret[3] = getByte(from, 5);
        ret[4] = getByte(from, 4);
        ret[5] = getByte(from, 3);
        ret[6] = getByte(from, 2);
        ret[7] = getByte(from, 1);
        return ret;
    }

    /**
     * Get one nybble from an integer
     *
     * @param from  the integer to retrive from
     * @param which which nybble(1 = lowest, 8 = highest)
     * @return the requested nybble
     */
    static byte getNybble(int from, int which) {
        if (which < 1 || which > 8) {
            failIllegalArgument("which must be between 1 and 8");
        }
        assert which >= 1 && which <= 8;

        int shift = (which - 1) * 4;
        return (byte) ((from >>> shift) & 0x0f);
    }

    /**
     * Performs an unsigned addition of two bytes.
     *
     * @param a one of the bytes to add
     * @param b the other one
     * @return the sum
     */
    static int addThem(byte a, byte b) {
        return ((a & 0xff) << 8) + (b & 0xff);
    }

    /**
     * Performs an unsigned addition of four bytes.
     *
     * @param a one of the bytes to add
     * @param b the other one
     * @param c the other other one
     * @param d the other other other one
     * @return the sum
     */
    static int addThem(byte a, byte b, byte c, byte d) {
        return ((a & 0xff) << 24) +
               ((b & 0xff) << 16) +
               ((c & 0xff) << 8) +
                (d & 0xff);
    }

    /**
     * Performs an unsigned addition of two integers.
     *
     * @param a one of the bytes to add
     * @param b the other one
     * @return the sum
     */
    static int addThem(int a, int b) {
        return ((a & 0x000000ff) << 8) + (b & 0x000000ff);
    }

    /**
     * Convert the address string into a byte array
     *
     * @param s the dotted IPV4 address
     * @return 4 byte array of the address
     */
    static byte[] IPV4(String s) {
        String[] a = s.split("\\.", -1);
        if (a.length != 4) {
            failIllegalArgument("Invalid IPv4 address: " + s);
        }

        byte[] r = new byte[4];

        for (int i = 0; i < a.length; i++) {
            String part = a[i];
            if (part.isEmpty() || !part.matches("\\d{1,3}")) {
                failIllegalArgument("Invalid IPv4 address: " + s);
            }

            int value;
            try {
                value = Integer.parseInt(part);
            } catch (NumberFormatException nfe) {
                failIllegalArgument("Invalid IPv4 address: " + s);
                return new byte[0];
            }

            if (value < 0 || value > 255) {
                failIllegalArgument("Invalid IPv4 address: " + s);
            }

            r[i] = (byte) value;
        }

        return r;
    }

    /**
     * Converts a String a <character-string> -- "this" into 4this
     *
     * @param s the original String
     * @return the converted form in bytes
     */
    static byte[] toCS(@NonNull final String s) {
        assert !s.equals("");

        byte[] a = new byte[1];
        a[0] = getByte(s.length(), 1);
        return combine(a, s.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Combine two byte arrays into one
     *
     * @param one one of the arrays
     * @param two the other one
     * @return byte array made from one and two
     */
    static byte[] combine(final byte[] one, final byte[] two) {
        if (one == null && two == null) {
            failIllegalArgument("At least one byte array must be non-null");
        }
        assert one != null || two != null;

        if (one == null) {
            return two;
        }
        if (two == null) {
            return one;
        }

        byte[] temp = new byte[one.length + two.length];
        System.arraycopy(one, 0, temp, 0, one.length);
        System.arraycopy(two, 0, temp, one.length, two.length);
        return temp;
    }

    static byte[] combine(final byte[] one, final byte two) {
        byte[] a = new byte[1];
        a[0] = two;

        if (one == null) {
            return a;
        }

        byte[] temp = new byte[one.length + 1];
        System.arraycopy(one, 0, temp, 0, one.length);
        System.arraycopy(a, 0, temp, one.length, 1);
        return temp;
    }

    static byte[] trimByteArray(@NonNull final byte[] old, final int length) {
        if (length <= 0 || length > old.length) {
            failIllegalArgument(length + " is invalid");
        }
        assert length > 0: length + " is invalid";
        assert length <= old.length: length + " is invalid";

        byte[] ret = new byte[length];
        System.arraycopy(old, 0, ret, 0, length);
        return ret;
    }

    /**
     * split the sting using the dots and reassemble backwards.
     */
    static String reverseIP(@NonNull final String s) {
        String[] a = s.split("\\.");

        // all dots
        if (a.length == 0) return s;

        List<String> list = Arrays.asList(a);
        Collections.reverse(list);
        return String.join(".", list);
    }

    /**
     * Return a string in reverse order
     *
     * @param s String to reverse
     * @return The reversed string
     */
    public static String reverse(@NonNull final String s) {
        return new StringBuilder(s).reverse().toString();
    }

    /**
     * How many times does one string exist in another?
     *
     * @param s the String to search
     * @param c what to search for
     * @return the number of matches
     */
    static int count(@NonNull final String s, @NonNull final String c) {
        if (s.equals("")) return 0;
        if (c.equals("")) return 0;

        int count = 0;
        int where = 0;

        while ((where = s.indexOf(c, where)) != -1) {
            where++;
            count++;
        }

        return count;
    }

    /**
     * A mixed v6/v4 address -- convert both and return the result
     *
     * @return the final answer
     */
    private static byte[] dodots(final String s) {
        // split at the v6/v4 boundary
        int splitat = s.lastIndexOf(":");
        if (splitat < 0) {
            failIllegalArgument("Invalid IPv6 address: " + s);
        }

        String colons = s.substring(0, splitat);
        String dots = s.substring(splitat + 1);
        byte[] ipv4 = IPV4(dots);

        if (colons.equals(":")) {
            colons = "::";
        }

        return combine(docolons(colons, 12), ipv4);
    }

    /**
     * Do all the v6 conversion
     *
     * @param s      the v6 String
     * @param length how long the String is(16 for v6, 12 for v6/v4)
     * @return the conversoin
     */
    private static byte[] docolons(final String s, final int length) {
        if (s == null || s.isEmpty()) {
            failIllegalArgument("Invalid IPv6 address: " + s);
        }

        // nothing but colons, IPv6 unspecified
        if (s.equals("::")) {
            byte[] ret = new byte[length];
            for (int i = 0; i < length; i++) {
                ret[i] = 0;
            }

            return ret;
        }

        final int expectedGroups = length / 2;

        if (count(s, "::") > 1) {
            failIllegalArgument("Invalid IPv6 address: " + s);
        }

        final List<String> groups = new ArrayList<>();

        if (s.contains("::")) {
            String[] halves = s.split("::", -1);
            if (halves.length != 2) {
                failIllegalArgument("Invalid IPv6 address: " + s);
            }

            String[] left = halves[0].isEmpty() ? new String[0] : halves[0].split(":", -1);
            String[] right = halves[1].isEmpty() ? new String[0] : halves[1].split(":", -1);
            validateIpv6Groups(left, s);
            validateIpv6Groups(right, s);

            int present = left.length + right.length;
            if (present >= expectedGroups) {
                failIllegalArgument("Invalid IPv6 address: " + s);
            }

            groups.addAll(Arrays.asList(left));
            for (int i = 0; i < expectedGroups - present; i++) {
                groups.add("0");
            }
            groups.addAll(Arrays.asList(right));
        } else {
            String[] all = s.split(":", -1);
            validateIpv6Groups(all, s);
            if (all.length != expectedGroups) {
                failIllegalArgument("Invalid IPv6 address: " + s);
            }
            groups.addAll(Arrays.asList(all));
        }

        if (groups.size() != expectedGroups) {
            failIllegalArgument("Invalid IPv6 address: " + s);
        }

        byte[] ret = new byte[length];
        int where = 0;
        for (String group : groups) {
            int conv;
            try {
                conv = Integer.parseInt(group, 16);
            } catch (NumberFormatException nfe) {
                failIllegalArgument("Invalid IPv6 address: " + s);
                return new byte[0];
            }
            ret[where++] = getByte(conv, 2);
            ret[where++] = getByte(conv, 1);
        }

        return ret;
    }

    private static void validateIpv6Groups(final String[] groups, final String source) {
        for (String group : groups) {
            if (group.isEmpty()) {
                failIllegalArgument("Invalid IPv6 address: " + source);
            }
            if (group.length() > 4) {
                failIllegalArgument("Invalid IPv6 address: " + source);
            }
            for (int i = 0; i < group.length(); i++) {
                char ch = group.charAt(i);
                boolean isHex = (ch >= '0' && ch <= '9')
                        || (ch >= 'a' && ch <= 'f')
                        || (ch >= 'A' && ch <= 'F');
                if (!isHex) {
                    failIllegalArgument("Invalid IPv6 address: " + source);
                }
            }
        }
    }

    /**
     * Convert an IPv6 String into its byte array
     *
     * @param s the IPv6 String
     * @return the IPv6 bytes
     */
    static byte[] IPV6(String s) {
        // if this is an v6t/v4 address
        if (count(s, ".") > 0) {
            return dodots(s);
        }

        return docolons(s, 16);
    }

    private static void failIllegalArgument(final String message) {
        logger.warn(message);
        throw new IllegalArgumentException(message);
    }
}
