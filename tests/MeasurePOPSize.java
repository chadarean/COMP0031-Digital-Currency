package tests;

import src.TODA.*;

import java.text.NumberFormat.Style;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import src.POP.*;

public class MeasurePOPSize {
    public static Random rand = new Random();

    public static void measureXTokensPerAddress(int numTokens) {
        ArrayList<String> C_ = new ArrayList<>();
        String aId = "user1";
        String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for A=user1

        String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for B=user2
        Owner a = new Owner(aId);
        ArrayList<Token> tokens = new ArrayList<>();

        MerkleTrie.TrieNode initialCycle = TestUtils.createGenesisCycleTrie(a.relay);

        C_.add(initialCycle.value); // creation cycle hash
        System.out.println("init cycle " + C_.get(0));
        System.out.println(a.relay.cycleId.get(C_.get(0)));

        for (int i = 0; i < numTokens; ++ i) {
            Token asset = a.createAsset(C_.get(0), addressA, i+1, "");
            tokens.add(asset);
            a.transferAsset(C_.get(0), addressA, asset, addressB);
        }
        a.sendUpdates(C_.get(0), addressA);

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        MerkleTrie.TrieNode cycleRootNode1 = a.relay.getMostRecentCycTrieNode();
        C_.add(cycleRootNode1.value); // update cycle hash
        POPSlice popSlice1 = a.relay.getPOPSlice(addressA, C_.get(1));
        a.receivePOP(addressA, popSlice1);

        long popSizeSum = 0;
        long tokenSizeSum = 0;
        long MerkleProofSizesSum = 0;
        
        for (int i = 0; i < numTokens; i ++) {
            ArrayList<POPSlice> pop;
            pop = a.getPOP(C_.get(1), addressA, tokens.get(i));
            popSizeSum += Utils.getObjectSize(pop);
            tokenSizeSum += Utils.getObjectSize(tokens.get(i));
        }
        
        for (Token asset : tokens) {
            MerkleProof proof = a.getFileProof(C_.get(1), addressA, Utils.convertKey(asset.getFileId()));
            MerkleProofSizesSum += Utils.getObjectSize(proof);
            String fileDetailHash = asset.getFileDetail();
            if (!proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                throw new RuntimeException("Incorrect proof created!");
            }
        }
        
        Utils.printObjectSize(a.assets);
        Utils.printObjectSize(a.fileDetails);
        Utils.printObjectSize(a.fileTrieCache);
        Utils.printObjectSize(a.crtFileTrie);
        System.out.printf("POP averge size = %f\n token average size = %f\n Merkle Proofs average size = %f\n", (float)popSizeSum / numTokens, 
        (float)tokenSizeSum / numTokens, 
        (float)MerkleProofSizesSum / numTokens);
    }

    public static void main(String[] args) {
        measureXTokensPerAddress(10);
        measureXTokensPerAddress(100);
    }
}
