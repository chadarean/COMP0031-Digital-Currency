package wallet;
import com.mycompany.app.*;

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


    private String create_asset(){
        /* Should create pop file here. To make sure the type of pop(Binary? String? Integer?)
           This asset (F0) should have public key A(known as address), relay G, signature for s((d,Id),I)
        */
        return null;
    }

    private static AsymmetricCipherKeyPair generate_keypairs(int keySize){
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


}
