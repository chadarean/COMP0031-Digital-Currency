package com.mycompany.app.TODA;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;

public class RelayDB
{

    public static Connection connect() {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:src/main/java/com/mycompany/app/TODA/RelayDB.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);

            System.out.println("Connection to SQLite has been established.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void insertTransaction(Connection conn, String addressOfAsset, String hashOfUpdate) {
        String sql = "INSERT INTO Transactions(AddressOfAsset, HashOfUpdate, CycleTrieId) VALUES(?,?,?)";
        int currentId = getMostRecentCycleId(conn);
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, addressOfAsset);
            pstmt.setString(2, hashOfUpdate);
            pstmt.setInt(3, currentId);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public int getMostRecentCycleId(Connection conn) {
        String sqlSelect = "SELECT CycleTrieId FROM CycleTries ORDER BY CycleTrieId DESC LIMIT 0, 1;";
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlSelect);
            return rs.getInt("CycleTrieId");
        } catch (SQLException e) {
            return 0; // first cycle trie
        }
    }

    public void insertNewCycleTrie(Connection conn) {
        String sql = "INSERT INTO CycleTries(Timestamp) VALUES(?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setTimestamp(1, Timestamp.from(Instant.now()));
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteTransactinData(Connection conn) {
        String sql = "DELETE FROM Transactions";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }

        sql = "UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='Transactions'";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            selectAllCycleTries(conn);
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteCycleTrieData(Connection conn) {
        String sql = "DELETE FROM CycleTries";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            selectAllCycleTries(conn);
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }

        sql = "UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='CycleTries'";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
            selectAllCycleTries(conn);
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void selectAllTransactions(Connection conn){
        String sql = "SELECT TransactionId, AddressOfAsset, HashOfUpdate, CycleTrieId FROM Transactions";

        try {
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);

            // loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("TransactionId") +  "\t" +
                        rs.getString("AddressOfAsset") + "\t" +
                        rs.getString("HashOfUpdate") + "\t" +
                        rs.getInt("CycleTrieId"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public ArrayList<Pair<String, String>>  selectAllTransactionsForCycle(Connection conn, int cycleId){
        String sql = "SELECT TransactionId, AddressOfAsset, HashOfUpdate, CycleTrieId FROM Transactions WHERE CycleTrieId == " + Integer.toString(cycleId);
        sql += " ORDER BY AddressOfAsset";
        ArrayList<Pair<String, String>> transactions = new ArrayList<Pair<String, String>>();
        try {
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);

            // loop through the result set
            while (rs.next()) {
                transactions.add(new Pair<String, String>(rs.getString("AddressOfAsset"), rs.getString("HashOfUpdate")));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return transactions;
    }

    public void selectAllCycleTries(Connection conn){
        String sql = "SELECT CycleTrieId, Timestamp FROM CycleTries";

        try {
            Statement stmt  = conn.createStatement();
            ResultSet rs    = stmt.executeQuery(sql);

            // loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("CycleTrieId") +  "\t" +
                        rs.getTimestamp("Timestamp"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        RelayDB relayDB = new RelayDB();
        Connection c = relayDB.connect();
        relayDB.insertTransaction(c, "address1", "hash1");
        relayDB.selectAllTransactions(c);
        relayDB.insertNewCycleTrie(c);
        relayDB.insertNewCycleTrie(c);
        relayDB.insertTransaction(c, "address1", "hash1");
        relayDB.selectAllTransactions(c);
        relayDB.selectAllCycleTries(c);
        relayDB.deleteCycleTrieData(c);
        relayDB.deleteTransactinData(c);
        relayDB.selectAllTransactions(c);
        relayDB.selectAllCycleTries(c);
        try {
            c.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}