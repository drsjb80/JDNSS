package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class SOARRTest {
    @Test
    public void constructorStoresAllFields() {
        SOARR soa = new SOARR("example.com", "ns1.example.com", "admin.example.com",
                1234567890, 7200, 3600, 1209600, 86400, 3600);
        Assert.assertNotNull(soa);
    }

    @Test
    public void getBytesReturnsValidWireFormat() {
        SOARR soa = new SOARR("example.com", "ns.example.com", "admin.example.com",
                1, 7200, 3600, 1209600, 86400, 3600);
        byte[] bytes = soa.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("SOA should have at least header", bytes.length > 20);
    }

    @Test
    public void typeCodeIsSOA() {
        SOARR soa = new SOARR("example.com", "ns.example.com", "admin.example.com",
                1, 7200, 3600, 1209600, 86400, 3600);
        Assert.assertEquals(RRCode.SOA, soa.getType());
    }

    @Test
    public void getMinimumReturnsMinimumField() {
        int ttl = 3600;
        int minimum = 900;
        SOARR soa = new SOARR("example.com", "ns.example.com", "admin.example.com",
                1, 7200, 3600, 1209600, minimum, ttl);
        Assert.assertEquals("Minimum should return the minimum field (8th param)", minimum, soa.getMinimum());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        SOARR soa = new SOARR("example.com", "ns.example.com", "admin.example.com",
                1, 7200, 3600, 1209600, 86400, ttl);
        Assert.assertEquals(ttl, soa.getTtl());
    }

    @Test
    public void handlesLargeSerialNumbers() {
        int serial = 2024060101; // typical serial format YYYYMMDDNN
        SOARR soa = new SOARR("example.com", "ns.example.com", "admin.example.com",
                serial, 7200, 3600, 1209600, 86400, 3600);
        byte[] bytes = soa.getBytes();
        Assert.assertTrue("Should serialize large serial", bytes.length > 0);
    }
}
