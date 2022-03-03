package TODA;

import java.util.*;
import POP.*;

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
}
