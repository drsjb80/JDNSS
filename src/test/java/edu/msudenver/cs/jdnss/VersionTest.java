package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class VersionTest
{
    @Test
    public void version()
    {
        Assert.assertEquals (new Version().getVersion(), "3.0");
    }
}
