package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class JDNSSDoargsTest {
    private final Map<String, Object> originalArgs = new HashMap<>();
    private Map<String, Zone> originalZones;
    private Object originalDbConnection;

    @Before
    public void setUp() throws Exception {
        originalZones = new HashMap<>(getBindZones());
        originalDbConnection = getDbConnection();
        captureJargsState();

        setDbConnection(null);
        resetJargsForTest();
    }

    @After
    public void tearDown() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);

        restoreJargsState();
        setDbConnection(originalDbConnection);
    }

    @Test
    public void doargsGeneratesSecretWhenMissing() throws Exception {
        setJargField("serverSecret", null);
        setJargField("additional", null);

        invokeDoargs();

        String generated = (String) getJargField("serverSecret");
        Assert.assertNotNull(generated);
        Assert.assertFalse(generated.isEmpty());
    }

    @Test
    public void doargsReplacesTooShortSecret() throws Exception {
        setJargField("serverSecret", "short");
        setJargField("additional", null);

        invokeDoargs();

        String generated = (String) getJargField("serverSecret");
        Assert.assertNotNull(generated);
        Assert.assertNotEquals("short", generated);
        Assert.assertTrue(generated.length() >= 16);
    }

    @Test
    public void doargsParsesAdditionalZoneFile() throws Exception {
        Path zoneFile = Files.createTempFile("jdnss-doargs", ".db");
        Files.writeString(zoneFile,
                String.join("\n",
                        "$ORIGIN test.com.",
                        "$TTL 300",
                        "@ IN SOA ns1.test.com. hostmaster.test.com. ( 1 7200 3600 1209600 3600 )",
                        "@ IN NS ns1.test.com.",
                        "www A 192.168.1.2"),
                StandardCharsets.UTF_8);

        try {
            setJargField("serverSecret", "0123456789abcdef");
            setJargField("additional", new String[] {zoneFile.toString()});

            invokeDoargs();

            Zone zone = JDNSS.getZone("www.test.com");
            Assert.assertFalse(zone.isEmpty());
            Assert.assertEquals("test.com", zone.getName());
            Assert.assertEquals(1, zone.get(RRCode.A, "www.test.com").size());
        } finally {
            Files.deleteIfExists(zoneFile);
        }
    }

    @Test
    public void doargsSkipsMissingAdditionalZoneFile() throws Exception {
        setJargField("serverSecret", "0123456789abcdef");
        setJargField("additional", new String[] {"/tmp/jdnss-file-does-not-exist.db"});

        invokeDoargs();

        Assert.assertTrue(getBindZones().isEmpty());
    }

    private static void invokeDoargs() throws Exception {
        Method method = JDNSS.class.getDeclaredMethod("doargs");
        method.setAccessible(true);
        method.invoke(null);
    }

    private void captureJargsState() throws Exception {
        for (String name : new String[] {
                "version", "help", "DBClass", "DBURL", "DBUser", "DBPass",
                "serverSecret", "additional", "IPaddresses", "logLevel"
        }) {
            originalArgs.put(name, getJargField(name));
        }
    }

    private void restoreJargsState() throws Exception {
        for (Map.Entry<String, Object> entry : originalArgs.entrySet()) {
            setJargField(entry.getKey(), entry.getValue());
        }
    }

    private static void resetJargsForTest() throws Exception {
        setJargField("version", false);
        setJargField("help", false);
        setJargField("DBClass", null);
        setJargField("DBURL", null);
        setJargField("DBUser", null);
        setJargField("DBPass", null);
        setJargField("additional", null);
        setJargField("logLevel", JDNSSLogLevels.ERROR);
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
