package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.io.UnsupportedEncodingException;

class ServerCookie {
    private final static Logger logger = JDNSS.logger;
    final private String serverSecret = JDNSS.jargs.serverSecret;
    private final long hash;

    ServerCookie(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie, "UTF-8") + clientIP + serverSecret);
        logger.trace(serverSecret);
        hash = fnv.getHash();
        logger.trace(hash);
    }

    //check server cookie based on a client cookie and IP Address
    boolean isValid(byte[] clientCookie, String clientIP) throws UnsupportedEncodingException {
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie, "UTF-8") + clientIP + serverSecret);
        return this.hash == fnv.getHash();
    }

    byte[] getBytes() {
        return Utils.getBytes(this.hash);
    }
}
