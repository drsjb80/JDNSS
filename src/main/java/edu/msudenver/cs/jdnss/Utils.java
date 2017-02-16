package edu.msudenver.cs.jdnss;
import java.text.DecimalFormat;
import java.net.DatagramPacket;
import edu.msudenver.cs.javaln.JavaLN;
import java.util.Enumeration;

/**
 * Common methods used throughout
 *
 * @author Steve Beaty
 * @version $Id: Utils.java,v 1.20 2011/03/14 19:07:22 drb80 Exp $
 */

public class Utils
{
    private static JavaLN logger = JDNSS.logger;

    /** The request/respose numbers */
    public static final int A       = 1;
    public static final int NS      = 2;
    public static final int CNAME   = 5;
    public static final int SOA     = 6;
    public static final int PTR     = 12;
    public static final int HINFO   = 13;
    public static final int MX      = 15;
    public static final int TXT     = 16;
    public static final int AAAA    = 28;
    public static final int A6      = 38;
    public static final int DNAME   = 39;
    public static final int OPT     = 41;
    public static final int DS      = 43;
    public static final int RRSIG   = 46;
    public static final int NSEC    = 47;
    public static final int DNSKEY  = 48;
    public static final int INCLUDE = 256;
    public static final int ORIGIN  = 257;
    public static final int TTL     = 258;

    public static final int NOERROR     = 0;
    public static final int FORMERROR   = 1;
    public static final int SERVFAIL    = 2;
    public static final int NAMEERROR   = 3;
    public static final int NOTIMPL     = 4;
    public static final int REFUSED     = 5;

    // time to make a map or two here...

    public static int mapStringToType (String s)
    {
        if (s.equalsIgnoreCase ("A"))               return (A);
        else if (s.equalsIgnoreCase ("NS"))         return (NS);
        else if (s.equalsIgnoreCase ("CNAME"))      return (CNAME);
        else if (s.equalsIgnoreCase ("SOA"))        return (SOA);
        else if (s.equalsIgnoreCase ("PTR"))        return (PTR);
        else if (s.equalsIgnoreCase ("HINFO"))      return (HINFO);
        else if (s.equalsIgnoreCase ("MX"))         return (MX);
        else if (s.equalsIgnoreCase ("TXT"))        return (TXT);
        else if (s.equalsIgnoreCase ("AAAA"))       return (AAAA);
        else if (s.equalsIgnoreCase ("A6"))         return (A6);
        else if (s.equalsIgnoreCase ("DNAME"))      return (DNAME);
        else if (s.equalsIgnoreCase ("INCLUDE"))    return (INCLUDE);
        else if (s.equalsIgnoreCase ("ORIGIN"))     return (ORIGIN);
        else return (0);
    }
    
    public static String mapTypeToString (int i)
    {
        switch (i)
        {
            case A : return "A";
            case NS : return "NS";
            case CNAME : return "CNAME";
            case SOA : return "SOA";
            case PTR : return "PTR";        
            case HINFO : return "HINFO";
            case MX : return "MX";        
            case TXT : return "TXT";
            case AAAA : return "AAAA";        
            case A6 : return "A6";
            case OPT : return "OPT";
            case DNAME : return "DNAME";
            case RRSIG : return "RRSIG";
            case NSEC : return "NSEC";
            case DNSKEY : return "DNSKEY";
            default : return "unknown";
        }
    }

    /**
     * an Assert that is independent of version and always executes...
     *
     * @param assertion        what to test
     */
    public static void Assert (boolean assertion)
    {
        if (!assertion) throw new IllegalArgumentException ("Assertion failed");
    }

    public static void Assert (boolean assertion, String message)
    {
        if (!assertion) throw new IllegalArgumentException (message);
    }

    /**
     * an Assert that is independent of version and always executes, and
     * throws a particular Exception
     *
     * @param assertion        what to test
     * @param e what exception to throw
     */
    public static void AssertAndThrow (boolean assertion, Exception e)
        throws Exception
    {
        if (!assertion) throw e;
    }

    /**
     * Ignoring case, find the String in the Enumeration that
     * 1) ends with s, and
     * 2) is the longest that does.
     */
    public static String findLongest (Enumeration e, String s)
    {
        logger.entering (s);

        String longest = null;

        while (e.hasMoreElements())
        {
            String next = (String) e.nextElement();

            if (s.toLowerCase().endsWith (next.toLowerCase()))
            {
                if (longest == null || next.length() > longest.length())
                {
                    longest = next;
                }
            }
        }

        logger.exiting (longest);
        return (longest);
    }

