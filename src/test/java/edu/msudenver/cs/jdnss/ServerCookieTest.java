package edu.msudenver.cs.jdnss;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class ServerCookieTest {

    private ServerCookie serverCookie;
    private final String clientIPaddress = "/0:0:0:0:0:0:0:1";
    private final byte[] cookie = {(byte) 0x33,
            (byte) 0x9c, (byte) 0xd1, (byte) 0xf3, (byte) 0xaf,
            (byte) 0x36, (byte) 0x46, (byte) 0x21};
    private final byte[] differentCookie = {(byte)0x21,
            (byte) 0x46, (byte) 0x36, (byte) 0xaf, (byte) 0xf3,
            (byte) 0xd1, (byte) 0x9c, (byte) 0x33};

    @Before
    public void setup() throws UnsupportedEncodingException {
        serverCookie = new ServerCookie(cookie, clientIPaddress);
    }

    @Test
    public void isValidTest() throws UnsupportedEncodingException {
        Assert.assertTrue(serverCookie.isValid(cookie, clientIPaddress));
        Assert.assertFalse(serverCookie.isValid(cookie, "not a good ip"));
        Assert.assertFalse(serverCookie.isValid(differentCookie, clientIPaddress));
        Assert.assertFalse(serverCookie.isValid(differentCookie, "not a good ip"));
    }
    
    @Test
    public void isValidPreservesArbitraryCookieBytes() throws UnsupportedEncodingException {
        byte[] rawCookie = new byte[] {(byte) 0xc3, (byte) 0x28, (byte) 0xff, (byte) 0x00,
                (byte) 0x10, (byte) 0x80, (byte) 0x7f, (byte) 0x01};
        ServerCookie arbitraryCookie = new ServerCookie(rawCookie, clientIPaddress);

        Assert.assertTrue(arbitraryCookie.isValid(rawCookie, clientIPaddress));
        Assert.assertFalse(arbitraryCookie.isValid(Arrays.copyOf(rawCookie, rawCookie.length),
                "not a good ip"));
    }
}

