package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class OPTRRTest {
    byte[] bytes = {(byte) 0x00, (byte) 0x00, (byte) 0x29, (byte) 0x10,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80,
            (byte) 0x00, (byte) 0x00, (byte) 0x0c, (byte) 0x00,
            (byte) 0x0a, (byte) 0x00, (byte) 0x08, (byte) 0x33,
            (byte) 0x9c, (byte) 0xd1, (byte) 0xf3, (byte) 0xaf,
            (byte) 0x36, (byte) 0x46, (byte) 0x21};
    byte[] cookie = {(byte) 0x33,
            (byte) 0x9c, (byte) 0xd1, (byte) 0xf3, (byte) 0xaf,
            (byte) 0x36, (byte) 0x46, (byte) 0x21};
    byte[] buffer = {(byte) 0x6b, (byte) 0xcd, (byte) 0x01, (byte) 0x20,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x03, (byte) 0x77, (byte) 0x77, (byte) 0x77,
            (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
            (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x29, (byte) 0x10, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x0c, (byte) 0x00, (byte) 0x0a, (byte) 0x00,
            (byte) 0x08, (byte) 0xc2, (byte) 0x0f, (byte) 0xef,
            (byte) 0xfa, (byte) 0xb4, (byte) 0xa5, (byte) 0xdf,
            (byte) 0x5e};

    OPTRR optrr;
    Query query;

    @Before
    public void setUp() {
        this.optrr = new OPTRR(bytes);
        this.query = new Query(buffer);
        query.parseQueries("/0:0:0:0:0:");
    }

    @Test
    public void optrrTest() {
        Assert.assertTrue(optrr.isDNSSEC());
        Assert.assertTrue(Arrays.equals(optrr.getClientCookie(), cookie));
    }


    @Test
    public void getBytesTest(){
        Assert.assertArrayEquals(bytes, optrr.getBytes());
    }

    @Test
    public void createServerCookieTest() {
        Query copyQuery = new Query(buffer);
        copyQuery.parseQueries("/0:0:0:0:0:");
        optrr = query.getOptrr();
        Assert.assertNotNull(query.getOptrr());
        Assert.assertNotEquals(this.cookie, this.optrr.getClientCookie());
        Assert.assertTrue(query.getHeader().toString().equals(copyQuery.getHeader().toString()));
        Assert.assertArrayEquals(optrr.getServerCookie(), copyQuery.getOptrr().getServerCookie());

        optrr.createServerCookie("/0:0:0:0:0:0", this.query.getHeader());
        Assert.assertFalse(query.getHeader().toString().equals(copyQuery.getHeader().toString()));
        Assert.assertNotEquals(optrr.getServerCookie(), copyQuery.getOptrr().getServerCookie());
    }
}
