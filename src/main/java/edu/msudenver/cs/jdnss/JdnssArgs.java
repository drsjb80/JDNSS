package edu.msudenver.cs.jdnss;

import lombok.Getter;
import lombok.Setter;

enum JDNSSLogLevels {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL}

class jdnssArgs {
    @Getter private boolean once = false;
    @Getter private int threads = 10;

    @Getter private boolean version;
    @Getter private String[] IPaddresses; // = {"TCP@0.0.0.0@53", "UDP@0.0.0.0@53"}; // "MC@224.0.0.251@5353"
    @Getter private int backlog = 4;
    @Getter private JDNSSLogLevels logLevel = JDNSSLogLevels.OFF;
    @Getter private boolean PID = false;

    @Getter private boolean help;

    @Getter private String DBClass;
    @Getter private String DBURL;
    @Getter private String DBUser;
    @Getter private String DBPass;

    @Getter @Setter private String serverSecret;

    @Getter private String additional[];
}
