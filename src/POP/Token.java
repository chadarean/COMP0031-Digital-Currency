package src.POP;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import src.TODA.MerkleTrie;

public class Token {

    private static final String SHA_256 = "SHA-256";

    FileKernel fileKernel;
    public FileDetail fileDetail;

    // TODO: Signature
    public Token createAsset(String cycleRoot, String creatorAddress, String address, String signature) {
        fileKernel = new FileKernel(cycleRoot, creatorAddress, null, null, null);
        fileDetail = new FileDetail(address, null, null);
        // TODO: Add asset to DB
        return this;
    }

    public String getFileId() {
        return getHashOfString(fileKernel.getIssuedCycleRoot() 
            + fileKernel.getCreatorAddress() 
            + fileKernel.getFileTypeIdentifier() 
            + fileKernel.getPayloadHash() 
            + fileKernel.getEncumbranceHash()
        );
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
            String encoded = Base64.getEncoder().encodeToString(hash);
            return encoded.substring(0, 32);
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String nullHash() {
        StringBuilder nullHashStr = new StringBuilder();
        for (int i = 0; i < MerkleTrie.ADDRESS_SIZE; ++ i) {
            nullHashStr.append("0");
        }
        return nullHashStr.toString();
    }
}