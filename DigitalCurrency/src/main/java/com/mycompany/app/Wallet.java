package com.mycompany.app; 

import java.math.BigInteger;
import java.util.ArrayList;
//import java.security.SecureRandom;

import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.TODA.Owner;
import com.mycompany.app.TODA.Utils;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.CipherParameters;

public class Wallet {

    // Each Asset has an public key
    // Write a method to generate a public key each time when create an asset. 
    
    public static String userId; // Link to the specific user.
    Owner new_owner = new Owner(userId); // Create an owner object to do asset releated actions.
    
    CipherParameters issuer_public_key;
    CipherParameters private_key;
    CipherParameters public_key;
    BigInteger blindingFactor;
    
    /*
      1. Wallet could create asset 
      2. Wallet could generate key pais (private and pubilc)
      3. Wallet could blind, unblind, verify the tokens or specific messages
      4. Wallet could send blinded message to MSBs
      5. Wallet could update asset state to F1
      6. It could send F0 and F1 to relay database or receiver 
    */

    public Wallet(String userId) {
        Wallet.userId = userId;
    }

    
    // No longer useful to generate keypairs for Alice or Bob, using MSbs' public key in current design 

    public void generate_keypairs(int keySize){
        // Calling BlindSignature class to generate key pairs (A,A*)
        // A* is private key and keep for users
        // A is public key which would be put in the asset, represent the address of this wallet. 
        AsymmetricCipherKeyPair wallet_keyPair = BlindSignature.generateKeys(keySize);
        public_key = wallet_keyPair.getPublic();
        private_key = wallet_keyPair.getPrivate();
    }
    
    public void get_issuer_publickey(CipherParameters key){
        issuer_public_key = key;
    }
    // Generate blinding factor using own public key
    public void setBlindingFactor(){
        blindingFactor = BlindSignature.generateBlindingFactor(issuer_public_key);
    }

    // Blinding message using own public key and blinding factor
    public byte[] blind_message(byte[] msg){
        return BlindSignature.blind(issuer_public_key, blindingFactor, msg);
    }

    // Unblind message using own private key and blinding factor
    public byte[] unblind_message(byte[] msg){
        return BlindSignature.unblind(issuer_public_key, blindingFactor, msg);
    }

    // Asset could be verified through central bank's public key 
    public boolean verify_message(byte[] msg, byte[] sig){
        return BlindSignature.verify(issuer_public_key, msg, sig);
    }

    public Token create_asset(String address, int d){
        return new_owner.createAsset(address,d);
    }

    public byte[] convert_token_to_byte(Token asset){
        String fileId = asset.getFileId();
        byte[] byte_msg = Utils.prefixToBytes(fileId);
        return byte_msg;
    }

    public void transferAsset(String cycleRoot, String address, Token asset, String destPk){
        new_owner.transferAsset(cycleRoot, address, asset, destPk);
    }

    public void sendUpdate(String address, String txpxHash){
        new_owner.sendUpdate(address, txpxHash);
    }

    public boolean verifyPOP(ArrayList<POPSlice> popSlices, String address, String destinationAddress, String signature){
        return new_owner.verifyPOP(popSlices,address,destinationAddress,signature);
    }

    public boolean receiveAsset(ArrayList<POPSlice> popSlices, String address, String destinationAddress, String signature, Token token) {
       return new_owner.receiveAsset(popSlices,address,destinationAddress,signature,token);
    }

}
