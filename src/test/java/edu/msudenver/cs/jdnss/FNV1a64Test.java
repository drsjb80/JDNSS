package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class FNV1a64Test {

    @Test
    public void getHashIsZeroBeforeInit() {
        FNV1a64 fnv = new FNV1a64();
        Assert.assertEquals(0L, fnv.getHash());
    }

    @Test
    public void initProducesKnownHashForHello() throws Exception {
        FNV1a64 fnv = new FNV1a64();

        fnv.init("hello");

        Assert.assertEquals(0xa430d84680aabd0bL, fnv.getHash());
    }

    @Test
    public void updateMatchesConcatenatedInit() throws Exception {
        FNV1a64 incremental = new FNV1a64();
        incremental.init("hel");
        incremental.update("lo");

        FNV1a64 oneShot = new FNV1a64();
        oneShot.init("hello");

        Assert.assertEquals(oneShot.getHash(), incremental.getHash());
    }

    @Test
    public void initResetsPreviousState() throws Exception {
        FNV1a64 fnv = new FNV1a64();

        fnv.init("alpha");
        long alphaHash = fnv.getHash();

        fnv.init("beta");
        long betaHash = fnv.getHash();

        Assert.assertNotEquals(alphaHash, betaHash);

        FNV1a64 fresh = new FNV1a64();
        fresh.init("beta");
        Assert.assertEquals(fresh.getHash(), betaHash);
    }
}
