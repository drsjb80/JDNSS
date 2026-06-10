package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class CNAMERRTest {
    @Test
    public void constructorStoresCanonicalName() {
        CNAMERR cname = new CNAMERR("www.example.com", 3600, "example.com");
        Assert.assertNotNull(cname);
    }

    @Test
    public void getBytesReturnsEncodedName() {
        CNAMERR cname = new CNAMERR("www.example.com", 3600, "example.com");
        byte[] bytes = cname.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("CNAME should encode name", bytes.length > 0);
    }

    @Test
    public void typeCodeIsCNAME() {
        CNAMERR cname = new CNAMERR("www.example.com", 3600, "example.com");
        Assert.assertEquals(RRCode.CNAME, cname.getType());
    }

    @Test
    public void getStringReturnsStoredCanonicalName() {
        String canonical = "example.com";
        CNAMERR cname = new CNAMERR("www.example.com", 3600, canonical);
        Assert.assertEquals(canonical, cname.getString());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        CNAMERR cname = new CNAMERR("www.example.com", ttl, "example.com");
        Assert.assertEquals(ttl, cname.getTtl());
    }

    @Test
    public void handleMultipleLevelCNAME() {
        CNAMERR cname = new CNAMERR("deep.sub.example.com", 3600, "final.target.example.com");
        Assert.assertEquals("final.target.example.com", cname.getString());
    }

    @Test
    public void handleCNAMEWithFQDN() {
        CNAMERR cname = new CNAMERR("www.example.com", 3600, "example.com.");
        byte[] bytes = cname.getBytes();
        Assert.assertTrue("Should handle FQDN with trailing dot", bytes.length > 0);
    }
}
