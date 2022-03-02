/** Simplifying assumptions:
 * 1. a user will not send mixed updates for assets under the same address
 * 2. the user will receive the POPSlice prooving that the update was included right after the relay constructs the trie 
 * (if this is not the case, the user could later find the cycle root of the updated by checking a small set of cycle hashes
 * published in the expected time interval) 
 * 3. using 2, the user will have the first cycle root required for the POP for each asset
*/

package src.TODA;

import java.util.*;
import src.POP.*;

// memory for user storage: crtFileTrie+fileTrieCache+fileDetails+assets+updateToCycleRoot+txpxs+addressToPOPSlice(cache)
// min memory required: crtFileTrie+fileTrieCache+fileDetails+assets+updateToCycleRoot+txpxs
// assets memory: crtFileTrie+fileDetails+assets

public class Owner {
    // Note: the user balance can either be stored on user side or in an MSB database. The former is more public, but
    // the latter could be more robust against technical failure of user application.
    public String userId;
    public HashMap<String, MerkleTrie.TrieNode> crtFileTrie;
    // crtFileTrie[address] = fileTrie object created for the current update of address
    public HashMap<String, HashMap<String, MerkleTrie.TrieNode>> fileTrieCache; // assoctiates addresses with file trie objects for each cycle root
    // fileTrieCache[cycleRoot][address] = fileTrie object with files under address in trie with root = cycleRoot
    public HashMap<String, TreeMap<String, FileDetail>> fileDetails; // stores pairs of <file_id, file_detail> sorted by file_id 
    // for each address: fileDetails[address] = sorted set of <file_id, file_detail>
    // Note that a file can have a single valid file detail at a given cycle
    public HashMap<String, ArrayList<Token>> assets; // assets[address]
    public HashMap<Integer, HashMap<String, POPSlice>> addressToPOPSlice; //cache POP for address but not for files since files will be removed after transacted
    public HashMap<String, String> updateToCycleRoot; // updateToCycleRoot[address] = the cycle root hash where the update for address was made
    public Relay relay; 
    //public HashMap<String, FileDetail> oldFileDetails;
    public HashMap<String, TransactionPacket> txpxs; // assume address has only one update before receiving its POP

    public Owner(String userId) {
        this.userId = userId;
        this.crtFileTrie = new HashMap<>();
        fileTrieCache = new HashMap<>();
        fileDetails = new HashMap<>();
        assets = new HashMap<>();
        relay = new Relay();
        txpxs = new HashMap<>();
        addressToPOPSlice = new HashMap<>();
        updateToCycleRoot = new HashMap<>();
    }

    public void addAsset(String address, Token asset) {
        ArrayList<Token> addressAssets = assets.get(address);
        if (addressAssets == null) {
            assets.put(address, new ArrayList<>(){{add(asset);}});
        } else {
            addressAssets.add(asset);
        }
    }

    public Token createAsset(String address, int d) {
        String cycleRoot = ""; // TODO: getCurrentCycleRoot() from Relay DB? via MSB? 
        String signature = ""; // TODO: get s(d, I_d) from DLT via MSB
        //TODO: should cycleRoot and signature be obtained by the wallet?
        Token token = new Token();
        token.createAsset(cycleRoot, address, address, signature, d);
        //String fileIdAddress = Utils.convertKey(token.getFileId());
        addAsset(address, token);
        // TODO: get signature for asset
        // TODO: should we add the token to a DB
        return token;
    }

    public Token createAsset(String cycleRoot, String address, int d, String signature) {
        Token token = new Token();
        token.createAsset(cycleRoot, address, address, signature, d);
        addAsset(address, token);
        return token;
    }

    public void transferAsset(String cycleRoot, String address, Token asset, String destPk) {
        // creates update for assetId to be transferred to destPk at a cycle following cycleRoot
        // using a map ensures that an address makes a single update in a cycle for an assetId
        // Note: in this version, cycleRoot = issuance cycle root
        String assetId = Utils.convertKey(asset.getFileId());
        asset.createUpdate(address, destPk);
        FileDetail fileDetail = asset.fileDetail; //TODO: is proofs packet hash required?
        if (fileDetails.containsKey(address)) {
            fileDetails.get(address).put(assetId, fileDetail);
        } else {
            fileDetails.put(address, new TreeMap<String, FileDetail>(){{put(assetId, fileDetail);}});
        }
    }

    public void sendUpdate(String address, String txpxHash) {
        relay.addUpdateFromDownstream(address, txpxHash);
        // sends request to Relay
        // TODO: the relay could send back the POP after creating the trie
    } 

