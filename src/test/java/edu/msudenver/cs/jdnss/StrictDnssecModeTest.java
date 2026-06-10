package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class StrictDnssecModeTest {
    private static final String ZONE_NAME = "strict.test.com";

    private BindZone zone;
    private Query query;
    private Map<String, Zone> originalZones;
    private Object originalRuntimeConfig;

    @Before
    public void setUp() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        originalZones = new HashMap<>(liveZones);

        liveZones.clear();
        zone = new BindZone(ZONE_NAME);
        zone.add(ZONE_NAME, new SOARR(ZONE_NAME,
            "ns." + ZONE_NAME, "admin." + ZONE_NAME, 1, 3600, 1800, 604800, 86400, 300));
        liveZones.put(ZONE_NAME, zone);

        byte[] queryBytes = buildQuery(0x1234, ZONE_NAME, RRCode.A);
        query = new Query(queryBytes);
        query.parseQueries("");

        saveRuntimeConfig();
    }

    @After
    public void tearDown() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
        restoreRuntimeConfig();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Zone> getBindZones() throws Exception {
        final Field field = JDNSS.class.getDeclaredField("bindZones");
        field.setAccessible(true);
        return (Map<String, Zone>) field.get(null);
    }

    private void saveRuntimeConfig() throws Exception {
        final Field field = JDNSS.class.getDeclaredField("currentRuntimeConfig");
        field.setAccessible(true);
        originalRuntimeConfig = field.get(null);
    }

    private void restoreRuntimeConfig() throws Exception {
        final Field field = JDNSS.class.getDeclaredField("currentRuntimeConfig");
        field.setAccessible(true);
        field.set(null, originalRuntimeConfig);
    }

    private void setRuntimeConfig(final boolean dnssecValidationEnabled,
                                   final boolean dnssecRefuseUnsigned) throws Exception {
        final JDNSS.RuntimeConfig config = new JDNSS.RuntimeConfig(
            "secret", new String[]{}, dnssecValidationEnabled, dnssecRefuseUnsigned
        );
        final Field field = JDNSS.class.getDeclaredField("currentRuntimeConfig");
        field.setAccessible(true);
        field.set(null, config);
    }

    private static byte[] buildQuery(final int id, final String qName, final RRCode type) {
        byte[] queryBytes = new byte[] {
                (byte) ((id >> 8) & 0xff), (byte) (id & 0xff),
                0x01, 0x00,
                0x00, 0x01,
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00
        };

        queryBytes = Utils.combine(queryBytes, DnsNameCodec.convertString(qName));
        queryBytes = Utils.combine(queryBytes, new byte[] {
                (byte) ((type.getCode() >> 8) & 0xff), (byte) (type.getCode() & 0xff),
                0x00, 0x01
        });
        return queryBytes;
    }

    private static byte[] buildQueryWithDnssec(final int id, final String qName, final RRCode type) {
        byte[] queryBytes = buildQuery(id, qName, type);

        queryBytes[11] = 0x01;

        byte[] zFlags = new byte[] {0x00, 0x00, (byte) 0x80, 0x00};

        queryBytes = Utils.combine(queryBytes, new byte[] {
                0x00,
                0x00, 0x29,
                0x04, (byte) 0xd0,
            zFlags[0], zFlags[1], zFlags[2], zFlags[3],
                0x00, 0x00
        });

        return queryBytes;
    }

    @Test
    public void strictModeDisabledReturnsRecords() throws Exception {
        setRuntimeConfig(true, false);
        zone.setDnssecEnabled(true);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        Response response = new Response(query, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
        assertTrue(result.length > 0);
        assertFalse(isServfail(result));
    }

    @Test
    public void strictModeEnabledWithUnsignedReturnsServfail() throws Exception {
        setRuntimeConfig(true, true);
        zone.setDnssecEnabled(true);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        byte[] queryBytes = buildQueryWithDnssec(0x5678, "www." + ZONE_NAME, RRCode.A);
        Query wwwQuery = new Query(queryBytes);
        wwwQuery.parseQueries("");

        Response response = new Response(wwwQuery, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
        assertTrue("Expected SERVFAIL response in strict mode", isServfail(result));
    }

    @Test
    public void strictModeDoesNotAffectSignedRecords() throws Exception {
        setRuntimeConfig(true, true);
        zone.setDnssecEnabled(true);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        String signature = Base64.getEncoder().encodeToString(new byte[256]);
        RRSIG rrsig = new RRSIG(
            "www." + ZONE_NAME, 300, RRCode.A,
            8, 3, 300,
            1704067200, 1704153600, 12345, ZONE_NAME,
            signature
        );

        zone.add("www." + ZONE_NAME, rrsig);

        byte[] queryBytes = buildQueryWithDnssec(0x9abc, "www." + ZONE_NAME, RRCode.A);
        Query wwwQuery = new Query(queryBytes);
        wwwQuery.parseQueries("");

        Response response = new Response(wwwQuery, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
    }

    @Test
    public void strictModeDisabledWithUnsignedDoesNotReturnServfail() throws Exception {
        setRuntimeConfig(true, false);
        zone.setDnssecEnabled(true);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        byte[] queryBytes = buildQueryWithDnssec(0xdef0, "www." + ZONE_NAME, RRCode.A);
        Query wwwQuery = new Query(queryBytes);
        wwwQuery.parseQueries("");

        Response response = new Response(wwwQuery, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
        assertFalse("Strict mode disabled - should not return SERVFAIL", isServfail(result));
    }

    @Test
    public void strictModeRequiresDnssecEnabled() throws Exception {
        setRuntimeConfig(false, true);
        zone.setDnssecEnabled(false);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        byte[] queryBytes = buildQuery(0x1111, "www." + ZONE_NAME, RRCode.A);
        Query wwwQuery = new Query(queryBytes);
        wwwQuery.parseQueries("");

        Response response = new Response(wwwQuery, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
        assertFalse("DNSSEC not enabled - strict mode should not apply", isServfail(result));
    }

    @Test
    public void strictModeDoesNotAffectValidRecords() throws Exception {
        setRuntimeConfig(true, true);
        zone.setDnssecEnabled(true);
        zone.add("valid." + ZONE_NAME, new ARR("valid." + ZONE_NAME, 300, "192.0.2.100"));

        byte[] queryBytes = buildQueryWithDnssec(0x2222, "valid." + ZONE_NAME, RRCode.A);
        Query validQuery = new Query(queryBytes);
        validQuery.parseQueries("");

        Response response = new Response(validQuery, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void runtimeConfigNullDoesNotCauseCrash() throws Exception {
        final Field field = JDNSS.class.getDeclaredField("currentRuntimeConfig");
        field.setAccessible(true);
        field.set(null, null);

        zone.setDnssecEnabled(true);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        byte[] queryBytes = buildQueryWithDnssec(0x3333, "www." + ZONE_NAME, RRCode.A);
        Query wwwQuery = new Query(queryBytes);
        wwwQuery.parseQueries("");

        Response response = new Response(wwwQuery, true);
        byte[] result = response.getBytes();

        assertNotNull(result);
        assertFalse("Null config - should not return SERVFAIL", isServfail(result));
    }

    private boolean isServfail(final byte[] response) {
        if (response.length < 4) {
            return false;
        }

        final int rcode = response[3] & 0x0f;
        return rcode == 2;
    }
}
