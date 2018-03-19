package edu.msudenver.cs.jdnss;

import edu.msudenver.cs.jclo.JCLO;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.message.ObjectMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class JDNSS {
    // a few AOP singletons
    static final jdnssArgs jargs = new jdnssArgs();
    static final Logger logger = LogManager.getLogger("JDNSS");
    static DBConnection DBConnection;

    private static final Map<String, Zone> bindZones = new Hashtable();

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
        try {
            if (jargs.isUDP()) new UDP().start();
            if (jargs.isTCP()) new TCP().start();
            if (jargs.isMC()) new MC().start();
        } catch (SocketException | UnknownHostException se) {
            logger.catching(se);
        } catch (IOException ie) {
            logger.catching(ie);
        }
    }

    private static void setLogLevel() {
        Level level = Level.OFF;

        switch (jargs.getLogLevel()) {
            case OFF: level = Level.OFF; break;
            case FATAL: level = Level.FATAL; break;
            case ERROR: level = Level.ERROR; break;
            case WARN: level = Level.WARN; break;
            case INFO: level = Level.INFO; break;
            case DEBUG: level = Level.DEBUG; break;
            case TRACE: level = Level.TRACE; break;
            case ALL: level = Level.ALL; break;
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

        if (jargs.serverSecretLocation == null) {
            logger.warn("serverSecretLocation not specified " +
                "location set: /etc/jnamed.conf");
            jargs.serverSecretLocation = "/etc/jnamed.conf";
        }

        if (jargs.serverSecret == null){
            jargs.serverSecret = readServerSecret();
        }
        else if (jargs.serverSecret != null){
            setServerSecret();
        }

        String additional[] = jargs.getAdditional();
        if (additional == null) {
            return;
        }

        for (String anAdditional : additional) {
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

    private static void setServerSecret(){
        try {
            File file = new File(jargs.serverSecretLocation);

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String confFile = new String(data, "UTF-8");
            fis.close();

            Pattern p = Pattern.compile("cookie-secret\\s+\"(.*)\"");
            Matcher m = p.matcher(confFile);
            confFile = m.replaceFirst("cookie-secret \"" + jargs.serverSecret + "\"");

            FileWriter fw = new FileWriter(file);
            fw.write(confFile);
            fw.flush();
            fw.close();
        }
        catch (IOException e){
            logger.error("IO error trying to write to " +
                jargs.serverSecretLocation + ": " + e);
            System.exit(1);
        }
    }

    private static String readServerSecret(){
        try {
            File file = new File(JDNSS.jargs.serverSecretLocation);
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            String confFile = new String(data, "UTF-8");
            fis.close();

            // Find serverSecret with Regex
            Pattern p = Pattern.compile("cookie-secret\\s+\"(.*)\"");
            Matcher m = p.matcher(confFile);

            // Here we will need to decide what to do if no server secret is found
            if (m.find()) {
                return m.group(1);
            }
            else {
                logger.warn("Couldnt find Server Secret");
                return "123456789"; //FIXME needs to be removed
            }
        }
        catch (IOException e){
            logger.warn("Couldnt find Server Secret" + e);
            return "123456789"; //FIXME needs to be removed

        }
    }

    /**
     * The main driver for the server; creates threads for TCP and UDP.
     */
    public static void main(String[] args) {
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
