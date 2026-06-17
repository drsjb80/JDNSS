package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ServerCookie {
    private final static Logger logger = JDNSS.logger;
    final private String serverSecret = JDNSS.jargs.serverSecret;
    private final long hash;

    private byte[] serverSecretBytes() {
        String secret = serverSecret == null ? "" : serverSecret;
        return secret.getBytes(StandardCharsets.UTF_8);
    }

    private long computeHash(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(clientCookie);
            md.update(clientIP.getBytes(StandardCharsets.UTF_8));
            md.update(serverSecretBytes());
            byte[] digest = md.digest();
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (digest[i] & 0xff);
            }
            return result;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    ServerCookie(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        hash = computeHash(clientCookie, clientIP);
        logger.trace(serverSecret);
        logger.trace(hash);
    }

    //check server cookie based on a client cookie and IP Address
    boolean isValid(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        return this.hash == computeHash(clientCookie, clientIP);
    }

    byte[] getBytes() {
        return Utils.getBytes(this.hash);
    }
}
