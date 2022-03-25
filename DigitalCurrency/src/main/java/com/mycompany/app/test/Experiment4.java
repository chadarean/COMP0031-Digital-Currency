package com.mycompany.app.test;

import com.google.gson.Gson;
import com.mycompany.app.BlindSignature;
import com.mycompany.app.MSB.MSB;
import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.StandardResponse;
import com.mycompany.app.StatusResponse;
import com.mycompany.app.TODA.*;
import com.mycompany.app.Wallet;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static java.lang.Math.max;
import static spark.Spark.get;
import static spark.Spark.port;

public class Experiment4 {
    public static Random rand = new Random();
    static ArrayList<String> C_; // array of cycle hashes
    static Relay r; // TODO: create multiple relays for future tests

    public static String getMostRecentCycle() throws IOException {
        HttpGet request = new HttpGet("http://localhost:8090/Relay/getMostRecentCycleTrieNode");
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String crtCycleString = EntityUtils.toString(entity);
        MerkleTrie.TrieNode crtCycle = new Gson().fromJson(crtCycleString, MerkleTrie.TrieNode.class);
        return crtCycle.value;
    }

    public static void setup() throws IOException {
        C_ = new ArrayList<>();
        C_.add(getMostRecentCycle()); // add creation cycle hash
    }

    public static void tearDown() {
        //r.closeConnection();
    }

    public static void measureRandom(int nUsers, int nWaitingCycles, int nCycles, int maxWaitingCycles, boolean oneTransaction) throws IOException {
        setup();

        int nTrans = 0;
        int nTransRec = 0;

        ArrayList<ArrayList<OwnerThread>> transactingThreads = new ArrayList<>();
        // The monitored users will transact after maxWaitingCycles have passed
        // The first cycle for which tokens will be created is maxWaitingCycles - nWaitingCycles and
        // the last one is nCycles-nWaitingCycles-2 => nCycles-2-maxWaitingCycles cycles in total
        // Users will transact from maxWaitingCycles until C+maxWaitingCycles and create tokens

        for (int c = 0; c < nCycles - 1; ++ c) {
            if (c + nWaitingCycles < nCycles - 1 && c >= maxWaitingCycles - nWaitingCycles) {
                int nTransactions = Math.abs(TestUtils.getNextInt()) % (nUsers/nCycles) + 1;
                // create nTransactions for cycle c+1
                transactingThreads.add(new ArrayList<>());
                nTrans += nTransactions;

                // run nTransactions indep threads
                for (int i = 0; i < nTransactions; ++ i) {
                    // a user will do one transaction; "we don't expect multiple transactions to change the result as the speed load is on the relay"
                    OwnerThread user = new OwnerThread(Integer.toString(i));
                    user.setAddresses();
                    // create asset for user_i : move method into thread's run
                    user.createTokens(C_.get(c), user.pkeyA); // multiple machines:
                    transactingThreads.get(c-maxWaitingCycles+nWaitingCycles).add(user);
                }
            }
            if (c < maxWaitingCycles) {
                int nRandTrans = Math.abs(TestUtils.getNextInt()) % (nUsers/nCycles) + 1;
                MerkleTrie.TrieNode crtCycle = TestUtils.createRandomCycleTrie(r, nRandTrans);
                C_.add(crtCycle.value);
            } else {
                // c >= maxWaitingCycles
                ArrayList<OwnerThread> t_c = transactingThreads.get(c-maxWaitingCycles);

                for (OwnerThread user : t_c) {
                    user.transferTokens(user.pkeyA, user.pkeyB, C_.get(c));
                }

                HttpGet request = new HttpGet("http://localhost:8090/Relay/createCycleTrie");
                CloseableHttpClient client = HttpClients.createDefault();
                CloseableHttpResponse response = client.execute(request);
                HttpEntity entity = response.getEntity();
                String merkleTrieString = EntityUtils.toString(entity);

                MerkleTrie.TrieNode crtCycle = new Gson().fromJson(merkleTrieString, MerkleTrie.TrieNode.class);
                C_.add(crtCycle.value); // cycle c+1
                for (OwnerThread user : t_c) {
                    // execute transaction made by user t_c.key
                    user.getVerifyPOP(user.pkeyA, C_.get(c+1), TestUtils.getCycleId(C_.get(c+1)));
                    nTransRec += 1;
                }
            }
        }

        if (nTransRec != nTrans) {
            throw new RuntimeException("Not all transactions have been received!");
        }
        tearDown();
    }

