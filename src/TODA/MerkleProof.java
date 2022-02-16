package src.TODA;

import java.util.*;

public class MerkleProof {
    public class Frame {
        String leftBranchHash;
        byte leftBranchPrefixLength;
        byte[] leftBranchPrefix;
        String rightBranchHash;
        byte rightBranchPrefixLength;
        byte[] rightBranchPrefix;
        String constructionDataHash;

        public Frame(leftBranchHash, byte[] leftBranchPrefix, rightBranchHash, byte[] rightBranchPrefix) {
            this.leftBranchHash = leftBranchHash;
            this.rightBranchHash = rightBranchHash;
            this.leftBranchPrefixLength = leftBranchPrefix[0];
            this.leftBranchPrefix = Arrays.copyOf(leftBranchPrefix);
            this.rightBranchPrefixLength = rightBranchPrefix[0];
            this.rightBranchPrefix = Arrays.copyOf(leftBranchPrefix);
        }
    }

    ArrayList<Frame> frames;

    public MerkleProof() {
        frames = new ArrayList<Frame>();
    }

    public void addFrame(Frame f) {
        frames.add(f);
    }

    public void appendBits(StringBuilder res, int value, int n) {
        for (int j = 0; j < n; ++ j) {
            if ((value & (1<<j)) == 0) {
                res.append('0');
            } else {
                res.append('1');
            }
        }
    }

    public String getStringFromByte(byte[] arr, int arrLengthBits) {
        int fullLength = arr.size();
        if (arrLengthBits % 8) {
            -- fullLength;
        }
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < fullLength; ++ i) {
            int value = arr[i];
            appendBits(res, arr[i], 8);
        }
        appendBits(res, arr[arr.size()-1], arrLengthBits%8);
        return res.toString();
    }

    public boolean verify(String address, String leafHash) {
        // TODO: check frame[0].constructionDataHash == cycleRoot on the ledger
        int numFrames = frames.size();
        int prevHash = frame[0].constructionDataHash;
        int prefSize = 0;

        for (int i = 0; i < numFrames; ++ i) {
            String expectedHash = getHash(address.substring(0, prefSize) +
                    getStringFromByte(frames[i].leftBranchPrefix, frames[i].leftBranchPrefixLength) +
                    new String(frames[i].leftBranchHash, ) +
                    address.substring(0, prefSize) +
                    getStringFromByte(frames[i].rightBranchPrefix, frames[i].rightBranchPrefixLength) +
                    new String(frames[i].rightBranchHash));
            if (expectedHash != prevHash) {
                return false;
            }
            if (address.charAt(prefSize) == '0') {
                prefSize += (int)frames[0].leftBranchPrefixLength + 1; //TODO:check conversion
            } else {
                prefSize += (int)frames[0].rightBranchPrefixLength + 1;
            }
        }
        if (prefSize != MerkleTrie.ADDRESS_SIZE) {
            return false;
        }
        return true;
    }
}