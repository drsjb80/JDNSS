package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class ErrorCodesTest {

    @Test
    public void enumCodesMatchDnsRcodes() {
        Assert.assertEquals(0, ErrorCodes.NOERROR.getCode());
        Assert.assertEquals(1, ErrorCodes.FORMERROR.getCode());
        Assert.assertEquals(2, ErrorCodes.SERVFAIL.getCode());
        Assert.assertEquals(3, ErrorCodes.NAMEERROR.getCode());
        Assert.assertEquals(4, ErrorCodes.NOTIMPL.getCode());
        Assert.assertEquals(5, ErrorCodes.REFUSED.getCode());
        Assert.assertEquals(7, ErrorCodes.YXRRSET.getCode());
    }
}