package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class RRCodeTest {

    @Test
    public void getCodeReturnsDnsTypeValues() {
        Assert.assertEquals(1, RRCode.A.getCode());
        Assert.assertEquals(2, RRCode.NS.getCode());
        Assert.assertEquals(5, RRCode.CNAME.getCode());
        Assert.assertEquals(6, RRCode.SOA.getCode());
        Assert.assertEquals(12, RRCode.PTR.getCode());
        Assert.assertEquals(15, RRCode.MX.getCode());
        Assert.assertEquals(28, RRCode.AAAA.getCode());
        Assert.assertEquals(46, RRCode.RRSIG.getCode());
        Assert.assertEquals(47, RRCode.NSEC.getCode());
        Assert.assertEquals(48, RRCode.DNSKEY.getCode());
    }

    @Test
    public void findCodeReturnsTheMatchingEnumConstant() {
        Assert.assertEquals(RRCode.A, RRCode.findCode(1));
        Assert.assertEquals(RRCode.MX, RRCode.findCode(15));
        Assert.assertEquals(RRCode.DNSKEY, RRCode.findCode(48));
    }

    @Test(expected = IllegalArgumentException.class)
    public void findCodeRejectsUnknownCode() {
        RRCode.findCode(999);
    }
}