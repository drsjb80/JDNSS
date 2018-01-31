package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class OPTRRTest {
    byte[] bytes = {(byte) 0x00, (byte) 0x00, (byte) 0x29, (byte) 0x10,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
            (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00,
            (byte) 0x0a, (byte) 0x00, (byte) 0x08, (byte) 0x33,
            (byte) 0x9c, (byte) 0xd1, (byte) 0xf3, (byte) 0xaf,
            (byte) 0x36, (byte) 0x46, (byte) 0x21
    };
    byte[] cookie = {(byte) 0x33,
            (byte) 0x9c, (byte) 0xd1, (byte) 0xf3, (byte) 0xaf,
            (byte) 0x36, (byte) 0x46, (byte) 0x21};

    OPTRR optrr = new OPTRR(bytes);

    @Test
    public void optrrTest() {
        Assert.assertTrue(optrr.isDNSSEC());
        Assert.assertTrue(Arrays.equals(optrr.getClientCookie(), cookie));
    }
}