    public void removeCycleRootData(ArrayList<String> cycleRoots) {
        for (String cycleRoot: cycleRoots) {
            // TODO: should it remove the addresses too or set the value to null?
            fileTrieCache.remove(cycleRoot);
        }
    }
    // TODO: implement function that detects the time when it is safe to remove the POPs data 

    private MerkleTrie.TrieNode getFileTrieForAddress(String cycleRoot, String address) {
        // returns the file trie for the address at trie with root = cycleRoot. if cycleRoot is null, then it returns the current cycle trie
        if (cycleRoot == null) {
            return crtFileTrie.get(address); // current file Trie for the most recent update under address
            // Note: once user has the proof for this update (and consequently the cycleRoot), the fileTrie will be cleared
            // and moved to the cache
        } 
        HashMap<String, MerkleTrie.TrieNode> crtFileTries = fileTrieCache.get(cycleRoot);
        if (crtFileTries == null) {
            return null;
        }
        MerkleTrie.TrieNode t = crtFileTries.get(address); //get pairs of <file, fileDetials> for address
        return t;
    }

    public ArrayList<Pair<String, String>> getFileDetailPairs(String address) {
        ArrayList<Pair<String, String>> fileDetailPairs = new ArrayList<Pair<String, String>>();

        TreeMap<String, FileDetail> t = fileDetails.get(address); //get pairs of <file, fileDetials> for address
        if (t == null) {
            return fileDetailPairs;
        }
        for (Map.Entry<String, FileDetail> entry : t.entrySet()) {
            FileDetail entryFileDetail = entry.getValue();
            fileDetailPairs.add(new Pair<String, String>(entry.getKey(), Token.getHashOfString(entryFileDetail.toString())));
        }
        return fileDetailPairs;
    }

    public void clearFileDetails() {
        fileDetails.clear(); // does this remove all objects in fileDetails?
    }

    public MerkleTrie.TrieNode createFileTrie(String address) {
        MerkleTrie.TrieNode fileTrie = MerkleTrie.createMerkleTrie(getFileDetailPairs(address)); 
        
        if (crtFileTrie.containsKey(address)) {
            throw new RuntimeException("The address is not allowed to create a new file trie! This limitation can be removed at the cost of additional memory!");
        }

        return fileTrie;
    }

    public void sendUpdates(String cycleRoot, String address) {
        updateToCycleRoot.remove(address);
        // send updates for address to relay; cycleRoot = cycleRoot at the time when the files transacted were created
        MerkleTrie.TrieNode fileTrie = createFileTrie(address);
        crtFileTrie.put(address, fileTrie);
        String signature = ""; // TODO: should this be user's signature, now that bsig is handled per file or null?
        TransactionPacket txpx = new TransactionPacket(cycleRoot, address, fileTrie.value, null, signature);
        txpxs.put(address, txpx);
        sendUpdate(address, Token.getTransactionPacket(txpx));
        // a thread executing this method could block until the relay returns the initial POPSlice
    }

    // Called either when relay sends the POPSlice for the cycle when the transaction for address took place
    // or when receiving the full POP.
    public void receivePOP(String address, POPSlice popSlice) {
        String cycleRoot = popSlice.cycleRoot;
        popSlice.transactionPacket = getTxpx(address, popSlice.addressProof.leafHash);
        if (popSlice.transactionPacket == null) {
            throw new RuntimeException(popSlice.addressProof.leafHash);
        }
        // adds the POPSlice to the cache containing popslices for address in trie with root = cycleRoot
        HashMap<String, POPSlice> addressPOPSlice = addressToPOPSlice.get(relay.cycleId.get(cycleRoot));
        if (addressPOPSlice == null) {
            addressToPOPSlice.put(relay.cycleId.get(cycleRoot), new HashMap<>(){{put(address, popSlice);}});
        } else {
            addressPOPSlice.put(address, popSlice);
        }
        if (!updateToCycleRoot.containsKey(address)) {
            MerkleTrie.TrieNode addressCrtFileTrie = crtFileTrie.get(address);
            HashMap<String, MerkleTrie.TrieNode> crtFileTries = fileTrieCache.get(cycleRoot);
            if (crtFileTries == null) {
                fileTrieCache.put(cycleRoot, new HashMap<>(){{put(address, addressCrtFileTrie);}});
            } else {
                crtFileTries.put(address, addressCrtFileTrie);
            }
            
            crtFileTrie.remove(address);
            updateToCycleRoot.put(address, cycleRoot);
        }
    }


    public MerkleProof getFileProof(String cycleRoot, String address, String assetId) {
        // Returns the Merkle File Proof for assetId in the fileTrie created for address in trie with cycle root = cycleRoot
        MerkleTrie.TrieNode fileTrie = getFileTrieForAddress(cycleRoot, address);
        return MerkleTrie.getMerkleProof(assetId, fileTrie);
    }

