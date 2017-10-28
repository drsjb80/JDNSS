package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import lombok.Getter;

class Queries {
    @Getter private String name;
    @Getter private int type;
    @Getter private int qclass;
    @Getter private boolean QU;

    public Queries(String name, int type, int qclass, boolean QU) {
        this.name = name;
        this.type = type;
        this.qclass = qclass;
        this.QU = QU;
    }
}

class Query {
    private final Logger logger = JDNSS.getLogger();

    @Getter private Header header;
    @Getter private byte[] buffer;
    @Getter private Queries[] queries;
    @Getter private byte[] rawQueries;
    @Getter private OPTRR optrr;

    private int maximumPayload = 512;

    /**
     * creates a Query from a packet
     */
    public Query(byte buffer[]) {
        this.buffer = buffer;
        this.header = new Header(buffer);
        this.rawQueries = Arrays.copyOfRange(buffer, 12, buffer.length);
    }

    /**
     * Evaluates and saves all questions
     */
    public void parseQueries() {
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

        int location = 12;
        queries = new Queries[header.getNumQuestions()];

        for (int i = 0; i < header.getNumQuestions(); i++) {
            StringAndNumber sn = Utils.parseName(location, buffer);

            location = sn.getNumber();
            int qtype = Utils.addThem(buffer[location++], buffer[location++]);
            int qclass = Utils.addThem(buffer[location++], buffer[location++]);
            boolean QU = (qclass & 0xc000) == 0xc000;

            queries[i] = new Queries(sn.getString(), qtype, qclass, QU);

            /*
            ** Multicast DNS defines the top bit in the class field of a
            ** DNS question as the unicast-response bit.  When this bit is
            ** set in a question, it indicates that the querier is willing
            ** to accept unicast replies in response to this specific
            ** query, as well as the usual multicast responses.  These
            ** questions requesting unicast responses are referred to as
            ** "QU" questions, to distinguish them from the more usual
            ** questions requesting multicast responses ("QM" questions).
            */
        }

        for (int i = 0; i < header.getNumAdditionals(); i++) {
            // for now, it has to be 1
            Assertion.aver(header.getNumAdditionals() == 1);

            optrr = new OPTRR(Arrays.copyOfRange(buffer, location, buffer.length));
        }
    }

    public String toString() {
        String s = header.toString();

        for (Queries q : queries) {
            s += "\nName: " + q.getName() +
                    " Type: " + q.getType() +
                    " Class: " + q.getQclass();
        }
        return s;
    }
}
