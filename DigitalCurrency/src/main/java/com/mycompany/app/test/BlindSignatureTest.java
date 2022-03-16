package com.mycompany.app.test;

import com.mycompany.app.BlindSignature;
import org.junit.Assert;
import java.math.BigInteger;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Before;
import org.junit.Test;


public class BlindSignatureTest
{
    AsymmetricCipherKeyPair alice_keyPair;
    AsymmetricCipherKeyPair bob_keyPair;

    @Before
    public void setup(){
        AsymmetricCipherKeyPair alice_keyPair = BlindSignature.generateKeys(1024);
        AsymmetricCipherKeyPair bob_keyPair = BlindSignature.generateKeys(1024);
    }

    @Test
    public void fullProtocolTest()
    {
        try {
            byte[] msg = "Hello There".getBytes("UTF-8");
            //Alice::Generating blinding factor based on Bob's public key
            BigInteger blindingFactor = BlindSignature.generateBlindingFactor(bob_keyPair.getPublic());
            //Alice::Blinding message with Bob's public key
            byte[] blinded_msg =BlindSignature.blind(bob_keyPair.getPublic(), blindingFactor, msg);
            //Alice::Signing blinded message with Alice's private key
            //Message is signed by Alice so that one blinded process produces at most one valid signed message
            //https://eprint.iacr.org/2001/002.pdf
            byte[] sig = BlindSignature.sign(alice_keyPair.getPrivate(), blinded_msg);
            //Bob::Verifying alice's signature
            if (BlindSignature.verify(alice_keyPair.getPublic(), blinded_msg, sig)) {
                //Bob::Signing blinded message with bob's private key
                byte[] sigBybob =BlindSignature.signBlinded(bob_keyPair.getPrivate(), blinded_msg);
                //Alice::Unblinding bob's signature
                byte[] unblindedSigBybob =BlindSignature.unblind(bob_keyPair.getPublic(), blindingFactor, sigBybob);
                //Alice::Verifying bob's unblinded signature
                Assert.assertTrue(BlindSignature.verify(bob_keyPair.getPublic(), msg, unblindedSigBybob));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void blindUnblindMessageTest(){
        try {
            byte[] msg = "Hello There".getBytes("UTF-8");
            //Alice::Generating blinding factor based on Bob's public key
            BigInteger blindingFactor = BlindSignature.generateBlindingFactor(bob_keyPair.getPublic());
            //Alice::Blinding message with Bob's public key
            byte[] blinded_msg =BlindSignature.blind(bob_keyPair.getPublic(), blindingFactor, msg);
            //Alice::Signing blinded message with Alice's private key
            byte[] unblinded_msg =BlindSignature.unblind(bob_keyPair.getPublic(), blindingFactor, blinded_msg);
            //Bob::Verifying alice's signature
            Assert.assertEquals(msg, unblinded_msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test
    public void verifySignatureTest(){
        try {
            byte[] msg = "Hello There".getBytes("UTF-8");
            //Alice::Generating blinding factor based on Bob's public key
            BigInteger blindingFactor = BlindSignature.generateBlindingFactor(bob_keyPair.getPublic());
            //Alice::Blinding message with Bob's public key
            byte[] blinded_msg =BlindSignature.blind(bob_keyPair.getPublic(), blindingFactor, msg);
            //Alice::Signing blinded message with Alice's private key
            byte[] sig = BlindSignature.sign(alice_keyPair.getPrivate(), blinded_msg);
            Assert.assertTrue(BlindSignature.verify(alice_keyPair.getPublic(), blinded_msg, sig));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void blindTest(){
        try {
            byte[] msg = "Hello There".getBytes("UTF-8");
            //Alice::Generating blinding factor based on Bob's public key
            BigInteger blindingFactor = BlindSignature.generateBlindingFactor(bob_keyPair.getPublic());
            //Alice::Blinding message with Bob's public key
            byte[] blinded_msg =BlindSignature.blind(bob_keyPair.getPublic(), blindingFactor, msg);
            Assert.assertNotEquals(blinded_msg, msg);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    @Test
    public void keyGenerationTest(){
        AsymmetricCipherKeyPair alice_keyPair = BlindSignature.generateKeys(1024);
        AsymmetricCipherKeyPair bob_keyPair = BlindSignature.generateKeys(1024);
        Assert.assertNotEquals(alice_keyPair.getPublic(),bob_keyPair.getPublic());
        Assert.assertNotEquals(alice_keyPair.getPrivate(),bob_keyPair.getPrivate());

    }



}