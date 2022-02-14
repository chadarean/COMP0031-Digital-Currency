package src.POP;

public class TransactionPacket {
    String currentCycleRoot;
    String address;
    String fileTrieRoot;
    /*
    The expansion slot MUST be the null hash.
    */
    String expansionSlot;
    String signaturePacket;


    public TransactionPacket(String currentCycleRoot, String address, String fileTrieRoot, String expansionSlot, String signaturePacket) {
        this.currentCycleRoot = currentCycleRoot;
        this.address = address;
        this.fileTrieRoot = fileTrieRoot;
        this.expansionSlot = expansionSlot;
        this.signaturePacket = signaturePacket;
    }


    public String getCurrentCycleRoot() {
        return this.currentCycleRoot;
    }

    public String getAddress() {
        return this.address;
    }

    public String getFileTrieRoot() {
        return this.fileTrieRoot;
    }

    public String getExpansionSlot() {
        return this.expansionSlot;
    }

    public String getSignaturePacket() {
        return this.signaturePacket;
    }


}