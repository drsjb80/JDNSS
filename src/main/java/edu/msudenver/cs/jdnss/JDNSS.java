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

class JDNSS {
    static final class RuntimeConfig {
        private final String serverSecret;
        private final String[] additional;
        private final boolean dnssecValidationEnabled;
        private final boolean dnssecRefuseUnsigned;

        RuntimeConfig(final String serverSecret, final String[] additional,
                      final boolean dnssecValidationEnabled, final boolean dnssecRefuseUnsigned) {
            this.serverSecret = serverSecret;
            this.additional = additional;
            this.dnssecValidationEnabled = dnssecValidationEnabled;
            this.dnssecRefuseUnsigned = dnssecRefuseUnsigned;
        }

        String getServerSecret() {
            return serverSecret;
        }

        String[] getAdditional() {
            return additional;
        }

        boolean isDnssecValidationEnabled() {
            return dnssecValidationEnabled;
        }

        boolean isDnssecRefuseUnsigned() {
            return dnssecRefuseUnsigned;
        }
    }

    // a few AOP singletons
    static final jdnssArgs jargs = new jdnssArgs();
    static final Logger logger = LogManager.getLogger("JDNSS");
    private static DBConnection DBConnection;

    static {
        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
	}

    private static final Map<String, Zone> bindZones = new HashMap<>();

    static void normalizeIpAddressOption(final String[] args) {
        for (String arg: args) {
            if (arg.startsWith("-IPaddresses") || arg.startsWith("--IPaddresses")) {
                jargs.IPaddresses = null;
            }
        }
    }

    static Thread.UncaughtExceptionHandler createUncaughtExceptionHandler() {
        return new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                System.err.print("Exception in thread \"" + t.getName());
                System.err.println(e.getLocalizedMessage());
                System.err.println(e.fillInStackTrace().toString());
            }
        };
    }

    static boolean hasConfiguredZoneSource() {
        return bindZones.size() != 0 || DBConnection != null;
    }

    static JCLO parseCommandLineArgs(final String[] args) {
        // Normalize so explicit --IPaddresses replaces defaults instead of appending to them.
        normalizeIpAddressOption(args);

        JCLO jclo = new JCLO(jargs);
        jclo.parse(args);
        return jclo;
    }

    /**
     * Finds the Zone associated with the domain name passed in
     *
     * @param name the name of the domain to find
     * @return the associated Zone
     * @see Zone
     */
    static Zone getZone(String name) {
        logger.traceEntry(new ObjectMessage(name));

        String longest = Utils.findLongest(bindZones.keySet(), name);

        if (longest == null) {
            if (DBConnection != null) {
                DBZone d = DBConnection.getZone(name);
                if (d.isEmpty()) {
                    return new BindZone();
                }
                return d;
            }
            return new BindZone();
        }

        Zone z = bindZones.getOrDefault(longest, null);
        if (z == null) {
            return new BindZone();
        }

        return z;
    }

    private static void start() {
        for (String IPAddress : jargs.IPaddresses) {
            String[] parts = IPAddress.split("@");

            switch(parts[0]) {
                case "TCP": case "TLS": new TCP(parts).start(); break;
                case "UDP": new UDP(parts).start(); break;
                case "MC": new MC(parts).start(); break;
                case "HTTPS": new HTTPS(parts); break;
                default:
                    logger.error("Invalid IP address specification");
                    System.exit(-1);
                    break;
            }
        }
    }

    private static void setLogLevel() {
        Level level = Level.OFF;

        switch (jargs.logLevel) {
            case FATAL: level = Level.FATAL; break;
            case ERROR: level = Level.ERROR; break;
            case WARN: level = Level.WARN; break;
            case INFO: level = Level.INFO; break;
            case DEBUG: level = Level.DEBUG; break;
            case TRACE: level = Level.TRACE; break;
            case ALL: level = Level.ALL; break;
            default:
                logger.warn("Invalid log level");
                break;
        }

        Configurator.setLevel("JDNSS", level);
    }

    static RuntimeConfig buildRuntimeConfig() {
        return new RuntimeConfig(
            resolveServerSecret(jargs.serverSecret),
            jargs.getAdditional(),
            jargs.isDnssecValidationEnabled(),
            jargs.isDnssecRefuseUnsigned()
        );
    }

    static String resolveServerSecret(final String configuredSecret) {
        if (configuredSecret == null){
            return String.valueOf(ThreadLocalRandom.current().nextLong());
        }

        if (configuredSecret.length() < 16) {
            logger.warn("Secret too short, generating random secret instead.");
            return String.valueOf(ThreadLocalRandom.current().nextLong());
        }

        return configuredSecret;
    }

    static String deriveZoneNameForAdditionalFile(final String additionalPath) {
        String name = new File(additionalPath).getName();

        if (name.endsWith(".db")) {
            name = name.replaceFirst("\\.db$", "");
            if (Character.isDigit(name.charAt(0))) {
                name = Utils.reverseIP(name);
                name = name + ".in-addr.arpa";
            }
        }

        return name;
    }

    static void loadAdditionalZones(final String[] additional, final RuntimeConfig runtimeConfig)
            throws UnsupportedEncodingException, ClassNotFoundException {
        if (additional == null) {
            return;
        }

        for (String anAdditional: additional) {
            try {
                final File additionalFile = new File(anAdditional);
                String name = deriveZoneNameForAdditionalFile(anAdditional);

                logger.info("Parsing: " + anAdditional);

                BindZone zone = new BindZone(name);
                new Parser(new FileInputStream(additionalFile), zone,
                        additionalFile.getAbsoluteFile().getParentFile()).RRs();
                logger.trace(zone);

                if (runtimeConfig.isDnssecValidationEnabled()) {
                    zone.setDnssecEnabled(true);
                    logger.info("DNSSEC validation enabled for zone: " + zone.getName());
                }

                // the name of the zone can change while parsing, so use
                // the name from the zone
                bindZones.put(zone.getName(), zone);
            } catch (FileNotFoundException e) {
                logger.warn("Couldn't open file " + anAdditional + '\n' + e);
            }
        }
    }

    private static void doargs() throws UnsupportedEncodingException, ClassNotFoundException {
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

        RuntimeConfig runtimeConfig = buildRuntimeConfig();
        jargs.serverSecret = runtimeConfig.getServerSecret();
        loadAdditionalZones(runtimeConfig.getAdditional(), runtimeConfig);
    }

    /**
     * The main driver for the server; creates threads for TCP and UDP.
     */
    public static void main(String[] args) throws UnsupportedEncodingException, ClassNotFoundException {
        JCLO jclo = parseCommandLineArgs(args);

        if (jargs.isHelp()) {
            System.out.println(jclo.usage());
            System.exit(0);
        }

        Thread.setDefaultUncaughtExceptionHandler(createUncaughtExceptionHandler());
        setLogLevel();
        doargs();

        if (!hasConfiguredZoneSource()) {
            logger.fatal("No zone files, traceExit.");
            System.exit(1);
        }

        start();
    }
}
