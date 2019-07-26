package edu.msudenver.cs.jdnss;

import com.sun.net.httpserver.*;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


// Source: https://stackoverflow.com/a/34483734

public class HTTPS {
    private final Logger logger = JDNSS.logger;

    public HTTPS(final String[] parts) {
        int port = Integer.parseInt(parts[2]);

        try {
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(parts[1]), port);

            HttpsServer httpsServer = HttpsServer.create(address, JDNSS.jargs.backlog);
            setSSLParameters(httpsServer);
            httpsServer.createContext("/dns-query", new MyHandler());
            httpsServer.setExecutor(null);
            httpsServer.start();
        } catch (Exception exception) {
            System.out.println("Failed to create HTTPS server on port " + port + " of localhost");
            exception.printStackTrace();
        }
    }

    private void setSSLParameters(final HttpsServer httpsServer) throws Exception {
        SSLContext context = getSslContext();
        httpsServer.setHttpsConfigurator(new HttpsConfigurator(context) {
            public void configure(HttpsParameters params) {
                try {
                    SSLEngine engine = context.createSSLEngine();
                    params.setNeedClientAuth(false);
                    params.setCipherSuites(engine.getEnabledCipherSuites());
                    params.setProtocols(engine.getEnabledProtocols());

                    SSLParameters sslParameters = context.getSupportedSSLParameters();
                    params.setSSLParameters(sslParameters);

                } catch (Exception ex) {
                    System.out.println("Failed to create SSL parameters");
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
            Response r = null;

            switch (t.getRequestMethod()) {
                case "GET":
                    r = getResponse(t);
                    break;
                case "POST":
                    r = postResponse(t);
                    break;
            }
            for (String key : t.getRequestHeaders().keySet()) {
                System.out.println(key);
                System.out.println(t.getRequestHeaders().get(key));
            }

            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String encoded_response = Base64.getEncoder().encodeToString(r.getBytes());
            System.out.println(encoded_response);
            t.sendResponseHeaders(200, encoded_response.getBytes().length);

            try (OutputStream os = t.getResponseBody()) {
                os.write(encoded_response.getBytes());
           }
        }

        private Response postResponse(HttpExchange t) throws IOException {
            Query q;
            Response r;
            List<Byte> ar = new ArrayList<>();

            try (InputStream is = t.getRequestBody()) {
                int c;
                while ((c = is.read()) != -1) {
                    ar.add((byte) c);
                }
            }

            byte[] post_query = new byte[ar.size()];
            int count = 0;
            for (Byte b : ar) {
                post_query[count++] = b;
            }

            q = new Query(post_query);
            q.parseQueries(t.getRemoteAddress().toString());
            r = new Response(q, false);
            return r;
        }

        private Response getResponse(HttpExchange t) {
            Query q;
            Response r;
            String[] both = t.getRequestURI().getQuery().split("=");
            byte[] decoded = Base64.getDecoder().decode(both[1]);

            q = new Query(decoded);
            q.parseQueries(t.getRemoteAddress().toString());
            r = new Response(q, false);
            return r;
        }
    }

    private SSLContext getSslContext() throws Exception {
        Assertion.aver(JDNSS.jargs.keystoreFile != null);
        Assertion.aver(JDNSS.jargs.keystorePassword!= null);

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
