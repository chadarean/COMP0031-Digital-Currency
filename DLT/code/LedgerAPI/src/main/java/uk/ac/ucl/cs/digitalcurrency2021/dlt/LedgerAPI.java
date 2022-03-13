package uk.ac.ucl.cs.digitalcurrency2021.dlt;

public interface LedgerAPI {

    /**
     * Create API instance.
     * First use {@link #connect()}
     * then {@link #shutdown()} when finished
     */
    static LedgerAPI newInstance() {
        return new FabricGateway();
    }

    /**
     * Establish connection to ledger
     */
    void connect() throws Exception;

    /**
     * Close connection to ledger
     */
    void shutdown() throws Exception;

    void writeHash(String cycleRootID, String hash) throws Exception;

    /**
     * Check if given cycle root has its hash recorded on the ledger
     * @param cycleRootID
     * @return boolean
     */
    boolean hasHash(String cycleRootID) throws Exception;

    /**
     *
     * @param cycleRootID
     * @return Hash of cycle root as String
     */
    String getHash(String cycleRootID) throws Exception;
}
