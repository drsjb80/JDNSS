package edu.msudenver.cs.jdnss;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class UtilsTest
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void reverse_IP()
    {
        Assert.assertEquals (Utils.reverseIP ("192.168.1.2"), "2.1.168.192");
        Assert.assertEquals (Utils.reverseIP (""), "");
        Assert.assertEquals (Utils.reverseIP ("foo"), "foo");
        Assert.assertEquals (Utils.reverseIP ("."), ".");
        Assert.assertEquals (Utils.reverseIP ("..."), "...");

        exception.expect (IllegalArgumentException.class);
        Assert.assertEquals (Utils.reverseIP (null), null);
    }

    @Test
    public void getByte()
    {
        Assert.assertEquals (Utils.getByte (0xfff00f00, 1), (byte) 0);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 2), (byte) 15);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 3), (byte) 240);
        Assert.assertEquals (Utils.getByte (0xfff00f00, 4), (byte) 255);
    }

    @Test
    public void getTwoBytes()
    {
        byte good1[] = {(byte) 0x0f, (byte) 0x00};
        byte test1[] = Utils.getTwoBytes (0xfff00f00, 2);
        Assert.assertTrue (good1[0] == test1[0]);
        Assert.assertTrue (good1[1] == test1[1]);

        byte good2[] = {(byte) 0xf0, (byte) 0x0f};
        byte test2[] = Utils.getTwoBytes (0xfff00f00, 3);
        Assert.assertTrue (good2[0] == test2[0]);
        Assert.assertTrue (good2[1] == test2[1]);

        byte good3[] = {(byte) 0xff, (byte) 0xf0};
        byte test3[] = Utils.getTwoBytes (0xfff00f00, 4);
        Assert.assertTrue (good3[0] == test3[0]);
        Assert.assertTrue (good3[1] == test3[1]);

        exception.expect (IllegalArgumentException.class);
        Utils.getTwoBytes (0xfff00f00, 0);
        Utils.getTwoBytes (0xfff00f00, 5);
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
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 1), (byte) 0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 2), (byte) 0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 3), (byte) 15);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 4), (byte) 0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 5), (byte) 0);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 6), (byte) 15);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 7), (byte) 15);
        Assert.assertEquals (Utils.getNybble (0xfff00f00, 8), (byte) 15);

        exception.expect (IllegalArgumentException.class);
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
        Assert.assertEquals (Utils.addThem (0xffffffff, 0x00000000), 65280);
        Assert.assertEquals (Utils.addThem (0x00000000, 0xffffffff), 255);
        Assert.assertEquals (Utils.addThem (0x00000000, 0x00000000), 0);
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

        exception.expect (IllegalArgumentException.class);
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

        exception.expect (IllegalArgumentException.class);
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

        exception.expect (IllegalArgumentException.class);
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
}
