package com.mycompany.app.MSB;

import java.nio.charset.StandardCharsets;
import java.sql.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mycompany.app.BlindSignature;
import com.mycompany.app.StandardResponse;
import com.mycompany.app.StatusResponse;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

import static spark.Spark.*;


public class MSB {

    // This class would contain not only MSB class but also issuer class.

    // TODO:
    /*
    getIssuerData(issuer_id, denominator=d) = returns I and s(d, I_d) for denominator d from issuer issuer_id from DLT
    getSignature(user_id, b_F) = forwards signature request for b_F to integrity provider and forwards the signature back to user_id
    redeemAsset(user_id, asset=F, POP_list) = forwards vefication to TODA API and possibly the ledger; on success updates the balance of user_id
    */

    static CipherParameters public_key;
    static CipherParameters private_key;


    // This public key generate here could send to Alice wallet, which could be used to blind the message.
    // Public key could also send to Bob to verify the tokens
    // Private key generate here could sign for blanded message
    public void generate_keypairs(int keySize){
        AsymmetricCipherKeyPair keyPair = BlindSignature.generateKeys(keySize);
        public_key = keyPair.getPublic();
        private_key = keyPair.getPrivate();
    }

    public byte[] signBlinded(byte[] msg) {
        // This could be used by MSBs to sign the Alice blinded message
        return BlindSignature.signBlinded(private_key,msg);
    }

    public CipherParameters share_publickey(){
        // Call this function to share publickey to Alice or Bob
        return public_key;
    }

    public boolean redeemAsset(Connection conn, String user_id, double amount){
        boolean success = updateBalance(conn,user_id,amount);
        return success;
    }

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
            String url = "jdbc:sqlite:src/main/java/com/mycompany/app/MSB/MSB_DB.db";
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

        sql = "UPDATE SQLITE_SEQUENCE SET SEQ=0 WHERE NAME='Users'";
        try {
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.executeUpdate();
        } catch(SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args)
    {

        MSB msb = new MSB();
        Connection c = msb.connect();
        msb.generate_keypairs(1024);
        //defines port for spark API to run on
        port(3080);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();


        get("/MSB/requestKey", (request, response) -> {
            response.type("application/json");
            //First converts public key into byte format and then into string for output
            byte[] publicKeyDer = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo((AsymmetricKeyParameter) msb.share_publickey()).getEncoded();
            String publicKey = new String(publicKeyDer, StandardCharsets.UTF_8);
            //Returns stringified public key as json response
            return gson.toJson(publicKey);
        });

        get("/MSB/requestSign/:blindedmsg", (request, response) -> {

            response.type("application/json");
            String msg=request.attribute(":blindedmsg");
            //Converts blindedmsg parameters from string to byte array
            byte[] blinded_message=msg.getBytes(StandardCharsets.UTF_8);
            //Signs blinded message
            byte[] signed_message=BlindSignature.signBlinded(private_key, blinded_message);
            //Outputs Json response of signed_blinded message in string format
            return gson.toJson(signed_message.toString());
        });


        try {
            c.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

}