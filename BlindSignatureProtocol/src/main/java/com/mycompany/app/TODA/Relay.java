package com.mycompany.app.TODA;

import com.mycompany.app.POP.*;
import java.util.*;

public class Relay {
    public int NCycleTries = 0;
    public void addUpdateFromUpstream(String address, String updateHash) {
        // calls insertTransaction(Connection conn, String addressOfAsset=address, String hashOfUpdate=updateHash)
    }

    public void addUpdateFromDownstream(String updateHash) {
    }

    public void publishHash(String hash) {
        //TODO: publish hash on DLT
    }

    public ArrayList<Pair<String, String>> getUpdatedUSOs() {
        // calls selectAllTransactions(Connection conn, NCycleTries) for the transactions not included in any CT
        return new ArrayList<Pair<String, String>>(); 
    }

    public MerkleTrie.TrieNode createCycleTrie() {
        ArrayList<Pair<String, String>> pairs = getUpdatedUSOs();
        NCycleTries += 1;
        return MerkleTrie.createMerkleTrie(pairs); //TODO: store in cache most recent cycle trie objects; check if memory problems occur
    }

    public static void main(String args[]) {
        ArrayList<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>(Arrays.asList(
                    new Pair<String, String>("0000", "V"),
                    new Pair<String, String>("0001", "W"),
                    new Pair<String, String>("0010", "X"),
                    new Pair<String, String>("0100", "Y"),
                    new Pair<String, String>("0101", "Z")));
        
        MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(pairs);
        System.out.println(root.value);
        for (Pair<String, String> p: pairs) {
            System.out.print("The value from trie for address " + p.key);
            System.out.print(" is " + MerkleTrie.findValueForAddress(p.key, root));
            System.out.println(" and the actual value is " + p.value);
        }
    }

    public ArrayList<Pair<String, String>> getPairsFromDB(String cycleId) {
        // gets the transactions from cycle trie with cycleId
        return null;
    }

    public POPSlice computePOPSlice(String address, MerkleTrie.TrieNode root) {
        MerkleProof addressProof = MerkleTrie.getMerkleProof(address, root);
        return new POPSlice(root.value, addressProof, null, null, null); 
    }

    public ArrayList<POPSlice> sendPOP(String address, String G_k, String G_n) {
        ArrayList<POPSlice> pop = new ArrayList<POPSlice>();
        String G_t = G_n;
        while (!G_t.equals(G_k)) {
            ArrayList<Pair<String, String>> pairs = getPairsFromDB(G_t);
            MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(pairs);
            POPSlice popSlice = computePOPSlice(address, root);
            pop.add(popSlice);
        }
        return pop;
    }
}