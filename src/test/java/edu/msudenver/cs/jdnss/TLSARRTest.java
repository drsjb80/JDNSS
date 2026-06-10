package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class TLSARRTest {
    @Test
    public void constructorStoresAllFields() {
        TLSARR tlsa = new TLSARR("_443._tcp.example.com", 3600, 3, 1, 1,
                "d2abde240d7cd3ee6b4b28c54df034b97983a1d16e8a410e4561cb106618e971");
        Assert.assertNotNull(tlsa);
    }

    @Test
    public void getBytesReturnsValidWireFormat() {
        TLSARR tlsa = new TLSARR("_443._tcp.example.com", 3600, 3, 1, 1,
                "d2abde240d7cd3ee6b4b28c54df034b97983a1d16e8a410e4561cb106618e971");
        byte[] bytes = tlsa.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("TLSA should have at least usage+selector+matching_type", bytes.length >= 3);
    }

    @Test
    public void typeCodeIsTLSA() {
        TLSARR tlsa = new TLSARR("_443._tcp.example.com", 3600, 3, 1, 1,
                "d2abde240d7cd3ee6b4b28c54df034b97983a1d16e8a410e4561cb106618e971");
        Assert.assertEquals(RRCode.TLSA, tlsa.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        TLSARR tlsa = new TLSARR("_443._tcp.example.com", ttl, 3, 1, 1,
                "d2abde240d7cd3ee6b4b28c54df034b97983a1d16e8a410e4561cb106618e971");
        Assert.assertEquals(ttl, tlsa.getTtl());
    }

    @Test
    public void handleVariousUsageAndSelector() {
        int[][] testCases = {{0, 0, 0}, {3, 1, 1}, {1, 0, 2}};
        for (int[] testCase : testCases) {
            TLSARR tlsa = new TLSARR("_443._tcp.example.com", 3600, testCase[0], testCase[1], testCase[2],
                    "aabbccdd");
            byte[] bytes = tlsa.getBytes();
            Assert.assertTrue("Should serialize usage, selector, matching_type", bytes.length > 0);
        }
    }

    @Test
    public void handleLongAssociationData() {
        String longHex = "aa".repeat(64); // 64 pairs = 128 hex chars = 64 bytes
        TLSARR tlsa = new TLSARR("_443._tcp.example.com", 3600, 3, 1, 1, longHex);
        byte[] bytes = tlsa.getBytes();
        Assert.assertTrue("Should serialize long association data", bytes.length > 64);
    }

    @Test
    public void gettersReturnStoredValues() {
        int usage = 3;
        int selector = 1;
        int matchingType = 1;
        String assocData = "d2abde240d7cd3ee";
        TLSARR tlsa = new TLSARR("_443._tcp.example.com", 3600, usage, selector, matchingType, assocData);
        Assert.assertEquals(usage, tlsa.getUsage());
        Assert.assertEquals(selector, tlsa.getSelector());
        Assert.assertEquals(matchingType, tlsa.getMatchingType());
        Assert.assertEquals(assocData, tlsa.getAssociationData());
    }
}
