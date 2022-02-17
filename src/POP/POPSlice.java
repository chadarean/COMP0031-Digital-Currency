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
        this.cycleRoot = cycleRoot;
        this.addressProof = addressProof;
        this.transactionPacket = transactionPacket;
        this.fileProof = fileProof;
        this.fileDetail = fileDetail;
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
        return true;
    }
}