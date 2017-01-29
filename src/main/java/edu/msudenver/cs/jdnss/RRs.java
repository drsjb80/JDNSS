package edu.msudenver.cs.jdnss;

import edu.msudenver.cs.javaln.JavaLN;

public class RRs
{
    private int location;
    private byte buffer[];
    private JavaLN logger = JDNSS.logger;
    private int numQuestions;
    private int numAnswers;
    private int numAuthorities;
    private int numAdditionals;

    private RR questions[];
    private RR answers[];
    private RR authorities[];
    private RR additionals[];

    public RRs (byte buffer[], int numQuestions, int numAnswers,
        int numAuthorities, int numAdditionals)
    {
        this.buffer = buffer;
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

    private void parseQuestions ()
    {
	logger.entering();

	/*
	The question section is used to carry the "question" in most queries,
	i.e., the parameters that define what is being asked.  The section
	contains QDCOUNT (usually 1) entries, each of the following format:

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

        for (int i = 0; i < numQuestions; i++)
        {
            SandN sn = Utils.parseName (location, buffer);
	    if (sn != null)
	    {
		location = sn.getNumber();
		int type =
		    Utils.addThem (buffer[location], buffer[location + 1]);
		location += 2;
		int junk =
		    Utils.addThem (buffer[location], buffer[location + 1]);
		location += 2;

		questions[i] = new QRR (sn.getString(), type);
	    }
	    else
	    {
		questions[i] = null;
	    }
        }
    }

    public void parseAnswers (int location)
    {
	logger.entering();

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
            location = parseName (location, i);
            type[i] = Utils.addThem (buffer[location], buffer[location + 1]);
            location += 2;
            qclass[i] = Utils.addThem (buffer[location], buffer[location + 1]);
            location += 2;
        }
	*/
    }

    private String refactor (String title, RR rrs[])
    {
	String s = title + "\n";

	for (int i = 0; i < rrs.length; i++)
	{
	    s += rrs[i] + (i < rrs.length-1 ? "\n" : "");
	}

	return (s);
    }

    public String toString()
    {
	String s = "";

        if (numQuestions > 0)
	    s += refactor ("Questions:", questions);
        if (numAnswers > 0)
	    s += refactor ("Answers:", answers);
        if (numAuthorities > 0)
	    s += refactor ("Authorities:", authorities);
        if (numAdditionals > 0)
	    s += refactor ("Additional:", additionals);

	return (s);
    }

    public static void main (String args[])
    {
        byte one[] =
	{
	    0x03, 'w', 'w', 'w',
	    0x04, 't', 'e', 's', 't',
	    0x03, 'c', 'o', 'm',
	    0x00,
	    0x00, 0x01, 0x00, 0x00
	};

	RRs rrs = new RRs (one, 1, 0, 0, 0);
	System.out.println (rrs);

        byte two[] =
	{
	    0x03, 'w', 'w', 'w',
	    0x04, 't', 'e', 's', 't',
	    0x03, 'c', 'o', 'm',
	    0x00,
	    0x00, 0x01, 0x00, 0x00,
	    0x04, 'm', 'a', 'i', 'l',
	    (byte) 0xc0, 0x04, 0x00,
	    0x00, 0x01, 0x00, 0x00
	};
	rrs = new RRs (two, 2, 0, 0, 0);
	System.out.println (rrs);

        byte three[] = { (byte) 0xc0, 0x00, 0x01, 0x00, 0x00, 0x00 };
	rrs = new RRs (three, 1, 0, 0, 0);

        byte four[] =
	{
	    0x03, 'w', 'w', 'w',
	    0x04, 't', 'e', 's', 't',
	    0x03, 'c', 'o', 'm',
	    (byte) 0xc0, 0x00, 0x0,
	    0x00, 0x01, 0x00, 0x000
	};
	rrs = new RRs (four, 1, 0, 0, 0);
    }
}
