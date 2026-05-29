package edu.msudenver.cs.jdnss;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class VersionTest
{
    @Test
    public void version() {
        String version = new Version().getVersion();
        assertFalse("unknown".equals(version));
        assertFalse(version.trim().isEmpty());
    }

    @Test
    public void readVersionReturnsUnknownForMissingStream() throws IOException {
        assertEquals("unknown", Version.readVersion(null));
    }
}
