package edu.msudenver.cs.jdnss;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;

class Header
{
    private final Logger logger = JDNSS.logger;

    // http://www.networksorcery.com/enp/protocol/dns.htm
    @Getter private byte[] header = new byte[12];
    @Getter private final int id;
    private final int opcode;
    @Getter private final int numQuestions;
    @Getter @Setter(AccessLevel.PACKAGE) private int numAnswers;
    @Getter @Setter(AccessLevel.PACKAGE) private int numAuthorities;
    @Getter @Setter(AccessLevel.PACKAGE) private int numAdditionals;
    @Setter(AccessLevel.PACKAGE) private int rcode;
    @Setter(AccessLevel.PACKAGE) private boolean TC = false; // truncation
    @Setter(AccessLevel.PACKAGE) private boolean QR = false; // query
    @Setter(AccessLevel.PACKAGE) private boolean AA = true;  // authoritative answer
    @Setter(AccessLevel.PACKAGE) private boolean RA = false; // recursion available

    private boolean RD = false; // recursion desired
    private boolean AD = false; // authenticated data
    private boolean CD = false; // checking disabled


    void build()
    {
        Assertion.aver(opcode == 0);
        Assertion.aver(rcode == ErrorCodes.NOERROR.getCode()
                || rcode == ErrorCodes.FORMERROR.getCode()
                || rcode == ErrorCodes.SERVFAIL.getCode()
                || rcode == ErrorCodes.NAMEERROR.getCode()
                || rcode == ErrorCodes.NOTIMPL.getCode()
                || rcode == ErrorCodes.REFUSED.getCode()
                || rcode == ErrorCodes.YXRRSET.getCode());
        Assertion.aver(numAnswers >= 0 && numAnswers <= 255);
        Assertion.aver(numAuthorities >= 0 && numAuthorities <= 255);
        Assertion.aver(numAdditionals >= 0 && numAdditionals <= 255);
        Assertion.aver(numAdditionals >= 0 && numAdditionals <= 255);

        header[0] = Utils.getByte(id, 2);
        header[1] = Utils.getByte(id, 1);
        header[2] = (byte)
        (
            (QR ? 128 : 0) |
            (opcode << 3) |
            (AA ? 4 : 0) |
            (TC ? 2 : 0) |
            (RD ? 1 : 0)
        );
        header[3] = (byte)
        (
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

    Header(byte buffer[])
    {
        // only grab the header from the query
        this.header = Arrays.copyOf(buffer, 12);

        id             =Utils.addThem(buffer[0], buffer[1]);
        numQuestions   =Utils.addThem(buffer[4], buffer[5]);
        numAnswers     =Utils.addThem(buffer[6], buffer[7]);
        numAuthorities =Utils.addThem(buffer[8], buffer[9]);
        numAdditionals =Utils.addThem(buffer[10], buffer[11]);

        Assertion.aver(numAnswers == 0);
        Assertion.aver(numAuthorities == 0);

        int flags = Utils.addThem(buffer[2], buffer[3]);
        QR =      (flags & 0x00008000) != 0;
        opcode =  (flags & 0x00007800) >> 11;
        AA =      (flags & 0x00000400) != 0;
        TC =      (flags & 0x00000200) != 0;
        RD =      (flags & 0x00000100) != 0;
        RA =      (flags & 0x00000080) != 0;
        AD =      (flags & 0x00000020) != 0;
        CD =      (flags & 0x00000010) != 0;
        rcode =   flags & 0x0000000f;
    }

    @Override public String toString()
    {
        String s = "Id: 0x" + Integer.toHexString(id) + "\n";
        s += "Questions: " + numQuestions + "\t";
        s += "Answers: " + numAnswers + "\n";
        s += "Authority RR's: " + numAuthorities + "\t";
        s += "Additional RR's: " + numAdditionals + "\n";

        s += "QR: " + QR + "\t";
        s += "AA: " + AA + "\t";
        s += "TC: " + TC + "\n";
        s += "RD: " + RD + "\t";
        s += "RA: " + RA + "\t";
        s += "AD: " + AD + "\n";
        s += "CD: " + CD + "\t";
        s += "opcode: " + opcode + "\n";
        s += "rcode: " + rcode;

        return s;
    }
}
