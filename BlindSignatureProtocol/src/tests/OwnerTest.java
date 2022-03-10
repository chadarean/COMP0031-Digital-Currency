package tests;

import src.TODA.*;
import src.POP.*;

public class OwnerTest {
    public static void testOwner() {
        String C_1 = "C1"; // creation cycle hash
        String C_2 = "C2"; // update cycle hash
        String aId = "";
        String addressA1 = "00000001";
        String addressB = "10000000";
        Owner a = new Owner(aId);
        Token asset1 = a.createAsset(addressA1, 1);
    
        System.out.println("Asset id is " + asset1.getFileId());
        a.transferAsset(C_2, addressA1, asset1.getFileId(), addressB);
        a.sendUpdates(C_2, addressA1);
        System.out.println("Asset update is " + asset1.getFileDetail());
        MerkleProof proof = a.getFileProof(C_2, addressA1, asset1.getFileId());
        String fileDetailHash = asset1.getFileDetail();
        System.out.println(fileDetailHash);
        if (!proof.verify(asset1.getFileId(), fileDetailHash)) {
            throw new RuntimeException("Incorrect proof created!");
        }
    }

    public static void main(String args[]) {
        testOwner();
    }

}
