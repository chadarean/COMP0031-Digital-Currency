package tests;

import src.TODA.*;

import java.text.NumberFormat.Style;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import src.POP.*;

public class MeasurePOPSize {
    public static Random rand = new Random();

    public static void measureXTokensYAddresses(int nTokens, int nAddr) {
        ArrayList<String> C_ = new ArrayList<>();
        Relay r = new Relay();
        MerkleTrie.TrieNode initialCycle = TestUtils.createGenesisCycleTrie(r);

        C_.add(initialCycle.value); // creation cycle hash
        System.out.println("init cycle " + C_.get(0));
        System.out.println(r.cycleId.get(C_.get(0)));

        String aId = "user1";
        String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for A=user1

        String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for B=user2
        Owner a = new Owner(aId);
        a.setRelay(r);
        ArrayList<Token> tokens = new ArrayList<>();
        for (int i = 0; i < nTokens; ++ i) {
            String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
            Token asset = a.createAsset(C_.get(0), addressA, i+1, signature);
            tokens.add(asset);
            a.transferAsset(C_.get(0), addressA, asset, addressB);
        }
        a.sendUpdates(C_.get(0), addressA); //sendUpdates for tokens withdrawn at C_.get(0) for addressA

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MerkleTrie.TrieNode cycleRootNode1 = r.getMostRecentCycTrieNode();
        C_.add(cycleRootNode1.value); // update cycle hash
        POPSlice popSlice1 = r.getPOPSlice(addressA, C_.get(1));
        a.receivePOP(addressA, popSlice1);

        long popSizeSum = 0;
        long tokenSizeSum = 0;
        long MerkleProofSizesSum = 0;
        
        for (int i = 0; i < nTokens; i ++) {
            ArrayList<POPSlice> pop;
            pop = a.getPOP(C_.get(1), addressA, tokens.get(i));
            for (POPSlice popSlice: pop) {
                popSizeSum += popSlice.getSize();
            }
            tokenSizeSum += Utils.getObjectSize(tokens.get(i));
        }
        
        for (Token asset : tokens) {
            MerkleProof proof = a.getFileProof(C_.get(1), addressA, Utils.convertKey(asset.getFileId()));
            MerkleProofSizesSum += proof.getSize();
            String fileDetailHash = asset.getFileDetail();
            if (!proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                throw new RuntimeException("Incorrect proof created!");
            }
        }
        
        Utils.printObjectSize(a.assets);
        Utils.printObjectSize(a.fileDetails);
        Utils.printObjectSize(a.fileTrieCache);
        Utils.printObjectSize(a.crtFileTrie);
        System.out.printf("POP averge size = %f\n token average size = %f\n Merkle Proofs average size = %f\n", (float)popSizeSum / nTokens, 
        (float)tokenSizeSum / nTokens, 
        (float)MerkleProofSizesSum / nTokens);
    }

    public static void main(String[] args) {
        measureXTokensYAddresses(10, 1);
        measureXTokensYAddresses(100, 1);
        System.out.println("Passed");
    }
}
