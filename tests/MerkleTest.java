package tests;
import java.util.*;
import src.TODA.*;

public class MerkleTest {
    public static void MerkleTreeTest(ArrayList<Pair<String, String>> pairs) {
        MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(pairs);
        for (Pair<String, String> p: pairs) {
            String trieValue = MerkleTrie.findValueForAddress(p.key, root);
            if (trieValue != p.value) {
                throw new RuntimeException("Incorrect address stored!");
            }
           
            MerkleProof proof = MerkleTrie.getMerkleProof(p.key, root);
            if (!proof.verify(p.key, p.value)) {
                throw new RuntimeException("Incorrect Merkle proof!");
            }
            
            MerkleProof.Frame changed_frame = proof.frames.get((proof.frames.size() == 1) ? 0 : proof.frames.size() - 2);
            // change frame to get incorrect results
            changed_frame.rightBranchHash = Utils.getHash(changed_frame.leftBranchHash);
            
            if (proof.verify(p.key, p.value)) {
                throw new RuntimeException("Incorrect Merkle proof!");
            }
            //System.out.println(proof.verify(p.key, p.value));
            //System.out.println(p.key + " has value=" + p.value + " and trie value=" + trieValue);
        }
    }

    public static void MerkleTreeTests() {
        ArrayList<Pair<String, String>> pairs1 = new ArrayList<>((List<Pair<String, String>>)
        Arrays.asList(
                new Pair<String, String>("0000000", "0"),
                new Pair<String, String>("0000001", "1"),
                new Pair<String, String>("0000100", "2"),
                new Pair<String, String>("0000101", "3"),
                new Pair<String, String>("0001100", "4"),
                new Pair<String, String>("0011110", "5"),
                new Pair<String, String>("0011111", "6"),
                new Pair<String, String>("0100011", "7"),
                new Pair<String, String>("0100111", "8"),
                new Pair<String, String>("1000111", "9")));
            
        MerkleTreeTest(pairs1);
        ArrayList<Pair<String, String>> pairs2 = new ArrayList<Pair<String, String>>(Arrays.asList(
                    new Pair<String, String>("0000", "V"),
                    new Pair<String, String>("0001", "W"),
                    new Pair<String, String>("0010", "X"),
                    new Pair<String, String>("0100", "Y"),
                    new Pair<String, String>("0101", "Z")));
        MerkleTreeTest(pairs2);
    }

    public static void main(String args[]) {
        MerkleTreeTests();
    }

}
