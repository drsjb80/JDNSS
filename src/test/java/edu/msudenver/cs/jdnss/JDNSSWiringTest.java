package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class JDNSSWiringTest {
    private String[] originalIpAddresses;
    private Map<String, Zone> originalZones;
    private Object originalDbConnection;

    @Before
    public void setUp() throws Exception {
        originalIpAddresses = JDNSS.jargs.IPaddresses;
        originalZones = new HashMap<>(getBindZones());
        originalDbConnection = getDbConnection();
    }

    @After
    public void tearDown() throws Exception {
        JDNSS.jargs.IPaddresses = originalIpAddresses;

        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);

        setDbConnection(originalDbConnection);
    }

    @Test
    public void normalizeIpAddressOptionClearsDefaultWhenFlagPresent() {
        JDNSS.jargs.IPaddresses = new String[] {"UDP@0.0.0.0@53"};

        JDNSS.normalizeIpAddressOption(new String[] {"--IPaddresses=UDP@127.0.0.1@53"});

        Assert.assertNull(JDNSS.jargs.IPaddresses);
    }

    @Test
    public void normalizeIpAddressOptionLeavesDefaultWhenFlagMissing() {
        JDNSS.jargs.IPaddresses = new String[] {"UDP@0.0.0.0@53"};

        JDNSS.normalizeIpAddressOption(new String[] {"--threads=4"});

        Assert.assertNotNull(JDNSS.jargs.IPaddresses);
        Assert.assertEquals(1, JDNSS.jargs.IPaddresses.length);
    }

    @Test
    public void uncaughtExceptionHandlerWritesExpectedPrefix() {
        Thread.UncaughtExceptionHandler handler = JDNSS.createUncaughtExceptionHandler();

        PrintStream originalErr = System.err;
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
        try {
            System.setErr(new PrintStream(errBuffer));
            handler.uncaughtException(new Thread("wire-test"), new RuntimeException("boom"));
        } finally {
            System.setErr(originalErr);
        }

        String text = errBuffer.toString();
        Assert.assertTrue(text.contains("Exception in thread \"wire-test"));
        Assert.assertTrue(text.contains("boom"));
    }

    @Test
    public void hasConfiguredZoneSourceTrueWhenBindZonesPresent() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", new BindZone("test.com"));
        setDbConnection(null);

        Assert.assertTrue(JDNSS.hasConfiguredZoneSource());
    }

    @Test
    public void hasConfiguredZoneSourceTrueWhenDbConnectionPresent() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        setDbConnection(new DBConnection("java.lang.String", "", "", ""));

        Assert.assertTrue(JDNSS.hasConfiguredZoneSource());
    }

    @Test
    public void hasConfiguredZoneSourceFalseWhenNoSourcesPresent() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        setDbConnection(null);

        Assert.assertFalse(JDNSS.hasConfiguredZoneSource());
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
