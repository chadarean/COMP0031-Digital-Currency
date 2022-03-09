package tests;

import src.TODA.*;

import java.text.NumberFormat.Style;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import src.POP.*;

public class MeasurePOPSize {
    public static class Structs<T> {
        T addressProofSize;
        T fileProofSize;
        T popSize; 
        T tokenSize;
        T userStorage;
        T fullUserStorage;
        T assetsStorage;

        public Structs(T addressProofSize, T fileProofSize, T popSize, T tokenSize, T userStorage, T fullUserStorage, T assetsStorage) {
            this.addressProofSize = addressProofSize;
            this.fileProofSize = fileProofSize;
            this.popSize = popSize;
            this.tokenSize = tokenSize;
            this.userStorage = userStorage;
            this.fullUserStorage = fullUserStorage;
            this.assetsStorage = assetsStorage;
        }
    }

    public static Random rand = new Random();

    public static Structs measureXTokensYAddresses(int nTokens, int nAddr) {
        ArrayList<String> C_ = new ArrayList<>();
        Relay r = new Relay(1, 1, TimeUnit.DAYS);
        MerkleTrie.TrieNode initialCycle = TestUtils.createRandomCycleTrie(r);

        C_.add(initialCycle.value); // creation cycle hash
        System.out.println("initial cycle " + C_.get(0));
        System.out.println(r.cycleId.get(C_.get(0)));

        ArrayList<ArrayList<Token>> tokens = new ArrayList<>();
        ArrayList<Owner> users = new ArrayList<>();
        ArrayList<Pair<String, String>> transactions = new ArrayList<>(); // transactions.get(i) = <souceAddr, destAddr> for transaction made by user i

        for (int i = 0; i < nAddr; ++ i) {
            String aId = "user" + Integer.toString(i);
            Owner a = new Owner(aId);
            users.add(a);
            String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for A = user_i
            String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for B = destination 
            transactions.add(new Pair<String,String>(addressA, addressB));
            a.setRelay(r); // link the user to the relay
            tokens.add(new ArrayList<>());
            for (int j = 0; j < nTokens; ++ j) {
                String certSignature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // certificate signature s((I_d, d), I)
                // create asset for addressA, issuance cycle root C_.get(0), denominator j+1 and certSignature 
                Token asset = a.createAsset(C_.get(0), addressA, j+1, certSignature);
                String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // unblinded bsig for asset.fileKernel
                asset.addSignature(signature);
                tokens.get(i).add(asset);
                a.transferAsset(C_.get(0), addressA, asset, addressB);
            }
            a.sendUpdates(C_.get(0), addressA); //sendUpdates for tokens withdrawn at C_.get(0) for addressA
        }

        r.createCycleTrie();
        MerkleTrie.TrieNode cycleRootNode1 = r.getMostRecentCycTrieNode();
        C_.add(cycleRootNode1.value); // update cycle hash

        long popSizeSum = 0;
        long tokenSizeSum = 0;
        long addressProofSizeSum = 0;
        long MerkleProofSizesSum = 0;
        long userStorageSum = 0;
        long fullUserStorageSum = 0;
        long assetsStorageSum = 0;

        for (int i = 0; i < nAddr; ++ i) {
            Owner a = users.get(i);
            String addressA = transactions.get(i).key;
            String addressB = transactions.get(i).value;

            POPSlice popSlice1 = r.getPOPSlice(addressA, C_.get(1));
            a.receivePOP(addressA, popSlice1);
            ArrayList<Token> tokens_i = tokens.get(i);
            for (int j = 0; j < nTokens; j ++) {
                ArrayList<POPSlice> pop;
                pop = a.getPOP(C_.get(1), addressA, tokens.get(i).get(j));
                for (POPSlice popSlice: pop) {
                    popSizeSum += popSlice.getSize();
                    addressProofSizeSum += popSlice.addressProof.getSize();
                }
                tokenSizeSum += tokens.get(i).get(j).getSize();
            }

            for (Token asset : tokens_i) {
                MerkleProof proof = a.getFileProof(C_.get(1), addressA, Utils.convertKey(asset.getFileId()));
                MerkleProofSizesSum += proof.getSize();
                String fileDetailHash = asset.getFileDetail();
                if (!proof.verify(Utils.convertKey(asset.getFileId()), fileDetailHash)) {
                    throw new RuntimeException("Incorrect proof created!");
                }
            }
            userStorageSum += a.getSize(false);
            fullUserStorageSum += a.getSize(true);
            assetsStorageSum += a.getAssetsSize();
        }
        
        System.out.printf("POP avg size = %f\n token avg size = %f\n Merkle File Proofs avg size = %f\n Address Proof avg size = %f\n",
        (float)(popSizeSum / nTokens) / nAddr, 
        (float)(tokenSizeSum / nTokens) / nAddr, 
        (float)(MerkleProofSizesSum / nTokens) / nAddr,
        (float)(addressProofSizeSum / nTokens) / nAddr);
        Structs<Long> structs = new Structs<Long>((addressProofSizeSum / nTokens) / nAddr,
        (MerkleProofSizesSum / nTokens) / nAddr, 
        (popSizeSum / nTokens) / nAddr, 
        (tokenSizeSum / nTokens) / nAddr, 
        userStorageSum / nAddr, 
        fullUserStorageSum / nAddr, 
        assetsStorageSum / nAddr);
        
        return structs;
    }

    public static void measureForNTokensNAddresses() {
        int nTokenValues[] = {1, 4, 8};
        int nAddrValue[] = {1024, 1700, 2048};

        for (int nTokens: nTokenValues) {
            for (int nAddr: nAddrValue) {
                // todo: repeat x times and add average
                Structs res = measureXTokensYAddresses(nTokens, nAddr);
                System.out.printf("Full user storage =%d\n user storage=%d\n assets storage=%d\n", res.fullUserStorage, res.userStorage, res.assetsStorage);
            }
        }
        // todo, either create plot in Java or write results to a file and use Python 
    }

    public static void measureForXWaitingCyclesNTokensNAddr(int nTokens, int nAddr, int nWaitingCycles) {
        
    }

    public static void measureForXWaitingCycles() {
        int nTokenValues[] = {8};
        int nAddrValue[] = {1024};
        int nWaitingCyclesValues[] = {1, 2, 4, 8, 16};
        for (int nTokens: nTokenValues) {
            for (int nAddr: nAddrValue) {
                for (int nWaitingCycles: nWaitingCyclesValues) {
                    measureForXWaitingCyclesNTokensNAddr(nTokens, nAddr, nWaitingCycles);
                }
            }
        }
    }

    public static void main(String[] args) {
        measureForNTokensNAddresses();
        System.out.println("Passed");
    }
}
