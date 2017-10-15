package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;

import java.util.Arrays;

import lombok.Getter;

class Queries {
    @Getter private String name;
    @Getter private int type;
    @Getter private int qclass;

    public Queries(String name, int type, int qclass) {
        this.name = name;
        this.type = type;
        this.qclass = qclass;
    }
}

class Query {
    private final Logger logger = JDNSS.getLogger();

    @Getter private Header header;
    @Getter private byte[] buffer;
    @Getter private Queries[] queries;
    @Getter private byte[] rawQueries;
    @Getter private Zone zone;

    private boolean QU;     // unicast response requested

    // private byte[] additional = new byte[0];
    // private byte[] authority = new byte[0];

    private final int numQuestions;

    // private boolean DNSSEC = false;
    // private int numAnswers;
    // private int numAuthorities;
    // private byte[] savedAdditional;
    // private int savedNumAdditionals;
    // public OPTRR optrr;

    private int maximumPayload = 512;

    /**
     * creates a Query from a packet
     */
    public Query(byte buffer[]) {
        this.buffer = buffer;
        logger.trace(buffer);
        this.header = new Header(buffer);
        this.numQuestions = header.getNumQuestions();
        this.rawQueries = Arrays.copyOfRange(buffer, 12, buffer.length);
        // numAnswers = header.getNumAnswers();
        // numAuthorities = header.getNumAuthorities();

        // FIXME: put a bunch of avers here
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
        queries = new Queries[numQuestions];

        for (int i = 0; i < numQuestions; i++) {
            StringAndNumber sn = Utils.parseName(location, buffer);

            location = sn.getNumber();

            queries[i] = new Queries(sn.getString(),
                    Utils.addThem(buffer[location++], buffer[location++]),
                    Utils.addThem(buffer[location++], buffer[location++]));

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
            QU = (queries[i].getQclass() & 0xc000) == 0xc000;
        }

        /*
        if (header.getNumAdditionals() > 0)
        {
            // for
            int length = buffer.length - location;
            int savedNumAdditionals = header.getNumAdditionals();
            byte[] savedAdditional = new byte[length];
            System.arraycopy(buffer, location, savedAdditional, 0, length);
            parseAdditional(savedAdditional, savedNumAdditionals);
            buffer = Utils.trimByteArray(buffer, location);
            header.setNumAdditionals(0);
        }
        */
    }

    /*
    public void parseAdditional(byte[] additional, int rrCount)
    {
        try
        {
            int rrLocation = 0;

            for (int i = 0; i < rrCount; i++)
            {
                byte[] bytes = new byte[additional.length - rrLocation];
                OPTRR optrr = new OPTRR(bytes);

                if (optrr.isValid())
                {
                    boolean DNSSEC = optrr.isDNSSEC();
                    int maximumPayload = optrr.getPayloadSize();
                }

                rrLocation = rrLocation + optrr.getByteSize() + 1;
            }
        }
        catch(Exception ex)
        {
            // FIXME
        }
    }
    */

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
