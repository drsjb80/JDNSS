package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/*
** N.B.: we use (byte) casts to make sure we're not dealing with signed
** integers in the constants, but that they are really unsigned bytes.
*/

public class UtilsTest
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void count()
    {
        Assert.assertEquals (Utils.count ("foo", "bar"), 0);
        Assert.assertEquals (Utils.count ("", "bar"), 0);
        Assert.assertEquals (Utils.count ("foo", ""), 0);
        Assert.assertEquals (Utils.count ("foo", "foo"), 1);
        Assert.assertEquals (Utils.count ("foofoofoo", "foo"), 3);

        exception.expect (AssertionError.class);
        Utils.count (null, "bar");
        Utils.count ("foo", null);
    }

    @Test
    public void reverse()
    {
        Assert.assertEquals (Utils.reverse (""), "");
        Assert.assertEquals (Utils.reverse ("foo"), "oof");

        exception.expect (AssertionError.class);
        Utils.reverse (null);
    }

    @Test
    public void reverse_IP()
    {
        Assert.assertEquals (Utils.reverseIP ("192.168.1.2"), "2.1.168.192");
        Assert.assertEquals (Utils.reverseIP (""), "");
        Assert.assertEquals (Utils.reverseIP ("foo"), "foo");
        Assert.assertEquals (Utils.reverseIP ("."), ".");
        Assert.assertEquals (Utils.reverseIP ("..."), "...");

        exception.expect (AssertionError.class);
        Utils.reverseIP (null);
    }

    @Test
    public void getByte()
    {
        Assert.assertEquals (Utils.getByte (0xfff00f00, 1), 0x00);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 2), 0x0f);
    }

    @Test
    public void getTwoBytes()
    {
        Assert.assertTrue (Arrays.equals (Utils.getTwoBytes (0xfff00f00, 2),
            new byte[]{(byte) 0x0f, (byte) 0x00}));

        Assert.assertTrue (Arrays.equals (Utils.getTwoBytes (0xfff00f00, 3),
            new byte[]{(byte) 0xf0, (byte) 0x0f}));

        Assert.assertTrue (Arrays.equals (Utils.getTwoBytes (0xfff00f00, 4),
            new byte[]{(byte) 0xff, (byte) 0xf0}));

        exception.expect (AssertionError.class);
        Utils.getTwoBytes (0xfff0f0, 0);
        Utils.getTwoBytes (0xfff0f0, 5);
    }

    @Test
    public void getBytes()
    {
        Assert.assertTrue (Arrays.equals (Utils.getBytes (0xfedc1234),
            new byte[]{(byte) 0xfe, (byte) 0xdc, (byte) 0x12, (byte) 0x34}));
    }
        
    @Test
    public void getNybble()
    {
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 1), (byte) 0x0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 2), (byte) 0x0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 3), (byte) 0xf);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 4), (byte) 0x0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 5), (byte) 0x0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 6), (byte) 0xf);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 7), (byte) 0xf);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 8), (byte) 0xf);

        exception.expect (AssertionError.class);
        Utils.getNybble (1, 0);
        Utils.getNybble (1, 9);
    }

    @Test
    public void twoBytesAddThem()
    {
        Assert.assertEquals (Utils.addThem ((byte) 0xff, (byte) 0x00), 65280);
        Assert.assertEquals (Utils.addThem ((byte) 0x00, (byte) 0xff), 255);
        Assert.assertEquals (Utils.addThem ((byte) 0x00, (byte) 0x00), 0);
        Assert.assertEquals (Utils.addThem ((byte) 0xff, (byte) 0xff), 65535);
    }

    @Test
    public void twoIntsAddThem()
    {
        Assert.assertEquals (Utils.addThem (0xffffffff, 0x00000), 65280);
        Assert.assertEquals (Utils.addThem (0x00000, 0xffffffff), 255);
        Assert.assertEquals (Utils.addThem (0x00000, 0x00000), 0);
        Assert.assertEquals (Utils.addThem (0xffffffff, 0xffffffff), 65535);
    }

    @Test
    public void fourBytesAddThem()
    {
        /*
        ** from: https://www.ietf.org/rfc/rfc1035.txt
        ** 2.3.4. Size limits
        ** TTL             positive values of a signed 32 bit number.
        **
        ** so, if this is only ever used for TTL, we're okay
        */
        Assert.assertEquals (Utils.addThem
            ((byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff), -1);

        /*
        ** The number 2,147,483,647 is the eighth Mersenne prime. It is
        ** one of only four known double Mersenne primes.
        ** https://en.wikipedia.org/wiki/2147483647_(number)
        */
        Assert.assertEquals (Utils.addThem
            ((byte) 0x7f, (byte) 0xff, (byte) 0xff, (byte) 0xff), 2147483647);
    }

    @Test
    public void IPV4()
    {
        Assert.assertTrue (Arrays.equals (Utils.IPV4 ("0.0.0.0"),
            new byte[]{0, 0, 0, 0}));
        Assert.assertTrue (Arrays.equals (Utils.IPV4 ("192.168.1.1"),
            new byte[]{(byte) 192, (byte) 168, (byte) 1, (byte) 1}));
    }

    @Test
    public void toCS()
    {
        Assert.assertTrue (Arrays.equals (Utils.toCS ("this"),
            new byte[]{(byte) 4, 't', 'h', 'i', 's'}));

        exception.expect (AssertionError.class);
        Utils.toCS ("");
        Utils.toCS (null);
    }

    @Test
    public void convertString()
    {
        Assert.assertTrue (Arrays.equals (Utils.convertString ("foo"),
            new byte[] {3, 'f', 'o', 'o', 0}));
        Assert.assertTrue (Arrays.equals (Utils.convertString
            ("www.foobar.org"), new byte[]
            {3,'w','w','w',6,'f','o','o','b','a','r',3,'o','r','g',0}));

        exception.expect (AssertionError.class);
        Utils.convertString ("");
        Utils.convertString (null);
    }

    @Test
    public void twoByteArraysCombine()
    {
        Assert.assertTrue (Arrays.equals (
            Utils.combine (new byte[]{1}, new byte[]{2}),
            new byte[]{1, 2}));
        Assert.assertTrue (Arrays.equals (
            Utils.combine (new byte[]{1}, null),
            new byte[]{1}));
        Assert.assertTrue (Arrays.equals (
            Utils.combine (null, new byte[]{1}),
            new byte[]{1}));

        exception.expect (AssertionError.class);
        Utils.combine (null, null);
    }

    @Test
    public void byteArrayAndByteCombine()
    {
        Assert.assertTrue (Arrays.equals (
            Utils.combine (new byte[]{1}, (byte) 2),
            new byte[]{1, 2}));
        Assert.assertTrue (Arrays.equals (
            Utils.combine (new byte[]{1, 2}, (byte) 3),
            new byte[]{1, 2, 3}));
        Assert.assertTrue (Arrays.equals (
            Utils.combine (null, (byte) 2),
            new byte[]{2}));
    }

    @Test
    public void trimByteArray()
    {
        byte initial[] = {0x001, 0x002, 0x003, 0x004};

        Assert.assertTrue (Arrays.equals (Utils.trimByteArray (initial, 1),
            new byte[]{0x001}));
        Assert.assertTrue (Arrays.equals (Utils.trimByteArray (initial, 2),
            new byte[]{0x001, 0x002}));
        Assert.assertTrue (Arrays.equals (Utils.trimByteArray (initial, 4),
            initial));

        exception.expect (AssertionError.class);
        Utils.trimByteArray (null, 2);
        Utils.trimByteArray (initial, 0);
        Utils.trimByteArray (initial, 5);
    }

    @Test
    public void findLongest ()
    {
        /*
        ** Say we have a bunch of domains that end similarly. We need to
        ** find the one that is the longest match for a requested domain.
        */

        Set<String> v = new HashSet<>();

        v.add ("b.c.d.e");
        Assert.assertTrue (
            Utils.findLongest (v, "a.b.c.d.e").equals("b.c.d.e"));

        v = new HashSet<>();
        v.add ("d.e");
        v.add ("b.c.d.e");
        Assert.assertTrue (
            Utils.findLongest (v, "z.d.e").equals("d.e"));
        Assert.assertTrue (
            Utils.findLongest (v, "z.c.d.e").equals("d.e"));
        Assert.assertTrue (
            Utils.findLongest (v, "a.b.c.d.e").equals("b.c.d.e"));

        exception.expect (AssertionError.class);

        v = new HashSet<>();
        v.add ("");
        Utils.findLongest (null, "string");
        Utils.findLongest (v, "string");

        v = new HashSet<>();
        v.add ("foo");
        Utils.findLongest (v, "bar");
        Utils.findLongest (v, null);
        Utils.findLongest (v, "");
    }

    @Test
    public void IPV6()
    {
        // IPv6 unassigned
        Assert.assertTrue (Arrays.equals (Utils.IPV6 ("::"),
            new byte[]
            {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00 
            }
        ));

        // System.out.println (Arrays.toString (Utils.IPV6 ("::1")));
        // IPv6 localhost
        Assert.assertTrue (Arrays.equals (Utils.IPV6 ("::1"),
            new byte[]
            {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01 
            }
        ));

        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"),
            new byte[]
            {
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF 
            }
        ));

        // 0's by themselves
        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("2001:db8:85a3:0:0:8a2e:370:7334"),
            new byte[]
            {
                (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
                (byte) 0x85, (byte) 0xa3, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x8a, (byte) 0x2e,
                (byte) 0x03, (byte) 0x70, (byte) 0x73, (byte) 0x34 
            }
        ));

        /*
        System.out.println (Arrays.toString (Utils.IPV6
            ("2001:db8:85a3::8a2e:370:7334")));
        */
        // less than four hex digits
        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("2001:db8:85a3::8a2e:370:7334"),
            new byte[]
            {
                (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
                (byte) 0x85, (byte) 0xa3, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x8a, (byte) 0x2e,
                (byte) 0x03, (byte) 0x70, (byte) 0x73, (byte) 0x34 
            }
        ));

        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("2001:0db8::0001"),
            new byte[]
            {
                (byte) 0x20, (byte) 0x01, (byte) 0x0d, (byte) 0xb8,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01
            }
        ));

        // System.out.println (Arrays.toString (Utils.IPV6 ("::ffff:c000:0280")));
        // mapped v4
        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("::ffff:c000:0280"),
            new byte[]
            {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff,
                (byte) 192,  (byte) 0,    (byte) 2,    (byte) 128
            }
        ));

        // mapped v4 again
        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("::ffff:192.0.2.128"),
            new byte[]
            {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0xff, (byte) 0xff,
                (byte) 192,  (byte) 0,    (byte) 2,    (byte) 128
            }
        ));

        // mapped v4 and again
        Assert.assertTrue (Arrays.equals (
            Utils.IPV6 ("::192.0.2.128"),
            new byte[]
            {
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 192,  (byte) 0,    (byte) 2,    (byte) 128
            }
        ));


    }
}
