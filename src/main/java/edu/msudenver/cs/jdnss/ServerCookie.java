package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

class ServerCookie {
    private final static Logger logger = JDNSS.logger;
    final private String serverSecret = JDNSS.jargs.serverSecret;
    private final long hash;

    private byte[] serverSecretBytes() {
        String secret = serverSecret == null ? "" : serverSecret;
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    ServerCookie(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        FNV1a64 fnv = new FNV1a64();
        fnv.init(clientCookie);
        fnv.update(clientIP.getBytes(StandardCharsets.UTF_8));
        fnv.update(serverSecretBytes());
        logger.trace(serverSecret);
        hash = fnv.getHash();
        logger.trace(hash);
    }

    //check server cookie based on a client cookie and IP Address
    boolean isValid(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        FNV1a64 fnv = new FNV1a64();
        fnv.init(clientCookie);
        fnv.update(clientIP.getBytes(StandardCharsets.UTF_8));
        fnv.update(serverSecretBytes());
        return this.hash == fnv.getHash();
    }

    byte[] getBytes() {
        return Utils.getBytes(this.hash);
    }
}
