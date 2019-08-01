package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;

class Utils {
    private static final Logger logger = JDNSS.logger;

    /**
     * Ignoring case, find the String in the Set that
     * matches the end of s, and is the longest that does so.
     */
    static String findLongest(final Set<String> e, final String s) {
        assert e != null;
        assert s != null;
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
    static String toString(byte buffer[]) {
        String s = "";
        DecimalFormat df = new DecimalFormat("000");

        for (int i = 0; i < buffer.length; i++) {
            if (i % 10 == 0) {
                // if (i != 0 && i % 16 == 0) s += "\n";
                if (i != 0) s += "\n";
                s += df.format(i) + ": ";
            }

            char c = (char) buffer[i];
            s += (c >= 33 && c <= 126) ? c + " " : ". ";

            s += df.format(buffer[i] & 0xFF) + " ";
        }

        return s;
    }

    /**
     * Get one byte from an integer
     *
     * @param from  the integer to retrive from
     * @param which which byte(1 = lowest, 4 = highest)
     * @return the requested byte
     */
    static byte getByte(int from, int which) {
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
        assert which > 1 && which <= 4;

        byte ret[] = new byte[2];
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
        byte ret[] = new byte[4];
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
        byte ret[] = new byte[8];
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
        String a[] = s.split("\\.");
        byte r[] = new byte[4];

        for (int i = 0; i < a.length; i++) {
            r[i] = (byte) Integer.parseInt(a[i]);
        }

        return r;
    }

    /**
     * Converts a String a <character-string> -- "this" into 4this
     *
     * @param s the original String
     * @return the converted form in bytes
     */
    static byte[] toCS(String s) {
        assert s != null && !s.equals("");

        byte a[] = new byte[1];
        a[0] = getByte(s.length(), 1);
        return combine(a, s.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Converts a domain string into the form needed for a response --
     * given a string "www.foobar.org" it is converted to the 3www6foobar3org0
     *
     * @param s the original String
     * @return the converted form in bytes
     */
    static byte[] convertString(String s) {
        assert s != null && !s.equals("");

        // there's an extra byte needed both before and after
        byte[] a = new byte[s.length() + 2];
        int pointer = 0;

        String b[] = s.split("\\.");

        // for (int i = 0; i < b.length; i++) {
        for (String foo: b) {
            // how long?
            int l = foo.length();
            a[pointer++] = (byte) l;

            // what characters?
            byte c[] = foo.getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(c, 0, a, pointer, l);

            pointer += l;
        }
        a[pointer] = 0;

        return a;
    }

    /**
     * Combine two byte arrays into one
     *
     * @param one one of the arrays
     * @param two the other one
     * @return byte array made from one and two
     */
    static byte[] combine(byte one[], byte two[]) {
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

    static byte[] combine(byte one[], byte two) {
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

    static byte[] trimByteArray(byte[] old, int length) {
        assert old != null: "Byte array is null";
        assert length > 0: length + " is invalid";
        assert length <= old.length: length + " is invalid";

        byte ret[] = new byte[length];
        System.arraycopy(old, 0, ret, 0, length);
        return ret;
    }

    /**
     * split the sting using the dots and reassemble backwards.
     */
    static String reverseIP(String s) {
        assert s != null;

        String a[] = s.split("\\.");

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
    public static String reverse(String s) {
        assert s != null;

        return new StringBuilder(s).reverse().toString();
    }

    /**
     * How many times does one string exist in another?
     *
     * @param s the String to search
     * @param c what to search for
     * @return the number of matches
     */
    static int count(String s, String c) {
        assert s != null;
        assert c != null;

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
    private static byte[] dodots(String s) {
        // split at the v6/v4 boundary
        int splitat = s.lastIndexOf(":");
        String colons = s.substring(0, splitat);
        String dots = s.substring(splitat + 1);

        if (colons.equals(":")) {
            colons = "::";
        }

        return combine(docolons(colons, 12), IPV4(dots));
    }

    /**
     * Do all the v6 conversion
     *
     * @param s      the v6 String
     * @param length how long the String is(16 for v6, 12 for v6/v4)
     * @return the conversoin
     */
    private static byte[] docolons(String s, int length) {
        int numColons = count(s, ":");
        // System.out.println("numColons = " + numColons);
        byte ret[] = new byte[length];

        // nothing but colons, IPv6 unspecified
        if (s.equals("::")) {
            for (int i = 0; i < length; i++) {
                ret[i] = 0;
            }

            return ret;
        }

        String split[] = s.split(":");
        // System.out.println("split.length = " + split.length);

        /*
        ** so, there should be eight total two-byte strings(six for v6 ->
        ** v4). subtract how many there really are and multiply by two.
        */
        int len = ((length / 2) - split.length + 1) * 2;
        int i = 0;

        if (s.startsWith("::")) {
            len += 2;
            i = 1;
        }

        // System.out.println("len = " + len);

        int where = 0;
        for (; i <= numColons; i++) {
            // if this is where things are missing, fill in zeros
            if (split[i].equals("")) {
                for (int j = 0; j < len; j++) {
                    ret[where++] = 0;
                }
            } else {
                int conv = Integer.parseInt(split[i], 16);
                ret[where++] = getByte(conv, 2);
                ret[where++] = getByte(conv, 1);
            }
        }
        return ret;
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

    static String toString(DatagramPacket dgp) {
        String s = "";

        s += "getAddress() = " + dgp.getAddress() + "\n";
        s += "getLength() = " + dgp.getLength() + "\n";
        s += "getOffset() = " + dgp.getOffset() + "\n";
        s += "getPort() = " + dgp.getPort() + "\n";
        s += "getSocketAddress() = " + dgp.getSocketAddress() + "\n";
        s += "getData() = " + Utils.toString(dgp.getData());

        return s;
    }

    private static String toString(DatagramSocket dgs) {
        String s = "";

        try {
            s += "getBroadcast() = " + dgs.getBroadcast() + "\n";
            // s += "getInetAddress = " + dgs.getInetAddress().getHostAddress() + "\n";
            s += "getInetAddress = " + dgs.getInetAddress() + "\n";
            // s += "getLocalAddress = " + dgs.getLocalAddress().getHostAddress() + "\n";
            s += "getLocalAddress = " + dgs.getLocalAddress() + "\n";
            s += "getLocalPort() = " + dgs.getLocalPort() + "\n";
            s += "getLocalSocketAddress() = " + dgs.getLocalSocketAddress() + "\n";
            s += "getReceiveBufferSize() = " + dgs.getReceiveBufferSize() + "\n";
            s += "getReuseAddress() = " + dgs.getReuseAddress() + "\n";
            s += "getSendBufferSize() = " + dgs.getSendBufferSize() + "\n";
            s += "getSoTimeout() = " + dgs.getSoTimeout() + "\n";
            s += "getTrafficClass() = " + dgs.getTrafficClass() + "\n";
            s += "isBound() = " + dgs.isBound() + "\n";
            s += "isClosed() = " + dgs.isClosed() + "\n";
            s += "isConnected() = " + dgs.isConnected();
        } catch (java.net.SocketException SE) {
            logger.catching(SE);
            return null;
        }

        return s;
    }

    static String toString(MulticastSocket mcs) {
        String s = toString((DatagramSocket) mcs) + "\n";

        try {
            s += "getInterface() = " + mcs.getInterface() + "\n";
            s += "getNetworkInterface() = " + mcs.getNetworkInterface() + "\n";
            s += "getTimeToLive() = " + mcs.getTimeToLive() + "\n";
            s += "getLoopbackMode() = " + mcs.getLoopbackMode();
        } catch (java.io.IOException ioe) {
            logger.catching(ioe);
            return null;
        }

        return s;
    }

    static Map.Entry<String, Integer> parseName(int start, byte buffer[]) {
        logger.traceEntry(new ObjectMessage(start));

        if (start >= buffer.length) {
            logger.warn("Illegal name");
            assert false;
        }

        int current = start;
        StringBuilder name = new StringBuilder();

        // if the first thing is a compression
        if ((buffer[current] & 0xc0) == 0xc0) {
            int tmp = addThem(buffer[current] & 0x3f, buffer[current + 1]);
            logger.trace(tmp);

            if (tmp >= start) {
                logger.warn("Illegal name");
                assert false;
            }

            name.append(parseName(tmp, buffer).getKey());
            current += 2;
        }

        int length = buffer[current++] & 0x3f;

        if (length == 0) {
            return Map.entry("", 1);
        }

        while (length > 0) {
            for (int i = 1; i <= length; i++) {
                char c = (char) buffer[current];
                logger.trace("Adding " + c);
                name.append(c);
                current++;
            }

            // if we get to the end of a real string and there's a
            // compression...
            if ((buffer[current] & 0xc0) == 0xc0) {
                name.append(".");

                int tmp = addThem(buffer[current] & 0x3f, buffer[current + 1]);
                logger.trace(tmp);

                if (tmp >= start) {
                    logger.warn("Illegal name");
                    assert false;
                }

                name.append(parseName(tmp, buffer).getKey());
                current += 2;
            }

            // if there's more, put in the separator
            length = buffer[current++] & 0x3f;
            if (length > 0) {
                name.append(".");
            }
        }

        return Map.entry(name.toString(), current);
    }
}
