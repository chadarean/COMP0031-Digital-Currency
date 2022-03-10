package tests;

import src.TODA.*;

import java.text.NumberFormat.Style;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.lang.Math;

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

    static ArrayList<String> C_;
    static Relay r;
    static MerkleTrie.TrieNode initialCycle;
    static ArrayList<ArrayList<Token>> tokens;
    static ArrayList<Owner> users;
    static ArrayList<Pair<String, String>> transactions; // transactions.get(i) = <souceAddr, destAddr> for transaction made by user i
    static HashMap <String, Integer> addrToId;

    public static void setup() {
        tokens = new ArrayList<>();
        users = new ArrayList<>();
        transactions = new ArrayList<>();
        addrToId = new HashMap<>();
        C_ = new ArrayList<>();
        r = new Relay(1, 1, TimeUnit.DAYS);
        initialCycle = TestUtils.createRandomCycleTrie(r);

        C_.add(initialCycle.value); // creation cycle hash
        System.out.println("initial cycle " + C_.get(0));
        System.out.println(r.cycleId.get(C_.get(0)));
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

    public static void createUsers(int nUsers) {
        for (int i = 0; i < nUsers; ++ i) {
            String aId = "user" + Integer.toString(i);
            Owner a = new Owner(aId);
            users.add(a);
            a.setRelay(r); // set the relay for user i to r
        }
    }

    public static Structs measureXTokensYAddressesZWaitingCycles(int nTokens, int nAddr, int nWaitingCycles) {
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
                Token asset = tokens_i.get(j);
                tokenSizeSum += asset.getSize();
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
                Structs res = measureXTokensYAddressesZWaitingCycles(nTokens, nAddr, 0);
                System.out.printf("Full user storage =%d\n user storage=%d\n assets storage=%d\n", res.fullUserStorage, res.userStorage, res.assetsStorage);
            }
        }
        // todo, either create plot in Java or write results to a file and use Python 
    }

    public static Structs measureRandom(int nTokens, int nUsers, int nWaitingCycles, int nCycles) {
        setup(); // create initial cycle
        createUsers(nUsers);

        long popSizeSum = 0;
        long tokenSizeSum = 0;
        long addressProofSizeSum = 0;
        long MerkleProofSizesSum = 0;
        long userStorageSum = 0;
        long fullUserStorageSum = 0;
        long assetsStorageSum = 0;
        int nTrans = 0;

        ArrayList<ArrayList<Pair<Integer, Pair<String, String>>>> transactions = new ArrayList<>();
        HashMap<String, ArrayList<Token>> tokensForAddr = new HashMap<>();
        for (int c = 0; c < nCycles - 1; ++ c) {
            if (c + nWaitingCycles < nCycles - 1) {
                int nTransactions = Math.abs(rand.nextInt()) % nUsers + 1;
                // System.out.printf("Creating %d trans at cycle %d\n", nTransactions, c);
                // create nTransactions for cycle
                ArrayList<Pair<Integer, Pair<String, String>>> t_c = new ArrayList<>();

                nTrans += nTransactions;
                for (int i = 0; i < nTransactions; ++ i) {
                    int user_i = Math.abs(rand.nextInt()) % nUsers;
                    Owner a = users.get(user_i);
                    String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                    String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                    // create asset for user_i
                    int tokens_i = Math.abs(rand.nextInt()) % nTokens + 1;
                    tokensForAddr.put(addressA, new ArrayList<Token>());
                    for (int j = 0; j < tokens_i; ++ j) {
                        String certSignature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // certificate signature s((I_d, d), I)
                        // create asset for addressA, issuance cycle root C_.get(0), denominator j+1 and certSignature 
                        Token asset = a.createAsset(C_.get(c), addressA, j+1, certSignature);
                        String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // unblinded bsig for asset.fileKernel
                        asset.addSignature(signature);
                        tokensForAddr.get(addressA).add(asset);
                    }
                    t_c.add(new Pair<Integer, Pair<String, String>>(i, new Pair<String, String> (addressA, addressB)));
                }
                transactions.add(t_c);
            }
            if (c < nWaitingCycles) {
                MerkleTrie.TrieNode crtCycle = TestUtils.createRandomCycleTrie(r, Math.abs(rand.nextInt()) % nUsers + 1);
                C_.add(crtCycle.value);
            } else {
                // c >= nWaitingCycles
                ArrayList<Pair<Integer, Pair<String, String>>> t_c = transactions.get(c-nWaitingCycles);
                for (Pair<Integer, Pair<String, String>> t : t_c) {
                    // execute transaction made by user t_c.key
                    Owner a = users.get(t.key);
                    String addressA = t.value.key;
                    String addressB = t.value.value;
                    ArrayList<Token> crtTokens = tokensForAddr.get(addressA);
                    for (Token asset: crtTokens) {
                        a.transferAsset(C_.get(c), addressA, asset, addressB);
                    }
                    a.sendUpdates(C_.get(c), addressA);
                }

                MerkleTrie.TrieNode crtCycle = r.createCycleTrie();
                C_.add(crtCycle.value); // cycle c+1
                for (Pair<Integer, Pair<String, String>> t : t_c) {
                    // execute transaction made by user t_c.key
                    Owner a = users.get(t.key);
                    String addressA = t.value.key;
                    String addressB = t.value.value;
                    POPSlice popSlice_t = r.getPOPSlice(addressA, C_.get(c+1));
                    a.receivePOP(addressA, popSlice_t);
                    ArrayList<Token> tokens_i = tokensForAddr.get(addressA);
                    for (Token token_j : tokens_i) {
                        ArrayList<POPSlice> pop;
                        pop = a.getPOP(C_.get(c+1), addressA, token_j);
                        for (POPSlice popSlice: pop) {
                            popSizeSum += popSlice.getSize();
                            addressProofSizeSum += popSlice.addressProof.getSize();
                        }
                        tokenSizeSum += token_j.getSize();
                        MerkleProof proof = a.getFileProof(C_.get(c+1), addressA, Utils.convertKey(token_j.getFileId()));
                        MerkleProofSizesSum += proof.getSize();
                        String fileDetailHash = token_j.getFileDetail();
                        if (!proof.verify(Utils.convertKey(token_j.getFileId()), fileDetailHash)) {
                            throw new RuntimeException("Incorrect proof created!");
                        }
                    }
                }
            }
        }
        
        System.out.printf("POP avg size = %f\n token avg size = %f\n Merkle File Proofs avg size = %f\n Address Proof avg size = %f\n",
        (float)(popSizeSum / nTrans), 
        (float)(tokenSizeSum / nTrans), 
        (float)(MerkleProofSizesSum / nTrans),
        (float)(addressProofSizeSum / nTrans));
        Structs<Long> structs = new Structs<Long>((addressProofSizeSum / nTrans),
        (MerkleProofSizesSum / nTrans), 
        (popSizeSum / nTrans), 
        (tokenSizeSum / nTrans), 
        userStorageSum / nUsers, 
        fullUserStorageSum / nUsers, 
        assetsStorageSum / nUsers);
        
        return structs;
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
        measureRandom(4, 1024, 3, 20);
        measureForXWaitingCycles();
        System.out.println("Passed");
    }
}