    /**
     * Returns a formatted nicely byte array
     *
     * @param buffer        what to format
     * @return                 a String with eight bytes per line
     */
    public static String toString (byte buffer[])
    {
        String s = "";
        DecimalFormat df = new DecimalFormat ("000");

        for (int i = 0; i < buffer.length; i++)
        {
            if (i % 10 == 0)
            {
                // if (i != 0 && i % 16 == 0) s += "\n";
                if (i != 0) s += "\n";
                s += df.format (i) + ": ";
            }

            char c = (char) buffer[i];
            s += (c >= 33 && c <= 126) ? c + " " : ". ";

            s += df.format ((int) (buffer[i] & 0xFF)) + " ";
        }

        return (s);
    }

    /**
     * Get one byte from an integer
     * @param from        the integer to retrive from
     * @param which        which byte (1 = lowest, 4 = highest)
     * @return                 the requested byte
     */
    public static byte getByte (int from, int which)
    {
        Assert (which >= 1 && which <= 4);

        int shift = (which - 1) * 8;
        return ((byte) ((from & (0xff << shift)) >> shift));
    }

    /**
     * Get two bytes from an integer
     * @param from        the integer to retrieve from
     * @param which        which byte to start with
     * @return                 the requested byte array
     */
    public static byte[] getTwoBytes (int from, int which)
    {
        Assert (which > 1 && which <= 4);

        byte ret[] = new byte[2];
        ret[0] = getByte (from, which);
        ret[1] = getByte (from, which-1);
        return (ret);
    }

    /**
     * Get all four bytes from an integer
     * @param from        the integer to retrive from
     * @return                 the requested byte array
     */
    public static byte[] getBytes (int from)
    {
        byte ret[] = new byte[4];
        ret[0] = getByte (from, 4);
        ret[1] = getByte (from, 3);
        ret[2] = getByte (from, 2);
        ret[3] = getByte (from, 1);
        return (ret);
    }

    /**
     * Get one nybble from an integer
     * @param from        the integer to retrive from
     * @param which        which nybble (1 = lowest, 8 = highest)
     * @return                 the requested nybble
     */
    public static byte getNybble (int from, int which)
    {
        Assert (which >= 1 && which <= 8);

        int shift = (which - 1) * 4;
        return ((byte) (from >>> shift & 0x0f));
    }

    /**
     * Performs an unsigned addition of two bytes.
     *
     * @param a        one of the bytes to add
     * @param b        the other one
     * @return        the sum
     */
    public static int addThem (byte a, byte b)
    {
        return
        (
            ((a & 0x000000ff) << 8) +
             (b & 0x000000ff)
        );

    }

    /**
     * Performs an unsigned addition of four bytes.
     *
     * @param a        one of the bytes to add
     * @param b        the other one
     * @param c        the other other one
     * @param d        the other other other one
     * @return        the sum
     */
    public static int addThem (byte a, byte b, byte c, byte d)
    {
        return
        (
            ((a & 0x000000ff) << 24) +
            ((b & 0x000000ff) << 16) +
            ((c & 0x000000ff) << 8) +
             (d & 0x000000ff)
        );
    }

    /**
     * Performs an unsigned addition of two integers.
     *
     * @param a        one of the bytes to add
     * @param b        the other one
     * @return        the sum
     */
    public static int addThem (int a, int b)
    {
        return
        (
            ((a & 0x000000ff) << 8) +
             (b & 0x000000ff)
        );
    }

    /**
     * Convert the address string into a byte array
     *
     * @param s        the dotted IPV4 address
     * @return        4 byte array of the address
     */
    public static byte[] IPV4 (String s)
    {
        String a[] = s.split ("\\.");
        byte r[] = new byte[4];

        for (int i = 0; i < a.length; i++)
        {
            r[i] = (byte) Integer.parseInt (a[i]);
        }

        return (r);
    }

    /**
     * Converts a String a <character-string> -- "this" into 4this
     * @param s        the original String
     * @return        the converted form in bytes
     */
    public static byte[] toCS (String s)
    {
        byte a[] = new byte[1];
        a[0] = getByte (s.length(), 1);
        return (combine (a, s.getBytes()));
    }

    /**
     * Converts a domain string into the form needed for a response --
     * given a string "www.foobar.org" it is converted to the 3www6foobar3org0
     * @param s        the original String
     * @return        the converted form in bytes
     */
    public static byte[] convertString (String s)
    {
        Assert (s != null && !s.equals (""));
        
        // there's an extra byte needed both before and after
        byte[] a = new byte[s.length() + 2];
        int pointer = 0;

        String b[] = s.split ("\\.");

        for (int i = 0; i < b.length; i++)
        {
            int l = b[i].length();
            a[pointer++] = (byte) l;
            byte c[] = b[i].getBytes();
            System.arraycopy (c, 0, a, pointer, l);
            pointer += l;
        }
        a[pointer] = 0;

        return (a);
    }

