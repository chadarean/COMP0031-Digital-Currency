package tests;

import java.util.*;
import src.TODA.*;

public class TestUtils {
    public static Random rand = new Random();
    
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

}
