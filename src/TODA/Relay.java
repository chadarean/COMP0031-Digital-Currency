package src.TODA;

import src.POP.*;
import java.util.*;

public class Relay {
    public void addUpdateFromUpstream(String address, String updateHash) {
    }

    public void addUpdateFromDownstream(String updateHash) {
    }

    public void publishHash(String hash) {
        //TODO: publish hash on DLT
    }

    public ArrayList<Pair<String, String>> getUpdatedUSOs() {
        return new ArrayList<Pair<String, String>>(); // TODO: get updates as pair (address, txpx)
    }

    public MerkleTrie.TrieNode getCycleTrie() {
        ArrayList<Pair<String, String>> pairs = getUpdatedUSOs();
        return MerkleTrie.createMerkleTrie(pairs);
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

    public String getCurrentCycleRoot() {
        return "";
    }

    public ArrayList<Pair<String, String>> getPairsFromDB(String cycleRoot) {
        return null;
    }

    public POPSlice computePOPSlice(String address, MerkleTrie.TrieNode root) {
        MerkleProof addressProof = MerkleTrie.getMerkleProof(address, root);
        return new POPSlice(root.value, addressProof, null, null, null); //TODO: is txpx created by relay or owner
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