    /**
     * Combine two byte arrays into one
     *
     * @param one        one of the arrays
     * @param two        the other one
     * @return                byte array made from one and two
     */
    public static byte[] combine (byte one[], byte two[])
    {
        if (one == null)
            return (two);

        byte[] temp = new byte[one.length + two.length];
        System.arraycopy (one, 0, temp, 0, one.length);
        System.arraycopy (two, 0, temp, one.length, two.length);
        return (temp);
    }

    public static byte[] combine (byte one[], byte two)
    {
        byte[] a = new byte[two];

        if (one == null)
            return (a);

        byte[] temp = new byte[one.length + a.length];
        System.arraycopy (one, 0, temp, 0, one.length);
        System.arraycopy (a, 0, temp, one.length, a.length);
        return (temp);
    }

    public static byte[] trimbytearray (byte[] old, int length)
    {
        byte ret[] = new byte[length];
        System.arraycopy (old, 0, ret, 0, length);
        return (ret);
    }

    /**
     *  split the sting using the dots and reassemble backwards.
     */
    public static String reverseIP (String s)
    {
        Assert (s != null);

        String a[] = s.split ("\\.");

        // all dots
        if (a.length == 0) return s;

        String ret = "";

        for (int i = (a.length-1); i > 0; i--)
            ret += a[i] + ".";

        ret += a[0];

        return (ret);
    }

    /**
     * Return a string in reverse order
     * @param s        String to reverse
     * @return        The reversed string
     */
    public static String reverse (String s)
    {
        String r = "";

        for (int i = s.length() - 1; i >= 0; i--)
        {
            r += s.charAt (i);
        }
        return (r);
    }

    /**
     * How many times does one string exist in another?
     *
     * @param s        the String to search
     * @param c        what to search for
     * @return        the number of matches
     */
    public static int count (String s, String c)
    {
        int count = 0;
        int where = 0;

        while ((where = s.indexOf (c, where)) != -1)
        {
            where++;
            count++;
        }

        return (count);
    }

    /**
     * A mixed v6/v4 address -- convert both and return the result
     *
     * @return        the final answer
     */
    private static byte[] dodots (String s)
    {
        // split at the v6/v4 boundary
        int splitat = s.lastIndexOf (":");
        String colons = s.substring (0, splitat);
        String dots = s.substring (splitat + 1);

        if (colons.equals (":")) colons = "::";

        return (combine (docolons (colons, 12), IPV4 (dots)));
    }

    /**
     * Do all the v6 conversion
     *
     * @param s                the v6 String
     * @param length        how long the String is (16 for v6, 12 for v6/v4)
     * @return                the conversoin
     */
    private static byte[] docolons (String s, int length)
    {
        int colons = count (s, ":");
        byte ret[] = new byte[length];

        // nothing but colons
        if (s.equals ("::"))
        {
            for (int i = 0; i < length; i++) ret[i] = 0;
            return (ret);
        }

        // the number of missing bytes
        int len = (length / 2 - colons) * 2;

        String split[] = s.split ("\\:");
        int i = 0;
        int where = 0;

        // leading "::"
        if (split[0].equals ("") && split[1].equals (""))
        {
            // the leading two bytes are also missing
            len = length - 2;
            i = 1;
        }

        for (; i <= colons; i++)
        {
            // if this is where things are missing, fill in zeros
            if (split[i].equals (""))
            {
                for (int j = 0; j < len; j++)
                {
                    ret[where++] = 0;
                }
            }
            else
            {
                int conv = Integer.parseInt (split[i], 16);
                ret[where++] = getByte (conv, 2);
                ret[where++] = getByte (conv, 1);
            }
        }
        return (ret);
    }

    /**
     * Convert an IPv6 String into its byte array
     *
     * @param s        the IPv6 String
     * @return        the IPv6 bytes
     */
    public static byte[] IPV6 (String s)
    {
        // if this is an v6t/v4 address
        if (count (s, ".") > 0)
            return (dodots (s));

        return (docolons (s, 16));
    }

    // replace with built-in
    public static byte[] decodeBase64 (String s)
    {
        int k = 0;
        int base = 0;
        int equals = 0;
        int vals[] = new int[4];
        byte ret[] = null;

        for (int i = 0; i < s.length(); i++)
        {
            char j = s.charAt (i);

                 if (j >= 'A' && j <= 'Z')        vals[k++] = j - 'A';
            else if (j >= 'a' && j <= 'z')        vals[k++] = j - 'a' + 26;
            else if (j >= '0' && j <= '9')        vals[k++] = j - '0' + 52;
            else if (j == '+')                        vals[k++] = 62;
            else if (j == '/')                        vals[k++] = 63;
            else if (j == '=')
            {
                vals[k++] = 0;
                equals++;
            }
            else
            {
                // logger.severe ("Illegal Base64 value: " + j);
                break;
            }
            
            if (k == 4)
            {
                byte tmp[] = new byte[base + 3 - equals];
                if (ret != null)
                    System.arraycopy (ret, 0, tmp, 0, ret.length);
                ret = tmp;

                    ret[base++] = (byte) ((vals[0] << 2) + (vals[1] >> 4));
                if (equals < 2)
                    ret[base++] = (byte) ((vals[1] << 4) + (vals[2] >> 2));
                if (equals < 1)
                    ret[base++] = (byte) ((vals[2] << 6) + (vals[3]));
                k = 0;
            }
        }

        return (ret);
    }

