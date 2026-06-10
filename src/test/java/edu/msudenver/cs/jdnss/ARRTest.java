package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class ARRTest {
    @Test
    public void constructorStoresAddress() {
        ARR arr = new ARR("example.com", 3600, "192.168.1.1");
        Assert.assertNotNull(arr);
    }

    @Test
    public void getBytesReturnsValidIPv4Bytes() {
        ARR arr = new ARR("example.com", 3600, "192.168.1.1");
        byte[] bytes = arr.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertEquals("IPv4 should be 4 bytes", 4, bytes.length);
        Assert.assertEquals("First octet", (byte)192, bytes[0]);
        Assert.assertEquals("Second octet", (byte)168, bytes[1]);
        Assert.assertEquals("Third octet", (byte)1, bytes[2]);
        Assert.assertEquals("Fourth octet", (byte)1, bytes[3]);
    }

    @Test
    public void getBytesHandlesVariousIPv4Addresses() {
        String[] testIPs = {"0.0.0.0", "127.0.0.1", "255.255.255.255", "10.20.30.40"};
        for (String ip : testIPs) {
            ARR arr = new ARR("test.com", 3600, ip);
            byte[] bytes = arr.getBytes();
            Assert.assertEquals("Each IPv4 should be 4 bytes", 4, bytes.length);
        }
    }

    @Test
    public void typeCodeIsA() {
        ARR arr = new ARR("example.com", 3600, "192.0.2.1");
        Assert.assertEquals(RRCode.A, arr.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        ARR arr = new ARR("example.com", ttl, "192.0.2.1");
        Assert.assertEquals(ttl, arr.getTtl());
    }
}
