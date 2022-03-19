package com.mycompany.app.test;

import static org.junit.Assert.assertTrue;
import java.sql.*;

import com.mycompany.app.MSB.MSB;
import com.mycompany.app.Wallet;
import org.junit.Test;
import java.io.UnsupportedEncodingException;

import org.bouncycastle.crypto.CipherParameters;

/**
 * Unit test for simple App.
 */
public class WalletMSB
{
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue()
    {
        assertTrue( true );
    }

    public static void main(String[] args) throws UnsupportedEncodingException{
        byte[] msg = "Hello There".getBytes("UTF-8"); // For test, no asset is created here
        MSB msb = new MSB();
        Connection c = msb.connect();
        msb.insertUser(c,"1",50);
        msb.insertUser(c,"2",50);
        Wallet Alice_wallet = new Wallet("1");
        Wallet Bob_wallet = new Wallet("2");
        msb.generate_keypairs(1024);
        CipherParameters public_key = msb.share_publickey();
        Alice_wallet.get_issuer_publickey(public_key);
        Bob_wallet.get_issuer_publickey(public_key);
        Alice_wallet.setBlindingFactor();
        byte[] blind = Alice_wallet.blind_message(msg);

        boolean enough = msb.verifyBalance(c, "1", 10);
        byte[] signed_blind = msb.signBlinded(blind);
        msb.updateBalance(c, "1", -10);
        byte[] unblind_sign = Alice_wallet.unblind_message(signed_blind);

        // Both Alice and bob could verify the message.
        System.out.println(Alice_wallet.verify_message(msg, unblind_sign));
        System.out.println(Bob_wallet.verify_message(msg, unblind_sign));
        msb.redeemAsset(c, "2", 10);
    }
}