package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class MXRRTest {
    @Test
    public void constructorStoresPreferenceAndHost() {
        MXRR mx = new MXRR("example.com", 3600, "mail.example.com", 10);
        Assert.assertNotNull(mx);
    }

    @Test
    public void getBytesReturnsValidWireFormat() {
        MXRR mx = new MXRR("example.com", 3600, "mail.example.com", 10);
        byte[] bytes = mx.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("MX should have preference (2 bytes) + host", bytes.length > 2);
    }

    @Test
    public void typeCodeIsMX() {
        MXRR mx = new MXRR("example.com", 3600, "mail.example.com", 10);
        Assert.assertEquals(RRCode.MX, mx.getType());
    }

    @Test
    public void getPreferenceReturnsStoredValue() {
        int preference = 20;
        MXRR mx = new MXRR("example.com", 3600, "mail.example.com", preference);
        Assert.assertEquals(preference, mx.getPreference());
    }

    @Test
    public void getHostReturnsStoredValue() {
        String host = "mail.example.com";
        MXRR mx = new MXRR("example.com", 3600, host, 10);
        Assert.assertEquals(host, mx.getHost());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        MXRR mx = new MXRR("example.com", ttl, "mail.example.com", 10);
        Assert.assertEquals(ttl, mx.getTtl());
    }

    @Test
    public void handleMultiplePriorities() {
        int[] preferences = {10, 20, 30};
        for (int pref : preferences) {
            MXRR mx = new MXRR("example.com", 3600, "mail" + pref + ".example.com", pref);
            Assert.assertEquals(pref, mx.getPreference());
        }
    }

    @Test
    public void handleZeroPreference() {
        MXRR mx = new MXRR("example.com", 3600, "mail.example.com", 0);
        Assert.assertEquals(0, mx.getPreference());
    }
}
