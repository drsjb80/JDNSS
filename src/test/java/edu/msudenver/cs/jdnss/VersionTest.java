package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class VersionTest
{
    @Test
    public void version() {
        Assert.assertEquals("3.0", new Version().getVersion());
    }

    @Test
    public void readVersionReturnsUnknownForMissingStream() throws IOException {
        assertEquals("unknown", Version.readVersion(null));
    }
}
