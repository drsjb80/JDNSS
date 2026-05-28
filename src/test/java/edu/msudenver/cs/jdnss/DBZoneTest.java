package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

public class DBZoneTest {

    @Test
    public void defaultZoneIsEmpty() {
        DBZone zone = new DBZone();

        Assert.assertTrue(zone.isEmpty());
        Assert.assertNull(zone.getName());
    }

    @Test
    public void constructedZoneIsNotEmptyAndReturnsName() throws Exception {
        DBConnection connection = new DBConnection("java.lang.String",
                "jdbc:invalid://localhost/jdnss", null, null);
        DBZone zone = new DBZone("test.com", 0, connection);

        Assert.assertFalse(zone.isEmpty());
        Assert.assertEquals("test.com", zone.getName());
    }

    @Test
    public void getCurrentlyReturnsNull() throws Exception {
        DBConnection connection = new DBConnection("java.lang.String",
                "jdbc:invalid://localhost/jdnss", null, null);
        DBZone zone = new DBZone("test.com", 0, connection);

        Assert.assertNull(zone.get(RRCode.A, "www.test.com"));
    }
}
