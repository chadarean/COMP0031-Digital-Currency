package com.mycompany.app.TODA;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import static com.mycompany.app.test.TestUtils.createRandomCycleTrie;
import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.options;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.port;


import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.StandardResponse;
import com.mycompany.app.StatusResponse;

public class Relay {
    public int NCycleTries = 0;
    public int lastCachedCycleTrieId = 1;
    public static final int cacheSize = 3600;
    public TreeMap<String, String> currentTransactions = new TreeMap<>();
    public HashMap<String, Integer> cycleId = new HashMap<>();
    public HashMap<Integer, String> cycleHash = new HashMap<>();
    public HashMap<Integer, MerkleTrie.TrieNode> cycleTrie = new HashMap<>();
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    RelayDB relayDB = new RelayDB();
    Connection c;


    public Relay() {
        c = RelayDB.connect();
        relayDB.deleteCycleTrieData(c);
        relayDB.deleteTransactinData(c);
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                createCycleTrie();
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public Relay(int delay, int time, TimeUnit unit) {
        c = RelayDB.connect();
        relayDB.deleteCycleTrieData(c);
        relayDB.deleteTransactinData(c);
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                createCycleTrie();
            }
        }, delay, time, unit);
    }

    public void addUpdateFromUpstream(String address, String updateHash) {
        System.out.println(updateHash);
    }

    public void addUpdateFromDownstream(String address, String updateHash) {
        //relayDB.insertTransaction(c, address, updateHash);
        currentTransactions.put(address, updateHash);
    }

    public void publishHash(String hash) {
        //TODO: publish hash on DLT
    }

    public ArrayList<Pair<String, String>> getTransactionsForCycleId(int cycleRootId) {
        return relayDB.selectAllTransactionsForCycle(c, cycleRootId);
    }

    public MerkleTrie.TrieNode constructCycleTrie(String cycleRoot) {
        Integer cycleRootId = cycleId.get(cycleRoot);
        if (cycleRootId == null) {
            return null;
        }
        MerkleTrie.TrieNode cachedRoot = cycleTrie.get(cycleRootId);
        if (cachedRoot != null) {
            return cachedRoot;
        }
        ArrayList<Pair<String, String>> pairs = getTransactionsForCycleId(cycleRootId);
        return MerkleTrie.createMerkleTrie(pairs); 
    }

    public ArrayList<Pair<String, String>>  getSortedTransactions() {
        // return getTransactionsForCycleId(relayDB.getMostRecentCycleId(c));
        ArrayList<Pair<String, String>> crtTransactions = new ArrayList<>();
        for (Map.Entry<String, String> entry : currentTransactions.entrySet()) {
            crtTransactions.add(new Pair<String, String>(entry.getKey(), entry.getValue()));
        }
        return crtTransactions;
    }

    public MerkleTrie.TrieNode createCycleTrie() {
        NCycleTries += 1;
        if (currentTransactions.size() == 0) {
            return null;
        }
        MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(getSortedTransactions()); 
        cycleId.put(root.value, NCycleTries);
        cycleHash.put(NCycleTries, root.value); 
        cycleTrie.put(NCycleTries, root);
        while (cycleTrie.size() >= cacheSize) {
            cycleTrie.remove(lastCachedCycleTrieId);
            lastCachedCycleTrieId += 1;
        }
        //TODO: check if memory problems occur due to cacheing
        // TODO: after deciding how/when relay creates the cycle trie, separate into different function
        // for (Pair<String, String> transaction : currentTransactions) {
        //     POPSlice crtPopSlice = getPOPSlice(transaction.key, root.value);
        //     //crtPopSlice.setUpdateHash(transaction.value);
        // }
        currentTransactions.clear();
        relayDB.insertNewCycleTrie(c);
        return root;
    }

    public POPSlice getPOPSlice(String address, String cycleRoot) {
        MerkleTrie.TrieNode root = constructCycleTrie(cycleRoot);
        MerkleProof addressProof = MerkleTrie.getMerkleProof(address, root);
        return new POPSlice(root.value, addressProof, null, null, null); 
    }

    public POPSlice getPOPSlice(String address, Integer cycleRootId) {
        MerkleTrie.TrieNode root = constructCycleTrie(cycleHash.get(cycleRootId));
        MerkleProof addressProof = MerkleTrie.getMerkleProof(address, root);
        return new POPSlice(root.value, addressProof, null, null, null); 
    }

    public ArrayList<POPSlice> getPOP(String address, String G_k, String G_n) { //*
        ArrayList<POPSlice> pop = new ArrayList<POPSlice>();
        int beginCycle = cycleId.get(G_k);
        int endCycle = cycleId.get(G_n);
        pop.add(getPOPSlice(address, G_k));
        for (int i = beginCycle+1; i < endCycle; ++ i) {
            pop.add(getPOPSlice(address, cycleHash.get(i)));
        }
        // pop.add(getPOPSlice(address, G_n)); it's assumed that the user has the POPSlice for the last cycle
        return pop;
    }

    public int getNoOfCyclesIssued() {
        return NCycleTries;
    }

    public MerkleTrie.TrieNode getMostRecentCycTrieNode() {
        return cycleTrie.get(NCycleTries);
    }

    public void closeConnection() {
        try {
            c.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args){
        Relay r = new Relay();
        MerkleTrie.TrieNode genesisCycleRoot = createRandomCycleTrie(r);


        port(8090);
        get("/Relay/getPOP/:stringAddress/:G_K:/:G_n:", (request, response) -> {
            response.type("application/json");

            return new Gson()
                    .toJson(new StandardResponse(StatusResponse.SUCCESS,new Gson().toJson(r.getPOP(request.attribute(":stringAddress"),request.attribute(":G_k"),request.attribute(":G_n")))));
        });
        get("/Relay/getPOPSlice/:stringAddress/:cycleRootId", (request, response) -> {
            response.type("application/json");

            return new Gson()
                    .toJson(new StandardResponse(StatusResponse.SUCCESS,new Gson().toJson(r.getPOPSlice(request.attribute(":stringAddress"),Integer.parseInt(request.attribute(":stringAddress"))))));
        });
        get("/Relay/getMostRecentCycleTrieNode", (request, response) -> {
            response.type("application/json");

            return new Gson()
                    .toJson(new StandardResponse(StatusResponse.SUCCESS,new Gson().toJson(r.getMostRecentCycTrieNode())));
        });
        post("/Relay/addUpdateFromDownstream/:stringAddress/:updateHash", (request, response) -> {
            response.type("application/json");
            try {
                r.addUpdateFromDownstream(request.attribute(":stringAddress"),request.attribute(":updateHash"));
                return new Gson()
                        .toJson(new StandardResponse(StatusResponse.SUCCESS, new Gson().toJson("Success")));
            }catch (Exception e){
                return new Gson()
                        .toJson(new StandardResponse(StatusResponse.ERROR, new Gson().toJson("Failed to add update")));
            }

        });

    }
}