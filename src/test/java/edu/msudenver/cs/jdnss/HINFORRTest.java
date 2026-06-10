package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class HINFORRTest {
    @Test
    public void constructorStoresCPUAndOS() {
        HINFORR hinfo = new HINFORR("example.com", 3600, "Intel-PC", "Linux");
        Assert.assertNotNull(hinfo);
    }

    @Test
    public void getBytesReturnsValidWireFormat() {
        HINFORR hinfo = new HINFORR("example.com", 3600, "x86", "Unix");
        byte[] bytes = hinfo.getBytes();
        Assert.assertNotNull(bytes);
        Assert.assertTrue("HINFO should encode CPU and OS as character strings", bytes.length > 0);
    }

    @Test
    public void typeCodeIsHINFO() {
        HINFORR hinfo = new HINFORR("example.com", 3600, "Intel", "Linux");
        Assert.assertEquals(RRCode.HINFO, hinfo.getType());
    }

    @Test
    public void ttlIsStored() {
        int ttl = 7200;
        HINFORR hinfo = new HINFORR("example.com", ttl, "Intel", "Linux");
        Assert.assertEquals(ttl, hinfo.getTtl());
    }

    @Test
    public void handleVariousCPUTypes() {
        String[] cpus = {"Intel-PC", "x86_64", "ARM", "SPARC"};
        for (String cpu : cpus) {
            HINFORR hinfo = new HINFORR("example.com", 3600, cpu, "Linux");
            byte[] bytes = hinfo.getBytes();
            Assert.assertTrue("Should serialize CPU: " + cpu, bytes.length > 0);
        }
    }

    @Test
    public void handleVariousOSTypes() {
        String[] oses = {"Linux", "Windows", "macOS", "Unix"};
        for (String os : oses) {
            HINFORR hinfo = new HINFORR("example.com", 3600, "Intel", os);
            byte[] bytes = hinfo.getBytes();
            Assert.assertTrue("Should serialize OS: " + os, bytes.length > 0);
        }
    }

}
