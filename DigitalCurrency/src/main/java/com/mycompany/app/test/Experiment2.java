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

public class Experiment2 {
    public static Random rand = new Random();
    static ArrayList<String> C_; // array of cycle hashes
    static Relay r; // TODO: create multiple relays for future tests
    static MerkleTrie.TrieNode initialCycle;
    static ArrayList<ArrayList<Token>> tokens;
    static ArrayList<Owner> users;
    static ArrayList<Pair<String, String>> transactions; // transactions.get(i) = <souceAddr, destAddr> for transaction made by user i

    public static void setup() {
        tokens = new ArrayList<>();
        users = new ArrayList<>();
        transactions = new ArrayList<>();
        C_ = new ArrayList<>();
        r = new Relay(1, 1, TimeUnit.DAYS); // TODO: change user cadence
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

    public static void measureRandom(int nUsers, int nWaitingCycles, int nCycles, boolean oneTransaction) {
        setup(); // create initial cycle
        createUsers(nUsers);

        HashMap<Integer, Integer> transactingUser = new HashMap<>();

        int nTrans = 0;
        int nTransRec = 0;

        ArrayList<ArrayList<Pair<Integer, Pair<String, String>>>> transactions = new ArrayList<>();
        HashMap<String, ArrayList<Token>> tokensForAddr = new HashMap<>();
        ArrayList<Integer> tokensInFlight = new ArrayList<>();

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
                    int tokens_i = 1; // only 1 token per address
                    tokensForAddr.put(addressA, new ArrayList<Token>());
                    for (int j = 0; j < tokens_i; ++j) {
                        String certSignature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // certificate signature s((I_d, d), I)
                        // create asset for addressA, issuance cycle root C_.get(c), denominator j+1 and certSignature
                        Token asset = a.createAsset(C_.get(c), addressA, j + 1, certSignature); // TODO: how to get c from relay/MSB
                        // TODO: get the unblinded blind signature on assest.getFileId()
                        String signature = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE); // unblinded bsig for asset.fileKernel
                        asset.addSignature(signature);
                        tokensForAddr.get(addressA).add(asset);
                    }

                    transactions.get(c).add(new Pair<Integer, Pair<String, String>>(user_i, new Pair<String, String>(addressA, addressB)));
                }
            }
            if (c < nWaitingCycles) {
                int nRandTrans = Math.abs(TestUtils.getNextInt()) % (nUsers/nCycles) + 1;
                MerkleTrie.TrieNode crtCycle = TestUtils.createRandomCycleTrie(r, nRandTrans);
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
                    ArrayList<Token> tokens_i = tokensForAddr.get(addressA);
                    for (Token token_j : tokens_i) {
                        ArrayList<POPSlice> pop;
                        pop = a.getPOPUsingCache(C_.get(c+1), addressA, token_j);
                        MerkleProof proof = a.getFileProof(C_.get(c+1), addressA, Utils.convertKey(token_j.getFileId()));
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
        tearDown();
    }

    public static void measureRandomExperim(String fileName, int nAddrValues[], int nWaitingCyclesValues[], boolean oneTransaction) {
        int[] nCyclesValues = {33};
        try {
            PrintWriter results = new PrintWriter(fileName);
                for (int nAddr:  nAddrValues) {
                    for (int nWaitingCycles : nWaitingCyclesValues) {
                        for (int nCycles : nCyclesValues) {
                            TestUtils.resetState();
                            measureRandom(nAddr, nWaitingCycles, nCycles, oneTransaction);
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
        measureRandomExperim("varyAddrSizes.txt", new int[]{128, 256, 512, 1024, 2048}, new int[]{0}, false);
        measureRandomExperim("varyWaitingCycles.txt", new int[]{512*33}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, true);
        System.out.println("Passed");
    }
}
