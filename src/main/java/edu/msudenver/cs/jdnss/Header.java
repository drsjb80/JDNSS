package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Vector;
import java.util.Arrays;
import java.net.DatagramPacket;

import lombok.AccessLevel;
import lombok.Getter;

public class Header
{
    private Logger logger = JDNSS.getLogger();

    // http://www.networksorcery.com/enp/protocol/dns.htm
    @Getter private int id;
    private int opcode;

    @Getter private int numQuestions;

    @Getter private int numAnswers;
    void incrementNumAnswers()
    {
        numAnswers++;
        Assertion.aver(numAnswers >= 0 && numAnswers <= 255);
        rebuild();
    }

    @Getter private int numAuthorities;
    void incrementNumAuthorities()
    {
        numAuthorities++;
        Assertion.aver(numAuthorities >= 0 && numAuthorities <= 255);
        rebuild();
    }

    public void setNumAuthorities(int i)
    {
        numAuthorities = i;
        Assertion.aver(numAuthorities >= 0 && numAuthorities <= 255);
        rebuild();
    };

    public void clearNumAuthorities()
    {
        numAuthorities = 0;
        rebuild();
    };

    @Getter private int numAdditionals;
    public void incrementNumAdditionals()
    {
        numAdditionals++;
        Assertion.aver(numAdditionals >= 0 && numAdditionals <= 255);
        rebuild();
    }

    public void setNumAdditionals(int i)
    {
        numAdditionals = i;
        Assertion.aver(numAdditionals >= 0 && numAdditionals <= 255);
        rebuild();
    };

    public void clearNumAdditionals()
    {
        numAdditionals = 0;
        rebuild();
    };

    private int rcode;
    public void setRcode(int rcode)
    {
        Assertion.aver(rcode >= 0 && rcode <= 5);
        this.rcode = rcode;
        rebuild();
    }

    private boolean TC = false; // truncation
    public void setTC() { TC = true; rebuild(); }

    @Getter private boolean QR = false; // query
    public void setResponse() { QR = true; rebuild(); }

    private boolean AA = true;  // authoritative answer
    public void setAuthoritative() { AA = true; rebuild(); }
    public void setNotAuthoritative() { AA = false; rebuild(); }

    private boolean RD = false; // recursion desired

    private boolean RA = false; // recursion available
    public void setNoRecurse() { RA = false; rebuild(); }

    private boolean AD = false; // authenticated data
    private boolean CD = false; // checking disabled

    private byte[] buffer;

    public void rebuild()
    {
        Assertion.aver(opcode == 0);
        Assertion.aver(rcode == Utils.NOERROR || rcode == Utils.FORMERROR ||
            rcode == Utils.SERVFAIL || rcode == Utils.NAMEERROR ||
            rcode == Utils.NOTIMPL || rcode == Utils.REFUSED);

        buffer[0] = Utils.getByte(id, 2);
        buffer[1] = Utils.getByte(id, 1);
        buffer[2] = (byte)
        (
            (QR ? 128 : 0) |
            (opcode << 3) |
            (AA ? 4 : 0) |
            (TC ? 2 : 0) |
            (RD ? 1 : 0)
        );
        buffer[3] = (byte)
        (
            (RA ? 128 : 0) |
            (AD ? 32 : 0) |
            (CD ? 16 : 0) |
            rcode
        );

        Assertion.aver(numQuestions >= 0 && numQuestions <= 255);
        buffer[4] = Utils.getByte(numQuestions, 2);
        buffer[5] = Utils.getByte(numQuestions, 1);

        Assertion.aver(numAnswers >= 0 && numAnswers <= 255);
        buffer[6] = Utils.getByte(numAnswers, 2);
        buffer[7] = Utils.getByte(numAnswers, 1);

        Assertion.aver(numAuthorities >= 0 && numAuthorities <= 255);
        buffer[8] = Utils.getByte(numAuthorities, 2);
        buffer[9] = Utils.getByte(numAuthorities, 1);

        Assertion.aver(numAdditionals >= 0 && numAdditionals <= 255);
        buffer[10] = Utils.getByte(numAdditionals, 2);
        buffer[11] = Utils.getByte(numAdditionals, 1);
    }

    public Header(byte buffer[])
    {
        this.buffer =   buffer;
        id =            Utils.addThem(buffer[0], buffer[1]);
        numQuestions =  Utils.addThem(buffer[4], buffer[5]);
        numAnswers =    Utils.addThem(buffer[6], buffer[7]);
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

    public String toString()
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
