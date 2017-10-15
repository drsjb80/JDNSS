package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Level;

enum JDNSSLogLevels {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL}

class jdnssArgs
{
    final boolean once = false;
    final int port = 53;
    final int threads = 10;
    final boolean TCP = true;
    final boolean UDP = true;
    final boolean MC = false;
    final int MCport = 5353;
    final String MCaddress = "224.0.0.251";
    boolean version;
    String IPaddress;
    final int backlog = 4;
    final JDNSSLogLevels logLevel = JDNSSLogLevels.OFF;

    boolean help;

    String DBClass;
    String DBURL;
    String DBUser;
    String DBPass;

    String additional[];
}