    /**
     * Some unit tests
     */
    public static void main (String args[])
    {
        // Assert (false);
        /*
        System.out.println (toString (IPV4 ("65.66.67.68")));
        System.out.println (toString (IPV6 ("::")));
        System.out.println (toString (IPV6 ("::1")));
        System.out.println (toString (IPV6 ("1::2:3:4:5")));
        System.out.println (toString (IPV6 ("0:1:2:3:4:5:6:7")));
        System.out.println (toString
            (IPV6 ("FFFF:00FF:FF00:F00F:0FF0:F0F0:0F0F:0000")));
        System.out.println (toString
            (IPV6 ("FEDC:BA98:7654:3210:FEDC:BA98:7654:3210")));
        System.out.println (toString (IPV6 ("0:0:0:0:0:0:13.1.68.3")));
        System.out.println (toString (IPV6 ("0:0:0:0:0:FFFF:129.144.52.38")));
        System.out.println (toString (IPV6 ("::13.1.68.3")));
        System.out.println (toString (IPV6 ("::FFFF:129.144.52.38")));
        System.out.println (count ("65.66.67.68", "."));
        System.out.println (count ("65.66.67.68", "6"));
        System.out.println (reverse ("123"));
        System.out.println (reverse ("1234"));
        */
        System.out.println (reverseIP ("192.168.1.2"));

        int i;
        byte a[];

        System.out.println ("----");
        a  = decodeBase64 ("TWFuTWFu");
        for (i = 0; i < a.length; i++) System.out.println (a[i]);
        System.out.println ("----");

        a  = decodeBase64 ("TWF=");
        for (i = 0; i < a.length; i++) System.out.println (a[i]);
        System.out.println ("----");

        a  = decodeBase64 ("TW==");
        for (i = 0; i < a.length; i++) System.out.println (a[i]);
        System.out.println ("----");
    }

    public static String toString (DatagramPacket dgp)
    {
        String s = "";

        s += "getAddress() = " + dgp.getAddress() + "\n";
        s += "getLength() = " + dgp.getLength() + "\n";
        s += "getOffset() = " + dgp.getOffset() + "\n";
        s += "getPort() = " + dgp.getPort() + "\n";
        s += "getSocketAddress() = " + dgp.getSocketAddress() + "\n";
        s += "getData() = " + Utils.toString (dgp.getData());

        return (s);
    }

    public static SandN parseName (int start, byte buffer[])
    {
        logger.entering (start);

        if (start >= buffer.length)
        {
            logger.warning ("Illegal name");
            return (null);
        }

        int current = start;
        String name = "";

        // if the first thing is a compression
        if ((buffer[current] & 0xc0) == 0xc0)
        {
            int tmp = addThem (buffer[current] & 0x3f, buffer[current + 1]);
            logger.finest (tmp);

            if (tmp >= start)
            {
                logger.warning ("Illegal name");
                return (null);
            }

            SandN sn = parseName (tmp, buffer);
            if (sn == null)
                return (null);        // error already flagged

            name += sn.getString();
            current += 2;
        }

        int length = buffer[current++] & 0x3f;

        if (length == 0)
            return (new SandN ("", 0));

        while (length > 0)
        {
            for (int i = 1; i <= length; i++)
            {
                char c = (char) buffer[current];
                logger.finest ("Adding " + c);
                name += c;
                current++;
            }

            // if we get to the end of a real string and there's a
            // compression...
            if ((buffer[current] & 0xc0) == 0xc0)
            {
                name += ".";

                int tmp = addThem (buffer[current] & 0x3f, buffer[current + 1]);
                logger.finest (tmp);

                if (tmp >= start)
                {
                    logger.warning ("Illegal name");
                    return (null);
                }

                SandN sn = parseName (tmp, buffer);
                if (sn == null)
                    return (null);        // error already flagged

                name += sn.getString();
                current += 2;
            }

            // if there's more, put in the separator
            length = buffer[current++] & 0x3f;
            if (length > 0)
            {
                name += ".";
            }
        }

        SandN sn = new SandN (name, current);
        logger.exiting (sn);
        return (sn);
    }
}
