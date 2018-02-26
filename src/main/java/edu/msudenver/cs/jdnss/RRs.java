package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Vector;

class RRs {
    private int location;
    private final byte[] buffer;
    private static final Logger logger = JDNSS.logger;
    private final int numQuestions;
    private final int numAnswers;
    private final int numAuthorities;
    private final int numAdditionals;

    private final RR[] questions;
    private final RR[] answers;
    private final RR[] authorities;
    private final RR[] additionals;

    public RRs(byte buffer[], int numQuestions, int numAnswers,
               int numAuthorities, int numAdditionals) {
        this.buffer = Arrays.copyOf(buffer, buffer.length);
        this.numQuestions = numQuestions;
        this.numAnswers = numAnswers;
        this.numAuthorities = numAuthorities;
        this.numAdditionals = numAdditionals;

        questions = new RR[numQuestions];
        answers = new RR[numAnswers];
        authorities = new RR[numAuthorities];
        additionals = new RR[numAdditionals];

        parseQuestions();
    }

    private void parseQuestions() {
        logger.traceEntry();

        /*
        The question section is used to carry the "question" in most queries,
        i.e., the parameters that deinfo what is being asked.  The section
        contains QDCOUNT(usually 1) entries, each of the following format:

        1  1  1  1  1  1
        0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                                               |
        /                     QNAME                     /
        /                                               /
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                     QTYPE                     |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                     QCLASS                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        */

        for (int i = 0; i < numQuestions; i++) {
            Vector<Object> StringAndNumber = null;

            try {
                StringAndNumber = Utils.parseName(location, buffer);
            } catch (AssertionError ae) {
                questions[i] = null;
                Assertion.fail();
            }

            location = (int) StringAndNumber.elementAt(1);
            int qtype = Utils.addThem(buffer[location], buffer[location + 1]);
            location += 2;
            // FIXME: QU/QM
            // logger.fatal(buffer[location] & 0x80);
            int qclass = Utils.addThem(buffer[location], buffer[location + 1]);
            location += 2;

            questions[i] = new QRR((String) StringAndNumber.elementAt(0),
                    RRCode.findCode(qtype));
        }
    }

    public void parseAnswers(int location) {
        logger.traceEntry();

        /*
        The answer, authority, and additional sections all share the same
        format: a variable number of resource records, where the number of
        records is specified in the corresponding count field in the header.
        Each resource record has the following format:
        1  1  1  1  1  1
        0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                                               |
        /                                               /
        /                      NAME                     /
        |                                               |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                      TYPE                     |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                     CLASS                     |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                      TTL                      |
        |                                               |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        |                   RDLENGTH                    |
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
        /                     RDATA                     /
        /                                               /
        +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
        */

        /*
        for (int i = 0; i < numAnswers; i++)
        {
            name[i] = "";
            location = parseName(location, i);
            type[i] = Utils.addThem(buffer[location], buffer[location + 1]);
            location += 2;
            qclass[i] = Utils.addThem(buffer[location], buffer[location + 1]);
            location += 2;
        }
        */
    }

    private String display(String title, RR rrs[]) {
        String s = title + "\n";

        for (int i = 0; i < rrs.length; i++) {
            // put a newline on all except the last
            s += rrs[i] + (i < rrs.length - 1 ? "\n" : "");
        }

        return s;
    }

    public String toString() {
        String s = "";

        if (numQuestions > 0) {
            s += display("Questions:", questions);
        }
        if (numAnswers > 0) {
            s += display("Answers:", answers);
        }
        if (numAuthorities > 0) {
            s += display("Authorities:", authorities);
        }
        if (numAdditionals > 0) {
            s += display("Additional:", additionals);
        }

        return s;
    }
}
