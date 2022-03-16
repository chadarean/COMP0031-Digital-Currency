package com.mycompany.app.POP;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import com.mycompany.app.TODA.MerkleTrie;
import com.mycompany.app.TODA.Utils;

public class Token {

    private static final String SHA_256 = "SHA-256";

    FileKernel fileKernel;
    public FileDetail fileDetail;

    // TODO: Signature
    public Token createAsset(String cycleRoot, String creatorAddress, String signature, int d) {
        fileKernel = new FileKernel(cycleRoot, creatorAddress, null, getHashOfString(Integer.toString(d)), null);
        fileDetail = new FileDetail(null, null, null);
        // TODO: Add asset to DB
        return this;
    }

    public void addSignature(String signature) {
        this.fileDetail.proofsPacketHash = signature;
    }

    public String getFileId() {
        return getHashOfString(fileKernel.getIssuedCycleRoot() 
            + fileKernel.getCreatorAddress() 
            + fileKernel.getFileTypeIdentifier() 
            + fileKernel.getPayloadHash() 
            + fileKernel.getEncumbranceHash()
        );
    }

    public String getIssuedCycleRoot() {
        return fileKernel.issuedCycleRoot;
    }

    public String getFileDetail() {
        return getHashOfString(fileDetail.getDestinationAddress() + fileDetail.getProofsPacketHash() + fileDetail.getMetadataHash());
    }

    public static String getTransactionPacket(TransactionPacket transactionPacket) {
         return getHashOfString(transactionPacket.getFileTrieRoot() 
            + transactionPacket.getCurrentCycleRoot() 
            + transactionPacket.getAddress() 
            + transactionPacket.getSignaturePacket()
        );
    }

    public void createUpdate(String address, String destinationAddress) {
        fileDetail.destinationAddress = destinationAddress;
    }

    public static String getHashOfString(String concatenation) {
        if (concatenation == null) {
            return nullHash();
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(concatenation.getBytes(StandardCharsets.UTF_8));
            String encoded = new String(hash, StandardCharsets.US_ASCII);
            System.out.printf("%d %d\n", encoded.length(), Utils.getObjectSize(encoded));
            return encoded;
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String nullHash() {
        StringBuilder nullHashStr = new StringBuilder();
        for (int i = 0; i < MerkleTrie.ADDRESS_SIZE; ++ i) {
            nullHashStr.append("0");
        }
        return getHashOfString(nullHashStr.toString());
    }

    public long getSize() {
        // todo: method that returns the size of fileKernel and fileDetail
        return Utils.getObjectSize(fileKernel) + Utils.getObjectSize(fileDetail);
    }
}