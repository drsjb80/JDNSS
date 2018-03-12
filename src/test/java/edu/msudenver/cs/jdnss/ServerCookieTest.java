package edu.msudenver.cs.jdnss;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ServerCookieTest {

    ServerCookie serverCookie;
    String clientIPaddress = "/0:0:0:0:0:0:0:1";
    byte[] cookie = {(byte) 0x33,
            (byte) 0x9c, (byte) 0xd1, (byte) 0xf3, (byte) 0xaf,
            (byte) 0x36, (byte) 0x46, (byte) 0x21};
    byte[] differentCookie = {(byte)0x21,
            (byte) 0x46, (byte) 0x36, (byte) 0xaf, (byte) 0xf3,
            (byte) 0xd1, (byte) 0x9c, (byte) 0x33};

    @Before
    public void setup() {
        serverCookie = new ServerCookie(cookie, clientIPaddress);
    }

    @Test
    public void isValidTest() {
        Assert.assertTrue(serverCookie.isValid(cookie, clientIPaddress));
        Assert.assertFalse(serverCookie.isValid(cookie, "not a good ip"));
        Assert.assertFalse(serverCookie.isValid(differentCookie, clientIPaddress));
        Assert.assertFalse(serverCookie.isValid(differentCookie, "not a good ip"));
    }
}

