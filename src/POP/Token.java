package src.POP;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import src.TODA.MerkleTrie;

public class Token {

    private final String SHA_256 = "SHA-256";

    FileKernel fileKernel;
    FileDetail fileDetail;
    MerkleTrie fileTrie;
    TransactionPacket transactionPacket;
    MerkleTrie cycleTrie;

    // TODO: Signature
    public Token createAsset(String creatorAddress, String address, String signature) {
         
        // TODO: Issued cycle root - will we get this from the DB?
        fileKernel = new FileKernel(null, creatorAddress, null, null, null);
        fileDetail = new FileDetail(address, null, null);
        transactionPacket = null;
        
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

    public String getTransactionPacket() {
         return getHashOfString(transactionPacket.getFileTrieRoot() 
            + transactionPacket.getCurrentCycleRoot() 
            + transactionPacket.getAddress() 
            + transactionPacket.getSignaturePacket()
        );
    }

    public MerkleTrie getFileTrie(String fileId, String fileDetail) {
        // TODO: getFileTrie([key=file_id, value=file_detail]) // files can be retrieved from DB and kept in memory when building the trie)
        return null;
    }

    public void createUpdate(String address, String destinationAddress) {
        // TODO: Create txpx
    }

    public String getHashOfString(String concatenation) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(concatenation.getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(hash);
            return encoded;
        } catch(NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}