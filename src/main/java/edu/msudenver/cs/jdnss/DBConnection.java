package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

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
            Assertion.fail();
        }

        try {
            stmt = conn.createStatement();
        } catch (SQLException sqle) {
            try {
                stmt.close();
                conn.close();
            } catch (SQLException sqle2) {
                logger.catching(sqle);
                Assertion.fail();
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
                Assertion.fail();
            }
        }

        Assertion.aver(v.size() != 0);

        // then, find the longest that matches
        String s = Utils.findLongest(v, name);
        logger.trace(s);

        // then, populate a DBZone with what we found.
        try {
            rs = stmt.executeQuery
                    ("SELECT * FROM domains WHERE name = '" + s + "'");

            rs.next();
            final int domainId = rs.getInt("id");
            logger.trace(domainId);

            Assertion.aver(!rs.next());

            logger.traceExit(s);
            return new DBZone(s, domainId, this);
        } catch (SQLException sqle) {
            try {
                Assertion.aver(rs != null);
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException sqle2) {
                logger.catching(sqle);
                Assertion.fail();
            }
        }

        Assertion.fail("DBConnection failed");
        return null;    // have to have this or javac complains
    }

    public Vector<RR> get(final RRCode type, final String name, final int domainId) {
        logger.traceEntry(new ObjectMessage(type));
        logger.traceEntry(new ObjectMessage(name));
        logger.traceEntry(new ObjectMessage(domainId));

        try {
            String stype = type.name();
            logger.trace(stype);
            Vector<RR> ret = new Vector<>();
            ResultSet rs = stmt.executeQuery(
                    "SELECT * FROM records where domain_id = " + domainId +
                            " AND name = \"" + name + "\"" +
                            " AND type = \"" + stype + "\"");

            while (rs.next()) {
                String dbname = rs.getString("name");
                String dbcontent = rs.getString("content");
                int dbttl = rs.getInt("ttl");
                int dbprio = rs.getInt("prio");

                switch (type) {
                    case SOA: {
                        String s[] = dbcontent.split("\\s+");
                        ret.add(new SOARR(dbname, s[0], s[1],
                                Integer.parseInt(s[2]), Integer.parseInt(s[3]),
                                Integer.parseInt(s[4]), Integer.parseInt(s[5]),
                                Integer.parseInt(s[6]), dbttl));
                        break;
                    }
                    case NS: {
                        ret.add(new NSRR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case A: {
                        ret.add(new ARR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case AAAA: {
                        ret.add(new AAAARR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case MX: {
                        ret.add(new MXRR(dbname, dbttl, dbcontent, dbprio));
                        break;
                    }
                    case TXT: {
                        ret.add(new TXTRR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case CNAME: {
                        ret.add(new CNAMERR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case PTR: {
                        ret.add(new PTRRR(dbname, dbttl, dbcontent));
                        break;
                    }
                    case HINFO: {
                        final String s[] = dbcontent.split("\\s+");
                        ret.add(new HINFORR(dbname, dbttl, s[0], s[1]));
                        break;
                    }
                    default: {
                        logger.warn("requested type " + type +
                                " for " + name + " not found");
                        Assertion.fail();
                    }
                }
            }

            Assertion.aver(ret.size() != 0);
            return ret;
        } catch (SQLException sqle) {
            logger.catching(sqle);
            Assertion.fail();
        }

        Assertion.aver(false);
        return null;    // have to have this or javac complains
    }
}
