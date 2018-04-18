package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.HashSet;
import java.util.Set;

public class RRsTest
{
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void rrsTest1() {
        byte one[] =
        {
            0x03, 'w', 'w', 'w',
            0x04, 't', 'e', 's', 't',
            0x03, 'c', 'o', 'm',
            0x00,
            0x00, 0x01, 0x00, 0x00
        };

        RRs rrs1 = new RRs(one, 1, 0, 0, 0);

        String expectedRRs1 = "Questions:\n" +
            "name = www.test.com, type = A, TTL = 0";

        Assert.assertEquals(rrs1.toString(), expectedRRs1);
    }

    @Test
    public void rrsTest2() {
        byte two[] =
        {
            0x03, 'w', 'w', 'w',
            0x04, 't', 'e', 's', 't',
            0x03, 'c', 'o', 'm',
            0x00,
            0x00, 0x01, 0x00, 0x00,
            0x04, 'm', 'a', 'i', 'l',
            (byte) 0xc0, 0x04, 0x00,
            0x00, 0x01, 0x00, 0x00
        };

        RRs rrs2 = new RRs(two, 2, 0, 0, 0);

        String expectedRRs2 = "Questions:\n" +
            "name = www.test.com, type = A, TTL = 0\n" +
            "name = mail.test.com, type = A, TTL = 0";

        Assert.assertEquals(rrs2.toString(), expectedRRs2);
    }

    // The two following check for bad names.
    @Test
    public void rrsTest3() {
        byte three[] = {(byte) 0xc0, 0x00, 0x01, 0x00, 0x00, 0x00};

        exception.expect (AssertionError.class);
        RRs rrs3 = new RRs(three, 1, 0, 0, 0);

        String expectedRRs3 = "Questions:\n" +
            "null";

        Assert.assertEquals(rrs3.toString(), expectedRRs3);
    }

    @Test
    public void rrsTest4() {
        byte four[] =
            {
                0x03, 'w', 'w', 'w',
                0x04, 't', 'e', 's', 't',
                0x03, 'c', 'o', 'm',
                (byte) 0xc0, 0x00, 0x0,
                0x00, 0x01, 0x00, 0x000
            };

        exception.expect(AssertionError.class);
        RRs rrs4 = new RRs(four, 1, 0, 0, 0);

        String expectedRRs4 = "Questions:\n" +
                "null";

        Assert.assertEquals(rrs4.toString(), expectedRRs4);
    }
    @Test
    public void nsecGetBytesTest() {
        byte expected[] = {0x04, 'h', 'o', 's', 't',
                0x07, 'e', 'x', 'a', 'm', 'p', 'l', 'e',
                0x03, 'c', 'o', 'm', 0x00,0x00,
                0x00, 0x06, 0x40, 0x01, 0x00, 0x00, 0x00, 0x03};
        Set<RRCode> rrSet = new HashSet<>();
        rrSet.add(RRCode.A);
        rrSet.add(RRCode.MX);
        rrSet.add(RRCode.RRSIG);
        rrSet.add(RRCode.NSEC);
        NSECRR nsec = new NSECRR("alfa.example.com.",
                86400,"host.example.com.", rrSet);

        Assert.assertArrayEquals(expected, nsec.getBytes());
    }

    @Test
    public void nsecGetBytesTest2() {
        byte expected[] = {0x04, 'h', 'o', 's', 't',
                0x07, 'e', 'x', 'a', 'm', 'p', 'l', 'e',
                0x03, 'c', 'o', 'm', 0x00,0x00,
                0x00, 0x07, 0x40, 0x00, 0x00, 0x08, 0x00, 0x03, 0x30};
        Set<RRCode> rrSet = new HashSet<>();
        rrSet.add(RRCode.A);
        rrSet.add(RRCode.AAAA);
        rrSet.add(RRCode.RRSIG);
        rrSet.add(RRCode.NSEC);
        rrSet.add(RRCode.NSEC3);
        rrSet.add(RRCode.NSEC3PARAM);
        NSECRR nsec = new NSECRR("alfa.example.com.",
                86400,"host.example.com.", rrSet);
        Assert.assertArrayEquals(expected, nsec.getBytes());
    }
}
