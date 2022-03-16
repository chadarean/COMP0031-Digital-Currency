package com.mycompany.app.test;

import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.TODA.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class InitialPOPSizeExperiment {
    public static class Structs {
        double addressProofSize;
        double fileProofSize;
        double popSize;
        double tokenSize;
        double userStorage;
        double fullUserStorage;
        double assetsStorage;

        public Structs(double addressProofSize, double fileProofSize, double popSize, double tokenSize, double userStorage, double fullUserStorage, double assetsStorage) {
            this.addressProofSize = addressProofSize;
            this.fileProofSize = fileProofSize;
            this.popSize = popSize;
            this.tokenSize = tokenSize;
            this.userStorage = userStorage;
            this.fullUserStorage = fullUserStorage;
            this.assetsStorage = assetsStorage;
        }

        public void add(MeasurePOPSize.Structs oth) {
            this.addressProofSize += oth.addressProofSize;
            this.fileProofSize += oth.fileProofSize;
            this.popSize += oth.popSize;
            this.tokenSize += oth.tokenSize;
            this.userStorage += oth.userStorage;
            this.fullUserStorage += oth.fullUserStorage;
            this.assetsStorage += oth.assetsStorage;
        }

        public void divBy(double x) {
            this.addressProofSize /= x;
            this.fileProofSize /= x;
            this.popSize /= x;
            this.tokenSize /= x;
            this.userStorage /= x;
            this.fullUserStorage /= x;
            this.assetsStorage /= x;
        }
    }

    public static Random rand = new Random();

    static ArrayList<String> C_;
    static Relay r;
    static MerkleTrie.TrieNode initialCycle;
    static ArrayList<ArrayList<Token>> tokens;
    static ArrayList<Owner> users;
    static ArrayList<Pair<String, String>> transactions; // transactions.get(i) = <souceAddr, destAddr> for transaction made by user i
    static HashMap<String, Integer> addrToId;

    public static void setup() {
        tokens = new ArrayList<>();
        users = new ArrayList<>();
        transactions = new ArrayList<>();
        addrToId = new HashMap<>();
        C_ = new ArrayList<>();
        r = new Relay(1, 1, TimeUnit.DAYS);
        initialCycle = TestUtils.createRandomCycleTrie(r);

        C_.add(initialCycle.value); // creation cycle hash
    }

    public static void tearDown() {
        r.closeConnection();
    }

    public static void createUsers(int nUsers) {
        for (int i = 0; i < nUsers; ++ i) {
            String aId = "user" + Integer.toString(i);
            Owner a = new Owner(aId);
            users.add(a);
            a.setRelay(r); // set the relay for user i to r
        }
    }

    public static void setupTransactions(int nTokens, int nAddr, boolean useRandom){
        createUsers(nAddr);

        for (int i = 0; i < nAddr; ++ i) {
            Owner a = users.get(i);
            String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for A = user_i
            while (addrToId.containsKey(addressA)) {
                addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
            }
            addrToId.put(addressA, i);
            String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // generate pubKey for B = destination
            transactions.add(new Pair<String,String>(addressA, addressB));
            tokens.add(new ArrayList<>());
            int tokens_i = nTokens;
            if (useRandom) {
                tokens_i = rand.nextInt() % nTokens + 1;
            }
            for (int j = 0; j < tokens_i; ++ j) {
                String certSignature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // certificate signature s((I_d, d), I)
                // create asset for addressA, issuance cycle root C_.get(0), denominator j+1 and certSignature
                Token asset = a.createAsset(C_.get(0), addressA, j+1, certSignature);
                tokens.get(i).add(asset);
            }
        }
    }

    public static MeasurePOPSize.Structs measureXTokensYAddressesZWaitingCycles(int nTokens, int nAddr, int nWaitingCycles) {
        setup();
        setupTransactions(nTokens, nAddr, false);

        for (int i = 0; i < nWaitingCycles; ++ i) {
            TestUtils.createRandomCycleTrie(r, nAddr);
        }

        for (int i = 0; i < nAddr; ++ i) {
            Owner a = users.get(i);
            String addressA = transactions.get(i).key;
            String addressB = transactions.get(i).value;

            for (int j = 0; j < nTokens; ++ j) {
                Token asset = tokens.get(i).get(j);
                String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // unblinded bsig for asset.fileKernel
                asset.addSignature(signature);
                a.transferAsset(C_.get(0), addressA, asset, addressB);
            }
            a.sendUpdates(C_.get(0), addressA); //sendUpdates for tokens withdrawn at C_.get(0) for addressA
        }

        r.createCycleTrie();
        MerkleTrie.TrieNode cycleRootNode1 = r.getMostRecentCycTrieNode();
        C_.add(cycleRootNode1.value); // update cycle hash

        int nAddrPfoofs = 0;
        int nFileProofs = 0;

        long popSizeSum = 0;
        long tokenSizeSum = 0;
        long addressProofSizeSum = 0;
        long fileProofSizesSum = 0;
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
                pop = a.getPOPUsingCache(C_.get(1), addressA, tokens.get(i).get(j));
                for (POPSlice popSlice: pop) {
                    popSizeSum += popSlice.getSize();
                    nAddrPfoofs += 1;
                    addressProofSizeSum += popSlice.addressProof.getSize();
                }
                Token asset = tokens_i.get(j);
                tokenSizeSum += asset.getSize();
                MerkleProof proof = a.getFileProof(C_.get(1), addressA, Utils.convertKey(asset.getFileId()));
                fileProofSizesSum += proof.getSize();
                nFileProofs += 1;
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
                (double)(popSizeSum / nTokens) / nAddr,
                (double)(tokenSizeSum / nTokens) / nAddr,
                (double)(fileProofSizesSum / nFileProofs),
                (double)(addressProofSizeSum / nAddrPfoofs));
        MeasurePOPSize.Structs structs = new MeasurePOPSize.Structs(((double)addressProofSizeSum / nAddrPfoofs),
                ((double)fileProofSizesSum / nFileProofs),
                ((double)popSizeSum / nTokens) / nAddr,
                ((double)tokenSizeSum / nTokens) / nAddr,
                (double)userStorageSum / nAddr,
                (double)fullUserStorageSum / nAddr,
                (double)assetsStorageSum / nAddr);
        tearDown();
        return structs;
    }

    public static void measureForNTokensNAddresses() {
        int nTokenValues[] = {1, 4, 8};
        int nAddrValue[] = {1024, 1700, 2048};

        for (int nTokens: nTokenValues) {
            for (int nAddr: nAddrValue) {
                // todo: repeat x times and add average
                MeasurePOPSize.Structs res = measureXTokensYAddressesZWaitingCycles(nTokens, nAddr, 0);
                System.out.printf("Full user storage =%f\n user storage=%f\n assets storage=%f\n", res.fullUserStorage, res.userStorage, res.assetsStorage);
            }
        }
    }


    public static void measureForXWaitingCycles() {
        int nTokenValues[] = {8};
        int nAddrValue[] = {1024};
        int nWaitingCyclesValues[] = {0, 1, 2, 4, 8, 16};
        for (int nTokens: nTokenValues) {
            for (int nAddr: nAddrValue) {
                for (int nWaitingCycles: nWaitingCyclesValues) {
                    measureXTokensYAddressesZWaitingCycles(nTokens, nAddr, nWaitingCycles);
                }
            }
        }
    }

    public static void main(String[] args) {
        TestUtils.setRandomNumbers();
        measureForXWaitingCycles();
        measureForNTokensNAddresses();
        System.out.println("Passed");
    }
}
