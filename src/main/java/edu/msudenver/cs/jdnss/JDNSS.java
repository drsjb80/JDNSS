package edu.msudenver.cs.jdnss;

import java.net.*;
import java.io.*;
import java.util.Hashtable;
import java.util.Enumeration;

import edu.msudenver.cs.javaln.*;
import edu.msudenver.cs.javaln.syslog.*;
import edu.msudenver.cs.jclo.JCLO;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.ConsoleHandler;

class JDNSSArgs
{
    boolean once = false;
    int port = 53;
    int threads = 10;
    boolean TCP = true;
    boolean UDP = true;
    boolean MC = false;
    int MCport = 5353;
    String MCaddress = "224.0.0.251";
    boolean version;
    String IPaddress;
    int backlog;

    // i'm going to make an assumption that not everyone who uses this will
    // be intimately familiar with Java logging and therefore won't want to
    // use the usual -Djava...
    String LogHandler;	// Syslogd, CLI, UNIXDomain, Console
    String LogLevel;	// SEVERE WARNING INFO CONFIG FINE FINER FINEST
                        // default: INFO

    String SyslogdHost = "localhost";
    int SyslogdPort = 514;

    boolean help;

    boolean RFC2671;

    String DBClass;
    String DBURL;
    String DBUser;
    String DBPass;

    String additional[];
}

public class JDNSS
{
    static JDNSSArgs jargs;
    private Hashtable bindZones = new Hashtable();
    static JavaLN logger = new JavaLN();
    DBConnection DBConnection;
    // DBZone DBZone;

    /**
     * Finds the Zone associated with the domain name passed in
     *
     * @param name	the name of the domain to find
     * @return		the associated Zone
     * @see Zone
     */
    public Zone getZone (String name)
    {
        logger.entering (name);

        String longest = null;

        // first, see if it's in the files
        try
        {
            longest = Utils.findLongest (bindZones.keys(), name);
        }
        catch (AssertionError AE)
        {
            // see if we have a DB connection and try there
            if (DBConnection != null)
            {
                DBZone d = null;
                try
                {
                    d = DBConnection.getZone (name);
                    return ((Zone) d);
                }
                catch (AssertionError AE2)
                {
                    Assertion.Assert (false);
                    // return (null);
                }
            }

            // it's not
            Assertion.Assert (false);
            // return (null);
        }
            
        return ((Zone) bindZones.get (longest));
    }

    public void start()
    {
        boolean udp = jargs.UDP;
        boolean tcp = jargs.TCP;
        boolean mc = jargs.MC;

        logger.finest (new Boolean (udp));
        logger.finest (new Boolean (tcp));
        logger.finest (new Boolean (mc));

        for (int i = 0; i < 3; i++)
        {
            Protos proto = null;

            try
            {
                if (i == 0 && udp)
                {
                    proto = new UDP (this);
                }
                if (i == 1 && tcp)
                {
                    proto = new TCP (this);
                }
                if (i == 2 && mc)
                {
                    proto = new MC (this);
                }
            }
            catch (SocketException se)
            {
                // logger.throwing (se);
                logger.severe (se);
                System.exit (1);
            }
            catch (IOException ioe)
            {
                // logger.throwing (ioe);
                logger.severe (ioe);
                System.exit (1);
            }

            if (proto != null)
            {
                proto.start();
            }
        }
    }

    private void setupLogging()
    {
        /*
        String lc = System.getProperty ("java.util.logging.config.class");
        String lf = System.getProperty ("java.util.logging.config.file");

        logger.finest (lc);
        logger.finest (lf);
        */

        logger.setThrowingLevel (Level.SEVERE);
        String SyslogdHost = jargs.SyslogdHost;
        int SyslogdPort = jargs.SyslogdPort;
        Handler newHandler = null;

        String handler = jargs.LogHandler;
        if (handler != null)
        {
            if (handler.equals ("Syslogd"))
            {
                newHandler = new SyslogdHandler (SyslogdHost, SyslogdPort);
            }
            else if (handler.equals ("CLI"))
            {
                newHandler = new CLIHandler();
            }
            else if (handler.equals ("UNIXDomain"))
            {
                newHandler = new UNIXDomainHandler();
            }
            else if (handler.equals ("Console"))
            {
                newHandler = new ConsoleHandler();
                newHandler.setFormatter (new LineNumberFormatter());
            }
            else
            {
                logger.info ("Invalid --LogHandler specified, using syslogd");
                newHandler = new SyslogdHandler (SyslogdHost, SyslogdPort);
            }
        }
        else
        {
            newHandler = new SyslogdHandler (SyslogdHost, SyslogdPort);
        }

        if (newHandler != null)
        {
            logger.addHandler (newHandler);
            logger.setUseParentHandlers (false);
        }

        String level = jargs.LogLevel;
        if (level != null)
        {
            Level l = Level.parse (level);
            logger.setLevel (l);

            Handler handlers[] = logger.getHandlers();

            for (int i = 0; i < handlers.length; i++)
            {
                handlers[i].setLevel (l);
            }
        }
    }

    private void doargs ()
    {
        setupLogging();

        logger.entering();
        logger.finest (jargs.toString());

        if (jargs.version)
        {
            logger.severe (new Version().getVersion());
            System.exit (0);
        }

        logger.info ("Starting JDNSS version " + new Version().getVersion());

        if (jargs.DBClass != null && jargs.DBURL != null)
        {
            DBConnection = new DBConnection (jargs.DBClass, jargs.DBURL,
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
                String name = new File (additional[i]).getName();

                logger.info ("Parsing: " + additional[i]);

                if (name.endsWith (".db"))
                {
                    name = name.replaceFirst ("\\.db$", "");
                    if (Character.isDigit (name.charAt (0)))
                    {
                        name = Utils.reverseIP (name);
                        name = name + ".in-addr.arpa";
                    }
                }

                BindZone zone = new BindZone (name);
                new Parser (new FileInputStream (additional[i]), zone).RRs();
                logger.finest (zone);

                // the name of the zone can change while parsing, so use
                // the name from the zone
                bindZones.put (zone.getName(), zone);
            }
            catch (FileNotFoundException e)
            {
                logger.warning ("Couldn't open file " + additional[i] +
                '\n' + e);
            }
        }
    }

    /**
     * The main driver for the server; creates threads for TCP and UDP.
     */
    public static void main(String[] args) // throws IOException
    {
        JDNSS dnsService = new JDNSS();

        jargs = new JDNSSArgs();
        JCLO jclo = new JCLO (dnsService.jargs);
        jclo.parse (args);

        if (jargs.help)
        {
            System.out.println (jclo.usage());
            System.exit (0);
        }

        dnsService.doargs ();

        if (dnsService.bindZones.size() == 0 && dnsService.DBConnection == null)
        {
            dnsService.logger.severe ("No zone files, exiting.");
            System.exit (1);
        }

        dnsService.start();
    }
}
