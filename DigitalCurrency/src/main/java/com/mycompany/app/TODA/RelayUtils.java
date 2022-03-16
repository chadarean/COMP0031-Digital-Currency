package com.mycompany.app.TODA;

import java.util.Random;
import java.util.*;
import com.mycompany.app.TODA.*;

public class RelayUtils {
    public static Random rand = new Random();

    public static String getRandomXBitAddr(Random rand, int addrSize) {
        StringBuilder addr = new StringBuilder();
        for (int j = 0; j < MerkleTrie.ADDRESS_SIZE; ++ j) {
            addr.append(Integer.toString(rand.nextInt(2)));
        }
        return addr.toString();
    }

    public static MerkleTrie.TrieNode createGenesisCycleTrie(Relay r) {
        r.addUpdateFromDownstream(RelayUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE), RelayUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE));
        return r.createCycleTrie();
    }

}