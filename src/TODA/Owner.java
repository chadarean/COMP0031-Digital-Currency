package src.TODA;

import java.util.*;
import src.POP.*;

public class Owner {
    // Note: the user balance can either be stored on user side or in an MSB database. The former is more public, but
    // the latter could be more robust against technical failure of user application.
    public String userId;
    public HashMap<String, HashMap<String, MerkleTrie.TrieNode>> fileTries; // assoctiates addresses with file trie objects for each cycle_root
    // fileTries[cycleRoot][address] = fileTrie object with files under address in cycleRoot
    public HashMap<String, HashMap<String, TreeMap<String, FileDetail>>> fileDetails; // stores pairs of <file_id, file_detail> sorted by file_id 
    // for each address for each cycle root: fileDetails[cycleRoot][address] = sorted set of <file_id, file_detail>
    public HashMap<String, Token> assets;

    public Owner(String userId) {
        this.userId = userId;
        fileTries = new HashMap<>();
        fileDetails = new HashMap<>();
        assets = new HashMap<>();

    }

    public Token createAsset(String address, int d) {
        String cycleRoot = ""; // TODO: getCurrentCycleRoot() from Relay DB? via MSB?
        String signature = ""; // TODO: get s(d, I_d) from DLT via MSB
        Token token = new Token();
        token.createAsset(cycleRoot, address, address, signature);
        assets.put(token.getFileId(), token);
        // TODO: should we add the token to a DB
        return token;
    }

    public void sendUpdate(String address, String txpxHash) {
        // sends request to Relay
        // TODO: the relay could send back the POP after creating the trie
    } 

    public void transferAsset(String cycleRoot, String address, String assetId, String dest_pk) {
        // creates update for assetId to be transferred to dest_pk at cycleRoot
        // using a map ensures that an address makes a single update in a cycle for an assetId
        Token asset = assets.get(assetId);
        asset.createUpdate(address, dest_pk);
        FileDetail fileDetail = asset.fileDetail; //TODO: is proofs packet hash required?
        if (fileDetails.containsKey(cycleRoot)) { 
            HashMap<String, TreeMap<String, FileDetail>> crtFileDetails = fileDetails.get(cycleRoot);
            if (crtFileDetails.containsKey(address)) {
                crtFileDetails.get(address).put(assetId, fileDetail);
            } else {
                crtFileDetails.put(address, new TreeMap<String, FileDetail>(){{put(assetId, fileDetail);}});
            }
        } else {
            fileDetails.put(cycleRoot, new HashMap<String, TreeMap<String, FileDetail>>() {{put(address,
                    new TreeMap<String, FileDetail>(){{put(assetId, fileDetail);}});}});
        }

    }

    public void removeCycleRootData(ArrayList<String> cycleRoots) {
        for (String cycleRoot: cycleRoots) {
            fileDetails.remove(cycleRoot); // TODO: should it remove the addresses too or set the value to null?
            fileTries.remove(cycleRoot);
        }
    }

    private TreeMap<String, FileDetail> getFileDetailForAddress(String cycleRoot, String address) {
        HashMap<String, TreeMap<String, FileDetail>> crtFileDeitails = fileDetails.get(cycleRoot);
        if (crtFileDeitails == null) {
            return null;
        }
        TreeMap<String, FileDetail> t = crtFileDeitails.get(address); //get pairs of <file, fileDetials> for address
        return t;
    }

    private MerkleTrie.TrieNode getFileTrieForAddress(String cycleRoot, String address) {
        HashMap<String, MerkleTrie.TrieNode> crtFileTries = fileTries.get(cycleRoot);
        if (crtFileTries == null) {
            return null;
        }
        MerkleTrie.TrieNode t = crtFileTries.get(address); //get pairs of <file, fileDetials> for address
        return t;
    }

    public ArrayList<Pair<String, String>> getFileDetailPairs(String cycleRoot, String address) {
        ArrayList<Pair<String, String>> fileDetailPairs = new ArrayList<Pair<String, String>>();

        TreeMap<String, FileDetail> t = getFileDetailForAddress(cycleRoot, address); //get pairs of <file, fileDetials> for address
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
        MerkleTrie.TrieNode fileTrie = MerkleTrie.createMerkleTrie(getFileDetailPairs(cycleRoot, address));
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
        String signature = ""; // TODO get signature on blinded fileTrie
        TransactionPacket txpx = new TransactionPacket(cycleRoot, address, fileTrie.value, null, signature);
        sendUpdate(address, Token.getHashOfString(txpx.toString()));
    }

    public MerkleProof getFileProof(String cycleRoot, String address, String assetId) {
        MerkleTrie.TrieNode fileTrie = getFileTrieForAddress(cycleRoot, address);
        return MerkleTrie.getMerkleProof(assetId, fileTrie);
    }

// TODO: getFiles(address) = retrieves assets for the address

// TODO: verifyIntegrity(POP_list) queries the ledger for the hashes contained in POP_lists and returns True/False depending on validity

// TODO: getPOP(address, asset)

// TODO: boolean VerifyPopSlice(POP)

// TODO: boolean verifyPOP(POP_list)

}