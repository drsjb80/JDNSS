package edu.msudenver.cs.jdnss;

import lombok.Getter;
import lombok.ToString;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

@ToString
class Queries {
    @Getter private String name;
    @Getter private RRCode type;
    @Getter private int qclass;
    @Getter private boolean QU;

    Queries(final String name, final RRCode type, final int qclass, final boolean QU) {
        this.name = name;
        this.type = type;
        this.qclass = qclass;
        this.QU = QU;
    }
}

@ToString
class Query {
    private final Logger logger = JDNSS.logger;

    @Getter private Header header;
    @Getter private byte[] buffer;
    @Getter private Queries[] queries;
    @Getter private OPTRR optrr;

    /**
     * creates a Query from a packet
     */
    Query(byte buffer[]) {
        this.buffer = buffer;
        this.header = new Header(buffer);
    }

    /**
     * Evaluates and saves all questions
     */
    void parseQueries(String clientIPaddress) {
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
            Map.Entry<String, Integer> StringAndNumber = Utils.parseName(location, buffer);

            location = StringAndNumber.getValue();
            int qtype = Utils.addThem(buffer[location++], buffer[location++]);
            int qclass = Utils.addThem(buffer[location++], buffer[location++]);
            boolean QU = (qclass & 0xc000) == 0xc000;

            queries[i] = new Queries(StringAndNumber.getKey(),
                    RRCode.findCode(qtype), qclass, QU);

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

        /* For servers with DNS Cookies enabled, the QUERY opcode behavior is
        extended to support queries with an empty Question Section (a QDCOUNT
            of zero (0)), provided that an OPT record is present with a COOKIE
        option.  Such servers will send a reply that has an empty
        Answer Section and has a COOKIE option containing the Client Cookie
        and a valid Server Cookie. */

         /*
        At a server where DNS Cookies are not implemented and enabled, the
        presence of a COOKIE option is ignored and the server responds as if
        no COOKIE option had been included in the request.
        */

        for (int i = 0; i < header.getNumAdditionals(); i++) {
            logger.traceEntry();

            // When an OPT RR is included within any DNS message, it MUST be the only OPT RR in that message.
            Assertion.aver(header.getNumAdditionals() == 1);
            this.optrr = new OPTRR(Arrays.copyOfRange(buffer, location, buffer.length));

            //process and transform? optrr for a resonse
            // need to set the RCODE in header for the response needs to be FORMERR
            if(optrr.hasFormErr()){
                header.setRcode( ErrorCodes.FORMERROR.getCode() );
            }

            if(optrr.isCookie()) {
                optrr.createServerCookie(clientIPaddress, header);
            }
        }
    }

    byte[] buildResponseQueries() {
        byte[] questions = new byte[0];
        for(Queries query: this.getQueries()) {
            questions = Utils.combine(questions, Utils.convertString(query.getName()));
            questions = Utils.combine(questions, Utils.getTwoBytes(query.getType().getCode(), 2));
            questions = Utils.combine(questions, Utils.getTwoBytes(query.getQclass(), 2));
        }
        return questions;
    }
}
