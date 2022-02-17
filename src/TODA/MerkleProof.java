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

        public Frame(String leftBranchHash, byte[] leftBranchPrefix, String rightBranchHash, byte[] rightBranchPrefix) {
            this.leftBranchHash = leftBranchHash;
            this.rightBranchHash = rightBranchHash;
            this.leftBranchPrefixLength = leftBranchPrefix[0];
            this.leftBranchPrefix = Arrays.copyOfRange(leftBranchPrefix, 1, leftBranchPrefix.length);
            this.rightBranchPrefixLength = rightBranchPrefix[0];
            this.rightBranchPrefix = Arrays.copyOfRange(rightBranchPrefix, 1, rightBranchPrefix.length);
        }

        public Frame(String leftBranchHash, byte leftBranchPrefixLength, byte[] leftBranchPrefix, String rightBranchHash, byte rightBranchPrefixLength, byte[] rightBranchPrefix) {
            this.leftBranchHash = leftBranchHash;
            this.rightBranchHash = rightBranchHash;
            this.leftBranchPrefixLength = leftBranchPrefixLength;
            this.leftBranchPrefix = Arrays.copyOf(leftBranchPrefix, leftBranchPrefix.length);
            this.rightBranchPrefixLength = rightBranchPrefixLength;
            this.rightBranchPrefix = Arrays.copyOf(rightBranchPrefix, rightBranchPrefix.length);
        }
    }

    ArrayList<Frame> frames;

    public MerkleProof() {
        this.frames = new ArrayList<Frame>();
    }

    public void addFrame(String leftBranchHash, byte leftBranchPrefixLength, byte[] leftBranchPrefix, String rightBranchHash, byte rightBranchPrefixLength, byte[] rightBranchPrefix) {
        this.frames.add(new Frame(leftBranchHash, leftBranchPrefixLength, leftBranchPrefix,
        rightBranchHash, rightBranchPrefixLength, rightBranchPrefix));
    }

    public boolean verify(String address, String leafHash) {
        // TODO: check frame[0].constructionDataHash == cycleRoot on the ledger
        String prevHash = frames.get(0).constructionDataHash;
        int prefSize = 0;

        for (Frame frame_i: frames) {
            String expectedHash = Utils.getHash(Utils.getStringFromByte(frame_i.leftBranchPrefix, frame_i.leftBranchPrefixLength) +
                    frame_i.leftBranchHash +
                    Utils.getStringFromByte(frame_i.rightBranchPrefix, frame_i.rightBranchPrefixLength) +
                    frame_i.rightBranchHash);
            if (expectedHash != prevHash) {
                return false;
            }
            if (address.charAt(prefSize) == '0') {
                prefSize += (int)frame_i.leftBranchPrefixLength + 1; //TODO:check conversion
                prevHash = frame_i.leftBranchHash;
            } else {
                prefSize += (int)frame_i.rightBranchPrefixLength + 1;
                prevHash = frame_i.rightBranchHash;
            }
        }
        if (prefSize != MerkleTrie.ADDRESS_SIZE || !(prevHash.equals(leafHash))) {
            return false;
        }
        return true;
    }
}