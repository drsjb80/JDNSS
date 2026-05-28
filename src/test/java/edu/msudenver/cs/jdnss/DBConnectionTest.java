package edu.msudenver.cs.jdnss;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DBConnectionTest {

    private static class TestableDBConnection extends DBConnection {
        private final Connection connection;

        TestableDBConnection(Connection connection) throws Exception {
            super("java.lang.String", "jdbc:invalid://localhost/jdnss", null, null);
            this.connection = connection;
        }

        @Override
        Connection createConnection() {
            return connection;
        }
    }

    @Test
    public void constructorRejectsUnknownDriverClass() {
        Assert.assertThrows(ClassNotFoundException.class,
                () -> new DBConnection("not.a.real.Driver", "jdbc:invalid", null, null));
    }

    @Test
    public void getZoneReturnsEmptyWhenConnectionFails() throws Exception {
        DBConnection connection = new DBConnection("java.lang.String",
                "jdbc:invalid://localhost/jdnss", null, null);

        DBZone zone = connection.getZone("www.test.com");

        Assert.assertTrue(zone.isEmpty());
    }

    @Test
    public void getReturnsEmptyListWhenConnectionFails() throws Exception {
        DBConnection connection = new DBConnection("java.lang.String",
                "jdbc:invalid://localhost/jdnss", null, null);

        List<RR> records = connection.get(RRCode.A, "www.test.com", 1);

        Assert.assertNotNull(records);
        Assert.assertTrue(records.isEmpty());
    }

    @Test
    public void getReturnsMappedRecordsWhenQuerySucceeds() throws Exception {
        Connection conn = Mockito.mock(Connection.class);
        PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
        ResultSet rs = Mockito.mock(ResultSet.class);

        when(conn.prepareStatement("SELECT name, content, ttl, prio FROM records where domain_id = ? AND name = ? AND type = ?"))
                .thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString("name")).thenReturn("www.test.com");
        when(rs.getString("content")).thenReturn("192.0.2.10");
        when(rs.getInt("ttl")).thenReturn(300);
        when(rs.getInt("prio")).thenReturn(10);

        DBConnection connection = new TestableDBConnection(conn);
        List<RR> records = connection.get(RRCode.A, "www.test.com", 7);

        Assert.assertEquals(1, records.size());
        Assert.assertEquals(RRCode.A, records.get(0).getType());
        Assert.assertEquals("www.test.com", records.get(0).getName());
        Assert.assertEquals(300, records.get(0).getTtl());

        verify(stmt).setInt(1, 7);
        verify(stmt).setString(2, "www.test.com");
        verify(stmt).setString(3, "A");
    }

    @Test
    public void getZoneReturnsResolvedZoneWhenDomainMatches() throws Exception {
        Connection conn = Mockito.mock(Connection.class);

        PreparedStatement domainsStmt = Mockito.mock(PreparedStatement.class);
        ResultSet domainsRs = Mockito.mock(ResultSet.class);
        when(conn.prepareStatement("SELECT name FROM domains")).thenReturn(domainsStmt);
        when(domainsStmt.executeQuery()).thenReturn(domainsRs);
        when(domainsRs.next()).thenReturn(true, true, false);
        when(domainsRs.getString("name")).thenReturn("example.com", "com");

        PreparedStatement zoneStmt = Mockito.mock(PreparedStatement.class);
        ResultSet zoneRs = Mockito.mock(ResultSet.class);
        when(conn.prepareStatement("SELECT id FROM domains WHERE name = ?")).thenReturn(zoneStmt);
        when(zoneStmt.executeQuery()).thenReturn(zoneRs);
        when(zoneRs.next()).thenReturn(true, false);
        when(zoneRs.getInt("id")).thenReturn(42);

        DBConnection connection = new TestableDBConnection(conn);
        DBZone zone = connection.getZone("www.example.com");

        Assert.assertFalse(zone.isEmpty());
        Assert.assertEquals("example.com", zone.getName());
        verify(zoneStmt).setString(1, "example.com");
    }
}
