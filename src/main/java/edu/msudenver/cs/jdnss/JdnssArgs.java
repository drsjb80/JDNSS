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
    int backlog = 4;

    boolean help;

    String DBClass;
    String DBURL;
    String DBUser;
    String DBPass;

    String additional[];
}
