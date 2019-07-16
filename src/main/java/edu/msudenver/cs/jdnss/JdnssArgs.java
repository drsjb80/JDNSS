package edu.msudenver.cs.jdnss;

import lombok.Getter;
import lombok.Setter;

enum JDNSSLogLevels {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL}

class jdnssArgs {
    @Getter private boolean once = false;
    @Getter private int threads = 10;

    @Getter private boolean version;
    String[] IPaddresses = {"TLS@0.0.0.0@853", "TCP@0.0.0.0@53", "UDP@0.0.0.0@53"}; // "MC@224.0.0.251@5353"
    int backlog = 4;
    JDNSSLogLevels logLevel = JDNSSLogLevels.ERROR;

    @Getter private boolean help;

    @Getter private String DBClass;
    @Getter private String DBURL;
    @Getter private String DBUser;
    @Getter private String DBPass;

    String serverSecret;

    String keystoreFile;
    String keystorePassword;
    boolean debugSSL;

    String prefsFile;

    @Getter private String additional[];
}
