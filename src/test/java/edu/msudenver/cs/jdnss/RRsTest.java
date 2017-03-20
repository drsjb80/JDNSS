package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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

        exception.expect (AssertionError.class);
        RRs rrs4 = new RRs (four, 1, 0, 0, 0);

        String expectedRRs4 = "Questions:\n" +
            "null";

        Assert.assertEquals(rrs4.toString(), expectedRRs4);
    }
}
