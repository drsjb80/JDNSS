package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.Vector;
import java.util.Arrays;
import java.net.DatagramPacket;

import lombok.AccessLevel;
import lombok.Getter;

class Queries
{
    @Getter private String name;
    @Getter private int type;
    @Getter private int qclass;

    public Queries(String name, int type, int qclass)
    {
        this.name = name;
        this.type = type;
        this.qclass = qclass;
    }
}

public class Query
{
    private Logger logger = JDNSS.getLogger();

    @Getter private Header header;
    @Getter private byte [] buffer;

    @Getter private Queries[] queries;
    @Getter private Zone zone;
    private SOARR SOA; // remove

    private boolean QU;     // unicast response requested
    private int minimum;

    private byte[] additional = new byte[0];
    private byte[] authority = new byte[0];

    private int numQuestions;
    private int numAnswers;
    private int numAuthorities;

    private byte[] savedAdditional;
    private int savedNumAdditionals;

    public OPTRR optrr;
    private int maximumPayload = 512;
    private boolean doDNSSEC = false;

    /**
     * creates a Query from a packet
     */
    public Query(byte b[])
    {
        buffer = Arrays.copyOf(b, b.length);
        header = new Header(buffer);
        numQuestions = header.getNumQuestions();
        numAnswers = header.getNumAnswers();
        numAuthorities = header.getNumAuthorities();

        // FIXME: put a bunch of avers here
    }

    /*
    public byte[] getBuffer()
    {
        return Arrays.copyOf(buffer, buffer.length);
    }
    */

    /**
     * Evaluates and saves all questions
     */
    public void parseQueries()
    {
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

        for (int i = 0; i < numQuestions; i++)
        {
            StringAndNumber sn = Utils.parseName(location, buffer);

            location = sn.getNumber();

            queries[i] = new Queries(sn.getString(), sn.getNumber(),
                Utils.addThem(buffer[location], buffer[location + 1]));

            location += 2;

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
            location += 2;
        }

        if (header.getNumAdditionals() > 0)
        {
            // for
            int length = buffer.length - location;
            savedNumAdditionals = header.getNumAdditionals();
            savedAdditional = new byte[length];
            System.arraycopy(buffer, location, savedAdditional, 0, length);
            parseAdditional(savedAdditional, savedNumAdditionals);
            buffer = Utils.trimByteArray(buffer, location);
            header.clearNumAdditionals();
        }
    }

    public void parseAdditional(byte[] additional, int rrCount)
    {
        try
        {
            int rrLocation = 0;

            for (int i = 0; i < rrCount; i++)
            {
                byte[] bytes = new byte[additional.length - rrLocation];
                System.arraycopy(additional, rrLocation, bytes, 0,
                    additional.length - rrLocation);
                OPTRR tempRR = new OPTRR(bytes);

                if (tempRR.isValid())
                {
                    optrr = new OPTRR(bytes);
                    maximumPayload = optrr.getPayloadSize();
                    doDNSSEC = optrr.dnssecAware();
                }
                rrLocation = rrLocation + tempRR.getByteSize() + 1;
            }
        }
        catch(Exception ex)
        {
            // FIXME
        }
    }

    public String toString()
    {
        String s = header.toString();

        for (Queries q: queries)
        {
            s += "\nName: " + q.getName() +
                " Type: " + q.getType() +
                " Class: " + q.getQclass();
        }
        return s;
    }
}
