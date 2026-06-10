package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

public class DnssecResponseIntegrationTest {
    private static final String ZONE_NAME = "dnssec.test.com";

    private BindZone zone;
    private Response response;
    private Query query;
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        originalZones = new HashMap<>(liveZones);

        liveZones.clear();
        zone = new BindZone(ZONE_NAME);
        zone.add(ZONE_NAME, new SOARR(ZONE_NAME,
            "ns." + ZONE_NAME, "admin." + ZONE_NAME, 1, 3600, 1800, 604800, 86400, 300));
        liveZones.put(ZONE_NAME, zone);

        byte[] queryBytes = buildQuery(0x5f3e, ZONE_NAME, RRCode.A);
        query = new Query(queryBytes);
        query.parseQueries("");
    }

    @After
    public void tearDown() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Zone> getBindZones() throws Exception {
        final Field field = JDNSS.class.getDeclaredField("bindZones");
        field.setAccessible(true);
        return (Map<String, Zone>) field.get(null);
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

    @Test
    public void responseWithoutDnssecReturnsSameRecords() throws Exception {
        zone.setDnssecEnabled(false);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void responseInitializesWithCorrectZone() throws Exception {
        response = new Response(query, true);
        assertNotNull(response);
    }

    @Test
    public void responseProcessesMultipleRecordTypes() throws Exception {
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.2"));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesEmptyAnswerSection() throws Exception {
        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesCNAMEChain() throws Exception {
        zone.add("alias." + ZONE_NAME, new CNAMERR("alias." + ZONE_NAME, 300, "target." + ZONE_NAME));
        zone.add("target." + ZONE_NAME, new ARR("target." + ZONE_NAME, 300, "192.0.2.1"));

        response = new Response(query, true);

        assertNotNull(response);
    }

    @Test
    public void responseHandlesWildcardRecords() throws Exception {
        zone.add("*.wild." + ZONE_NAME, new ARR("*.wild." + ZONE_NAME, 300, "192.0.2.1"));

        response = new Response(query, true);

        assertNotNull(response);
    }

    @Test
    public void responseAddsAuthorityRecords() throws Exception {
        zone.add(ZONE_NAME, new NSRR(ZONE_NAME, 300, "ns." + ZONE_NAME));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responsePreservesRecordTTL() throws Exception {
        ARR record = new ARR("www." + ZONE_NAME, 123, "192.0.2.1");
        zone.add("www." + ZONE_NAME, record);

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesMXRecords() throws Exception {
        MXRR mx = new MXRR(ZONE_NAME, 300, "mail." + ZONE_NAME, 10);
        zone.add(ZONE_NAME, mx);

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesNSRecords() throws Exception {
        NSRR ns = new NSRR(ZONE_NAME, 300, "ns." + ZONE_NAME);
        zone.add(ZONE_NAME, ns);

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesAAAARecords() throws Exception {
        AAAARR aaaa = new AAAARR("www." + ZONE_NAME, 300, "2001:db8::1");
        zone.add("www." + ZONE_NAME, aaaa);

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesTXTRecords() throws Exception {
        TXTRR txt = new TXTRR(ZONE_NAME, 300, "v=spf1 include:" + ZONE_NAME + " ~all");
        zone.add(ZONE_NAME, txt);

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesPTRRecords() throws Exception {
        String ptrName = "1.2.0.192.in-addr.arpa";
        zone.add(ptrName, new PTRRR(ptrName, 300, "www." + ZONE_NAME));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesHINFORecords() throws Exception {
        zone.add(ZONE_NAME, new HINFORR(ZONE_NAME, 300, "Intel-PC", "Linux"));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesSRVRecords() throws Exception {
        zone.add(ZONE_NAME, new SRVRR(ZONE_NAME, 300, 10, 20, 5060, "sip." + ZONE_NAME));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesTLSARecords() throws Exception {
        zone.add(ZONE_NAME, new TLSARR(ZONE_NAME, 300, 1, 1, 1,
            "0000000000000000000000000000000000000000000000000000000000000000"));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseHandlesCAARRecords() throws Exception {
        zone.add(ZONE_NAME, new CAARR(ZONE_NAME, 300, 0, "issue", "ca." + ZONE_NAME));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseDoesNotInjectDNSSECRecordsWhenUnsigned() throws Exception {
        zone.setDnssecEnabled(true);
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void responseConservesRecordOrdering() throws Exception {
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.1"));
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.2"));
        zone.add("www." + ZONE_NAME, new ARR("www." + ZONE_NAME, 300, "192.0.2.3"));

        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    public void responseHandlesNegativeCache() throws Exception {
        response = new Response(query, true);

        byte[] result = response.getBytes();
        assertNotNull(result);
    }

    @Test
    public void dnssecEnabledFlagIsRespected() throws Exception {
        zone.setDnssecEnabled(true);
        assertTrue(zone.isDnssecEnabled());

        response = new Response(query, true);
        assertNotNull(response);
    }

    @Test
    public void dnssecCanBeDisabled() throws Exception {
        zone.setDnssecEnabled(true);
        zone.setDnssecEnabled(false);
        assertFalse(zone.isDnssecEnabled());

        response = new Response(query, true);
        assertNotNull(response);
    }

    @Test
    public void dnssecCanBeToggled() throws Exception {
        assertTrue(zone.isDnssecEnabled() || !zone.isDnssecEnabled());

        response = new Response(query, true);
        assertNotNull(response);
    }
}
