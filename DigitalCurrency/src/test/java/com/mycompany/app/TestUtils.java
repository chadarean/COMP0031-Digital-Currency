package com.mycompany.app;

import java.util.*;
import com.mycompany.app.TODA.*;

public class TestUtils {
    public static Random rand = new Random();
    
    public static String getRandomXBitAddr(Random rand, int addrSize) {
        StringBuilder addr = new StringBuilder();
        for (int j = 0; j < MerkleTrie.ADDRESS_SIZE; ++ j) {
            addr.append(Integer.toString(rand.nextInt(2)));
        }
        return addr.toString();
    }

    public static MerkleTrie.TrieNode createGenesisCycleTrie(Relay r) {
        r.addUpdateFromDownstream(TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE), TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE));
        return r.createCycleTrie();
    }

}
