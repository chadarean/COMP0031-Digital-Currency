package com.mycompany.app;

import com.mycompany.app.TODA.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.mycompany.app.POP.*;

public class OwnerTest {
    public static Random rand = new Random();

    public static void testOwner(int numTokens) {
        ArrayList<String> C_ = new ArrayList<>();
        Relay r = new Relay(1, 1, TimeUnit.DAYS);

        String aId = "user1";
        String[] addressA = {TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE), TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE)};
        String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
        Owner a = new Owner(aId);
        a.setRelay(r);
        ArrayList<Token> tokens = new ArrayList<>();
        a.setRelay(r);

        MerkleTrie.TrieNode initialCycle = TestUtils.createRandomCycleTrie(a.relay);

        C_.add(initialCycle.value); // creation cycle hash
        System.out.println("init cycle " + C_.get(0));
        System.out.println(a.relay.cycleId.get(C_.get(0)));

        for (int i = 0; i < numTokens; ++ i) {
            Token asset = a.createAsset(C_.get(0), addressA[i%2], i+1, "");
            tokens.add(asset);
            //System.out.println("Initial file detail " + "for i=" + Integer.toString(i) + " = " + asset.getFileDetail());
            if (i % 2 == 0) {
                a.transferAsset(C_.get(0), addressA[i%2], asset, addressB);
            }
        }
        a.sendUpdates(C_.get(0), addressA[0]);

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MerkleTrie.TrieNode cycleRootNode1 = a.relay.getMostRecentCycTrieNode();
        C_.add(cycleRootNode1.value); // update cycle hash
        POPSlice popSlice1 = a.relay.getPOPSlice(addressA[0], C_.get(1));
        a.receivePOP(addressA[0], popSlice1);
        for (int i = 0; i < numTokens; i += 2) {
            a.getPOP(C_.get(1), addressA[0], tokens.get(i));
        }
        for (int i = 1; i < numTokens; i += 2) {
            a.transferAsset(C_.get(1), addressA[1], tokens.get(i), addressB);
        }

        a.sendUpdates(C_.get(1), addressA[1]);
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        MerkleTrie.TrieNode cycleRootNode2 = a.relay.getMostRecentCycTrieNode();
        C_.add(cycleRootNode2.value);
        POPSlice popSlice2 = a.relay.getPOPSlice(addressA[1], C_.get(2));
        a.receivePOP(addressA[1], popSlice2);
        

        int addressId = 0;
        for (Token asset : tokens) {
            a.getPOP(C_.get(1+addressId), addressA[addressId], asset);
            MerkleProof proof = a.getFileProof(C_.get(1), addressA[addressId], Utils.convertKey(asset.getFileId()));
            String fileDetailHash = asset.getFileDetail();
            //System.out.println(fileDetailHash);
            if (addressId == 0 && !proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                throw new RuntimeException("Incorrect proof created!");
            }
            if (addressId == 1) {
                if (proof != null && proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                    throw new RuntimeException("Incorrect proof should not be valid!");
                }
                proof = a.getFileProof(C_.get(2),  addressA[addressId], Utils.convertKey(asset.getFileId()));
                if (!proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                    throw new RuntimeException("Incorrect proof created!");
                }
            }
            addressId = 1-addressId;
        }

        Utils.printObjectSize(a.assets);
        Utils.printObjectSize(a.fileDetails);
        Utils.printObjectSize(a.assets);
    }

    public static void main(String args[]) {
        testOwner(10);
        System.out.println("Test passed");
    }

}
