package src.POP;

public class FileKernel {

    // The FileKernel is globally unique

    /*
    The issued cycle root is the current cycle root at the time of the file’s creation. 
    For a file to be valid its initial transaction MUST take place in
    the immediately following cycle
    */
    String issuedCycleRoot;
    
    /*
    The creator address is the address of the creator of this file. For a file
    to be valid, the creator address MUST match the address of the initial
    transaction.
    */
    String creatorAddress;
   
    /*
    The file type identifier is a file id or null. That is, a file’s type is either
    the id of another file, which is designated as the type of this file, or it is null
    if this file has no type. For a file f to be valid its file type MUST either
    be null, or a file type file that was valid during f’s issued cycle root.
    */
    String fileTypeIdentifier;
    
    /*
    The payload hash is the hash of any arbitrary bytes that make up the
    initial content of this file. There are no restrictions on this, and it is allowed
    to be null. It is perfectly acceptable to re-use a file’s payload in a different
    file.
    */
    String payloadHash;
    
    /*
    The encumbrance hash is the hash of a proper encumbrance packet. The
    encumbrance packet restricts instances of this file type (that is, files which
    use this file as their type). Those restrictions are not within the scope of
    this document. For a file to be valid its encumbrance hash MUST either
    be null or the hash of a proper encumbrance packet.
    */
    String encumbranceHash;


    public FileKernel(String issuedCycleRoot, String creatorAddress, String fileTypeIdentifier, String payloadHash, String encumbranceHash) {
        this.issuedCycleRoot = issuedCycleRoot;
        this.creatorAddress = creatorAddress;
        this.fileTypeIdentifier = fileTypeIdentifier;
        this.payloadHash = payloadHash;
        this.encumbranceHash = encumbranceHash;
    }

    public String getIssuedCycleRoot() {
        return this.issuedCycleRoot;
    }

    public String getCreatorAddress() {
        return this.creatorAddress;
    }

    public String getFileTypeIdentifier() {
        return this.fileTypeIdentifier;
    }

    public String getPayloadHash() {
        return this.payloadHash;
    }

    public String getEncumbranceHash() {
        return this.encumbranceHash;
    }

    
}