    public static void measureRandomExperim(String fileName, int[] nAddrValues, int[] nWaitingCyclesValues, int maxWaitingCycles, int[] nCyclesValues, boolean oneTransaction) {
        try {
            PrintWriter results = new PrintWriter(fileName);
            for (int nAddrPerCycle:  nAddrValues) {
                for (int nWaitingCycles : nWaitingCyclesValues) {
                    for (int nCycles : nCyclesValues) {
                        System.out.println(nWaitingCycles);
                        TestUtils.resetState();
                        measureRandom(nAddrPerCycle * nCycles, nWaitingCycles, nCycles+maxWaitingCycles, maxWaitingCycles, oneTransaction);
                    }
                }
            }
            results.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        TestUtils.setRandomNumbers();
        for (int rep = 0; rep < 1; ++ rep) {
            System.out.printf("Time before rep %d:", rep);
            System.out.println(new Timestamp(System.currentTimeMillis()));
            //measureRandomExperim("varyWaitingCycles.txt", new int[]{512*16}, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, true);
            measureRandomExperim("varyWaitingCycles" + Integer.toString(rep) + ".txt",
                    new int[]{512}, new int[]{4},  8, new int[]{16}, true);
            System.out.printf("Time after rep %d:", rep);
            System.out.println(new Timestamp(System.currentTimeMillis()));
        }
//
//        port(3456);
//        TestUtils.setRandomNumbers();
//        get("/Experiment", (request, response) -> {
//            response.type("application/json");
//            int nReps = 1;
//            try{
//                /*
//                                measureRandomExperim("varyAddrSizes.txt", new int[]{128, 256}, new int[]{0}, false);
//                measureRandomExperim("varyWaitingCycles.txt", new int[]{512*4}, new int[]{0, 1, 2, 3, 4, 5, 6}, true);
//                 */
//                //measureRandomExperim("varyAddrSizes.txt", new int[]{128, 256, 512, 1024, 2048}, new int[]{0}, false);
//                for (int rep = 0; rep < nReps; ++ rep) {
//                    System.out.printf("Time before rep %d:", rep);
//                    System.out.println(new Timestamp(System.currentTimeMillis()));
//                    // Results for repetition rep, maximum 512 addresses transacting per cycle, 8 waiting cycles and 16+8 total cycles
//                    measureRandomExperim("varyWaitingCycles" + Integer.toString(rep) + ".txt",
//                            new int[]{512}, new int[]{8},  8, new int[]{16}, true);
//                    /*
//                    // Results for repetition rep, maximum 512 addresses transacting per cycle, 4 waiting cycles and 16+8 total cycles
//                    measureRandomExperim("varyWaitingCycles" + Integer.toString(rep) + ".txt",
//                            new int[]{512}, new int[]{4},  8, new int[]{16}, true);
//                    // Results for repetition rep, maximum 512 addresses transacting per cycle, 2 waiting cycles and 16+8 total cycles
//                    measureRandomExperim("varyWaitingCycles" + Integer.toString(rep) + ".txt",
//                            new int[]{512}, new int[]{2},  8, new int[]{16}, true);
//                    // Results for repetition rep, maximum 512 addresses transacting per cycle, 1 waiting cycles and 16+8 total cycles
//                    measureRandomExperim("varyWaitingCycles" + Integer.toString(rep) + ".txt",
//                            new int[]{512}, new int[]{1},  8, new int[]{16}, true);
//                    // Results for repetition rep, maximum 512 addresses transacting per cycle, 0 waiting cycles and 16+8 total cycles
//                    measureRandomExperim("varyWaitingCycles" + Integer.toString(rep) + ".txt",
//                            new int[]{512}, new int[]{0},  8, new int[]{16}, true);
//                     */
//
//                    System.out.printf("Time after rep %d:", rep);
//                    System.out.println(new Timestamp(System.currentTimeMillis()));
//                }
//
//
//
//                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,"Passed"));
//            }catch(Exception e){
//                return null;
//            }
//
//
//        });



    }
}
