package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class AAAARRTest {
    @Test
    public void constructorStoresAddress() {
        AAAARR aaaa = new AAAARR("example.com", 3600, "2001:db8::1");
        Assert.assertNotNull(aaaa);
    }

    @Test
    public void getBytesReturnsValidIPv6Bytes() {
        AAAARR aaaa = new AAAARR("example.com", 3600, "::1");
        byte[] bytes = aaaa.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertEquals("IPv6 should be 16 bytes", 16, bytes.length);
    }

    @Test
    public void getBytesHandlesFullIPv6Address() {
        AAAARR aaaa = new AAAARR("example.com", 3600, "2001:0db8:85a3:0000:0000:8a2e:0370:7334");
        byte[] bytes = aaaa.getBytes();
        Assert.assertEquals("Full IPv6 should be 16 bytes", 16, bytes.length);
    }

    @Test
    public void getBytesHandlesCompressedIPv6Address() {
        String[] testIPs = {"::", "::1", "fe80::", "2001:db8::1"};
        for (String ip : testIPs) {
            AAAARR aaaa = new AAAARR("test.com", 3600, ip);
            byte[] bytes = aaaa.getBytes();
            Assert.assertEquals("Compressed IPv6 should be 16 bytes", 16, bytes.length);
        }
    }

    @Test
    public void typeCodeIsAAAA() {
        AAAARR aaaa = new AAAARR("example.com", 3600, "2001:db8::1");
        Assert.assertEquals(RRCode.AAAA, aaaa.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        AAAARR aaaa = new AAAARR("example.com", ttl, "2001:db8::1");
        Assert.assertEquals(ttl, aaaa.getTtl());
    }
}
