package edu.msudenver.cs.jdnss;

import edu.msudenver.cs.jclo.JCLO;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.message.ObjectMessage;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import java.util.concurrent.ThreadLocalRandom;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;

class JDNSS {
    // a few AOP singletons
    static final jdnssArgs jargs = new jdnssArgs();
    static final Logger logger = LogManager.getLogger("JDNSS");
    private static DBConnection DBConnection;

    private static final Map<String, Zone> bindZones = new HashMap<>();

    /**
     * Finds the Zone associated with the domain name passed in
     *
     * @param name the name of the domain to find
     * @return the associated Zone
     * @see Zone
     */
     static Zone getZone(String name) {
        logger.traceEntry(new ObjectMessage(name));

        String longest = null;

        // first, see if it's in the files
        try {
            longest = Utils.findLongest(bindZones.keySet(), name);
        } catch (AssertionError AE) {
            // see if we have a DB connection and try there
            if (DBConnection != null) {
                DBZone d;
                try {
                    d = DBConnection.getZone(name);
                    return d;
                } catch (AssertionError AE2) {
                    Assertion.fail();
                }
            }

            // it's not
            Assertion.fail();
        }

        return bindZones.get(longest);
    }

    private static void start() {
        for (String IPAddress : jargs.IPaddresses) {
            String[] parts = IPAddress.split("@");

            switch(parts[0]) {
                case "TCP": new TCP(parts).start(); break;
                case "UDP": new UDP(parts).start(); break;
                case "MC": new MC(parts).start(); break;
                case "TLS": new TCP(parts).start(); break;
                default:
                    Assertion.fail("Invalid IP address specification");
                    System.exit(-1);
                    break;
            }
        }
    }

    private static void setLogLevel() {
        Level level = Level.OFF;

        switch (jargs.logLevel) {
            case OFF: level = Level.OFF; break;
            case FATAL: level = Level.FATAL; break;
            case ERROR: level = Level.ERROR; break;
            case WARN: level = Level.WARN; break;
            case INFO: level = Level.INFO; break;
            case DEBUG: level = Level.DEBUG; break;
            case TRACE: level = Level.TRACE; break;
            case ALL: level = Level.ALL; break;
            default:
                Assertion.fail("Invalid log level");
                System.exit(-1);
                break;
        }

        Configurator.setLevel("JDNSS", level);
    }

    private static void doargs() {
        logger.traceEntry();
        logger.trace(jargs.toString());

        if (jargs.isVersion()) {
            System.out.println(new Version().getVersion());
            System.exit(0);
        }

        logger.info("Starting JDNSS version " + new Version().getVersion());

        if (jargs.getDBClass() != null && jargs.getDBURL() != null) {
            DBConnection = new DBConnection(jargs.getDBClass(), jargs.getDBURL(),
                    jargs.getDBUser(), jargs.getDBPass());
        }


        if (jargs.serverSecret == null){
            jargs.serverSecret = String.valueOf(ThreadLocalRandom.current().nextLong());
        }

        if (jargs.serverSecret != null && jargs.serverSecret.length() < 16) {
            logger.warn("Secret too short, generating random secret instead.");
            jargs.serverSecret = String.valueOf(ThreadLocalRandom.current().nextLong());
        }

        String additional[] = jargs.getAdditional();
        if (additional == null) {
            return;
        }

        for (String anAdditional: additional) {
            try {
                String name = new File(anAdditional).getName();

                logger.info("Parsing: " + anAdditional);

                if (name.endsWith(".db")) {
                    name = name.replaceFirst("\\.db$", "");
                    if (Character.isDigit(name.charAt(0))) {
                        name = Utils.reverseIP(name);
                        name = name + ".in-addr.arpa";
                    }
                }

                BindZone zone = new BindZone(name);
                new Parser(new FileInputStream(anAdditional), zone).RRs();
                logger.trace(zone);

                // the name of the zone can change while parsing, so use
                // the name from the zone
                bindZones.put(zone.getName(), zone);
            } catch (FileNotFoundException e) {
                logger.warn("Couldn't open file " + anAdditional + '\n' + e);
            }
        }
    }

    private void readPrefs() {
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(jargs.prefsFile));
            Preferences.importPreferences(is);
        } catch (IOException | InvalidPreferencesFormatException e) {
            e.printStackTrace();
            return;
        }

        Preferences prefs = Preferences.userRoot().node("edu/msudenver/cs/jdnss");
        try {
            for (String s: prefs.keys()) {
                switch(s) {
                    case "IPaddresses":
                        String addresses = prefs.get("IPaddresses", "");
                        jargs.IPaddresses = addresses.split("\\s*,\\s*");
                        break;
                    case "serverSecret":
                        jargs.serverSecret = prefs.get("serverSecret", "");
                        break;
                    case "keystoreFile":
                        jargs.serverSecret = prefs.get("keystoreFile", "");
                        break;
                    case "keystorePassword":
                        jargs.serverSecret = prefs.get("keystorePassword", "");
                        break;
                    case "debugSSL":
                        jargs.debugSSL = prefs.getBoolean("debugSSL", false);
                        break;
                    case "backlog":
                        jargs.backlog = prefs.getInt("backlog", 4);
                        break;
                    case "logLevel":
                        String level = prefs.get("logLevel", "ERROR");
                        switch (level.toUpperCase()) {
                            case "FATAL": jargs.logLevel = JDNSSLogLevels.FATAL; break;
                            case "ERROR": jargs.logLevel = JDNSSLogLevels.ERROR; break;
                            case "WARN": jargs.logLevel = JDNSSLogLevels.WARN; break;
                            case "INFO": jargs.logLevel = JDNSSLogLevels.INFO; break;
                            case "DEBUG": jargs.logLevel = JDNSSLogLevels.DEBUG; break;
                            case "TRACE": jargs.logLevel = JDNSSLogLevels.TRACE; break;
                            case "ALL": jargs.logLevel = JDNSSLogLevels.ALL; break;
                            case "OFF": jargs.logLevel = JDNSSLogLevels.OFF; break;
                        }
                        break;
                }
            }
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }


    /**
     * The main driver for the server; creates threads for TCP and UDP.
     */
    public static void main(String[] args) {
        // i'm not sure of a better way to do this. i want command-line options to overwrite
        // the defaults.
        for (String arg: args) {
            if (arg.startsWith("-IPAddresses") || arg.startsWith("--IPAddresses")) {
                jargs.IPaddresses = null;
            }
        }

        JCLO jclo = new JCLO(jargs);
        jclo.parse(args);

        if (jargs.isHelp()) {
            System.out.println(jclo.usage());
            System.exit(0);
        }

        setLogLevel();
        doargs();

        if (bindZones.size() == 0 && DBConnection == null) {
            logger.fatal("No zone files, traceExit.");
            System.exit(1);
        }

        start();
    }
}
