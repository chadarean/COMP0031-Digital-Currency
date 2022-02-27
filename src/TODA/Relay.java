package src.TODA;

import src.POP.*;
import java.util.*;

public class Relay {
    public int NCycleTries = 0;
    public int lastCachedCycleTrieId = 1;
    public static final int cacheSize = 3600;
    public ArrayList<Pair<String, String>> currentTransactions = new ArrayList<>();
    public HashMap<String, Integer> cycleId = new HashMap<>();
    public HashMap<Integer, String> cycleHash = new HashMap<>();
    public HashMap<Integer, MerkleTrie.TrieNode> cycleTrie = new HashMap<>();

    public void addUpdateFromUpstream(String address, String updateHash) {
        // calls insertTransaction(Connection conn, String addressOfAsset=address, String hashOfUpdate=updateHash)
    }

    public void addUpdateFromDownstream(String address, String updateHash) {
        currentTransactions.add(new Pair<String, String>(address, updateHash));
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

    public MerkleTrie.TrieNode createCycleTrie() {
        NCycleTries += 1;
        MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(currentTransactions); 
        cycleId.put(root.value, NCycleTries);
        cycleHash.put(NCycleTries, root.value);
        cycleTrie.put(NCycleTries, root);
        while (cycleTrie.size() >= cacheSize) {
            cycleTrie.remove(lastCachedCycleTrieId);
            lastCachedCycleTrieId += 1;
        }
        //TODO: check if memory problems occur due to cacheing
        return root;
    }

    public POPSlice getPOPSlice(String address, String cycleRoot) {
        MerkleTrie.TrieNode root = constructCycleTrie(cycleRoot);
        MerkleProof addressProof = MerkleTrie.getMerkleProof(address, root);
        return new POPSlice(root.value, addressProof, null, null, null); 
    }

    // public ArrayList<POPSlice> sendPOP(String address, String G_k, String G_n) {
    //     ArrayList<POPSlice> pop = new ArrayList<POPSlice>();
    //     String G_t = G_n;
    //     while (!G_t.equals(G_k)) {
    //         POPSlice popSlice = getPOPSlice(address, G_t);
    //         pop.add(popSlice);
    //     }
    //     return pop;
    // }
}