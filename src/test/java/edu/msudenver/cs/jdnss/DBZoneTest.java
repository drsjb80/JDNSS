package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DBZoneTest {

    private static class RecordingDBConnection extends DBConnection {
        private RRCode recordedType;
        private String recordedName;
        private int recordedDomainId;
        private final List<RR> response;

        RecordingDBConnection(List<RR> response) throws Exception {
            super("java.lang.String", "jdbc:invalid://localhost/jdnss", null, null);
            this.response = response;
        }

        @Override
        public List<RR> get(final RRCode type, final String name, final int domainId) {
            recordedType = type;
            recordedName = name;
            recordedDomainId = domainId;
            return response;
        }
    }

    @Test
    public void defaultZoneIsEmpty() {
        DBZone zone = new DBZone();

        Assert.assertTrue(zone.isEmpty());
        Assert.assertNull(zone.getName());
        Assert.assertNotNull(zone.get(RRCode.A, "www.test.com"));
        Assert.assertTrue(zone.get(RRCode.A, "www.test.com").isEmpty());
    }

    @Test
    public void constructedZoneIsNotEmptyAndReturnsName() throws Exception {
        RecordingDBConnection connection = new RecordingDBConnection(Collections.emptyList());
        DBZone zone = new DBZone("test.com", 0, connection);

        Assert.assertFalse(zone.isEmpty());
        Assert.assertEquals("test.com", zone.getName());
    }

    @Test
    public void getDelegatesToConnectionAndReturnsRecords() throws Exception {
        ARR arr = new ARR("www.test.com", 300, "192.0.2.8");
        List<RR> dbResponse = new ArrayList<>();
        dbResponse.add(arr);

        RecordingDBConnection connection = new RecordingDBConnection(dbResponse);
        DBZone zone = new DBZone("test.com", 42, connection);

        List<RR> result = zone.get(RRCode.A, "www.test.com");

        Assert.assertNotNull(result);
        Assert.assertEquals(1, result.size());
        Assert.assertSame(arr, result.get(0));
        Assert.assertEquals(RRCode.A, connection.recordedType);
        Assert.assertEquals("www.test.com", connection.recordedName);
        Assert.assertEquals(42, connection.recordedDomainId);
    }
}
