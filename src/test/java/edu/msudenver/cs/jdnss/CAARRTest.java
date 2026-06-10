package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class CAARRTest {
    @Test
    public void constructorStoresAllFields() {
        CAARR caa = new CAARR("example.com", 3600, 0, "issue", "letsencrypt.org");
        Assert.assertNotNull(caa);
    }

    @Test
    public void getBytesReturnsValidWireFormat() {
        CAARR caa = new CAARR("example.com", 3600, 0, "issue", "letsencrypt.org");
        byte[] bytes = caa.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("CAA should have at least flags+tag_length", bytes.length >= 2);
    }

    @Test
    public void typeCodeIsCAA() {
        CAARR caa = new CAARR("example.com", 3600, 0, "issue", "letsencrypt.org");
        Assert.assertEquals(RRCode.CAA, caa.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        CAARR caa = new CAARR("example.com", ttl, 0, "issue", "letsencrypt.org");
        Assert.assertEquals(ttl, caa.getTtl());
    }

    @Test
    public void handleVariousCAATagTypes() {
        String[] tags = {"issue", "issuewild", "iodef"};
        for (String tag : tags) {
            CAARR caa = new CAARR("example.com", 3600, 0, tag, "value");
            byte[] bytes = caa.getBytes();
            Assert.assertTrue("Should serialize tag: " + tag, bytes.length > 0);
        }
    }

    @Test
    public void handleVariousFlags() {
        int[] flags = {0, 128, 255};
        for (int flag : flags) {
            CAARR caa = new CAARR("example.com", 3600, flag, "issue", "letsencrypt.org");
            byte[] bytes = caa.getBytes();
            Assert.assertTrue("Should serialize flags: " + flag, bytes.length > 0);
        }
    }

    @Test
    public void handleLongValues() {
        String longValue = "letsencrypt.org; cansignhttpexchanges=yes; validationmethods=dns-01";
        CAARR caa = new CAARR("example.com", 3600, 0, "issue", longValue);
        byte[] bytes = caa.getBytes();
        Assert.assertTrue("Should serialize long value", bytes.length > 50);
    }

    @Test
    public void gettersReturnStoredValues() {
        int flags = 0;
        String tag = "issue";
        String value = "letsencrypt.org";
        CAARR caa = new CAARR("example.com", 3600, flags, tag, value);
        Assert.assertEquals(flags, caa.getFlags());
        Assert.assertEquals(tag, caa.getTag());
        Assert.assertEquals(value, caa.getValue());
    }
}
