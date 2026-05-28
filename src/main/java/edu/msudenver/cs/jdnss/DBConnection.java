package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.sql.*;
import java.util.*;

class DBConnection {
    private static final Logger logger = JDNSS.logger;
    private final String dbUser;
    private final String dbPass;
    private final String dbURL;

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBConnection(final String dbClass, final String dbURL, final String dbUser,
                 final String dbPass) throws ClassNotFoundException {
        this.dbUser = dbUser == null ? "" : dbUser;
        this.dbPass = dbPass == null ? "" : dbPass;
        this.dbURL = dbURL;

        Class.forName(dbClass);
    }

    Connection createConnection() throws SQLException {
        return DriverManager.getConnection(dbURL, dbUser, dbPass);
    }

    private Set<String> getDomains(final Connection conn) {
        Set<String> v = new HashSet<>();

        try (PreparedStatement stmt = conn.prepareStatement("SELECT name FROM domains");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                v.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            logger.throwing(e);
        }
        return v;
    }

    private DBZone getZone(final Connection conn, final String s) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM domains WHERE name = ?")) {
            stmt.setString(1, s);
            try (ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return new DBZone();
            }
            final int domainId = rs.getInt("id");
            logger.trace(domainId);

            if (rs.next()) {
                logger.warn("multiple domains returned for " + s);
            }

            logger.traceExit(s);
            return new DBZone(s, domainId, this);
            }
        } catch (SQLException sqle) {
            logger.catching(sqle);
        }
        return new DBZone();
    }

    DBZone getZone(final String name) {
        logger.traceEntry(name);

        try (Connection conn = createConnection()) {

            Set<String> v = getDomains(conn);
            if (v.isEmpty()) {
                return new DBZone();
            }

            String s = Utils.findLongest(v, name);
            if (s == null) {
                return new DBZone();
            }
            logger.trace(s);

            return getZone(conn, s);
        } catch (SQLException sqle) {
            logger.catching(sqle);
        }
        return new DBZone();
    }

    public List<RR> get(final RRCode type, final String name, final int domainId) {
        logger.traceEntry(new ObjectMessage(type));
        logger.traceEntry(new ObjectMessage(name));
        logger.traceEntry(new ObjectMessage(domainId));

        List<RR> ret = new ArrayList<>();

        try (Connection conn = createConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT name, content, ttl, prio FROM records where domain_id = ? AND name = ? AND type = ?")) {

            stmt.setInt(1, domainId);
            stmt.setString(2, name);
            stmt.setString(3, type.name());

            try (ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                RR rr = addRR(type, name, rs);
                if (rr != null) {
                    ret.add(rr);
                }
            }
            }

            return ret;
        } catch (SQLException sqle) {
            logger.catching(sqle);
        }

        return ret;
    }

    private RR addRR(final RRCode type, final String name, final ResultSet rs) throws SQLException {
        final String dbname = rs.getString("name");
        final String dbcontent = rs.getString("content");
        final int dbttl = rs.getInt("ttl");
        final int dbprio = rs.getInt("prio");

        switch (type) {
            case SOA: {
                String[] s = dbcontent.split("\\s+");
                return new SOARR(dbname, s[0], s[1],
                        Integer.parseInt(s[2]), Integer.parseInt(s[3]),
                        Integer.parseInt(s[4]), Integer.parseInt(s[5]),
                        Integer.parseInt(s[6]), dbttl);
            }
            case NS: {
                return new NSRR(dbname, dbttl, dbcontent);
            }
            case A: {
                return new ARR(dbname, dbttl, dbcontent);
            }
            case AAAA: {
                return new AAAARR(dbname, dbttl, dbcontent);
            }
            case MX: {
                return new MXRR(dbname, dbttl, dbcontent, dbprio);
            }
            case TXT: {
                return new TXTRR(dbname, dbttl, dbcontent);
            }
            case CNAME: {
                return new CNAMERR(dbname, dbttl, dbcontent);
            }
            case PTR: {
                return new PTRRR(dbname, dbttl, dbcontent);
            }
            case HINFO: {
                final String[] s = dbcontent.split("\\s+");
                return new HINFORR(dbname, dbttl, s[0], s[1]);
            }
            case RRSIG:
            case NSEC:
            case DNSKEY:
            case NSEC3:
            case NSEC3PARAM: {
                return null;
            }
            default: {
                logger.warn("requested type " + type + " for " + name + " not found");
                break;
            }
        }

        return null;
    }
}
