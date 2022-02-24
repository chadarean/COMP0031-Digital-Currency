package com.mycompany.app; 

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;

public class Wallet {
    
    /*
      1. Wallet could create asset(POP file)
      2. Wallet could generate key pais(private and pubilc)
      3. Wallet could blind, unblind, sign and verify the tokens or specific messages
      4. Wallet could send F0 to MSBs and receive the signed F0
      5. Wallet could update asset state to F1 and transfer to another wallet
      6. It could send F0 and F1 to relay database
      7. For the recevier, wallet could verify the tokens
    */


    public String create_asset(){
        /* Should create pop file here. To make sure the type of pop(Binary? String? Integer?)
           This asset (F0) should have public key A(known as address), relay G, signature for s((d,Id),I)
        */
        return null;
    }

    public static AsymmetricCipherKeyPair generate_keypairs(int keySize){
        // Calling BlindSignature class to generate key pairs (A,A*)
        // A* is private key and keep for users
        // A is public key which would be put in the asset, represent the address of this wallet. 
        return BlindSignature.generateKeys(keySize);
    }

    // Parameters should be corrected (?? What kind of message should be blind)
    public static byte[] blind_message(CipherParameters key, BigInteger factor, byte[] msg){
        return BlindSignature.blind(key, factor, msg);
    }

    // Alice could request a blind signature and unblind it to view as well
    public static byte[] unblind_message(CipherParameters key, BigInteger factor, byte[] msg){
        return BlindSignature.unblind(key, factor, msg);
    }

    // Non-custodial wallet could sign for the information of ((d,Id),I)
    public static byte[] sign_message(CipherParameters key, byte[] toSign) {
        return BlindSignature.sign(key, toSign);
    }

    // Non-custodial wallet supprot to verify the sign to make sure it is a valid tokens
    public static boolean verify_message(CipherParameters key, byte[] msg, byte[] sig){
        return BlindSignature.verify(key, msg, sig);
    }

    public static void update_asset(){
        // This function would update asset state from F0 to F1 
    }

    public static void main(String[] args) {
        AsymmetricCipherKeyPair alice_keyPair = Wallet.generate_keypairs(1024);
        AsymmetricCipherKeyPair bob_keyPair = Wallet.generate_keypairs(1024);

        try {
            byte[] msg = "OK".getBytes("UTF-8");

            //Alice-Generating blinding factor based on Bob's public key
            BigInteger blindingFactor = BlindSignature.generateBlindingFactor(bob_keyPair.getPublic());

            //Alice-Blinding message with Bob's public key
            byte[] blinded_msg =
                    BlindSignature.blind(bob_keyPair.getPublic(), blindingFactor, msg);

            //Alice-Signing blinded message with Alice's private key
            byte[] sig = BlindSignature.sign(alice_keyPair.getPrivate(), blinded_msg);

            //Bob-Verifying alice's signature
            if (BlindSignature.verify(alice_keyPair.getPublic(), blinded_msg, sig)) {

                //Bob-Signing blinded message with bob's private key
                byte[] sigBybob =
                        BlindSignature.signBlinded(bob_keyPair.getPrivate(), blinded_msg);

                //Alice-Unblinding bob's signature 
                byte[] unblindedSigBybob =
                        BlindSignature.unblind(bob_keyPair.getPublic(), blindingFactor, sigBybob);

                //Alice-Verifying bob's unblinded signature
                System.out.println(BlindSignature.verify(bob_keyPair.getPublic(), msg,
                        unblindedSigBybob));
                        
               // Now alice has bob's signature for the original message
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
