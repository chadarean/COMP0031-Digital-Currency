package src.TODA;

import java.util.*;
import src.POP.*;

public class Utils {
    public static int ubyte(byte b) {
        if (b < 0) {
            return (int)b + 256;
        }
        return b;
    }

    public static void appendBits(StringBuilder res, int value, int n) {
        for (int j = 0; j < n; ++ j) {
            if ((value & (1<<j)) == 0) {
                res.append('0');
            } else {
                res.append('1');
            }
        }
    }

    public static String getStringFromByte(byte[] arr, int arrLengthBits) {
        int fullLength = arr.length;
        if ((arrLengthBits % 8) != 0) {
            -- fullLength;
        }
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < fullLength; ++ i) {
            appendBits(res, ubyte(arr[i]), 8);
        }
        appendBits(res, ubyte(arr[arr.length-1]), arrLengthBits%8);
        return res.toString();
    }

    public static byte[] prefixToBytes(String prefix) {
        //TODO: double check that negative values are correctly computed
        byte[] res = new byte[(prefix.length()-1)/8 + 1];
        int num_bytes = (prefix.length()-1)/8 + 1;
        int idx = 0;
        for (int i = 0; i < num_bytes; ++ i) {
            for (int j = 0; (j < 8) && (idx < prefix.length()); ++ j, ++ idx) {
                if (prefix.charAt(idx) == '1') {
                    res[i] |= (1<<j); // set bit j in byte i to 1
                }
            }
        }
        return res;
    }

    public static String convertKey(String s) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < s.length(); ++ i) {
            appendBits(res, s.charAt(i), 8);
        }
        return res.toString();
    }

    public static String getHash(String value) {
        return Token.getHashOfString(value);
    }

    public static long getObjectSize(Object object) {
        if (object == null) {
            return 0;
        }

        if (object.getClass().getName().equals("src.TODA.MerkleTrie$TrieNode")) {
            // slow recursive method
            return ((MerkleTrie.TrieNode)object).getSize();
        }
    
        if (object.getClass().getName().equals("src.TODA.MerkleProof")) {
            return ((MerkleProof)object).getSize();
        }

        if (object.getClass().getName().equals("src.TODA.MerkleProof$Frame")) {
            return ((MerkleProof.Frame)object).getSize();
        }

        if (object.getClass().getName().equals("src.POP.FileDetail")) {
            return 0;
        }

        if (object.getClass().getName().equals("src.POP.TransactionPacket")) {
            return ((TransactionPacket)object).getSize();
        }

        if (object.getClass().getName().equals("src.POP.TransactionPacket")) {
            return ((TransactionPacket)object).getSize();
        }

        if (object.getClass().getName().equals("src.POP.POPSlice")) {
            return ((POPSlice)object).getSize();
        }

        if (object.getClass().getName().equals("src.POP.Token")) {
            return ((Token)object).getSize();
        }

        if (object.getClass().getName().contains("src") && !object.getClass().getName().contains("File"))
        System.out.println(object.getClass().getName());

        // TODO: change it to call .getSize() if object is of type \in {Token, TransactionPacket, POPSlice, FileDetail,
        // MerkleTrie.TrieNode, MerkleProof}

        return InstrumentationAgent.getObjectSize(object);
    }

    public static void printObjectSize(Object object) {
        System.out.println("Object type: " + object.getClass() + ", size: " + InstrumentationAgent.getObjectSize(object) + " bytes");
    }

    public static <T1, R, T2> long getSize(HashMap<T1, HashMap<T2, R>> map) {
        long size = 0;
        for(var res : map.keySet()) {
            for(var r : map.get(res).keySet()) {
                size += Utils.getObjectSize(map.get(res).get(r));
            }
        }
        return size;
    }

    public static <T, R> long getSizeT(HashMap<T, TreeMap<T, R>> map) {
        long size = 0;
        for(var res : map.keySet()) {
            for(var r : map.get(res).keySet()) {
                size += Utils.getObjectSize(map.get(res).get(r));
            }
        }
        return size;
    }

    public static <T, R> long getSizeMapAndList(HashMap<T, ArrayList<R>> map) {
        long size = 0;
        for(var res : map.keySet()) {
            for(var r : map.get(res)) {
                size += Utils.getObjectSize(r);
            }
        }
        return size;
    }

    public static <T, R> long getSizeCrt(HashMap<T, R> map) {
        long size = 0;
        for(var res : map.keySet()) {
            size += Utils.getObjectSize(map.get(res)); 
        }
        return size;
    }
}
