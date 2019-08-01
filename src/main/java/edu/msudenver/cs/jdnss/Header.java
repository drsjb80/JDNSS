package edu.msudenver.cs.jdnss;

import lombok.*;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

@ToString
@EqualsAndHashCode
class Header {
    private final Logger logger = JDNSS.logger;

    private static final int MAXIMUM_VALUE_FOR_TWO_BYTES = 255;

    private static final int QR_BIT = 0x00008000;
    private static final int OPCODE_BITS = 0x00007800;
    private static final int AA_BIT = 0x00000400;
    private static final int TC_BIT = 0x00000200;
    private static final int RD_BIT = 0x00000100;
    private static final int RA_BIT = 0x00000080;
    private static final int AD_BIT = 0x00000020;
    private static final int CD_BIT = 0x00000010;
    private static final int RCODE_BITS = 0x0000000F;


    // http://www.networksorcery.com/enp/protocol/dns.htm
    @Getter private byte[] header = new byte[12];
    @Getter private final int id;
    @Getter private final int opcode;
    @Getter private final int numQuestions;
    @Getter private int numAnswers;
    @Getter @Setter(AccessLevel.PACKAGE) private int numAuthorities;
    @Getter @Setter(AccessLevel.PACKAGE) private int numAdditionals;
    @Getter @Setter(AccessLevel.PACKAGE) private int rcode;
    @Getter @Setter(AccessLevel.PACKAGE) private boolean TC; // truncation
    @Getter @Setter(AccessLevel.PACKAGE) private boolean QR; // query
    @Getter @Setter(AccessLevel.PACKAGE) private boolean AA; // authoritative answer
    @Getter @Setter(AccessLevel.PACKAGE) private boolean RA; // recursion available

    @Getter private final boolean RD; // recursion desired
    @Getter private boolean AD; // authenticated data
    @Getter private final boolean CD; // checking disabled

    void incrementNumAnswers() {
        numAnswers++;
    }

    void build() {
        checkValidity();

        header[0] = Utils.getByte(id, 2);
        header[1] = Utils.getByte(id, 1);
        header[2] = (byte) (
            (QR ? 128 : 0) |
            (opcode << 3) |
            (AA ? 4 : 0) |
            (TC ? 2 : 0) |
            (RD ? 1 : 0)
        );
        header[3] = (byte) (
            (RA ? 128 : 0) |
            (AD ? 32 : 0) |
            (CD ? 16 : 0) |
            rcode
        );

        header[4] = Utils.getByte(numQuestions, 2);
        header[5] = Utils.getByte(numQuestions, 1);
        header[6] = Utils.getByte(numAnswers, 2);
        header[7] = Utils.getByte(numAnswers, 1);
        header[8] = Utils.getByte(numAuthorities, 2);
        header[9] = Utils.getByte(numAuthorities, 1);
        header[10] = Utils.getByte(numAdditionals, 2);
        header[11] = Utils.getByte(numAdditionals, 1);
    }

    private void checkValidity() {
        assert opcode == 0;

        boolean good = false;
        for (ErrorCodes errorCode : ErrorCodes.values()) {
            if (rcode == errorCode.getCode()) {
                good = true;
                break;
            }
        }
        assert good;

        assert numAnswers >= 0 && numAnswers <= MAXIMUM_VALUE_FOR_TWO_BYTES;
        assert numAuthorities >= 0 && numAuthorities <= MAXIMUM_VALUE_FOR_TWO_BYTES;
        assert numAdditionals >= 0 && numAdditionals <= MAXIMUM_VALUE_FOR_TWO_BYTES;
        assert numAdditionals >= 0 && numAdditionals <= MAXIMUM_VALUE_FOR_TWO_BYTES;
    }

    Header(byte buffer[]) {
        final int HEADER_LENGTH = 12;
        // only grab the header from the query
        this.header = Arrays.copyOf(buffer, HEADER_LENGTH);

        id             = Utils.addThem(buffer[0], buffer[1]);
        numQuestions   = Utils.addThem(buffer[4], buffer[5]);
        numAnswers     = Utils.addThem(buffer[6], buffer[7]);
        numAuthorities = Utils.addThem(buffer[8], buffer[9]);
        numAdditionals = Utils.addThem(buffer[10], buffer[11]);

        assert numQuestions > 0;
        assert numAnswers == 0;
        assert numAuthorities == 0;

        int flags = Utils.addThem(buffer[2], buffer[3]);

        QR =      (flags & QR_BIT) != 0;
        assert ! QR;
        opcode =  (flags & OPCODE_BITS) >> 11;
        AA =      (flags & AA_BIT) != 0;
        assert ! AA;
        TC =      (flags & TC_BIT) != 0;
        assert ! TC;
        RD =      (flags & RD_BIT) != 0;
        RA =      (flags & RA_BIT) != 0;
        assert ! RA;
        AD =      (flags & AD_BIT) != 0;
        // can't assert because nslookup doesn't set this but dig does
        // so, we have to unset it
        AD = false;
        // TODO: find out why DNSSEC doesn't set AD
        CD =      (flags & CD_BIT) != 0;
        rcode =   flags & RCODE_BITS;

        checkValidity();
    }
}
