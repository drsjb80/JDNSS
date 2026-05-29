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
    private String originalServerSecret;
    private String[] originalAdditional;
    private Map<String, Zone> originalZones;
    private Object originalDbConnection;

    @Before
    public void setUp() throws Exception {
        originalIpAddresses = JDNSS.jargs.IPaddresses;
        originalServerSecret = (String) getJargField("serverSecret");
        originalAdditional = (String[]) getJargField("additional");
        originalZones = new HashMap<>(getBindZones());
        originalDbConnection = getDbConnection();
    }

    @After
    public void tearDown() throws Exception {
        JDNSS.jargs.IPaddresses = originalIpAddresses;
        setJargField("serverSecret", originalServerSecret);
        setJargField("additional", originalAdditional);

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
    public void normalizeIpAddressOptionClearsDefaultWhenSingleDashFlagPresent() {
        JDNSS.jargs.IPaddresses = new String[] {"UDP@0.0.0.0@53"};

        JDNSS.normalizeIpAddressOption(new String[] {"-IPaddresses=TCP@127.0.0.1@53"});

        Assert.assertNull(JDNSS.jargs.IPaddresses);
    }

    @Test
    public void parseCommandLineArgsUsesOverrideSemanticsForIpAddresses() {
        JDNSS.jargs.IPaddresses = new String[] {"UDP@0.0.0.0@53"};

        JDNSS.parseCommandLineArgs(new String[] {"--IPaddresses=UDP@127.0.0.1@5300"});

        Assert.assertNotNull(JDNSS.jargs.IPaddresses);
        Assert.assertEquals(1, JDNSS.jargs.IPaddresses.length);
        Assert.assertEquals("UDP@127.0.0.1@5300", JDNSS.jargs.IPaddresses[0]);
    }

    @Test
    public void buildRuntimeConfigPreservesBoundaryLengthSecret() throws Exception {
        setJargField("serverSecret", "0123456789abcdef");
        setJargField("additional", new String[] {"example.com"});

        JDNSS.RuntimeConfig runtimeConfig = JDNSS.buildRuntimeConfig();

        Assert.assertEquals("0123456789abcdef", runtimeConfig.getServerSecret());
        Assert.assertArrayEquals(new String[] {"example.com"}, runtimeConfig.getAdditional());
    }

    @Test
    public void deriveZoneNameForAdditionalFileMapsReverseDbName() {
        String zoneName = JDNSS.deriveZoneNameForAdditionalFile("/tmp/192.168.1.db");

        Assert.assertEquals("1.168.192.in-addr.arpa", zoneName);
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

    private static Object getJargField(final String fieldName) throws Exception {
        Field field = jdnssArgs.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(JDNSS.jargs);
    }

    private static void setJargField(final String fieldName, final Object value) throws Exception {
        Field field = jdnssArgs.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(JDNSS.jargs, value);
    }

    private static void setDbConnection(final Object dbConnectionValue) throws Exception {
        Field dbConnectionField = JDNSS.class.getDeclaredField("DBConnection");
        dbConnectionField.setAccessible(true);
        dbConnectionField.set(null, dbConnectionValue);
    }
}
