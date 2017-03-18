package edu.msudenver.cs.jdnss;

class jdnssArgs
{
    boolean once = false;
    int port = 53;
    int threads = 10;
    boolean TCP = true;
    boolean UDP = true;
    boolean MC = false;
    int MCport = 5353;
    String MCaddress = "224.0.0.251";
    boolean version;
    String IPaddress;
    int backlog;

    // i'm going to make an assumption that not everyone who uses this will
    // be intimately familiar with Java logging and therefore won't want to
    // use the usual -Djava...
    String LogHandler;	// Syslogd, CLI, UNIXDomain, Console
    String LogLevel;	// SEVERE WARNING INFO CONFIG FINE FINER FINEST
                        // default: INFO

    String SyslogdHost = "localhost";
    int SyslogdPort = 514;

    boolean help;

    boolean RFC2671;

    String DBClass;
    String DBURL;
    String DBUser;
    String DBPass;

    String additional[];
}
