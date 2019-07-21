package edu.msudenver.cs.jdnss;

import com.sun.net.httpserver.*;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.Base64;

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
            System.out.println(t.getRequestMethod());
            String query = t.getRequestURI().getQuery();
            System.out.println(query);

            String both[] = query.split("=");
            byte decoded[] = Base64.getDecoder().decode(both[1]);
            System.out.println(Utils.toString(decoded));

            Query q = new Query(decoded);
            q.parseQueries(t.getRemoteAddress().toString());
            System.out.println(q);

            System.out.println("BEFORE CREATING QUERY");
            Response r = new Response(q, false);
            System.out.println(r);

            // getRequestBody()

            for (String key: t.getRequestHeaders().keySet()) {
                System.out.println(key);
                System.out.println(t.getRequestHeaders().get(key));
            }

            String response = "This is the response";
            t.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            String encoded_response = Base64.getEncoder().encodeToString(response.getBytes());
            System.out.println(encoded_response);
            t.sendResponseHeaders(200, encoded_response.getBytes().length);
            //t.sendResponseHeaders(200, response.getBytes().length);
            //System.out.println(response);

            try (OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
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
