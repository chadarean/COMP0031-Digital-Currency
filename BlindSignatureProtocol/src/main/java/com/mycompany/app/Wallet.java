package com.mycompany.app; 

import java.math.BigInteger;
//import java.security.SecureRandom;

import com.mycompany.app.TODA.Owner;

//import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;

public class Wallet {
    
    public String userId; // Link to the specific user.
    Owner new_owner = new Owner(userId); // Create an owner object to do asset releated actions.
    
    static CipherParameters issuer_public_key;
    // static CipherParameters private_key;
    static BigInteger blindingFactor;
    
    /*
      1. Wallet could create asset 
      2. Wallet could generate key pais (private and pubilc)
      3. Wallet could blind, unblind, verify the tokens or specific messages
      4. Wallet could send blinded message to MSBs
      5. Wallet could update asset state to F1
      6. It could send F0 and F1 to relay database or receiver 
    */

    public Wallet(String userId) {
        this.userId = userId;
    }

    
    // No longer useful to generate keypairs for Alice or Bob, using MSbs' public key in current design 

    // public static void generate_keypairs(int keySize){
    //     // Calling BlindSignature class to generate key pairs (A,A*)
    //     // A* is private key and keep for users
    //     // A is public key which would be put in the asset, represent the address of this wallet. 
    //     AsymmetricCipherKeyPair wallet_keyPair = BlindSignature.generateKeys(keySize);
    //     public_key = wallet_keyPair.getPublic();
    //     private_key = wallet_keyPair.getPrivate();
    // }
    
    public static void get_issuer_publickey(CipherParameters key){
        issuer_public_key = key;
    }
    // Generate blinding factor using own public key
    public static void setBlindingFactor(){
        blindingFactor = BlindSignature.generateBlindingFactor(issuer_public_key);
    }

    // Blinding message using own public key and blinding factor
    public static byte[] blind_message(byte[] msg){
        return BlindSignature.blind(issuer_public_key, blindingFactor, msg);
    }

    // Unblind message using own private key and blinding factor
    public static byte[] unblind_message(byte[] msg){
        return BlindSignature.blind(issuer_public_key, blindingFactor, msg);
    }

    // Asset could be verified through central bank's public key 
    public static boolean verify_message(byte[] msg, byte[] sig){
        return BlindSignature.verify(issuer_public_key, msg, sig);
    }


}
