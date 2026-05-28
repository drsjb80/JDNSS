package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class JDNSSTest {
    private Map<String, Zone> originalZones;
    private Object originalDbConnection;

    @Before
    public void setUp() throws Exception {
        originalZones = new HashMap<>(getBindZones());
        originalDbConnection = getDbConnection();
        setDbConnection(null);
    }

    @org.junit.After
    public void tearDown() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
        setDbConnection(originalDbConnection);
    }

    @Test
    public void getZoneReturnsLongestSuffixMatch() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();

        BindZone comZone = new BindZone("com");
        BindZone testComZone = new BindZone("test.com");
        liveZones.put("com", comZone);
        liveZones.put("test.com", testComZone);

        Zone result = JDNSS.getZone("www.test.com");
        Assert.assertSame(testComZone, result);
    }

    @Test
    public void getZoneReturnsEmptyWhenNoMatchExists() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("example.org", new BindZone("example.org"));

        Zone result = JDNSS.getZone("www.test.com");

        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(result instanceof BindZone);
    }

    @Test
    public void getZoneReturnsEmptyWhenLongestKeyMapsToNull() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", null);

        Zone result = JDNSS.getZone("api.test.com");

        Assert.assertTrue(result.isEmpty());
        Assert.assertTrue(result instanceof BindZone);
    }

    @Test
    public void getZoneAssertsWhenNoConfiguredZones() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();

        Assert.assertThrows(AssertionError.class, () -> JDNSS.getZone("www.test.com"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Zone> getBindZones() throws Exception {
        Field bindZonesField = JDNSS.class.getDeclaredField("bindZones");
        bindZonesField.setAccessible(true);
        return (Map<String, Zone>) bindZonesField.get(null);
    }

    private static Object getDbConnection() throws Exception {
        Field dbConnectionField = JDNSS.class.getDeclaredField("DBConnection");
        dbConnectionField.setAccessible(true);
        return dbConnectionField.get(null);
    }

    private static void setDbConnection(final Object dbConnectionValue) throws Exception {
        Field dbConnectionField = JDNSS.class.getDeclaredField("DBConnection");
        dbConnectionField.setAccessible(true);
        dbConnectionField.set(null, dbConnectionValue);
    }
}
