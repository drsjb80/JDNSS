package edu.msudenver.cs.jdnss;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.ObjectMessage;

import java.util.ArrayList;

class DBZone extends Zone {
    private static final Logger logger = JDNSS.logger;
    private final String zoneName;
    private final int domainId;
    private final DBConnection dbConnection;

    DBZone() {
        zoneName = null;
        domainId = -1;
        dbConnection = null;
    }

    @Override
    boolean isEmpty() { return zoneName == null; }

    // com.mysql.jdbc.Driver
    // jdbc:mysql://localhost/JDNSS
    DBZone(final String zoneName, final int domainId, final DBConnection dbConnection)
    {
        this.zoneName = zoneName;
        this.domainId = domainId;
        this.dbConnection = dbConnection;
        assert domainId >= 0;
        assert dbConnection != null;
    }

    public String getName() { return zoneName; }

    public ArrayList<RR> get(final RRCode type, final String name)
    {
        logger.traceEntry(new ObjectMessage(type));
        logger.traceEntry(new ObjectMessage(name));

        if (isEmpty() || dbConnection == null) {
            return new ArrayList<>();
        }

        return new ArrayList<>(dbConnection.get(type, name, domainId));
    }
}