    public boolean verifyPOP(ArrayList<POPSlice> popSlices, String address, String destinationAddress, String signature) {
        // TODO: check that the s(popSlice[numPOPSlices-1].fileId, I_d) == signature
        int numPOPSlices = popSlices.size();
        if (numPOPSlices == 0) {
            return false;
        }
        if (!popSlices.get(numPOPSlices-1).fileDetail.getDestinationAddress().equals(destinationAddress)) {
            System.out.println("Destination address not matching!");
            return false;
        }

        for (int i = 0; i < numPOPSlices; ++ i) {
            POPSlice popSlice = popSlices.get(i);
            if (!popSlice.verify(address)) {
                System.out.println("incorrect POP" + Integer.toString(i));
                return false;
            }
            if (i != 0 && i != (numPOPSlices - 1)) {
                if (popSlice.fileProof != null && !popSlice.fileProof.null_proof) {
                    // the owner must prove that they did not transact the asset=>POPSlice must be a null proof 
                    System.out.println("non-null POP");
                    return false;
                }
            }
        }
        return true;
    }

    public TransactionPacket getTxpx(String address, String txpxHash) {
        TransactionPacket txpx = txpxs.get(address);
        if (!Token.getTransactionPacket(txpx).equals(txpxHash)) {
            throw new RuntimeException("Error transaction packet doesn't match!");
        }
        txpxs.remove(address);
        return txpx;
    }

    public void completePOPSlice(POPSlice popSlice, String address, String fileId) {
        popSlice.fileId = fileId;
        popSlice.fileDetail = fileDetails.get(address).get(fileId);
        popSlice.fileProof = getFileProof(popSlice.cycleRoot, address, fileId);
    }

    public POPSlice getPOPSliceForCycle(String address, String fileId, String cycleRoot) {
        // Obtains the POPSlice for address in trie with root cycleRoot and completes it with data for fileId
        POPSlice popSlice = relay.getPOPSlice(address, cycleRoot);
        receivePOP(address, popSlice);
        completePOPSlice(popSlice, address, fileId);
        return popSlice;
    }

    public ArrayList<POPSlice> getPOPUsingCache(String cycleRoot, String address, Token asset) {
        int beginCycle = relay.cycleId.get(asset.getIssuedCycleRoot());
        int endCycle = relay.cycleId.get(cycleRoot);
        ArrayList<POPSlice> pop = new ArrayList<>();
        for (int i = beginCycle; i <= endCycle; ++ i) {
            HashMap <String, POPSlice> crtCycleSlice = addressToPOPSlice.get(i);
            POPSlice popSlice;
            if (crtCycleSlice == null || !crtCycleSlice.containsKey(address)) {
                popSlice = relay.getPOPSlice(address, i);
            } else {
                popSlice = crtCycleSlice.get(address);
            }
            if (!popSlice.addressProof.null_proof) {
                completePOPSlice(popSlice, address,  Utils.convertKey(asset.getFileId()));
            } else {
                popSlice.transactionPacket = new TransactionPacket(null, address, null, null, null);
            }
            pop.add(popSlice);
        }
        return pop;
    }

    public ArrayList<POPSlice> getPOP(String cycleRoot, String address, Token asset) {
        // Constructs the POP for asset by obtaining all POPSlices from the asset cycle root issuance to cycleRoot = the hash of the cycle trie
        // containing the update to asset
        ArrayList<POPSlice> pop = relay.getPOP(address, asset.getIssuedCycleRoot(), cycleRoot);
        pop.add(addressToPOPSlice.get(relay.cycleId.get(cycleRoot)).get(address));
    
        for (POPSlice popSlice: pop) {
            if (!popSlice.addressProof.null_proof) {
                // cycle Root contained in popSlice, but not known to user
                if (popSlice.transactionPacket == null) {
                    HashMap <String, POPSlice> crtCycleSlice = addressToPOPSlice.get(popSlice.cycleRoot);
                    popSlice.transactionPacket = crtCycleSlice.get(address).transactionPacket;
                    
                }
                completePOPSlice(popSlice, address, Utils.convertKey(asset.getFileId()));
            } else {
                popSlice.transactionPacket = new TransactionPacket(null, address, null, null, null);
            }
        }
        fileDetails.get(address).remove(Utils.convertKey(asset.getFileId()));
        return pop;
    }

    public boolean receiveAsset(ArrayList<POPSlice> popSlices, String address, String destinationAddress, String signature, Token token) {
        if (!verifyPOP(popSlices, address, destinationAddress, signature)) {
            return false;
        }
        addAsset(destinationAddress, token); //store the token under receiver's address
        return true;
    }

// TODO: verifyIntegrity(POP_list) queries the ledger for the hashes contained in POP_lists and returns True/False depending on validity

}