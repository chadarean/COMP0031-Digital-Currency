package src.TODA;

import java.util.*;
import src.POP.*;

public class Owner {
    // Note: the user balance can either be stored on user side or in an MSB database. The former is more public, but
    // the latter could be more robust against technical failure of user application.
    public String userId;
    public HashMap<String, HashMap<String, MerkleTrie.TrieNode>> fileTries;
    // fileTries[cycleRoot][address] = fileTrie object with files under address at current cycle root = cycleRoot
    public HashMap<String, HashMap<String, MerkleTrie.TrieNode>> fileTrieCache; // assoctiates addresses with file trie objects for each cycle_root
    // fileTrieCache[cycleRoot][address] = fileTrie object with files under address in cycleRoot
    public HashMap<String, TreeMap<String, FileDetail>> fileDetails; // stores pairs of <file_id, file_detail> sorted by file_id 
    // for each address: fileDetails[address] = sorted set of <file_id, file_detail>
    // Note that a file can have a single valid file detail at a given cycle
    public HashMap<String, Token> assets; // assets[address]
    public HashMap<String, POPSlice> addressToPOPSlice; //cache POP for address but not for files since files will be removed after transacted
    public HashMap<String, String> lastProovedCycle; // lastProovedCycle[assetId] = the latest cycle for which there is non-null proof for assetId
    public Relay relay; 
    public HashMap<String, FileDetail> oldFileDetails;
    public HashMap<String, TransactionPacket> txpxs; // assume address has only one update 

    public Owner(String userId) {
        this.userId = userId;
        this.fileTries = new HashMap<>();
        fileTrieCache = new HashMap<>();
        fileDetails = new HashMap<>();
        assets = new HashMap<>();
        relay = new Relay();
    }

    public Token createAsset(String address, int d) {
        String cycleRoot = ""; // TODO: getCurrentCycleRoot() from Relay DB? via MSB?
        String signature = ""; // TODO: get s(d, I_d) from DLT via MSB
        Token token = new Token();
        token.createAsset(cycleRoot, address, address, signature);
        //String fileIdAddress = Utils.convertKey(token.getFileId());
        assets.put(address, token);
        lastProovedCycle.put(Utils.convertKey(token.getFileId()), cycleRoot);
        // TODO: should we add the token to a DB
        return token;
    }

    public void sendUpdate(String address, String txpxHash) {
        relay.addUpdateFromDownstream(address, txpxHash);
        // sends request to Relay
        // TODO: the relay could send back the POP after creating the trie
    } 

    public void transferAsset(String cycleRoot, String address, Token asset, String destPk) {
        // creates update for assetId to be transferred to destPk at a cycle following cycleRoot
        // using a map ensures that an address makes a single update in a cycle for an assetId
        String assetId = Utils.convertKey(asset.getFileId());
        asset.createUpdate(address, destPk);
        FileDetail fileDetail = asset.fileDetail; //TODO: is proofs packet hash required?
        if (fileDetails.containsKey(address)) {
            fileDetails.get(address).put(assetId, fileDetail);
        } else {
            fileDetails.put(address, new TreeMap<String, FileDetail>(){{put(assetId, fileDetail);}});
        }
    }

    public void removeCycleRootData(ArrayList<String> cycleRoots) {
        for (String cycleRoot: cycleRoots) {
            // TODO: should it remove the addresses too or set the value to null?
            fileTrieCache.remove(cycleRoot);
        }
    }
    // TODO: implement function that detects the time when it is safe to remove the POPs data 

