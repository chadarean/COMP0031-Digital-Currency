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
        ArrayList<Integer> tokensInFlight = new ArrayList<>();

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

        public void addAnonimitySet(ArrayList<Integer> tokensInFlight) {
            this.tokensInFlight.addAll(tokensInFlight);
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

        C_.add(initialCycle.value); // add creation cycle hash
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

    public static Structs measureRandom(int nTokens, int nUsers, int nWaitingCycles, int nCycles, boolean oneTransaction) throws IOException {
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
        ArrayList<Integer> tokensInFlight = new ArrayList<>();
        tokensInFlight.add(0);
        int crtSet = 0;
        for (int c = 0; c < nCycles - 1; ++ c) {
            if (c + nWaitingCycles < nCycles - 1) {
                int nTransactions = Math.abs(TestUtils.getNextInt()) % (nUsers/nCycles) + 1;
                //System.out.printf("Creating %d trans at cycle %d\n", nTransactions, c);
                // create nTransactions for cycle c+1
                transactions.add(new ArrayList<>());

                nTrans += nTransactions;

                for (int i = 0; i < nTransactions; ++ i) {
                    int user_i = Math.abs(TestUtils.getNextInt()) % nUsers; // get random user id
                    if (oneTransaction) {
                        // A user can only transact once
                        while (transactingUser.containsKey(user_i)) {
                            user_i = Math.abs(TestUtils.getNextInt()) % nUsers;
                        }
                        transactingUser.put(user_i, -1);
                    }
                    Owner a = users.get(user_i);
                    String addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                    String addressB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                    // create asset for user_i
                    int tokens_i = Math.abs(TestUtils.getNextInt()) % nTokens + 1; // generate number of tokens (default=1)

                    tokensForAddr.put(addressA, new ArrayList<Token>());
                    for (int j = 0; j < tokens_i; ++j) {
                        String certSignature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // certificate signature s((I_d, d), I)
                        // create asset for addressA, issuance cycle root C_.get(c), denominator j+1 and certSignature
                        Token asset = a.createAsset(C_.get(c), addressA, j + 1, certSignature);
                        String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // unblinded bsig for asset.fileKernel
                        asset.addSignature(signature);
                        tokensForAddr.get(addressA).add(asset);
                    }

                    transactions.get(c).add(new Pair<Integer, Pair<String, String>>(user_i, new Pair<String, String>(addressA, addressB)));
                }
            }
            if (c < nWaitingCycles) {
                int nRandTrans = Math.abs(TestUtils.getNextInt()) % (nUsers/nCycles) + 1;
                tokensInFlight.set(crtSet, tokensInFlight.get(crtSet) + nRandTrans);
                MerkleTrie.TrieNode crtCycle = TestUtils.createRandomCycleTrie(r, nRandTrans);
                C_.add(crtCycle.value);
            } else {
                // c >= nWaitingCycles
                ArrayList<Pair<Integer, Pair<String, String>>> t_c = transactions.get(c-nWaitingCycles);
                tokensInFlight.add(tokensInFlight.get(crtSet));
                tokensInFlight.set(crtSet, tokensInFlight.get(crtSet) + t_c.size());

                crtSet += 1;
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
                    ArrayList<Token> tokens_i = tokensForAddr.get(addressA);
                    for (Token token_j : tokens_i) {
                        ArrayList<POPSlice> pop;
                        pop = a.getPOPUsingCache(C_.get(c+1), addressA, token_j);
                        nTokensTrans += 1;
                        for (POPSlice popSlice: pop) {
                            if (popSlice.getSize() != a.addressToPOPSlice.get(r.cycleId.get(popSlice.cycleRoot)).get(addressA).getSize()) {
                                throw new RuntimeException("Cache data doesn't match relay data!");
                            }

                            popSizeSum += popSlice.getSize();
                            nAddrProofs += 1;
                            addressProofSizeSum += popSlice.addressProof.getSize();
                        }
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
                }
            }
        }

        if (nTransRec != nTrans) {
            throw new RuntimeException("Not all transactions have been received!");
        }

        int nActiveUsers = 0;
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
        structs.addAnonimitySet(tokensInFlight);
        tearDown();
        return structs;
    }

    public static void measureRandomExperim(String fileName, int nTokenValues[], int nAddrValues[], int nWaitingCyclesValues[], boolean oneTransaction) {
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
                            System.out.println(nWaitingCycles);
                            results.printf("%d %d %d %d %f %f %f %f %f %f %f\n", nTokens, nAddr, nWaitingCycles, nCycles, res.addressProofSize,
                                    res.fileProofSize, res.popSize, res.userStorage, res.fullUserStorage,
                                    Utils.getMean(res.tokensInFlight), Utils.getStdev(res.tokensInFlight));
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
        measureRandomExperim("varyAddrSizes.txt", new int[]{1}, new int[]{128, 256, 512, 1024, 2048}, new int[]{0}, false);
        //measureRandomExperim("varyWaitingCycles.txt", new int[]{1}, new int[]{512*33}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, true);
        System.out.println("Passed");
    }
}