package src.POP;

import java.util.*;

import src.TODA.MerkleTrie;

public class POPSlice {
    String fileId;
    String cycleRoot;
    MerkleProof addressProof;
    TransactionPacket transactionPacket; //TODO: 2check that transactionPacket.address is sender's address
    MerkleProof fileProof;
    FileDetail fileDetail;

    public POPSlice(String cycleRoot, MerkleProof addressProof, TransactionPacket transactionPacket, MerkleProof fileProof, FileDetail fileDetail) {

    }

    public boolean verify(dest_pk) {
        // TODO: verify signature in txpx
        if (!fileDetail.destinationAddress.equals(dest_pk)) {
            return false;
        }
        if (!addressProof.verify(transactionPacket.address, getHash(transactionPacket))) {
            return false;
        }
        if (!fileProof.verify(fileId, getHash(fileDetail))) {
            return false;
        }
    }
}