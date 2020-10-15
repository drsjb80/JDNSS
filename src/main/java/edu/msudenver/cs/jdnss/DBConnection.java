package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.sql.*;
import java.util.*;

class DBConnection {
    private Connection conn;
    private Statement stmt;
    private static final Logger logger = JDNSS.logger;

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBConnection(final String dbClass, final String dbURL, final String dbUser,
                 final String dbPass) {
        String user = dbUser == null ? "" : dbUser;
        String pass = dbPass == null ? "" : dbPass;

        // load up the class
        try {
            Class.forName(dbClass);
        } catch (ClassNotFoundException cnfe) {
            logger.catching(cnfe);
        }

        try {
            conn = DriverManager.getConnection(dbURL, user, pass);
        } catch (SQLException sqle) {
            logger.catching(sqle);
            assert false;
        }

        try {
            stmt = conn.createStatement();
        } catch (SQLException sqle) {
            try {
                stmt.close();
                conn.close();
            } catch (SQLException sqle2) {
                logger.catching(sqle);
                assert false;
            }
        }
    }

    DBZone getZone(final String name) {
        logger.traceEntry(new ObjectMessage(name));

        Set<String> v = new HashSet<>();

        // first, get them all
        ResultSet rs = null;
        try {
            rs = stmt.executeQuery("SELECT * FROM domains");

            while (rs.next()) {
                v.add(rs.getString("name"));
            }
        } catch (SQLException sqle) {
            try {
                stmt.close();
                conn.close();
            } catch (SQLException sqle2) {
                logger.catching(sqle);
                return new DBZone();
            }
        }

        if (v.size() == 0) {
            return new DBZone();
        }

        // then, find the longest that matches
        String s = Utils.findLongest(v, name);
        logger.trace(s);

        // then, populate a DBZone with what we found.
        try {
            rs = stmt.executeQuery ("SELECT * FROM domains WHERE name = '" + s + "'");

            rs.next();
            final int domainId = rs.getInt("id");
            logger.trace(domainId);

            assert !rs.next();

            logger.traceExit(s);
            return new DBZone(s, domainId, this);
        } catch (SQLException sqle) {
            try {
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException sqle2) {
                logger.catching(sqle);
                return new DBZone();
            }
        }

        return new DBZone();
    }

    public List<RR> get(final RRCode type, final String name, final int domainId) {
        logger.traceEntry(new ObjectMessage(type));
        logger.traceEntry(new ObjectMessage(name));
        logger.traceEntry(new ObjectMessage(domainId));

        try {
            String stype = type.name();
            logger.trace(stype);
            List<RR> ret = new ArrayList<>();
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM records where domain_id = " + domainId +
                            " AND name = \"" + name + "\"" +
                            " AND type = \"" + stype + "\"");

            while (rs.next()) {
                addRR(type, name, rs);
            }

            return ret;
        } catch (SQLException sqle) {
            logger.catching(sqle);
        }

        return Collections.emptyList();
    }

    private void addRR(final RRCode type, final String name, final ResultSet rs) throws SQLException {
        final String dbname = rs.getString("name");
        final String dbcontent = rs.getString("content");
        final int dbttl = rs.getInt("ttl");
        final int dbprio = rs.getInt("prio");

        switch (type) {
            case SOA: {
                String[] s = dbcontent.split("\\s+");
                new SOARR(dbname, s[0], s[1],
                        Integer.parseInt(s[2]), Integer.parseInt(s[3]),
                        Integer.parseInt(s[4]), Integer.parseInt(s[5]),
                        Integer.parseInt(s[6]), dbttl);
                return;
            }
            case NS: {
                new NSRR(dbname, dbttl, dbcontent);
                return; }
            case A: {
                new ARR(dbname, dbttl, dbcontent);
                return; }
            case AAAA: {
                new AAAARR(dbname, dbttl, dbcontent);
                return; }
            case MX: {
                new MXRR(dbname, dbttl, dbcontent, dbprio);
                return; }
            case TXT: {
                new TXTRR(dbname, dbttl, dbcontent);
                return; }
            case CNAME: {
                new CNAMERR(dbname, dbttl, dbcontent);
                return; }
            case PTR: {
                new PTRRR(dbname, dbttl, dbcontent);
                return; }
            case HINFO: {
                final String[] s = dbcontent.split("\\s+");
                new HINFORR(dbname, dbttl, s[0], s[1]);
                return;
            }
            case RRSIG:
            case NSEC:
            case DNSKEY:
            case NSEC3:
            case NSEC3PARAM: { return; }
            default: {
                logger.warn("requested type " + type + " for " + name + " not found");
                break;
            }
        }
    }
}
