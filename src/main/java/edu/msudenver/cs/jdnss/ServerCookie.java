package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

class ServerCookie {
    private final static Logger logger = JDNSS.logger;
    final private String serverSecret = JDNSS.jargs.getServerSecret();
    private final long hash;

    ServerCookie(byte[] clientCookie, String clientIP) {
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie) + clientIP + serverSecret);
        logger.trace(serverSecret);
        hash = fnv.getHash();
        logger.trace(hash);
    }

    //check server cookie based on a client cookie and IP Address
    boolean isValid(byte[] clientCookie, String clientIP) {
        FNV1a64 fnv = new FNV1a64();
        fnv.init(new String(clientCookie) + clientIP + serverSecret);
        return this.hash == fnv.getHash();
    }

    byte[] getBytes() {
        return Utils.getBytes(this.hash);
    }
}
