package src.POP;

import java.util.*;
import src.TODA.*;

import src.TODA.MerkleTrie;

public class POPSlice {
    String fileId;
    String cycleRoot;
    MerkleProof addressProof;
    TransactionPacket transactionPacket; //TODO: 2check that transactionPacket.address is sender's address
    MerkleProof fileProof;
    FileDetail fileDetail;

    public POPSlice(String cycleRoot, MerkleProof addressProof, TransactionPacket transactionPacket, MerkleProof fileProof, FileDetail fileDetail) {
        this.cycleRoot = cycleRoot; // the Cycle hash Ck
        this.addressProof = addressProof; // Merkle proof associating the (possibly null) value p with the address a in the cycle trie whose root hash is Ck
        this.transactionPacket = transactionPacket; // the txpx whose hash is equal to p
        this.fileProof = fileProof; // Merkle proof associating the (possibly null) value f with the file id id in the file trie whose root hash is the file trie root in the txpx
        this.fileDetail = fileDetail; // file detail whose hash is equal to f
    }

    public boolean verify(String dest_pk) {
        // TODO: verify signature in txpx
        if (!fileDetail.destinationAddress.equals(dest_pk)) {
            return false;
        }
        if (!addressProof.verify(transactionPacket.address, Utils.getHash(transactionPacket.toString()))) { //TODO: write toString() methods
            return false;
        }
        if (!fileProof.verify(fileId, Utils.getHash(fileDetail.toString()))) {
            return false;
        }
        //TODO: verify signature of txpx
        return true;
    }
}