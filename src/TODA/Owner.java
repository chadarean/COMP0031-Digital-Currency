package src.TODA;
import java.util.*;

public class Owner {
    // Note: the user balance can either be stored on user side or in an MSB database. The former is more private, but
    // the latter could be more robust against technical failure of user application.
    private String userId;
    private HashMap<String, FileTrie> fileTries; // assoctiates file trie objects with addresses for each cycle_root
    private HashMap<String, HashMap<String, TreeMap<String, FileDetail>>> fileDetails; // stores pairs of <file_id, file_detail> sorted by file_id for each address
    // for each cycle root

    public Owner(user_id) {
        this.user_id = user_id;
    }

    private sendUpdate(String address, String txpx); // sends request to Relay

    private transferAsset(String cycleRoot, String address, String asset_id, String dest_pk) {
        FileDetail fileDetail = new FileDetail(dest_pk, null, null); //TODO: is proofs packet hash required?
        if (fileDetails.contains(cycleRoot)) {
            crtFileDetails = fileDetails.get(cycleRoot);
            if (crtFileDetails.contains(address)) {
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

    private ArrayList<Pair<String, FileDetail>> getFileDetailPairs(String cycleRoot, String address) {
        ArrayList<Pair<String, FileDetail>> fileDetailPairs = new ArrayList<Pair<String, FileDetail>>();
        TreeMap t = fileDetails.get(address); //get pairs of <file, fileDetials> for address
        for (Map.Entry<String, String> entry : t.entrySet()) {
            fileDetailPairs.add(new Pair<String, String>(entry.key(), Token.getHashOfString(entry.value())));
        }
        return fileDetailPairs;
    }

    private void createFileTrie(String cycleRoot, String address) {
        MerkleTrie fileTrie = MerkleTrie.createMerkleTrie(getFileDetailPairs(address));
        fileTries.put(address, fileTrie);
    }

// TODO: transferAsset(asset_id, dest_pk) = creates update for asset_id to be transferred to dest_pk

// TODO: getFiles(address) = retrieves assets for the address

// TODO: verifyIntegrity(POP_list) queries the ledger for the hashes contained in POP_lists and returns True/False depending on validity

// TODO: getPOP(address, asset)

// TODO: boolean VerifyPopSlice(POP)

// TODO: boolean verifyPOP(POP_list)

}