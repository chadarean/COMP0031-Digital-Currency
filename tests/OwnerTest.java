package tests;

import src.TODA.*;

import java.text.NumberFormat.Style;
import java.util.ArrayList;

import src.POP.*;

public class OwnerTest {
    public static void testOwner(int numTokens) {
        String C_1 = "C1"; // creation cycle hash
        String C_2 = "C2"; // update cycle hash
        String aId = "user1";
        String[] addressA = {"00000001", "00000010"};
        String addressB = "10000000";
        Owner a = new Owner(aId);
        ArrayList<Token> tokens = new ArrayList<>();

        for (int i = 0; i < numTokens; ++ i) {
            Token asset = a.createAsset(addressA[i%2], i+1);
            tokens.add(asset);
            //System.out.println("Initial file detail " + "for i=" + Integer.toString(i) + " = " + asset.getFileDetail());
            a.transferAsset(C_2, addressA[i%2], Utils.convertKey(asset.getFileId()), addressB);
        }
        a.sendUpdates(C_2, addressA[0]);
        //a.sendUpdates(C_2, addressA[1]);
        int addressId = 0;
        for (Token asset : tokens) {
            MerkleProof proof = a.getFileProof(C_2, addressA[addressId], Utils.convertKey(asset.getFileId()));
            String fileDetailHash = asset.getFileDetail();
            System.out.println(fileDetailHash);
            if (addressId == 0 && !proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                throw new RuntimeException("Incorrect proof created!");
            }
            if (addressId == 1 && proof != null && proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                throw new RuntimeException("Incorrect proof should not be valid!");
            }
            addressId = 1-addressId;
        }
    }

    // public static void testMultipleTransactions(int n) {
    //     String C_1 = "C1"; // creation cycle hash
    //     String C_2 = "C2"; // update cycle hash
    //     ArrayList <Owner> users = new ArrayList<>();
    //     for (int i = 0; i < n; ++ i) {
    //         String id = "user" + Integer.toString(i);
    //         ArrayList<String> addresses = new ArrayList<>();
    //         for (int j = 0; j < 1 + (i % 5); ++ j) {
    //             addresses
    //         }
    //     }
        
    //     String addressA1 = "00000001";
    //     String addressB = "10000000";
    //     Owner a = new Owner(aId);
    //     Token asset1 = a.createAsset(addressA1, 1);

    //     a.transferAsset(C_2, addressA1, Utils.convertKey(asset1.getFileId()), addressB);
    //     a.sendUpdates(C_2, addressA1);
    //     // System.out.println("Asset update is " + asset1.getFileDetail());
    //     MerkleProof proof = a.getFileProof(C_2, addressA1, Utils.convertKey(asset1.getFileId()));
    //     String fileDetailHash = asset1.getFileDetail();
    //     if (!proof.verify(Utils.convertKey(asset1.getFileId()), fileDetailHash)) {
    //         throw new RuntimeException("Incorrect proof created!");
    //     }
    // }

    public static void main(String args[]) {
        testOwner(10);
    }

}
