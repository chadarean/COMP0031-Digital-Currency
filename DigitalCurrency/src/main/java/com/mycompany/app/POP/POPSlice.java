package com.mycompany.app.POP;

import java.util.*;
import com.mycompany.app.TODA.*;

import com.mycompany.app.TODA.MerkleTrie;

public class POPSlice {
    public String fileId;
    public String cycleRoot;
    public MerkleProof addressProof;
    public TransactionPacket transactionPacket; //TODO: 2check that transactionPacket.address is sender's address
    public MerkleProof fileProof;
    public FileDetail fileDetail;

    public POPSlice(String cycleRoot, MerkleProof addressProof, TransactionPacket transactionPacket, MerkleProof fileProof, FileDetail fileDetail) {
        this.cycleRoot = cycleRoot; // the Cycle hash Ck
        this.addressProof = addressProof; // Merkle proof associating the (possibly null) value p with the address a in the cycle trie whose root hash is Ck
        this.transactionPacket = transactionPacket; // the txpx whose hash is equal to p
        this.fileProof = fileProof; // Merkle proof associating the (possibly null) value f with the file id id in the file trie whose root hash is the file trie root in the txpx
        this.fileDetail = fileDetail; // file detail whose hash is equal to f
    }

    public boolean verify(String address) {
        // Note: It should be safe to only check the signature on the owner side, as the slice validity should not depend on the sender
        if (addressProof != null && !addressProof.null_proof && transactionPacket.address != address) {
            return false;
        }
        if (!addressProof.verify(transactionPacket.address, Token.getTransactionPacket(transactionPacket))) { 
            return false;
        }
        if (fileDetail == null && fileProof != null && !fileProof.null_proof) {
            return false;
        }
        if (fileProof == null || (fileDetail == null && fileProof.null_proof)) {
            return true;
        }
        if (!fileProof.verify(fileId, Utils.getHash(fileDetail.toString()))) {
            return false;
        }
        return true;
    }
}