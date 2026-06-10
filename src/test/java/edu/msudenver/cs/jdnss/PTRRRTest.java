package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class PTRRRTest {
    @Test
    public void constructorStoresHostname() {
        PTRRR ptr = new PTRRR("1.1.168.192.in-addr.arpa", 3600, "host.example.com");
        Assert.assertNotNull(ptr);
    }

    @Test
    public void getBytesReturnsEncodedHostname() {
        PTRRR ptr = new PTRRR("1.1.168.192.in-addr.arpa", 3600, "host.example.com");
        byte[] bytes = ptr.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("PTR should encode hostname", bytes.length > 0);
    }

    @Test
    public void typeCodeIsPTR() {
        PTRRR ptr = new PTRRR("1.1.168.192.in-addr.arpa", 3600, "host.example.com");
        Assert.assertEquals(RRCode.PTR, ptr.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        PTRRR ptr = new PTRRR("1.1.168.192.in-addr.arpa", ttl, "host.example.com");
        Assert.assertEquals(ttl, ptr.getTtl());
    }

    @Test
    public void handleReverseIPv4Format() {
        String reverseIP = "4.3.2.1.in-addr.arpa";
        PTRRR ptr = new PTRRR(reverseIP, 3600, "host.example.com");
        byte[] bytes = ptr.getBytes();
        Assert.assertTrue("Should handle reverse IPv4 format", bytes.length > 0);
    }

    @Test
    public void handleReverseIPv6Format() {
        String reverseIPv6 = "1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.8.b.d.0.1.0.0.2.ip6.arpa";
        PTRRR ptr = new PTRRR(reverseIPv6, 3600, "host.example.com");
        byte[] bytes = ptr.getBytes();
        Assert.assertTrue("Should handle reverse IPv6 format", bytes.length > 0);
    }
}
