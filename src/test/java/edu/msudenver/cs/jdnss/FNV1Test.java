package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class FNV1Test {

    private static final class TestFNV1 extends FNV1 {
        private TestFNV1(final long init) {
            INIT = init;
        }

        @Override
        protected long fnv(final byte[] buf, final int offset, final int len, final long seed) {
            long hash = seed;
            for (int i = offset; i < offset + len; i++) {
                hash += buf[i] & 0xff;
            }
            return hash;
        }
    }

    @Test
    public void getHashStartsAtZero() {
        Assert.assertEquals(0L, new TestFNV1(7L).getHash());
    }

    @Test
    public void initAndUpdateUseFnvImplementation() throws Exception {
        TestFNV1 fnv = new TestFNV1(7L);

        fnv.init("ab");
        Assert.assertEquals(202L, fnv.getHash());

        fnv.update("c");
        Assert.assertEquals(301L, fnv.getHash());
    }

    @Test
    public void initResetsPreviousHashState() throws Exception {
        TestFNV1 fnv = new TestFNV1(7L);

        fnv.init("abc");
        Assert.assertEquals(301L, fnv.getHash());

        fnv.init("d");
        Assert.assertEquals(107L, fnv.getHash());
    }
}