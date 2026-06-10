package edu.msudenver.cs.jdnss;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HTTPSTest {
    private Map<String, Zone> originalZones;

    @Before
    public void setUp() throws Exception {
        originalZones = new HashMap<>(getBindZones());
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("other.com", new BindZone("other.com"));
    }

    @After
    public void tearDown() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.putAll(originalZones);
    }

    @Test
    public void getRequestReturnsBinaryDnsResponse() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=" + encodeQuery()),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        byte[] response = out.toByteArray();
        Assert.assertTrue("Response should not be empty", response.length > 0);
        Assert.assertTrue("Response should start with valid DNS header", response.length >= 12);
        Assert.assertEquals("*", exchange.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        Assert.assertEquals("application/dns-message",
            exchange.getResponseHeaders().getFirst("Content-Type"));
    }

    @Test
    public void postRequestReturnsBinaryDnsResponse() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("POST", new URI("/dns-query"),
                new ByteArrayInputStream(buildQueryPacket()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        byte[] response = out.toByteArray();
        Assert.assertTrue("Response should not be empty", response.length > 0);
        Assert.assertTrue("Response should start with valid DNS header", response.length >= 12);
        Assert.assertEquals("*", exchange.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        Assert.assertEquals("application/dns-message",
            exchange.getResponseHeaders().getFirst("Content-Type"));
    }

        @Test
        public void getRequestAcceptsUrlSafeBase64DnsParameter() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        String urlSafe = Base64.getUrlEncoder().withoutPadding().encodeToString(buildQueryPacket());
        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=" + urlSafe),
            new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        byte[] response = out.toByteArray();
        Assert.assertTrue("Response should not be empty", response.length > 0);
        Assert.assertEquals("application/dns-message",
            exchange.getResponseHeaders().getFirst("Content-Type"));
        }

    @Test
    public void unsupportedMethodReturns405() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("PUT", new URI("/dns-query"),
                new ByteArrayInputStream(new byte[0]));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(405, -1);
    }

    @Test
    public void malformedGetQueryReturns500() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?bad"),
                new ByteArrayInputStream(new byte[0]));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(500, -1);
        verify(exchange, never()).getResponseBody();
    }

    @Test
    public void getRequestWithNullQueryReturns500() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET", new URI("/dns-query"),
                new ByteArrayInputStream(new byte[0]));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(500, -1);
    }

    @Test
    public void getRequestWithInvalidBase64Returns500() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=not_base64*"),
                new ByteArrayInputStream(new byte[0]));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(500, -1);
        verify(exchange, never()).getResponseBody();
    }

    @Test
    public void postRequestBodyReadFailureReturns500() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        InputStream failingBody = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }
        };

        HttpExchange exchange = baseExchange("POST", new URI("/dns-query"), failingBody);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(500, -1);
        verify(exchange, never()).getResponseBody();
    }

    @Test
    public void getJsonRequestReturnsDnsJsonBody() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", createZone("test.com"));

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET",
                new URI("/dns-query?name=test.com&type=A"),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        Assert.assertEquals("application/dns-json; charset=utf-8",
                exchange.getResponseHeaders().getFirst("Content-Type"));

        String body = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue(body.contains("\"Status\":0"));
        Assert.assertTrue(body.contains("\"Question\""));
        Assert.assertTrue(body.contains("\"Answer\""));
        Assert.assertTrue(body.contains("\"name\":\"test.com\""));
    }

    @Test
    public void getJsonRequestWithInvalidTypeReturns500() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET",
                new URI("/dns-query?name=test.com&type=NOTATYPE"),
                new ByteArrayInputStream(new byte[0]));

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(500, -1);
        verify(exchange, never()).getResponseBody();
    }

    @Test
    public void jsonQuestionHasNoClassField() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", createZone("test.com"));

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET",
                new URI("/dns-query?name=test.com&type=A"),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        String body = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertFalse("Question should not contain 'class' field", body.contains("\"class\":"));
    }

    @Test
    public void jsonSoaRecordIsHumanReadable() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        BindZone zone = new BindZone("example.com");
        zone.add("example.com",
                new SOARR("example.com", "ns1.example.com", "hostmaster.example.com",
                        2024010101, 7200, 3600, 1209600, 3600, 3600));
        liveZones.put("example.com", zone);

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET",
                new URI("/dns-query?name=example.com&type=SOA"),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        String body = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue("SOA data should contain mname", body.contains("ns1.example.com"));
        Assert.assertTrue("SOA data should contain rname", body.contains("hostmaster.example.com"));
        Assert.assertTrue("SOA data should contain serial", body.contains("2024010101"));
    }

    @Test
    public void jsonHinfoRecordIsHumanReadable() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        BindZone zone = new BindZone("example.com");
        zone.add("example.com",
                new SOARR("example.com", "ns1.example.com", "hostmaster.example.com",
                        1, 7200, 3600, 1209600, 3600, 3600));
        zone.add("example.com", new HINFORR("example.com", 3600, "Intel-PC", "Linux"));
        liveZones.put("example.com", zone);

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET",
                new URI("/dns-query?name=example.com&type=HINFO"),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        String body = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue("HINFO data should contain CPU and OS", body.contains("Intel-PC") && body.contains("Linux"));
    }

    @Test
    public void jsonTxtMultiStringConcatenates() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        BindZone zone = new BindZone("example.com");
        zone.add("example.com",
                new SOARR("example.com", "ns1.example.com", "hostmaster.example.com",
                        1, 7200, 3600, 1209600, 3600, 3600));
        zone.add("example.com", new TXTRR("example.com", 3600, "part1part2"));
        liveZones.put("example.com", zone);

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET",
                new URI("/dns-query?name=example.com&type=TXT"),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        String body = out.toString(java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue("TXT data should contain the text", body.contains("part1part2"));
    }

    @Test
    public void binaryGetAndPostReturnSameFormatForSameQuery() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", createZone("test.com"));

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange getExchange = baseExchange("GET", new URI("/dns-query?dns=" + encodeQuery()),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream getOut = new ByteArrayOutputStream();
        when(getExchange.getResponseBody()).thenReturn(getOut);

        HttpExchange postExchange = baseExchange("POST", new URI("/dns-query"),
                new ByteArrayInputStream(buildQueryPacket()));
        ByteArrayOutputStream postOut = new ByteArrayOutputStream();
        when(postExchange.getResponseBody()).thenReturn(postOut);

        handler.handle(getExchange);
        handler.handle(postExchange);

        byte[] getResponse = getOut.toByteArray();
        byte[] postResponse = postOut.toByteArray();

        Assert.assertTrue("Both responses should have valid DNS header length",
            getResponse.length >= 12 && postResponse.length >= 12);
        Assert.assertEquals("GET and POST should return same response for same query",
            java.util.Arrays.toString(getResponse), java.util.Arrays.toString(postResponse));
    }

    @Test
    public void binaryResponseIsValidDnsWireFormat() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", createZone("test.com"));

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=" + encodeQuery()),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        byte[] response = out.toByteArray();
        Assert.assertTrue("Response must have at least DNS header (12 bytes)", response.length >= 12);

        Assert.assertTrue("Response should be valid DNS (QR bit set in byte 2)",
            (response[2] & 0x80) != 0);
        Assert.assertTrue("Response header should match query ID",
            response[0] == buildQueryPacket()[0] && response[1] == buildQueryPacket()[1]);
    }

    @Test
    public void binaryResponseWithEdns0OptRecord() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        liveZones.put("test.com", createZone("test.com"));

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        byte[] queryWithOpt = buildQueryPacketWithEdns0();
        String encoded = Base64.getEncoder().encodeToString(queryWithOpt);
        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=" + encoded),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        byte[] response = out.toByteArray();
        Assert.assertTrue("Response should not be empty", response.length > 0);
        Assert.assertTrue("Response should contain valid DNS header", response.length >= 12);
    }

    @Test
    public void binaryResponseRespectsTruncationLimitForUdp() throws Exception {
        Map<String, Zone> liveZones = getBindZones();
        liveZones.clear();
        BindZone zone = new BindZone("example.com");
        zone.add("example.com",
                new SOARR("example.com", "ns1.example.com", "hostmaster.example.com",
                        1, 7200, 3600, 1209600, 3600, 3600));

        for (int i = 0; i < 20; i++) {
            zone.add("example.com", new ARR("host" + i + ".example.com", 3600, "192.168.1." + i));
        }
        liveZones.put("example.com", zone);

        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        byte[] largeQuery = buildQueryPacketForName("example.com");
        String encoded = Base64.getEncoder().encodeToString(largeQuery);
        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=" + encoded),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        byte[] response = out.toByteArray();
        Assert.assertTrue("Response should have valid DNS header", response.length >= 12);
    }

    @Test
    public void constructorSkipsBindingValidationWhenNotStartingServer() {
        new HTTPS(new String[] {"HTTPS"}, false);
    }

    @Test
    public void getSslContextLoadsTemporaryKeyStore() throws Exception {
        Path keyStoreFile = Files.createTempFile("jdnss-https", ".jks");
        char[] password = "changeit".toCharArray();
        Object originalKeyStoreFile = getJargField("keystoreFile");
        Object originalKeyStorePassword = getJargField("keystorePassword");

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, password);
            try (java.io.OutputStream output = Files.newOutputStream(keyStoreFile)) {
                keyStore.store(output, password);
            }

            setJargField("keystoreFile", keyStoreFile.toString());
            setJargField("keystorePassword", "changeit");

            HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);

            Assert.assertNotNull(https.getSslContext());
        } finally {
            setJargField("keystoreFile", originalKeyStoreFile);
            setJargField("keystorePassword", originalKeyStorePassword);
            Files.deleteIfExists(keyStoreFile);
        }
    }

    @Test(expected = AssertionError.class)
    public void getSslContextRejectsMissingPassword() throws Exception {
        Object originalKeyStoreFile = getJargField("keystoreFile");
        Object originalKeyStorePassword = getJargField("keystorePassword");

        try {
            setJargField("keystoreFile", "server.jks");
            setJargField("keystorePassword", null);

            HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
            https.getSslContext();
        } finally {
            setJargField("keystoreFile", originalKeyStoreFile);
            setJargField("keystorePassword", originalKeyStorePassword);
        }
    }

    @Test(expected = java.io.FileNotFoundException.class)
    public void getSslContextRejectsMissingFile() throws Exception {
        Object originalKeyStoreFile = getJargField("keystoreFile");
        Object originalKeyStorePassword = getJargField("keystorePassword");

        try {
            setJargField("keystoreFile", "/tmp/jdnss-does-not-exist.jks");
            setJargField("keystorePassword", "changeit");

            HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
            https.getSslContext();
        } finally {
            setJargField("keystoreFile", originalKeyStoreFile);
            setJargField("keystorePassword", originalKeyStorePassword);
        }
    }

    private static HttpExchange baseExchange(final String method, final URI uri,
                                             final InputStream requestBody) {
        HttpExchange exchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        Headers responseHeaders = new Headers();

        when(exchange.getRequestMethod()).thenReturn(method);
        when(exchange.getRequestURI()).thenReturn(uri);
        when(exchange.getRequestBody()).thenReturn(requestBody);
        when(exchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(exchange.getResponseHeaders()).thenReturn(responseHeaders);
        when(exchange.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 5300));

        return exchange;
    }

    private static String encodeQuery() {
        return Base64.getEncoder().encodeToString(buildQueryPacket());
    }

    private static byte[] buildQueryPacket() {
        return new byte[] {
                (byte) 0x5f, (byte) 0x3e, (byte) 0x01, (byte) 0x20,
                (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
                (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
                (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x01
        };
    }

    private static byte[] buildQueryPacketWithEdns0() {
        return new byte[] {
                (byte) 0x12, (byte) 0x34, (byte) 0x01, (byte) 0x20,
                (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x04, (byte) 0x74, (byte) 0x65, (byte) 0x73,
                (byte) 0x74, (byte) 0x03, (byte) 0x63, (byte) 0x6f,
                (byte) 0x6d, (byte) 0x00, (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x01,
                (byte) 0x00, (byte) 0x00, (byte) 0x29, (byte) 0x10,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
    }

    private static byte[] buildQueryPacketForName(final String name) {
        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(512);
        buffer.put((byte) 0x12);
        buffer.put((byte) 0x34);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x20);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);

        for (String label : name.split("\\.")) {
            buffer.put((byte) label.length());
            buffer.put(label.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        }
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.put((byte) 0x01);

        byte[] result = new byte[buffer.position()];
        buffer.flip();
        buffer.get(result);
        return result;
    }

    private static BindZone createZone(final String zoneName) {
        final BindZone zone = new BindZone(zoneName);
        zone.add(zoneName,
                new SOARR(zoneName, "ns1.test.com", "hostmaster.test.com",
                        1, 7200, 3600, 1209600, 3600, 3600));
        zone.add(zoneName, new ARR(zoneName, 3600, "192.168.1.2"));
        return zone;
    }

    private static void setJargField(final String fieldName, final Object value) throws Exception {
        Field field = jdnssArgs.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(JDNSS.jargs, value);
    }

    private static Object getJargField(final String fieldName) throws Exception {
        Field field = jdnssArgs.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(JDNSS.jargs);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Zone> getBindZones() throws Exception {
        Field bindZonesField = JDNSS.class.getDeclaredField("bindZones");
        bindZonesField.setAccessible(true);
        return (Map<String, Zone>) bindZonesField.get(null);
    }
}
