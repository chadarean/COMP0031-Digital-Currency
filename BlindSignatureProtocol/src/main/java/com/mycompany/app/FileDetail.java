package com.mycompany.app;

//import com.mycompany.app.*;

public class FileDetail {
    
    // A given file may have at most one file detail per cycle in its POP
    // Does not have to be unique

    /*
    The destination address is the address of the intended recipient of this
    file. If this file detail is part of the file’s POP, and there is no more recent
    file detail in the file’s POP, then the destination address is said to own the
    file. If the destination address is null then this file is considered invalid
    beginning in this cycle
    */
    String destinationAddress;
    
    /*
    The proofs packet contains proof that this file detail entry met any
    requirements that were put on it by encumbrances or operations, if any
    such requirements exist. If no such requirements exist then the proofs
    packet hash should be null.
    */
    String proofsPacketHash;
    
    /*
    The metadata hash is the hash of arbitrary metadata to be associated
    with this file in this cycle. If there is no metadata to associate with this
    file, this should be the null hash. There are no restrictions on this field,
    and like the payload field the protocol never examines it.
    */
    String metadataHash;


    public FileDetail(String destinationAddress, String proofsPacketHash, String metadataHash) {
        this.destinationAddress = destinationAddress;
        this.proofsPacketHash = proofsPacketHash;
        this.metadataHash = metadataHash;
    }

    public String getDestinationAddress() {
        return this.destinationAddress;
    }

    public String getProofsPacketHash() {
        return this.proofsPacketHash;
    }

    public String getMetadataHash() {
        return this.metadataHash;
    }

    @Override
    public String toString() {
        return this.destinationAddress + this.proofsPacketHash + this.metadataHash;
    }

}