    private MerkleTrie.TrieNode getFileTrieForAddress(String cycleRoot, String address) {
        HashMap<String, MerkleTrie.TrieNode> crtFileTries = fileTries.get(cycleRoot);
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
            String entryFleId = entry.getKey();
            FileDetail entryFileDetail = entry.getValue();
            if (!entryFileDetail.getDestinationAddress().equals(address)) {
                // remove file ids transferred to a different address (assuming that the address doesn't belong to the same user).
                assets.remove(entryFleId);
            }
            
            fileDetailPairs.add(new Pair<String, String>(entry.getKey(), Token.getHashOfString(entryFileDetail.toString())));
            System.out.println("Adding " + entry.getKey() + " and val=" + Token.getHashOfString(entryFileDetail.toString()));
        }
        return fileDetailPairs;
    }

    public MerkleTrie.TrieNode createFileTrie(String cycleRoot, String address) {
        MerkleTrie.TrieNode fileTrie = MerkleTrie.createMerkleTrie(getFileDetailPairs(address));
        HashMap<String, MerkleTrie.TrieNode> crtFileTrie = fileTries.get(cycleRoot);
        if (crtFileTrie != null) {
            crtFileTrie.put(address, fileTrie);
        } else {
            fileTries.put(cycleRoot, new HashMap<String, MerkleTrie.TrieNode>() {{put(address, fileTrie);}});
        }
        
        return fileTrie;
    }

    public void sendUpdates(String cycleRoot, String address) {
        MerkleTrie.TrieNode fileTrie = createFileTrie(cycleRoot, address);
        String signature = ""; // TODO: should this be user's signature, now that bsig is handled per file or null?
        TransactionPacket txpx = new TransactionPacket(cycleRoot, address, fileTrie.value, null, signature);
        addressTxpx.put(address, txpx);
        sendUpdate(address, Token.getTransactionPacket(txpx));
    }

    public MerkleProof getFileProof(String cycleRoot, String address, String assetId) {
        MerkleTrie.TrieNode fileTrie = getFileTrieForAddress(cycleRoot, address);
        return MerkleTrie.getMerkleProof(assetId, fileTrie);
    }

    public boolean verifyPOP(ArrayList<POPSlice> popSlices, String address, String destinationAddress, String signature) {
        // TODO: check that the s(popSlice[numPOPSlices-1].fileId, I_d) == signature
        int numPOPSlices = popSlices.size();
        if (!popSlices.get(numPOPSlices-1).fileDetail.getDestinationAddress().equals(destinationAddress)) {
            return false;
        }
        if (popSlices.get(0).fileProof == null || popSlices.get(0).fileProof.null_proof) {
            // the first POP slice should not be a null proof
            return false;
        }

        for (int i = 0; i < numPOPSlices; ++ i) {
            POPSlice popSlice = popSlices.get(i);
            if (!popSlice.verify(address)) {
                return false;
            }
            if (i != 0 && i != (numPOPSlices - 1)) {
                if (popSlice.fileProof != null && !popSlice.fileProof.null_proof) {
                    return false;
                }
            }
        }
        return true;
    }

    public TransactionPacket getTxpx(String txpx) {
        return txpxs.get(txpx);
    }

    public void getPOPSliceForCycle(String address, String fileId, String cycleRoot) {
        POPSlice popSlice = relay.getPOPSlice(address, cycleRoot);
        popSlice.transactionPacket = getTxpx(popSlice.addressProof.leafHash);
        popSlice.fileId = fileId;
        popSlice.fileDetail = fileDetails.get(popSlice.transactionPacket.getCurrentCycleRoot()).get(fileId);
        popSlice.fileProof = getFileProof(popSlice.transactionPacket.getCurrentCycleRoot(), address, fileId);
    }

    public void getPOP(String cycleRoot, String address, String fileId) {
        ArrayList<POPSlice> pop = relay.getPOP(address, lastProovedCycle.get(fileId), cycleRoot);
        // TODO: remove first POPs with null proof
        
    }

    public boolean receiveAsset(ArrayList<POPSlice> popSlices, String address, String destinationAddress, String signature, Token token) {
        if (!verifyPOP(popSlices, address, destinationAddress, signature)) {
            return false;
        }
        assets.put(token.getFileId(), token); //store the token under receiver's address
        return true;
    }

// TODO: verifyIntegrity(POP_list) queries the ledger for the hashes contained in POP_lists and returns True/False depending on validity

}