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
    public void getRequestReturnsBase64Response() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("GET", new URI("/dns-query?dns=" + encodeQuery()),
                new ByteArrayInputStream(new byte[0]));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        Assert.assertTrue(out.size() > 0);
        Assert.assertEquals("*", exchange.getResponseHeaders().getFirst("Access-Control-Allow-Origin"));
        Assert.assertEquals("application/dns-message",
            exchange.getResponseHeaders().getFirst("Content-Type"));
    }

    @Test
    public void postRequestReturnsBase64Response() throws Exception {
        HTTPS https = new HTTPS(new String[] {"HTTPS", "127.0.0.1", "0"}, false);
        HttpHandler handler = https.createHandler();

        HttpExchange exchange = baseExchange("POST", new URI("/dns-query"),
                new ByteArrayInputStream(buildQueryPacket()));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(exchange.getResponseBody()).thenReturn(out);

        handler.handle(exchange);

        verify(exchange).sendResponseHeaders(eq(200), anyLong());
        Assert.assertTrue(out.size() > 0);
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
        Assert.assertTrue(out.size() > 0);
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
