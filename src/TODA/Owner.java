package src.TODA;

import java.util.*;
import src.POP.*;

public class Owner {
    // Note: the user balance can either be stored on user side or in an MSB database. The former is more private, but
    // the latter could be more robust against technical failure of user application.
    private String userId;
    private HashMap<String, MerkleTrie.TrieNode> fileTries; // assoctiates file trie objects with addresses for each cycle_root
    private HashMap<String, HashMap<String, TreeMap<String, FileDetail>>> fileDetails; // stores pairs of <file_id, file_detail> sorted by file_id for each address
    // for each cycle root

    public Owner(String userId) {
        this.userId = userId;
    }

    private void sendUpdate(String address, String txpxHash) {
        // sends request to Relay
    } 

    private void transferAsset(String cycleRoot, String address, String asset_id, String dest_pk) {
        // creates update for asset_id to be transferred to dest_pk at cycleRoot
        // using a map ensures that an address makes a single update in a cycle for an asset_id
        FileDetail fileDetail = new FileDetail(dest_pk, null, null); //TODO: is proofs packet hash required?
        if (fileDetails.containsKey(cycleRoot)) { 
            HashMap<String, TreeMap<String, FileDetail>> crtFileDetails = fileDetails.get(cycleRoot);
            if (crtFileDetails.containsKey(address)) {
                crtFileDetails.get(address).put(asset_id, fileDetail);
            } else {
                crtFileDetails.put(address, new TreeMap<String, FileDetail>(){{put(asset_id, fileDetail)}});
            }
        } else {
            fileDetails.put(cycleRoot, new HashMap<String, TreeMap<String, FileDetail>>() {{put(address,
                    new TreeMap<String, FileDetail>(){{put(asset_id, fileDetail)}})}});
        }
    }

    private void removeCycleRootData(ArrayList<String> cycleRoots) {
        for (String cycleRoot: cycleRoots) {
            fileDetails.remove(cycleRoot);
            fileTries.remove(cycleRoot);
        }
    }

    private ArrayList<Pair<String, String>> getFileDetailPairs(String cycleRoot, String address) {
        ArrayList<Pair<String, String>> fileDetailPairs = new ArrayList<Pair<String, String>>();
        if (!fileDetails.containsKey(cycleRoot) || !(fileDetails.get(cycleRoot).containsKey(address))) {
            return fileDetailPairs;
        }

        TreeMap<String, FileDetail> t = fileDetails.get(cycleRoot).get(address); //get pairs of <file, fileDetials> for address
        for (Map.Entry<String, FileDetail> entry : t.entrySet()) {
            fileDetailPairs.add(new Pair<String, String>(entry.getKey(), Token.getHashOfString(entry.getValue().toString())));
        }
        return fileDetailPairs;
    }

    private MerkleTrie.TrieNode createFileTrie(String cycleRoot, String address) {
        MerkleTrie.TrieNode fileTrie = MerkleTrie.createMerkleTrie(getFileDetailPairs(cycleRoot, address));
        fileTries.put(address, fileTrie);
        return fileTrie;
    }

    private void sendUpdates(String cycleRoot, String address, String signature) {
        MerkleTrie.TrieNode fileTrie = createFileTrie(cycleRoot, address);
        TransactionPacket txpx = new TransactionPacket(cycleRoot, address, fileTrie.value, null, signature);
        
    }

// TODO: getFiles(address) = retrieves assets for the address

// TODO: verifyIntegrity(POP_list) queries the ledger for the hashes contained in POP_lists and returns True/False depending on validity

// TODO: getPOP(address, asset)

// TODO: boolean VerifyPopSlice(POP)

// TODO: boolean verifyPOP(POP_list)

}