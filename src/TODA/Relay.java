package src.TODA;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import src.POP.POPSlice;

public class Relay {
    public int NCycleTries = 0;
    public int lastCachedCycleTrieId = 1;
    public static final int cacheSize = 3600;
    public TreeMap<String, String> currentTransactions = new TreeMap<>();
    public HashMap<String, Integer> cycleId = new HashMap<>();
    public HashMap<Integer, String> cycleHash = new HashMap<>();
    public HashMap<Integer, MerkleTrie.TrieNode> cycleTrie = new HashMap<>();
    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);


    public Relay() {
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                // calls insertNewCycleTrie(Connection conn)
                createCycleTrie();
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    public Relay(int delay, int time, TimeUnit unit) {
        executorService.scheduleAtFixedRate(new Runnable() {
            public void run() {
                // calls insertNewCycleTrie(Connection conn)
                createCycleTrie();
            }
        }, delay, time, unit);
    }

    public void addUpdateFromUpstream(String address, String updateHash) {
    }

    public void addUpdateFromDownstream(String address, String updateHash) {
        // calls insertTransaction(Connection conn, String addressOfAsset=address, String hashOfUpdate=updateHash)
        currentTransactions.put(address, updateHash);
    }

    public void publishHash(String hash) {
        //TODO: publish hash on DLT
    }

    public ArrayList<Pair<String, String>> getTransactionsForCycleId(int cycleRootId) {
        // calls selectAllTransactions(Connection conn, NCycleTries) for the transactions included in CT with id cycleRootId
        return new ArrayList<Pair<String, String>>(); 
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
        ArrayList<Pair<String, String>> crtTransactions = new ArrayList<>();
        for (Map.Entry<String, String> entry : currentTransactions.entrySet()) {
            crtTransactions.add(new Pair<String, String>(entry.getKey(), entry.getValue()));
        }
        //Collections.sort(crtTransactions);
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

    public ArrayList<POPSlice> getPOP(String address, String G_k, String G_n) {
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
}