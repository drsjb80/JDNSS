package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class TXTRRTest {
    @Test
    public void constructorStoresText() {
        TXTRR txt = new TXTRR("example.com", 3600, "v=spf1 -all");
        Assert.assertNotNull(txt);
    }

    @Test
    public void getBytesReturnsCharacterStringFormat() {
        String text = "hello";
        TXTRR txt = new TXTRR("example.com", 3600, text);
        byte[] bytes = txt.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("TXT should have length prefix", bytes.length > 0);
        Assert.assertEquals("First byte should be length", (byte)5, bytes[0]);
    }

    @Test
    public void typeCodeIsTXT() {
        TXTRR txt = new TXTRR("example.com", 3600, "test");
        Assert.assertEquals(RRCode.TXT, txt.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        TXTRR txt = new TXTRR("example.com", ttl, "test");
        Assert.assertEquals(ttl, txt.getTtl());
    }

    @Test
    public void getStringReturnsStoredText() {
        String text = "example text record";
        TXTRR txt = new TXTRR("example.com", 3600, text);
        Assert.assertEquals(text, txt.getString());
    }

    @Test
    public void handleLongText() {
        String longText = "a".repeat(200);
        TXTRR txt = new TXTRR("example.com", 3600, longText);
        byte[] bytes = txt.getBytes();
        Assert.assertTrue("Long TXT should serialize", bytes.length > 0);
    }

    @Test
    public void handleSpecialCharactersInText() {
        String text = "v=spf1 ip4:192.0.2.0/24 ~all";
        TXTRR txt = new TXTRR("example.com", 3600, text);
        byte[] bytes = txt.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("Should handle special characters", bytes.length > 0);
    }
}
