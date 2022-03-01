package tests;

import java.util.*;
import java.sql.Timestamp;
import src.TODA.*;

public class MerkleTest {
    public static void MerkleTreeTest(ArrayList<Pair<String, String>> pairs, ArrayList<String> idleAddresses) {
        MerkleTrie.TrieNode root = MerkleTrie.createMerkleTrie(pairs);
        System.out.println("Time after Merkle Trie is created:");
        System.out.println(new Timestamp(System.currentTimeMillis()));
        for (String idleAddress : idleAddresses) {
            if (MerkleTrie.findValueForAddress(idleAddress, root) != null && 
            !MerkleTrie.findValueForAddress(idleAddress, root).equals(Utils.getHash(null))) {
                throw new RuntimeException("Address not stored in trie but has path!");
            }
            MerkleProof proof_for_idle = MerkleTrie.getMerkleProof(idleAddress, root);
            if (!proof_for_idle.null_proof) {
                throw new RuntimeException("Found non-null proof for idle address!");
            }
            if (!proof_for_idle.verify(idleAddress, null)) {
                throw new RuntimeException("Invalid null proof!" + idleAddress);
            }
            proof_for_idle.null_proof = false;
            if (proof_for_idle.verify(idleAddress, null)) {
                throw new RuntimeException("Invalid proof for null is considered correct!");
            }
        }
    
        for (Pair<String, String> p: pairs) {
            String trieValue = MerkleTrie.findValueForAddress(p.key, root);
            if (trieValue != p.value) {
                throw new RuntimeException("Incorrect address stored!");
            }
           
            MerkleProof proof = MerkleTrie.getMerkleProof(p.key, root);
            if (!proof.leafHash.equals(p.value)) {
                throw new RuntimeException("Incorrect Leaf hash!");
            }
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
                new Pair<String, String>("0000100", "2"), // lcp = 4
                new Pair<String, String>("0000101", "3"), // lcp = 6
                new Pair<String, String>("0001100", "4"), // lcp = 3
                new Pair<String, String>("0011110", "5"), // lcp = 2
                new Pair<String, String>("0011111", "6"), // lcp = 6
                new Pair<String, String>("0100011", "7"), // lcp = 1
                new Pair<String, String>("0100111", "8"), // lcp = 4
                new Pair<String, String>("1000111", "9"))); // lcp = 0
            
        MerkleTreeTest(pairs1, new ArrayList<>((List<String>)Arrays.asList("0100010", "1000100", "1001111", "0001000")));
        ArrayList<Pair<String, String>> pairs2 = new ArrayList<Pair<String, String>>(Arrays.asList(
                    new Pair<String, String>("0000", "V"),
                    new Pair<String, String>("0001", "W"),
                    new Pair<String, String>("0010", "X"),
                    new Pair<String, String>("0100", "Y"),
                    new Pair<String, String>("0101", "Z")));

                    ArrayList<Pair<String, String>> pairs3 = new ArrayList<>((List<Pair<String, String>>)
                    Arrays.asList(
                            new Pair<String, String>("0000000000000000000011", "0"),
                            new Pair<String, String>("0000000100000000000011", "1"),
                            new Pair<String, String>("0000010000000000000011", "2"),
                            new Pair<String, String>("0000010100000000000011", "3"),
                            new Pair<String, String>("1000110000000000000011", "4"),
                            new Pair<String, String>("1001111000000000000011", "5"),
                            new Pair<String, String>("1001111100000000000011", "6"),
                            new Pair<String, String>("1010001100000000000011", "7"),
                            new Pair<String, String>("1010011100000000000011", "8"),
                            new Pair<String, String>("1100011100000000000011", "9")));
                       
                    MerkleTreeTest(pairs3, new ArrayList<>((List<String>)Arrays.asList("1010001000000000000011",
                    "1100010000000000000011", "1100111100000000000011", "0000100000000000000011", "0000000000000000000010")));
    }

    public static String getRandomXBitAddr(Random rand, int addrSize) {
        StringBuilder addr = new StringBuilder();
        for (int j = 0; j < MerkleTrie.ADDRESS_SIZE; ++ j) {
            addr.append(Integer.toString(rand.nextInt(2)));
        }
        return addr.toString();
    }

    public static void randomTest(int numAddresses) {
        Random rand = new Random();
        HashMap <String, Boolean> usedAddresses = new HashMap<>();
        ArrayList<Pair<String, String>> updates = new ArrayList<>();
        ArrayList<String> addresses = new ArrayList<>();
        ArrayList<String> idleAddresses = new ArrayList<>();
        for (int i = 0; i < numAddresses; ++ i) {
            String addrString;
            while (true) {
                addrString = getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                if (!usedAddresses.containsKey(addrString)) {
                    break;
                }
            }
            usedAddresses.put(addrString, true);
            if (i % 2 == 0) {
                idleAddresses.add(addrString);
            } else {
                addresses.add(addrString); //(new Pair<String, String>(addrString, Integer.toString(i)));
            }
        }
        Collections.sort(addresses);
        for (String addr : addresses) {
            updates.add(new Pair<String, String>(addr, Utils.getHash(getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE))));
        }
        System.out.println("Time before:");
        System.out.println(new Timestamp(System.currentTimeMillis()));
        MerkleTreeTest(updates, idleAddresses);
        System.out.println("Time after verifying all proofs:");
        System.out.println(new Timestamp(System.currentTimeMillis()));
    }

    public static void main(String args[]) {
        MerkleTreeTests();
        for (int j = 1; j < 10; ++ j)
        for (int i = 2; i < 20; i += 2) {
            randomTest(i);
        }
        randomTest(20);
        randomTest(2000);
        randomTest(20000);
        randomTest(200000);
        // // randomTest(2000000);
       System.out.println("All tests passed!");
    }

}
