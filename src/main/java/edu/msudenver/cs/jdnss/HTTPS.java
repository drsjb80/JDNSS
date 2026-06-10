package edu.msudenver.cs.jdnss;

import com.sun.net.httpserver.*;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Source: https://stackoverflow.com/a/34483734

public class HTTPS {
    private final Logger logger = JDNSS.logger;
    private static final String DNS_MESSAGE_CONTENT_TYPE = "application/dns-message";
    private static final String JSON_CONTENT_TYPE = "application/dns-json; charset=utf-8";

    public HTTPS(@NotNull final String[] parts) {
        this(parts, true);
    }

    HTTPS(@NotNull final String[] parts, final boolean startServer) {
        if (!startServer) {
            return;
        }

        NetworkBinding binding = NetworkBinding.fromParts(parts);
        int port = binding.getPort();

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(binding.getHost()), port);

            HttpsServer httpsServer = HttpsServer.create(address, JDNSS.jargs.backlog);
            setSSLParameters(httpsServer);
            httpsServer.createContext("/dns-query", new MyHandler());
            httpsServer.setExecutor(null);
            httpsServer.start();
        } catch (Exception exception) {
            logger.error("Failed to create HTTPS server on port {} of {}", port,
                    binding.getHost(), exception);
        }
    }

    HttpHandler createHandler() {
        return new MyHandler();
    }

    private void setSSLParameters(final HttpsServer httpsServer) throws Exception {
        SSLContext context = getSslContext();
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(context) {
            public void configure(HttpsParameters params) {
                try {
                    SSLEngine engine = context.createSSLEngine();
                    SSLParameters sslParameters = context.getSupportedSSLParameters();
                    sslParameters.setNeedClientAuth(false);
                    sslParameters.setCipherSuites(engine.getEnabledCipherSuites());
                    sslParameters.setProtocols(engine.getEnabledProtocols());
                    params.setSSLParameters(sslParameters);

                } catch (Exception ex) {
                    logger.error("Failed to create SSL parameters", ex);
                }
            }

        });
    }

    /*
    When the HTTP method is GET,
    the single variable "dns" is defined as the content of the DNS
    request (as described in Section 6), encoded with base64url
    [RFC4648].

    When using the POST method, the DNS query is included as the message
    body of the HTTP request, and the Content-Type request header field
    indicates the media type of the message.
    */

    private class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            RenderedResponse rendered = null;

            switch (t.getRequestMethod()) {
                case "GET":
                    rendered = getResponse(t);
                    break;
                case "POST":
                    rendered = postResponse(t);
                    break;
                default:
                    logger.warn("Unsupported DoH method: {}", t.getRequestMethod());
                    t.sendResponseHeaders(405, -1);
                    return;
            }

            if (rendered == null) {
                t.sendResponseHeaders(500, -1);
                return;
            }

            for (String key : t.getRequestHeaders().keySet()) {
                logger.debug("Request header {}: {}", key, t.getRequestHeaders().get(key));
            }

            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if (rendered.contentType != null) {
                t.getResponseHeaders().set("Content-Type", rendered.contentType);
            }

            logger.trace("Encoded response length: {}", rendered.body.length);
            t.sendResponseHeaders(200, rendered.body.length);

            try (OutputStream os = t.getResponseBody()) {
                os.write(rendered.body);
            }
        }

        private RenderedResponse postResponse(HttpExchange t) {
            Query q;
            Response r;
            List<Byte> ar = new ArrayList<>();

            try (InputStream is = t.getRequestBody()) {
                int c;
                while ((c = is.read()) != -1) {
                    ar.add((byte) c);
                }
            } catch (IOException ioe) {
                logger.debug("Unable to read DoH POST request body: {}", ioe.getMessage());
                return null;
            }

            byte[] post_query = new byte[ar.size()];
            int count = 0;
            for (Byte b : ar) {
                post_query[count++] = b;
            }

            try {
                q = new Query(post_query);
                q.parseQueries(t.getRemoteAddress().toString());
                r = new Response(q, true);
                return new RenderedResponse(r.getBytes(), DNS_MESSAGE_CONTENT_TYPE);
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid DoH POST query: {}", e.getMessage());
                return null;
            } catch (RuntimeException e) {
                logger.catching(e);
                return null;
            }
        }

        private RenderedResponse getResponse(HttpExchange t) {
            String query = t.getRequestURI().getQuery();
            if (query == null) {
                return null;
            }

            Map<String, String> params = parseQueryParameters(query);
            if (params.containsKey("dns")) {
                return getBinaryResponse(t, params.get("dns"));
            }

            if (!params.containsKey("name")) {
                return null;
            }

            try {
                byte[] queryBytes = buildJsonQuery(params);
                Query q = new Query(queryBytes);
                q.parseQueries(t.getRemoteAddress().toString());
                Response response = new Response(q, false);
                String json = toJsonResponse(response.getBytes());
                return new RenderedResponse(json.getBytes(StandardCharsets.UTF_8), JSON_CONTENT_TYPE);
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid DoH JSON query: {}", e.getMessage());
                return null;
            } catch (RuntimeException e) {
                logger.catching(e);
                return null;
            }
        }

        private RenderedResponse getBinaryResponse(final HttpExchange t, final String encodedQuery) {
            try {
                byte[] decoded = decodeDnsQuery(encodedQuery);
                Query q = new Query(decoded);
                q.parseQueries(t.getRemoteAddress().toString());
                Response r = new Response(q, true);
                return new RenderedResponse(r.getBytes(), DNS_MESSAGE_CONTENT_TYPE);
            } catch (IllegalArgumentException e) {
                logger.debug("Invalid DoH binary query: {}", e.getMessage());
                return null;
            } catch (RuntimeException e) {
                logger.catching(e);
                return null;
            }
        }

        private byte[] decodeDnsQuery(final String encodedQuery) {
            String normalized = encodedQuery.replace('-', '+').replace('_', '/');
            int remainder = normalized.length() % 4;
            if (remainder != 0) {
                normalized = normalized + "=".repeat(4 - remainder);
            }
            return Base64.getDecoder().decode(normalized);
        }

        private Map<String, String> parseQueryParameters(final String query) {
            Map<String, String> params = new LinkedHashMap<>();

            for (String pair : query.split("&")) {
                if (pair.isEmpty()) {
                    continue;
                }

                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                String value = parts.length > 1 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                params.put(key, value);
            }

            return params;
        }

        private byte[] buildJsonQuery(final Map<String, String> params) {
            final String name = params.get("name");
            final RRCode type = resolveQueryType(params.get("type"));

            byte[] queryBytes = new byte[] {
                    0x00, 0x01,
                    0x01, 0x00,
                    0x00, 0x01,
                    0x00, 0x00,
                    0x00, 0x00,
                    0x00, 0x00
            };

            queryBytes = Utils.combine(queryBytes, DnsNameCodec.convertString(name));
            queryBytes = Utils.combine(queryBytes, new byte[] {
                    (byte) ((type.getCode() >> 8) & 0xff), (byte) (type.getCode() & 0xff),
                    0x00, 0x01
            });

            return queryBytes;
        }

        private RRCode resolveQueryType(final String typeName) {
            if (typeName == null || typeName.isEmpty()) {
                return RRCode.A;
            }
            try {
                return RRCode.valueOf(typeName.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(typeName);
            }
        }

        private String toJsonResponse(final byte[] responseBytes) {
            final int questionCount = readUInt16(responseBytes, 4);
            final int answerCount = readUInt16(responseBytes, 6);
            final int authorityCount = readUInt16(responseBytes, 8);
            final int additionalCount = readUInt16(responseBytes, 10);

            int current = 12;
            StringBuilder json = new StringBuilder();
            json.append('{');
            json.append("\"Status\":").append(responseBytes[3] & 0x0f).append(',');
            json.append("\"TC\":").append((responseBytes[2] & 0x02) != 0).append(',');
            json.append("\"RD\":").append((responseBytes[2] & 0x01) != 0).append(',');
            json.append("\"RA\":").append((responseBytes[3] & 0x80) != 0).append(',');
            json.append("\"AD\":").append((responseBytes[3] & 0x20) != 0).append(',');
            json.append("\"CD\":").append((responseBytes[3] & 0x10) != 0).append(',');

            ParseResult<QuestionRecord> questions = parseQuestions(responseBytes, current, questionCount);
            current = questions.nextIndex;
            ParseResult<ResourceRecord> answers = parseResourceRecords(responseBytes, current, answerCount);
            current = answers.nextIndex;
            ParseResult<ResourceRecord> authorities = parseResourceRecords(responseBytes, current, authorityCount);
            current = authorities.nextIndex;
            ParseResult<ResourceRecord> additionals = parseResourceRecords(responseBytes, current, additionalCount);

            json.append("\"Question\":").append(renderQuestions(questions.records)).append(',');
            json.append("\"Answer\":").append(renderResourceRecords(answers.records)).append(',');
            json.append("\"Authority\":").append(renderResourceRecords(authorities.records)).append(',');
            json.append("\"Additional\":").append(renderResourceRecords(additionals.records));
            json.append('}');
            return json.toString();
        }

        private ParseResult<QuestionRecord> parseQuestions(final byte[] responseBytes, final int start,
                                                           final int count) {
            int current = start;
            List<QuestionRecord> records = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                Map.Entry<String, Integer> parsedName = DnsNameCodec.parseName(current, responseBytes);
                current = parsedName.getValue();

                final int type = readUInt16(responseBytes, current);
                current += 2;
                final int clazz = readUInt16(responseBytes, current);
                current += 2;

                records.add(new QuestionRecord(parsedName.getKey(), type, clazz));
            }

            return new ParseResult<>(records, current);
        }

        private ParseResult<ResourceRecord> parseResourceRecords(final byte[] responseBytes,
                                                                 final int start,
                                                                 final int count) {
            int current = start;
            List<ResourceRecord> records = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                Map.Entry<String, Integer> parsedName = DnsNameCodec.parseName(current, responseBytes);
                current = parsedName.getValue();

                final int type = readUInt16(responseBytes, current);
                current += 2;
                current += 2;
                final long ttl = readUInt32(responseBytes, current);
                current += 4;
                final int rdlength = readUInt16(responseBytes, current);
                current += 2;

                final int rdataStart = current;
                current += rdlength;

                if (type != RRCode.OPT.getCode()) {
                    final String data = formatRdata(type, responseBytes, rdataStart, rdlength);
                    records.add(new ResourceRecord(parsedName.getKey(), type, ttl, data));
                }
            }

            return new ParseResult<>(records, current);
        }

        private String renderQuestions(final List<QuestionRecord> records) {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                QuestionRecord record = records.get(i);
                json.append('{')
                        .append("\"name\":\"").append(escapeJson(record.name)).append("\",")
                        .append("\"type\":").append(record.type)
                        .append('}');
            }
            json.append(']');
            return json.toString();
        }

        private String renderResourceRecords(final List<ResourceRecord> records) {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < records.size(); i++) {
                if (i > 0) {
                    json.append(',');
                }
                ResourceRecord record = records.get(i);
                json.append('{')
                        .append("\"name\":\"").append(escapeJson(record.name)).append("\",")
                        .append("\"type\":").append(record.type).append(',')
                        .append("\"TTL\":").append(record.ttl).append(',')
                        .append("\"data\":\"").append(escapeJson(record.data)).append("\"")
                        .append('}');
            }
            json.append(']');
            return json.toString();
        }

        private String formatRdata(final int type, final byte[] responseBytes,
                                   final int rdataStart, final int rdlength) {
            final RRCode rrCode = RRCode.findCode(type);

            try {
                switch (rrCode) {
                    case A:
                    case AAAA:
                        return InetAddress.getByAddress(
                                copyRange(responseBytes, rdataStart, rdlength)).getHostAddress();
                    case CNAME:
                    case DNAME:
                    case NS:
                    case PTR:
                        return normalizeDnsName(DnsNameCodec.parseName(rdataStart, responseBytes).getKey());
                    case MX: {
                        final int preference = readUInt16(responseBytes, rdataStart);
                        final String exchange = normalizeDnsName(
                                DnsNameCodec.parseName(rdataStart + 2, responseBytes).getKey());
                        return preference + " " + exchange;
                    }
                    case SOA: {
                        int offset = rdataStart;
                        Map.Entry<String, Integer> mnameEntry = DnsNameCodec.parseName(offset, responseBytes);
                        String mname = normalizeDnsName(mnameEntry.getKey());
                        offset = mnameEntry.getValue();

                        Map.Entry<String, Integer> rnameEntry = DnsNameCodec.parseName(offset, responseBytes);
                        String rname = normalizeDnsName(rnameEntry.getKey());
                        offset = rnameEntry.getValue();

                        long serial = readUInt32(responseBytes, offset);
                        offset += 4;
                        long refresh = readUInt32(responseBytes, offset);
                        offset += 4;
                        long retry = readUInt32(responseBytes, offset);
                        offset += 4;
                        long expire = readUInt32(responseBytes, offset);
                        offset += 4;
                        long minimum = readUInt32(responseBytes, offset);

                        return mname + " " + rname + " " + serial + " " + refresh + " " + retry + " " + expire + " " + minimum;
                    }
                    case HINFO: {
                        int offset = rdataStart;
                        int cpuLen = responseBytes[offset++] & 0xff;
                        String cpu = new String(responseBytes, offset, cpuLen, StandardCharsets.US_ASCII);
                        offset += cpuLen;
                        int osLen = responseBytes[offset++] & 0xff;
                        String os = new String(responseBytes, offset, osLen, StandardCharsets.US_ASCII);
                        return cpu + " " + os;
                    }
                    case SRV: {
                        int offset = rdataStart;
                        int priority = readUInt16(responseBytes, offset);
                        offset += 2;
                        int weight = readUInt16(responseBytes, offset);
                        offset += 2;
                        int port = readUInt16(responseBytes, offset);
                        offset += 2;
                        String target = normalizeDnsName(DnsNameCodec.parseName(offset, responseBytes).getKey());
                        return priority + " " + weight + " " + port + " " + target;
                    }
                    case TLSA: {
                        int offset = rdataStart;
                        int usage = responseBytes[offset++] & 0xff;
                        int selector = responseBytes[offset++] & 0xff;
                        int matchingType = responseBytes[offset++] & 0xff;
                        byte[] assocData = copyRange(responseBytes, offset, rdlength - 3);
                        String hex = bytesToHex(assocData);
                        return usage + " " + selector + " " + matchingType + " " + hex;
                    }
                    case CAA: {
                        int offset = rdataStart;
                        int flags = responseBytes[offset++] & 0xff;
                        int tagLen = responseBytes[offset++] & 0xff;
                        String tag = new String(responseBytes, offset, tagLen, StandardCharsets.US_ASCII);
                        offset += tagLen;
                        byte[] value = copyRange(responseBytes, offset, rdlength - 2 - tagLen);
                        String valueStr = new String(value, StandardCharsets.US_ASCII);
                        return flags + " " + tag + " " + valueStr;
                    }
                    case TXT:
                        return decodeTxt(copyRange(responseBytes, rdataStart, rdlength));
                    default:
                        return Base64.getEncoder().encodeToString(copyRange(responseBytes, rdataStart, rdlength));
                }
            } catch (Exception e) {
                logger.trace("Unable to format RDATA for {}", rrCode, e);
                return Base64.getEncoder().encodeToString(copyRange(responseBytes, rdataStart, rdlength));
            }
        }

        private String decodeTxt(final byte[] rdata) {
            if (rdata.length == 0) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            int offset = 0;
            while (offset < rdata.length) {
                int len = rdata[offset++] & 0xff;
                if (offset + len > rdata.length) {
                    return Base64.getEncoder().encodeToString(rdata);
                }
                sb.append(new String(rdata, offset, len, StandardCharsets.US_ASCII));
                offset += len;
            }
            return sb.toString();
        }

        private String normalizeDnsName(final String name) {
            if (name == null || name.isEmpty()) {
                return name;
            }

            return name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
        }

        private byte[] copyRange(final byte[] bytes, final int start, final int length) {
            byte[] copy = new byte[length];
            System.arraycopy(bytes, start, copy, 0, length);
            return copy;
        }

        private String escapeJson(final String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"");
        }

        private int readUInt16(final byte[] bytes, final int offset) {
            return ((bytes[offset] & 0xff) << 8) | (bytes[offset + 1] & 0xff);
        }

        private long readUInt32(final byte[] bytes, final int offset) {
            return ((long) (bytes[offset] & 0xff) << 24)
                    | ((long) (bytes[offset + 1] & 0xff) << 16)
                    | ((long) (bytes[offset + 2] & 0xff) << 8)
                    | (long) (bytes[offset + 3] & 0xff);
        }

        private String bytesToHex(final byte[] bytes) {
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
    }

    private static final class RenderedResponse {
        private final byte[] body;
        private final String contentType;

        private RenderedResponse(final byte[] body, final String contentType) {
            this.body = body;
            this.contentType = contentType;
        }
    }

    private static final class ParseResult<T> {
        private final List<T> records;
        private final int nextIndex;

        private ParseResult(final List<T> records, final int nextIndex) {
            this.records = records;
            this.nextIndex = nextIndex;
        }
    }

    private static final class QuestionRecord {
        private final String name;
        private final int type;
        private final int clazz;

        private QuestionRecord(final String name, final int type, final int clazz) {
            this.name = name;
            this.type = type;
            this.clazz = clazz;
        }
    }

    private static final class ResourceRecord {
        private final String name;
        private final int type;
        private final long ttl;
        private final String data;

        private ResourceRecord(final String name, final int type,
                               final long ttl, final String data) {
            this.name = name;
            this.type = type;
            this.ttl = ttl;
            this.data = data;
        }
    }

    public SSLContext getSslContext() throws Exception {
        assert JDNSS.jargs.keystoreFile != null;
        assert JDNSS.jargs.keystorePassword!= null;

        SSLContext sslContext = SSLContext.getInstance("TLS");

        char[] password = JDNSS.jargs.keystorePassword.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");

        try (FileInputStream fis = new FileInputStream(JDNSS.jargs.keystoreFile)) {
            ks.load(fis, password);
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }
}
