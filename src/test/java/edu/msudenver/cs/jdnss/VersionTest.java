package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class VersionTest
{
    @Test
    public void version()
    {
        Assert.assertEquals (new Version().getVersion(), "2.0");
    }
}
