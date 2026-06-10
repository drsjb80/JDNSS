package edu.msudenver.cs.jdnss;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.EnumSet;

/**
 * Created by beatys on 10/17/17.
 */
public class ResponseTest {
    private static final String ZONE_NAME = "test.com";

    private Response response;
    private Query query;
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        originalZones = new HashMap<>(liveZones);

        liveZones.clear();
        liveZones.put(ZONE_NAME, createTestZone());

        query = new Query(queryNoCookie);
        query.parseQueries("");
        response = new Response(query, true);
    }

    @After
    public void tearDown() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @Test
    public void getBytesIncludesQuestionAndAnswer() {
        final byte[] result = response.getBytes();

        Assert.assertNotNull(result);
        Assert.assertTrue(result.length > queryNoCookie.length);

        Assert.assertEquals(0x5f, unsignedByte(result[0]));
        Assert.assertEquals(0x3e, unsignedByte(result[1]));
        Assert.assertEquals(0x80, unsignedByte(result[2]) & 0x80);
        Assert.assertEquals(0x04, unsignedByte(result[2]) & 0x04);
        Assert.assertEquals(0x00, unsignedByte(result[3]) & 0x80);

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));

        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    @Test
    public void getBytesMissingARecordReturnsNameErrorWithAuthoritySoa() {
        final Query missingQuery = new Query(queryMissingARecord);
        missingQuery.parseQueries("");

        final Response missingResponse = new Response(missingQuery, true);
        final byte[] result = missingResponse.getBytes();

        Assert.assertNotNull(result);
        Assert.assertEquals(0x80, unsignedByte(result[2]) & 0x80);
        Assert.assertEquals(0x04, unsignedByte(result[2]) & 0x04);
        Assert.assertEquals(0x00, unsignedByte(result[3]) & 0x80);
        Assert.assertEquals(3, unsignedByte(result[3]) & 0x0f);

        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
    }

    @Test
    public void missingTypeForExistingNameReturnsNoErrorNodataWithAuthoritySoa() {
        final Query txtQuery = new Query(buildQuery(0x2468, ZONE_NAME, RRCode.TXT));
        txtQuery.parseQueries("");

        final byte[] result = new Response(txtQuery, true).getBytes();

        Assert.assertEquals(ErrorCodes.NOERROR.getCode(), unsignedByte(result[3]) & 0x0f);
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
    }

    @Test
    public void missingMxForMissingNameReturnsNameErrorWithAuthoritySoa() {
        final Query mxQuery = new Query(buildQuery(0x1357, "missing.test.com", RRCode.MX));
        mxQuery.parseQueries("");

        final byte[] result = new Response(mxQuery, true).getBytes();

        Assert.assertEquals(ErrorCodes.NAMEERROR.getCode(), unsignedByte(result[3]) & 0x0f);
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
    }

    @Test
    public void getBytesLargeTxtResponseMarksTruncatedForUdp() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createLargeTxtZone());

        final Query txtQuery = new Query(buildQueryWithOptPayload(0x4321, ZONE_NAME,
            RRCode.TXT, 64));
        txtQuery.parseQueries("");

        final Response txtResponse = new Response(txtQuery, true);
        final byte[] result = txtResponse.getBytes();

        Assert.assertNotNull(result);
        Assert.assertEquals(0x80, unsignedByte(result[2]) & 0x80);
        Assert.assertEquals(0x04, unsignedByte(result[2]) & 0x04);
        Assert.assertEquals(0x02, unsignedByte(result[2]) & 0x02);
        Assert.assertEquals(1, readUInt16(result, 4));
    }

    @Test
    public void ednsQueryIncludesOptRecordInResponseAdditionalCount() throws Exception {
        final Query ednsQuery = new Query(buildQueryWithOptPayload(0x6d11, ZONE_NAME,
                RRCode.A, 1232));
        ednsQuery.parseQueries("");

        final byte[] result = new Response(ednsQuery, true).getBytes();
        final int optOffset = result.length - 11;

        Assert.assertEquals(1, readUInt16(result, 10));
        Assert.assertEquals(0, unsignedByte(result[optOffset]));
        Assert.assertEquals(RRCode.OPT.getCode(), readUInt16(result, optOffset + 1));
        Assert.assertEquals(1232, readUInt16(result, optOffset + 3));
        Assert.assertEquals(0, readUInt16(result, optOffset + 9));
    }

    @Test
    public void getBytesWithoutZoneReturnsRefused() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("other.com", createZone("other.com"));

        final Query aQuery = new Query(buildQuery(0x5555, ZONE_NAME, RRCode.A));
        aQuery.parseQueries("");

        final Response refusedResponse = new Response(aQuery, true);
        final byte[] result = refusedResponse.getBytes();

        Assert.assertNotNull(result);
        Assert.assertEquals(0x80, unsignedByte(result[2]) & 0x80);
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x80);
        Assert.assertEquals(ErrorCodes.REFUSED.getCode(), unsignedByte(result[3]) & 0x0f);
        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
    }

    @Test
    public void getBytesWithDnssecAddsRrsigToAnswerSection() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createDnssecAZone());

        final Query dnssecQuery = new Query(buildQueryWithOptPayload(0x7788, ZONE_NAME,
                RRCode.A, 1232, true));
        dnssecQuery.parseQueries("");

        final Response dnssecResponse = new Response(dnssecQuery, true);
        final byte[] result = dnssecResponse.getBytes();

        Assert.assertNotNull(result);
        Assert.assertEquals(0x80, unsignedByte(result[2]) & 0x80);
        Assert.assertEquals(0x04, unsignedByte(result[2]) & 0x04);
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 2));
    }

    @Test
    public void getBytesMissingARecordHasSingleAuthorityAndNoAdditionals() {
        final Query missingQuery = new Query(queryMissingARecord);
        missingQuery.parseQueries("");

        final Response missingResponse = new Response(missingQuery, true);
        final byte[] result = missingResponse.getBytes();

        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
    }

    @Test
    public void largeTxtResponseNotTruncatedForTcpMode() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createLargeTxtZone());

        final byte[] payloadLimitedQuery = buildQueryWithOptPayload(0x9123, ZONE_NAME,
                RRCode.TXT, 64);

        final Query udpQuery = new Query(payloadLimitedQuery);
        udpQuery.parseQueries("");
        final byte[] udpResult = new Response(udpQuery, true).getBytes();

        final Query tcpQuery = new Query(payloadLimitedQuery);
        tcpQuery.parseQueries("");
        final byte[] tcpResult = new Response(tcpQuery, false).getBytes();

        Assert.assertEquals(0x02, unsignedByte(udpResult[2]) & 0x02);
        Assert.assertEquals(0x00, unsignedByte(tcpResult[2]) & 0x02);
        Assert.assertEquals(1, readUInt16(tcpResult, 6));
    }

    @Test
    public void cnameLookupIncludesAliasAndResolvedRecordInAnswerCount() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createCnameZone());

        final Query cnameQuery = new Query(buildQuery(0x2211, "www.test.com", RRCode.A));
        cnameQuery.parseQueries("");

        final byte[] result = new Response(cnameQuery, true).getBytes();

        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 42));
    }

    @Test
    public void dnssecMissingRecordAddsAuthorityMaterialWhenPayloadAllows() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createDnssecNegativeZone());

        final Query missingQuery = new Query(buildQueryWithOptPayload(0x3311, "missing.test.com",
                RRCode.A, 1232, true));
        missingQuery.parseQueries("");

        final byte[] result = new Response(missingQuery, true).getBytes();

        Assert.assertEquals(0x00, unsignedByte(result[2]) & 0x02);
        Assert.assertEquals(3, unsignedByte(result[3]) & 0x0f);
        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(0, readUInt16(result, 6));
        Assert.assertTrue(readUInt16(result, 8) >= 3);
    }

    @Test
    public void dnssecAnswerSignatureCanBeDroppedWhenUdpPayloadIsSmall() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createDnssecAZoneWithLargeSignature());

        final Query dnssecQuery = new Query(buildQueryWithOptPayload(0x4411, ZONE_NAME,
                RRCode.A, 80, true));
        dnssecQuery.parseQueries("");

        final byte[] result = new Response(dnssecQuery, true).getBytes();

        Assert.assertEquals(0x02, unsignedByte(result[2]) & 0x02);
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(1, readUInt16(result, 10));
    }

    @Test
    public void dnssecSoaQueryAddsDnskeyMaterialToAdditionalSection() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createDnssecSoaAdditionalZone());

        final Query soaQuery = new Query(buildQueryWithOptPayload(0x7711, ZONE_NAME,
                RRCode.SOA, 1232, true));
        soaQuery.parseQueries("");

        final byte[] result = new Response(soaQuery, true).getBytes();

        Assert.assertEquals(0x00, unsignedByte(result[2]) & 0x02);
        Assert.assertEquals(0, unsignedByte(result[3]) & 0x0f);
        Assert.assertTrue(readUInt16(result, 6) >= 2);
        Assert.assertTrue(readUInt16(result, 10) >= 2);
    }

    @Test
    public void dnssecAuthorityOverflowCanSetTruncationWhileHeaderAuthorityCountStaysZero() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createDnssecNegativeZone());

        final Query missingQuery = new Query(buildQueryWithOptPayload(0x5511, "missing.test.com",
                RRCode.A, 64, true));
        missingQuery.parseQueries("");

        final Response response = new Response(missingQuery, true);
        final byte[] result = response.getBytes();

        final Field numAuthoritiesField = Response.class.getDeclaredField("numAuthorities");
        numAuthoritiesField.setAccessible(true);
        final int internalAuthorityCount = (int) numAuthoritiesField.get(response);

        Assert.assertEquals(0x02, unsignedByte(result[2]) & 0x02);
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertTrue(internalAuthorityCount > 0);
    }

    @Test
    public void truncatedResponseHeaderCountsRemainInternallyConsistent() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createLargeTxtZone());

        final Query txtQuery = new Query(buildQueryWithOptPayload(0x6611, ZONE_NAME,
                RRCode.TXT, 64));
        txtQuery.parseQueries("");

        final byte[] result = new Response(txtQuery, true).getBytes();

        Assert.assertEquals(0x02, unsignedByte(result[2]) & 0x02);
        Assert.assertEquals(1, readUInt16(result, 4));
        Assert.assertEquals(1, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(1, readUInt16(result, 10));
    }

    @Test
    public void multiQuestionQueryReturnsAnswersForEachQuestionWithoutAuthorityLeak() throws Exception {
        final Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put(ZONE_NAME, createMultiQuestionZone());

        final Query multiQuery = new Query(buildMultiQuestionQuery(0x7a11,
                new String[] {"a.test.com", "b.test.com"},
                new RRCode[] {RRCode.A, RRCode.A}));
        multiQuery.parseQueries("");

        final byte[] result = new Response(multiQuery, true).getBytes();

        Assert.assertEquals(2, readUInt16(result, 4));
        Assert.assertEquals(2, readUInt16(result, 6));
        Assert.assertEquals(0, readUInt16(result, 8));
        Assert.assertEquals(0, readUInt16(result, 10));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 11));
        Assert.assertTrue(containsIpv4(result, 192, 168, 1, 12));
    }

    private static BindZone createTestZone() {
        return createZone(ZONE_NAME);
    }

    private static BindZone createZone(final String zoneName) {
        final BindZone zone = new BindZone(zoneName);
        zone.add(zoneName,
                new SOARR(zoneName, "ns1.test.com", "hostmaster.test.com",
                        1, 7200, 3600, 1209600, 3600, 3600));
        zone.add(zoneName, new ARR(zoneName, 3600, "192.168.1.2"));
        return zone;
    }

    private static BindZone createLargeTxtZone() {
        final BindZone zone = new BindZone(ZONE_NAME);
        zone.add(ZONE_NAME,
                new SOARR(ZONE_NAME, "ns1.test.com", "hostmaster.test.com",
                        1, 7200, 3600, 1209600, 3600, 3600));

        zone.add(ZONE_NAME, new TXTRR(ZONE_NAME, 3600, repeatedText(220)));
        return zone;
    }

    private static BindZone createDnssecAZone() {
        final BindZone zone = createZone(ZONE_NAME);
        zone.add(ZONE_NAME,
                new RRSIG(ZONE_NAME, 3600, RRCode.A, 10, 2, 3600,
                        0x5af00000, 0x5ae00000, 12023, ZONE_NAME, "AQ=="));
        return zone;
    }

        private static BindZone createDnssecAZoneWithLargeSignature() {
        final BindZone zone = createZone(ZONE_NAME);
        zone.add(ZONE_NAME,
            new RRSIG(ZONE_NAME, 3600, RRCode.A, 10, 2, 3600,
                0x5af00000, 0x5ae00000, 12023, ZONE_NAME, largeSignatureBase64()));
        return zone;
        }

        private static BindZone createDnssecNegativeZone() {
        final BindZone zone = createZone(ZONE_NAME);
        zone.add(ZONE_NAME,
            new NSECRR(ZONE_NAME, 3600, ZONE_NAME,
                EnumSet.of(RRCode.SOA, RRCode.RRSIG, RRCode.NSEC)));
        zone.add(ZONE_NAME,
            new RRSIG(ZONE_NAME, 3600, RRCode.SOA, 10, 2, 3600,
                0x5af00000, 0x5ae00000, 12023, ZONE_NAME, "AQ=="));
        zone.add(ZONE_NAME,
            new RRSIG(ZONE_NAME, 3600, RRCode.NSEC, 10, 2, 3600,
                0x5af00000, 0x5ae00000, 12023, ZONE_NAME, "AQ=="));
        return zone;
        }

            private static BindZone createDnssecSoaAdditionalZone() {
            final BindZone zone = createZone(ZONE_NAME);
            zone.add(ZONE_NAME,
                new DNSKEYRR(ZONE_NAME, 3600, 257, 3, 8, "AQ=="));
            zone.add(ZONE_NAME,
                new RRSIG(ZONE_NAME, 3600, RRCode.SOA, 10, 2, 3600,
                    0x5af00000, 0x5ae00000, 12023, ZONE_NAME, "AQ=="));
            zone.add(ZONE_NAME,
                new RRSIG(ZONE_NAME, 3600, RRCode.DNSKEY, 10, 2, 3600,
                    0x5af00000, 0x5ae00000, 12023, ZONE_NAME, "AQ=="));
            return zone;
            }

    private static BindZone createCnameZone() {
        final BindZone zone = new BindZone(ZONE_NAME);
        zone.add(ZONE_NAME,
                new SOARR(ZONE_NAME, "ns1.test.com", "hostmaster.test.com",
                        1, 7200, 3600, 1209600, 3600, 3600));

        zone.add("www.test.com", new CNAMERR("www.test.com", 3600, "alias.test.com"));
        zone.add("alias.test.com", new ARR("alias.test.com", 3600, "192.168.1.42"));
        return zone;
    }

    private static BindZone createMultiQuestionZone() {
        final BindZone zone = new BindZone(ZONE_NAME);
        zone.add(ZONE_NAME,
                new SOARR(ZONE_NAME, "ns1.test.com", "hostmaster.test.com",
                        1, 7200, 3600, 1209600, 3600, 3600));
        zone.add("a.test.com", new ARR("a.test.com", 3600, "192.168.1.11"));
        zone.add("b.test.com", new ARR("b.test.com", 3600, "192.168.1.12"));
        return zone;
    }

    private static String repeatedText(final int size) {
        final char[] chars = new char[size];
        Arrays.fill(chars, 'x');
        return new String(chars);
    }

    private static String largeSignatureBase64() {
        final byte[] bytes = new byte[128];
        Arrays.fill(bytes, (byte) 0x01);
        return Base64.getEncoder().encodeToString(bytes);
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

    private static byte[] buildMultiQuestionQuery(final int id, final String[] qNames,
                                                   final RRCode[] types) {
        if (qNames.length != types.length) {
            throw new IllegalArgumentException("Question names and types must align");
        }

        byte[] queryBytes = new byte[] {
                (byte) ((id >> 8) & 0xff), (byte) (id & 0xff),
                0x01, 0x00,
                (byte) ((qNames.length >> 8) & 0xff), (byte) (qNames.length & 0xff),
                0x00, 0x00,
                0x00, 0x00,
                0x00, 0x00
        };

        for (int i = 0; i < qNames.length; i++) {
            queryBytes = Utils.combine(queryBytes, DnsNameCodec.convertString(qNames[i]));
            queryBytes = Utils.combine(queryBytes, new byte[] {
                    (byte) ((types[i].getCode() >> 8) & 0xff),
                    (byte) (types[i].getCode() & 0xff),
                    0x00, 0x01
            });
        }

        return queryBytes;
    }

        private static byte[] buildQueryWithOptPayload(final int id, final String qName,
                               final RRCode type, final int payloadSize) {
        return buildQueryWithOptPayload(id, qName, type, payloadSize, false);
        }

        private static byte[] buildQueryWithOptPayload(final int id, final String qName,
                               final RRCode type, final int payloadSize,
                               final boolean dnssecEnabled) {
        byte[] queryBytes = buildQuery(id, qName, type);

        queryBytes[11] = 0x01;

        byte[] zFlags = dnssecEnabled
            ? new byte[] {0x00, 0x00, (byte) 0x80, 0x00}
            : new byte[] {0x00, 0x00, 0x00, 0x00};

        queryBytes = Utils.combine(queryBytes, new byte[] {
                0x00,
                0x00, 0x29,
                (byte) ((payloadSize >> 8) & 0xff), (byte) (payloadSize & 0xff),
            zFlags[0], zFlags[1], zFlags[2], zFlags[3],
                0x00, 0x00
        });

        return queryBytes;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Zone> getBindZones() throws Exception {
        final Field bindZonesField = JDNSS.class.getDeclaredField("bindZones");
        bindZonesField.setAccessible(true);
        return (Map<String, Zone>) bindZonesField.get(null);
    }

    private static int readUInt16(final byte[] bytes, final int offset) {
        return (unsignedByte(bytes[offset]) << 8) | unsignedByte(bytes[offset + 1]);
    }

    private static int unsignedByte(final byte value) {
        return value & 0xff;
    }

    private static boolean containsIpv4(final byte[] bytes, final int a, final int b,
                                        final int c, final int d) {
        for (int i = 0; i <= bytes.length - 4; i++) {
            if (unsignedByte(bytes[i]) == a
                    && unsignedByte(bytes[i + 1]) == b
                    && unsignedByte(bytes[i + 2]) == c
                    && unsignedByte(bytes[i + 3]) == d) {
                return true;
            }
        }
        return false;
    }

    private final byte[] queryNoCookie = {(byte) 0x5f, (byte) 0x3e, (byte) 0x01
            , (byte) 0x20, (byte) 0x00, (byte) 0x01, (byte) 0x00
            , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
            , (byte) 0x00, (byte) 0x04, (byte) 0x74, (byte) 0x65
            , (byte) 0x73, (byte) 0x74, (byte) 0x03, (byte) 0x63
            , (byte) 0x6f, (byte) 0x6d, (byte) 0x00, (byte) 0x00
            , (byte) 0x01, (byte) 0x00, (byte) 0x01};

    private final byte[] queryMissingARecord = {
            (byte) 0x12, (byte) 0x34, (byte) 0x01, (byte) 0x00,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x07, (byte) 0x6d, (byte) 0x69, (byte) 0x73,
            (byte) 0x73, (byte) 0x69, (byte) 0x6e, (byte) 0x67,
            (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
            (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
            (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
            (byte) 0x00, (byte) 0x01
    };
}
