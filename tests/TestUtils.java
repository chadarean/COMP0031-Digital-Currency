package tests;

import java.util.*;
import src.TODA.*;

public class TestUtils {
    public static Random rand = new Random();

    public static int randState = 0;

    public static int maxRandNumbers = 10000000;

    public static ArrayList<Integer> randNumbers;

    public static void resetState() {
        randState = 0;
    }

    public static void setRandomNumbers() {
        randNumbers = new ArrayList<>();
        for (int i = 0; i < maxRandNumbers; ++ i) {
            randNumbers.add(rand.nextInt());
        }
    }

    public static int getNextInt() {
        randState += 1;
        return randNumbers.get(randState-1);
    }
    
    public static String getRandomXBitAddr(Random rand, int addrSize) {
        StringBuilder addr = new StringBuilder();
        for (int j = 0; j < MerkleTrie.ADDRESS_SIZE; ++ j) {
            addr.append(Integer.toString(rand.nextInt(2)));
        }
        return addr.toString();
    }

    public static MerkleTrie.TrieNode createRandomCycleTrie(Relay r) {
        r.addUpdateFromDownstream(TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE), TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE));
        return r.createCycleTrie();
    }

    public static MerkleTrie.TrieNode createRandomCycleTrie(Relay r, int nUpdates) {
        for (int i = 0; i < nUpdates; ++ i) {
            r.addUpdateFromDownstream(TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE), TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE));
        }
        return r.createCycleTrie();
    }


}
