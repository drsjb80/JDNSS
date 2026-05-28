package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class DBConnectionTest {

    @Test
    public void constructorRejectsUnknownDriverClass() {
        Assert.assertThrows(ClassNotFoundException.class,
                () -> new DBConnection("not.a.real.Driver", "jdbc:invalid", null, null));
    }

    @Test
    public void getZoneReturnsEmptyWhenConnectionFails() throws Exception {
        DBConnection connection = new DBConnection("java.lang.String",
                "jdbc:invalid://localhost/jdnss", null, null);

        DBZone zone = connection.getZone("www.test.com");

        Assert.assertTrue(zone.isEmpty());
    }

    @Test
    public void getReturnsEmptyListWhenConnectionFails() throws Exception {
        DBConnection connection = new DBConnection("java.lang.String",
                "jdbc:invalid://localhost/jdnss", null, null);

        List<RR> records = connection.get(RRCode.A, "www.test.com", 1);

        Assert.assertNotNull(records);
        Assert.assertTrue(records.isEmpty());
    }
}
