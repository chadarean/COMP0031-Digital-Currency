package com.mycompany.app.TODA;

import java.util.*;

public class MerkleProof {
    public String leafHash;

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

        public long getSize() {
            return Utils.getObjectSize(this.leftBranchHash) + Utils.getObjectSize(this.leftBranchPrefix) +
            Utils.getObjectSize(this.leftBranchPrefixLength) + Utils.getObjectSize(this.rightBranchHash) + Utils.getObjectSize(this.rightBranchPrefix) +
            Utils.getObjectSize(this.rightBranchPrefixLength);
        }
    }

    public ArrayList<Frame> frames;
    public boolean null_proof; // for null proofs, choose the immediately next address
    // if on branch 0 and addr_pref > pref, go to branch 1 and take only 0s;
    // if on branch 0 and addr_pref < pref, stay on branch 0 and take only 0s
    // if on branch 1 and addr_pref > pref, stay on branch 1 and take only 1s, resulting in immediately prev addr
    // if on branch 1 and addr_pref < pref, stay on branch 1 and take only 0s

    public long getSize() {
        long proofSize = Utils.getObjectSize(leafHash) + Utils.getObjectSize(null_proof);
        System.out.printf("%d, %d=proof size metadata for %d frames\n", Utils.getObjectSize(leafHash), proofSize, frames.size());
        for (Frame f: frames) {
            System.out.printf("%d=frame size\n", Utils.getObjectSize(f));
            proofSize += Utils.getObjectSize(f);
        }
        return proofSize;
    }

    public MerkleProof() {
        this.frames = new ArrayList<Frame>();
        null_proof = false;
    }

    public void setHash(String leafHash) {
        this.leafHash = leafHash;
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
        int chosen_branch = -1; // in the case of null proof, at the point where the tree branches from the address, the value
        // will be set to the branch of the immediately next address (or previous in the case of the largest address)
        boolean exceeded_lcp = false;
        //System.out.println("Nf = " + Integer.toString(frames.size()));
        //System.out.println("Proof for " + address);
        for (Frame frame_i: frames) {
            int leftBranchPLen = Utils.ubyte(frame_i.leftBranchPrefixLength)+1;
            int rightBranchPLen = Utils.ubyte(frame_i.rightBranchPrefixLength)+1;
            //System.out.println("left branch pref len=" + Integer.toString(leftBranchPLen) + "an first is" +
            //Integer.toString(frame_i.leftBranchPrefix[0]));
            //System.out.println("right branch pref len=" + Integer.toString(rightBranchPLen) + "an first is" +
            //Integer.toString(frame_i.rightBranchPrefix[0]));
            String leftPrefStr = Utils.getStringFromByte(frame_i.leftBranchPrefix, leftBranchPLen);
            String rightPrefStr = Utils.getStringFromByte(frame_i.rightBranchPrefix, rightBranchPLen);
        
            String expectedHash = Utils.getHash(leftPrefStr + frame_i.leftBranchHash +
                    rightPrefStr + frame_i.rightBranchHash);
            //System.out.println(frame_i.rightBranchHash + ";;" + frame_i.leftBranchHash + ";;");
            //System.out.println("expected hash=" + expectedHash + "actual = " + prevHash + " pref_sz=" + Integer.toString(prefSize));
            int expBranch = address.charAt(prefSize) - 48;
            // if (expBranch == 0)
            // System.out.println("Next preflen= " + Integer.toString(leftBranchPLen));
            // else
            // System.out.println("Next preflen= " + Integer.toString(rightBranchPLen));
            if (!expectedHash.equals(prevHash)) {
                return false;
            }
            
            if (null_proof && chosen_branch != -1) {
                if (chosen_branch == 0) {
                    prefSize += leftBranchPLen; 
                    prevHash = frame_i.leftBranchHash;
                } else {
                    prefSize += rightBranchPLen; 
                    prevHash = frame_i.rightBranchHash;
                }
            } else {
                int cmp = (expBranch == 0) ? address.substring(prefSize, prefSize + leftBranchPLen).compareTo(leftPrefStr)
                : address.substring(prefSize, prefSize + rightBranchPLen).compareTo(rightPrefStr);

                //System.out.println("cmp is " + Integer.toString(cmp) + " exp= " + Integer.toString(expBranch));
                
                if (!null_proof && cmp != 0) {
                    // the address doesn't match the prefix
                    return false;
                }
                if (null_proof) {
                    //System.out.println("NULL proof" + Integer.toString(cmp) + " " + Integer.toString(expBranch));
                    if (cmp < 0 || (cmp == 0 && (frame_i.leftBranchHash == null || frame_i.leftBranchHash.equals(Utils.getHash(null))) && expBranch == 0)) {
                        chosen_branch = 0;
                        //System.out.println("choose b 0");
                        // stay on branch and choose only 0;
                    } else if (cmp > 0 || (cmp == 0 && (frame_i.rightBranchHash == null || frame_i.rightBranchHash.equals(Utils.getHash(null))) && expBranch == 1)) {
                        chosen_branch = expBranch; 
                        expBranch = 1;
                       //System.out.println("choose b 1");
                    } // for cmp == 0, chosen_branch remains -1 until address diverges from prefix
                }
                if (expBranch == 0) {
                    prefSize += leftBranchPLen; 
                    prevHash = frame_i.leftBranchHash;
                } else { 
                    prefSize += rightBranchPLen;
                    prevHash = frame_i.rightBranchHash;
                } 
            }
        }
        //System.out.println(prefSize);
        if (null_proof && chosen_branch == -1) {
            return false;
        }
        if (!null_proof && (prefSize != address.length() || !(prevHash.equals(leafHash)))) {
            //System.out.println("pref_sz=" + Integer.toString(address.length()) + " and prevHash=" + prevHash + "and leaf=" + leafHash);
            return false;
        }
        return true;
    }
}