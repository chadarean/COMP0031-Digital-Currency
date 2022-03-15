package com.mycompany.app.test;

import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.TODA.*;

import java.io.PrintWriter; 
import java.io.IOException;  
import java.lang.Math;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class MeasurePOPSize {
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

        public void add(Structs oth) {
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
        // System.out.println("initial cycle " + C_.get(0));
        // System.out.println(r.cycleId.get(C_.get(0)));
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
        Structs structs = new Structs(((double)addressProofSizeSum / nAddrPfoofs),
                ((double)fileProofSizesSum / nFileProofs),
                ((double)popSizeSum / nTokens) / nAddr,
                ((double)tokenSizeSum / nTokens) / nAddr,
                (double)userStorageSum / nAddr,
                (double)fullUserStorageSum / nAddr,
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

    public static Structs measureRandom(int nTokens, int nUsers, int nWaitingCycles, int nCycles, boolean oneTransaction) {
        setup(); // create initial cycle
        createUsers(nUsers);

        HashMap<Integer, Integer> transactingUser = new HashMap<>();

        long popSizeSum = 0;
        long tokenSizeSum = 0;
        long addressProofSizeSum = 0;
        long fileProofSizesSum = 0;
        long userStorageSum = 0;
        long fullUserStorageSum = 0;
        long assetsStorageSum = 0;
        int nTrans = 0;
        int nTokensTrans = 0;
        int nAddrProofs = 0;
        int nFileProofs = 0;
        int nTransRec = 0;

        ArrayList<ArrayList<Pair<Integer, Pair<String, String>>>> transactions = new ArrayList<>();
        HashMap<String, ArrayList<Token>> tokensForAddr = new HashMap<>();
        for (int c = 0; c < nCycles - 1; ++ c) {
            if (c + nWaitingCycles < nCycles - 1) {
                int nTransactions = Math.abs(TestUtils.getNextInt()) % (nUsers/nCycles) + 1;
                //System.out.printf("Creating %d trans at cycle %d\n", nTransactions, c);
                // create nTransactions for cycle
                transactions.add(new ArrayList<>());

                nTrans += nTransactions;
                for (int i = 0; i < nTransactions; ++ i) {
                    int user_i = Math.abs(TestUtils.getNextInt()) % nUsers;
                    if (oneTransaction) {
                        while (transactingUser.containsKey(user_i)) {
                            user_i = Math.abs(TestUtils.getNextInt()) % nUsers;
                        }
                        transactingUser.put(user_i, -1);
                    }
                    Owner a = users.get(user_i);
                    String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                    String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                    // create asset for user_i
                    int tokens_i = Math.abs(TestUtils.getNextInt()) % nTokens + 1;

                    tokensForAddr.put(addressA, new ArrayList<Token>());
                    for (int j = 0; j < tokens_i; ++j) {
                        String certSignature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // certificate signature s((I_d, d), I)
                        // create asset for addressA, issuance cycle root C_.get(0), denominator j+1 and certSignature 
                        Token asset = a.createAsset(C_.get(c), addressA, j + 1, certSignature);
                        String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // unblinded bsig for asset.fileKernel
                        asset.addSignature(signature);
                        tokensForAddr.get(addressA).add(asset);
                    }

                    transactions.get(c).add(new Pair<Integer, Pair<String, String>>(user_i, new Pair<String, String>(addressA, addressB)));
                }
            }
            if (c < nWaitingCycles) {
                MerkleTrie.TrieNode crtCycle = TestUtils.createRandomCycleTrie(r, Math.abs(TestUtils.getNextInt()) % nUsers + 1);
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

                    ++ nTransRec;
                    int pops = 0;
                    ArrayList<Token> tokens_i = tokensForAddr.get(addressA);
                    for (Token token_j : tokens_i) {
                        ArrayList<POPSlice> pop;
                        pop = a.getPOPUsingCache(C_.get(c+1), addressA, token_j);
                        nTokensTrans += 1;
                        for (POPSlice popSlice: pop) {
                            if (popSlice.getSize() != a.addressToPOPSlice.get(a.relay.cycleId.get(popSlice.cycleRoot)).get(addressA).getSize()) {
                                throw new RuntimeException("Incorrect!");
                            }
//                            System.out.printf("pop for cycle %s and slice_sz=%d cache_slice_sz=%d\n", a.relay.cycleId.get(popSlice.cycleRoot),
//                                    popSlice.getSize(), a.addressToPOPSlice.get(a.relay.cycleId.get(popSlice.cycleRoot)).get(addressA).getSize());
                            //System.out.printf("cycle1 = %d cycle2 = %d", a.relay.cycleId.get(popSlice.cycleRoot), )
                            popSizeSum += popSlice.getSize();
                            nAddrProofs += 1;
                            addressProofSizeSum += popSlice.addressProof.getSize();
                        }
                        pops += pop.size();
                        tokenSizeSum += token_j.getSize();
                        MerkleProof proof = a.getFileProof(C_.get(c+1), addressA, Utils.convertKey(token_j.getFileId()));
                        fileProofSizesSum += proof.getSize();
                        nFileProofs += 1;
                        String fileDetailHash = token_j.getFileDetail();
                        if (!proof.verify(Utils.convertKey(token_j.getFileId()), fileDetailHash)) {
                            throw new RuntimeException("Incorrect proof created!");
                        }
                    }
                    transactingUser.put(t.key, a.addressToPOPSlice.size());
//                    if (a.addressToPOPSlice.size() < pops) {
//                        throw new RuntimeException(Integer.toString(a.addressToPOPSlice.size()));
//                    }
                }
            }
        }

        int nActiveUsers = 0;
        for (HashMap.Entry<Integer, Integer> entry: transactingUser.entrySet()) {
            if (entry.getValue() == -1) {
                throw new RuntimeException("wtf");
            }
            //System.out.printf("userid=%d val1=%d exp=%d\n", entry.getKey(), entry.getValue(), users.get(entry.getKey()).addressToPOPSlice.size());
        }
        for (Owner a: users) {
            if (a.addressToPOPSlice.size() > 0) {
                ++ nActiveUsers;
                userStorageSum += a.getSize(false);
                fullUserStorageSum += a.getSize(true);
                assetsStorageSum += a.getAssetsSize();
            }
        }

        Structs structs = new Structs(((double)addressProofSizeSum / nAddrProofs),
                ((double)fileProofSizesSum / nFileProofs),
                ((double)popSizeSum / nTokensTrans),
                ((double)tokenSizeSum / nTrans),
                (double)userStorageSum / nActiveUsers,
                (double)fullUserStorageSum / nActiveUsers,
                (double)assetsStorageSum / nActiveUsers);

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

    public static void measureRandomExperim(String fileName, int nTokenValues[], int nAddrValues[], int nWaitingCyclesValues[], boolean oneTransaction) {
//       {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        int nCyclesValues[] = {33};
        try {
            PrintWriter results = new PrintWriter(fileName);
            for (int nTokens: nTokenValues) {
                for (int nAddr:  nAddrValues) {
                    for (int nWaitingCycles : nWaitingCyclesValues) {
                        for (int nCycles : nCyclesValues) {
                            TestUtils.resetState();
                            Structs res = measureRandom(nTokens, nAddr, nWaitingCycles, nCycles, oneTransaction);
                            for (int reps = 1; reps < 5; ++ reps) {
                                res.add(measureRandom(nTokens, nAddr, nWaitingCycles, nCycles, oneTransaction));
                            }
                            res.divBy(5);
                            results.printf("%d %d %d %d %f %f %f %f %f\n", nTokens, nAddr, nWaitingCycles, nCycles, res.addressProofSize, res.fileProofSize, res.popSize, res.userStorage, res.fullUserStorage);
                            System.out.printf
                                    ("address proof avg sz = %f \n file proof avg sz = %f \n pop avg sz = %f\nuser storage avg = %f\n+cache storage = %f\n",
                                            res.addressProofSize, res.fileProofSize, res.popSize, res.userStorage, res.fullUserStorage);
                        }
                    }
                }
            }
            results.close();
        } catch (IOException e) {
            System.out.println("IO Error");
        }
    }

    public static void main(String[] args) {
        TestUtils.setRandomNumbers();
        //measureRandomExperim("varyTokenSizes.txt", new int[]{1, 2, 4, 8, 16}, new int[]{512}, new int[]{0}, false);
        //measureRandomExperim("varyAddrSizes.txt", new int[]{8}, new int[]{128, 256, 512, 1024, 2048}, new int[]{0}, false);
        measureRandomExperim("varyWaitingCycles.txt", new int[]{8}, new int[]{512*33}, new int[]{16}, true);
        //measureForXWaitingCycles();
        System.out.println("Passed");
    }
}