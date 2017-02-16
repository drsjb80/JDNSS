package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class UtilsTest
{
    @Test
    public void reverse_IP()
    {
        Assert.assertEquals (Utils.reverseIP ("192.168.1.2"), "2.1.168.192");
    }
}
