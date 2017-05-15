package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ObjectMessage;
import edu.msudenver.cs.jclo.JCLO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;

import lombok.AccessLevel;
import lombok.Getter;

public class JDNSS
{
    // a few AOP singletons
    @Getter private static jdnssArgs jargs;
    @Getter private static Logger logger = LogManager.getLogger("JDNSS");
    @Getter private static DBConnection DBConnection;

    private Hashtable bindZones = new Hashtable();

    /**
     * Finds the Zone associated with the domain name passed in
     *
     * @param name	the name of the domain to find
     * @return		the associated Zone
     * @see Zone
     */
    public Zone getZone(String name)
    {
        logger.traceEntry(new ObjectMessage(name));

        String longest = null;

        // first, see if it's in the files
        try
        {
            longest = Utils.findLongest(bindZones.keys(), name);
        }
        catch (AssertionError AE)
        {
            // see if we have a DB connection and try there
            if (DBConnection != null)
            {
                DBZone d = null;
                try
                {
                    d = DBConnection.getZone(name);
                    return (Zone) d;
                }
                catch (AssertionError AE2)
                {
                    Assertion.aver(false);
                }
            }

            // it's not
            Assertion.aver(false);
        }
            
        return (Zone) bindZones.get(longest);
    }

    public void start()
    {
        /*
        ** Yeah, I know this is a little messy, but it does keep it DRY. I
        ** need to start each thread independently of whether any of the
        ** others are started.
        */
        for (int i = 0; i < 3; i++)
        {
            try
            {
                switch (i)
                {
                    case 0: if (jargs.UDP) new UDP(this).start(); break;
                    case 1: if (jargs.TCP) new TCP(this).start(); break;
                    case 2: if (jargs.MC) new MC(this).start(); break;
                    default: Assertion.aver(false);
                }
            }
            catch (SocketException se)
            {
                logger.catching (se);
            }
            catch (UnknownHostException uhe)
            {
                logger.catching (uhe);
            }
            catch (IOException ie)
            {
                logger.catching (ie);
            }
        }
    }

    private void doargs()
    {
        logger.traceEntry();
        logger.trace(jargs.toString());

        if (jargs.version)
        {
            logger.fatal(new Version().getVersion());
            System.exit(0);
        }

        logger.info("Starting JDNSS version " + new Version().getVersion());

        if (jargs.DBClass != null && jargs.DBURL != null)
        {
            DBConnection = new DBConnection(jargs.DBClass, jargs.DBURL,
                jargs.DBUser, jargs.DBPass);
        }

        String additional[] = jargs.additional;

        if (additional == null)
        {
            return;
        }

        for (int i = 0; i < additional.length; i++)
        {
            try
            {
                String name = new File(additional[i]).getName();

                logger.info("Parsing: " + additional[i]);

                if (name.endsWith(".db"))
                {
                    name = name.replaceFirst("\\.db$", "");
                    if (Character.isDigit(name.charAt(0)))
                    {
                        name = Utils.reverseIP(name);
                        name = name + ".in-addr.arpa";
                    }
                }

                BindZone zone = new BindZone(name);
                new Parser(new FileInputStream(additional[i]), zone).RRs();
                logger.trace(zone);

                // the name of the zone can change while parsing, so use
                // the name from the zone
                bindZones.put(zone.getName(), zone);
            }
            catch (FileNotFoundException e)
            {
                logger.warn("Couldn't open file " + additional[i] +
                '\n' + e);
            }
        }
    }

    /**
     * The main driver for the server; creates threads for TCP and UDP.
     */
    public static void main(String[] args)
    {
        JDNSS dnsService = new JDNSS();

        jargs = new jdnssArgs();
        JCLO jclo = new JCLO(dnsService.jargs);
        jclo.parse(args);

        if (jargs.help)
        {
            System.out.println(jclo.usage());
            System.exit(0);
        }

        dnsService.doargs();

        if (dnsService.bindZones.size() == 0 && dnsService.DBConnection == null)
        {
            dnsService.logger.fatal("No zone files, traceExit.");
            System.exit(1);
        }

        dnsService.start();
    }
}
