package src.TODA;

import java.util.*;
import src.POP.*;

public class Utils {
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
            int value = arr[i];
            appendBits(res, arr[i], 8);
        }
        appendBits(res, arr[arr.length-1], arrLengthBits%8);
        return res.toString();
    }

    public static byte[] prefixToBytes(String prefix) {
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

    public static String getHash(String value) {
        return value;
        //return Token.getHashOfString(value);
        // 000000000011000000000000001141111000000000000011511111000000000000116100011000000000000117100111000000000000118
        //         0011000000000000001141111000000000000011511111000000000000116100011000000000000117100111000000000000118
        
    }
}
