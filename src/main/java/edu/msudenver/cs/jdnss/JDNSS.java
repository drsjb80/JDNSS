package edu.msudenver.cs.jdnss;

/*
import org.apache.logging.log4j.Logger;
import edu.msudenver.cs.javaln.LineNumberFormatter;
import edu.msudenver.cs.javaln.syslog.CLIHandler;
import edu.msudenver.cs.javaln.syslog.SyslogdHandler;
import edu.msudenver.cs.javaln.syslog.UNIXDomainHandler;
*/

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.message.ObjectMessage;
import edu.msudenver.cs.jclo.JCLO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.util.Hashtable;
/*
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
*/

public class JDNSS
{
    // a few AOP singletons
    private static jdnssArgs jargs;
    public static jdnssArgs getJdnssArgs()
    {
        return jargs;
    }

    private static Logger logger = LogManager.getLogger();
    public static Logger getLogger()
    {
        return logger;
    }

    private static DBConnection DBConnection;
    public static DBConnection getDBConnection()
    {
        return DBConnection;
    }

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
        boolean udp = jargs.UDP;
        boolean tcp = jargs.TCP;
        boolean mc = jargs.MC;

        logger.trace(Boolean.valueOf(udp));
        logger.trace(Boolean.valueOf(tcp));
        logger.debug(Boolean.valueOf(mc));

        for (int i = 0; i < 3; i++)
        {
            Protos proto = null;

            try
            {
                if (i == 0 && udp)
                {
                    proto = new UDP(this);
                }
                if (i == 1 && tcp)
                {
                    proto = new TCP(this);
                }
                if (i == 2 && mc)
                {
                    proto = new MC(this);
                }
            }
            catch (SocketException se)
            {
                logger.fatal(se);
                System.exit(1);
            }
            catch (IOException ioe)
            {
                logger.fatal(ioe);
                System.exit(1);
            }

            if (proto != null)
            {
                proto.start();
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
