package src.TODA;

import java.util.*;

public class Relay {
    protected void addUpdateFromUpstream() {
    }

    protected void addUpdateFromDownstream() {
    }

    protected void publishHash(String hash) {
    }

    protected ArrayList<Pair<String, String>> getUpdatedUSOs() {
        return new ArrayList<Pair<String, String>>(); // TODO: get updates as pair (address, txpx)
    }

    public MerkleTrie.TrieNode getCycleTrie() {
        ArrayList<Pair<String, String>> pairs = getUpdatedUSOs();
        return MerkleTrie.createMerkleTrie(pairs);
    }

    public static void main(String args[]) {
        List<Pair<String, String>> pairs_list = Arrays.asList(
                new Pair("0000000", "0"),
                new Pair("0000001", "1"),
                new Pair("0000100", "2"),
                new Pair("0000101", "3"),
                new Pair("0001100", "4"),
                new Pair("0011110", "5"),
                new Pair("0011111", "6"),
                new Pair("0100011", "7"),
                new Pair("0100111", "8"),
                new Pair("1000111", "9"));
        ArrayList<Pair<String, String>> pairs = new ArrayList<>();
        for (Pair<String, String> pair : pairs_list) {
            pairs.add(pair);
        }
        MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(pairs);
        System.out.println(root.value);
        for (Pair<String, String> p: pairs) {
            System.out.print("The value from trie for address " + p.key);
            System.out.print(" is " + MerkleTrie.findValueForAddress(p.key, root));
            System.out.println(" and the actual value is " + p.value);
        }
    }

    public String getCurrentCycleRoot();

    public ArrayList<Pair<String, String>> getPairsFromDB(String cycleRoot);

    public POPSlice computePOPSlice(String address, MerkleTrie.TrieNode root) {
        MerkleProof addressProof = new MerkleProof(getMerkleFrames(address, root));
        return POPSlice(root.value, addressProof, null, null, null); //TODO: is txpx created by relay or owner
    }

    public ArrayList<PopSlice> sendPOP(String address) {
        ArrayList<PopSlice> pop = new ArrayList<PopSlice>();
        String G_t = getCurrentCycleRoot();
        while (true) {
            ArrayList<Pair<String, String>> pairs = getPairsFromDB(G_t);
            MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(pairs);
            popSlice = computePOPSlice(address, root)
            pop.append(popSlice);
            if (!popSlice.addressProof.nullProof) {
                break;
            }
            // TODO: how to link G_T with previous cycle root?
        }
        return pop;
    }
}