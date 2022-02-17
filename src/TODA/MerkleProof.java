package src.TODA;

import java.util.*;

public class MerkleProof {
    public class Frame {
        public String leftBranchHash;
        public byte leftBranchPrefixLength;
        public byte[] leftBranchPrefix;
        public String rightBranchHash;
        public byte rightBranchPrefixLength;
        public byte[] rightBranchPrefix;
        public String constructionDataHash;

        public Frame(String leftBranchHash, byte[] leftBranchPrefix, String rightBranchHash, byte[] rightBranchPrefix, String constructionDataHash) {
            this.leftBranchHash = leftBranchHash;
            this.rightBranchHash = rightBranchHash;
            this.leftBranchPrefixLength = leftBranchPrefix[0];
            this.leftBranchPrefix = Arrays.copyOfRange(leftBranchPrefix, 1, leftBranchPrefix.length);
            this.rightBranchPrefixLength = rightBranchPrefix[0];
            this.rightBranchPrefix = Arrays.copyOfRange(rightBranchPrefix, 1, rightBranchPrefix.length);
            this.constructionDataHash = constructionDataHash;
        }

        public Frame(String leftBranchHash, byte leftBranchPrefixLength, byte[] leftBranchPrefix, String rightBranchHash, byte rightBranchPrefixLength, 
        byte[] rightBranchPrefix, String constructionDataHash) {
            this.leftBranchHash = leftBranchHash;
            this.rightBranchHash = rightBranchHash;
            this.leftBranchPrefixLength = leftBranchPrefixLength;
            this.leftBranchPrefix = Arrays.copyOf(leftBranchPrefix, leftBranchPrefix.length);
            this.rightBranchPrefixLength = rightBranchPrefixLength;
            this.rightBranchPrefix = Arrays.copyOf(rightBranchPrefix, rightBranchPrefix.length);
            this.constructionDataHash = constructionDataHash;
        }
    }

    public ArrayList<Frame> frames;

    public MerkleProof() {
        this.frames = new ArrayList<Frame>();
    }

    public void addFrame(String leftBranchHash, byte leftBranchPrefixLength, byte[] leftBranchPrefix, String rightBranchHash, byte rightBranchPrefixLength, 
    byte[] rightBranchPrefix, String constructionDataHash) {
        this.frames.add(new Frame(leftBranchHash, leftBranchPrefixLength, leftBranchPrefix,
        rightBranchHash, rightBranchPrefixLength, rightBranchPrefix, constructionDataHash));
    }

    public boolean verify(String address, String leafHash) {
        // TODO: check frame[0].constructionDataHash == cycleRoot on the ledger
        String prevHash = frames.get(0).constructionDataHash;
        int prefSize = 0;
        System.out.println("Nf = " + Integer.toString(frames.size()));
        for (Frame frame_i: frames) {
            String expectedHash = Utils.getHash(Utils.getStringFromByte(frame_i.leftBranchPrefix, frame_i.leftBranchPrefixLength+1) +
                    frame_i.leftBranchHash +
                    Utils.getStringFromByte(frame_i.rightBranchPrefix, frame_i.rightBranchPrefixLength+1) +
                    frame_i.rightBranchHash);
            System.out.println("expected hash=" + expectedHash + "actual = " + prevHash + " pref_sz=" + Integer.toString(prefSize));
            if (!expectedHash.equals(prevHash)) {
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

        if (prefSize != address.length() || !(prevHash.equals(leafHash))) {
            System.out.println("pref_sz=" + Integer.toString(address.length()) + " and prevHash=" + prevHash + "and leaf=" + leafHash);
            return false;
        }
        return true;
    }
}