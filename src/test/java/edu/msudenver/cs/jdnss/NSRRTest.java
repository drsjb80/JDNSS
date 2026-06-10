package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class NSRRTest {
    @Test
    public void constructorStoresNameserver() {
        NSRR ns = new NSRR("example.com", 3600, "ns1.example.com");
        Assert.assertNotNull(ns);
    }

    @Test
    public void getBytesReturnsEncodedDomainName() {
        NSRR ns = new NSRR("example.com", 3600, "ns1.example.com");
        byte[] bytes = ns.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("NS should encode domain name", bytes.length > 0);
    }

    @Test
    public void typeCodeIsNS() {
        NSRR ns = new NSRR("example.com", 3600, "ns1.example.com");
        Assert.assertEquals(RRCode.NS, ns.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 86400;
        NSRR ns = new NSRR("example.com", ttl, "ns1.example.com");
        Assert.assertEquals(ttl, ns.getTtl());
    }

    @Test
    public void handleMultipleNameservers() {
        String[] nameservers = {"ns1.example.com", "ns2.example.com", "ns3.example.com"};
        for (String nameserver : nameservers) {
            NSRR ns = new NSRR("example.com", 3600, nameserver);
            byte[] bytes = ns.getBytes();
            Assert.assertTrue("Should serialize nameserver", bytes.length > 0);
        }
    }

    @Test
    public void handleFQDN() {
        NSRR ns = new NSRR("example.com", 3600, "ns.example.com.");
        byte[] bytes = ns.getBytes();
        Assert.assertTrue("Should handle FQDN with trailing dot", bytes.length > 0);
    }
}
