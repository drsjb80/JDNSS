package edu.msudenver.cs.jdnss;

enum RRCode {
    A(1),
    NS(2),
    CNAME(5),
    SOA(6),
    PTR(12),
    HINFO(13),
    MX(15),
    TXT(16),
    AAAA(28),
    A6(38),
    DNAME(39),
    OPT(41),
    DS(43),
    RRSIG(46),
    NSEC(47),
    DNSKEY(48),
    NSEC3(50),
    NSEC3PARAM(51), // from here down, these aren't RRCode but are needed for parsing
    INCLUDE,
    ORIGIN,
    TTL,
    EOF,
    NOTOK,
    IPV4ADDR,
    IPV6ADDR,
    LCURLY,
    RCURLY,
    LPAREN,
    RPAREN,
    STRING,
    IN,
    DN,
    INT,
    INADDR,
    STAR,
    BASE64,
    HEX,
    DATE;

    private int code;

    RRCode() {
    }

    RRCode(int code) {
        this.code = code;
    }

    int getCode() {
        assert code != 0;
        return code;
    }

    static RRCode findCode(final int number) {
        for (RRCode rrCode : values()) {
            if (number == rrCode.getCode()) {
                return rrCode;
            }
        }

        throw new IllegalArgumentException(number + " not an RRCode");
    }
}
