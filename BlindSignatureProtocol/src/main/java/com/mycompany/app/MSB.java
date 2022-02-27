package com.mycompany.app;

import java.sql.*;

public class MSB {


    // TODO:
    /*
    getIssuerData(issuer_id, denominator=d) = returns I and s(d, I_d) for denominator d from issuer issuer_id from DLT

    getSignature(user_id, b_F) = forwards signature request for b_F to integrity provider and forwards the signature back to user_id

    redeemAsset(user_id, asset=F, POP_list) = forwards vefication to TODA API and possibly the ledger; on success updates the balance of user_id
    */

    // returns true if user_id has at least amount tokens, false otherwise
    public boolean verifyBalance(Connection conn, String user_id, double amount) {
        if (getBalanceOfUser(conn, user_id) - amount < 0) {
            return false;
        }
        return true;
    }

    // returns true if balance successfuly updates, false otherwise
    // amount to deposit oor withdraw
    public boolean updateBalance(Connection conn, String user_id, double amount) {
        if(updateBalanceOfUser(conn, user_id, amount) < 0) {
            return false;
        }
        return true;
    }

    public Connection connect() {
        Connection conn = null;
        try {
            // db parameters
            String url = "jdbc:sqlite:src/main/java/com/mycompany/app/MSB_DB.db";
            // create a connection to the database
            conn = DriverManager.getConnection(url);
            
            System.out.println("Connection to SQLite has been established.");
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public void insertUser(Connection conn, String user_id, double balance) {
        String sql = "INSERT INTO Users(user_id, balance) VALUES(?, ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, user_id);
            pstmt.setDouble(2, balance);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // returns positive balance of user if successful, otherwise -1
    public double getBalanceOfUser(Connection conn, String user_id){
        String sql = "SELECT balance FROM Users WHERE Users.user_id = ?";

        try {
            PreparedStatement pstmt  = conn.prepareStatement(sql);
            pstmt.setString(1, user_id);

            ResultSet rs  = pstmt.executeQuery();

            if(!rs.next()) {
                return -1;
            } else {
                return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return -1;
        }
    }


    // Update balance of user, returns 1 if successful, otherwise returns -1
    public double updateBalanceOfUser(Connection conn, String user_id, double amount) {
        double finalBalance = amount + getBalanceOfUser(conn, user_id);
        if(finalBalance < 0) {
            return -1;
        }
        String sql = "UPDATE Users SET balance = ? WHERE user_id = ?";

        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, finalBalance);
            pstmt.setString(2, user_id);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
            return -1;
        }

        return 1;
    }

    public void selectAll(){
        String sql = "SELECT id, user_id, balance FROM Users";
        
        try (Connection conn = this.connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(sql)){
            
            // loop through the result set
            while (rs.next()) {
                System.out.println(rs.getInt("id") +  "\t" + 
                                   rs.getString("user_id") + "\t" +
                                   rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void deleteUserData(Connection conn) {
        String sql = "DELETE FROM Users";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args)
    {
        // Example workflow
        MSB msb = new MSB();
        Connection c = msb.connect();
        // create a user
        msb.insertUser(c, "User1", 10);
        // verify balance
        msb.verifyBalance(c, "User1", 5);
        // update balance
        msb.updateBalance(c, "User1", -5);
        msb.selectAll();
        msb.deleteUserData(c);
        msb.selectAll();
        try {
            c.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
}