package edu.msudenver.cs.jdnss;

import lombok.Getter;

enum JDNSSLogLevels {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL}

class jdnssArgs {
    @Getter
    private boolean once = false;
    @Getter
    private int port = 53;
    @Getter
    private int threads = 10;
    @Getter
    private boolean TCP = true;
    @Getter
    private boolean UDP = true;
    @Getter
    private boolean MC = false;
    @Getter
    private int MCport = 5353;
    @Getter
    private String MCaddress = "224.0.0.251";
    @Getter
    private boolean version;
    @Getter
    private String IPaddress;
    @Getter
    private int backlog = 4;
    @Getter
    private JDNSSLogLevels logLevel = JDNSSLogLevels.OFF;

    @Getter
    private boolean help;

    @Getter
    private String DBClass;
    @Getter
    private String DBURL;
    @Getter
    private String DBUser;
    @Getter
    private String DBPass;

    @Getter
    private String serverSecretLocation;
    @Getter
    private String serverSecret;

    @Getter
    private String additional[];
}
