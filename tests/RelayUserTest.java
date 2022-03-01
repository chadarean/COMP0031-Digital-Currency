package tests;

import src.TODA.*;

import java.lang.reflect.Array;
import java.util.*;

import src.POP.*;

public class RelayUserTest {
    public static Random rand = new Random();
    public static void testMutipleTransactions(int numConsumers, int numMerchants, int numTokens, int X) {
        ArrayList<String> cycleRoots = new ArrayList<>();
        Relay r = new Relay();
        MerkleTrie.TrieNode genesisCycleRoot = TestUtils.createGenesisCycleTrie(r);
        cycleRoots.add(genesisCycleRoot.value);
        ArrayList<Owner> consumers = new ArrayList<>();
        ArrayList<Owner> merchants = new ArrayList<>();
        HashMap<Owner, String> ownerPubKey = new HashMap<>();
        HashMap<Integer, ArrayList<Token>> ownerAsset = new HashMap<>();

        for (int i = 0; i < numConsumers+numMerchants; ++ i) {
            if (i < numConsumers) {
                Owner a = new Owner(Integer.toString(i));
                String address = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
                consumers.add(a);
                ownerPubKey.put(a, address);
                ownerAsset.put(i, new ArrayList<>());
                for (int j = 0; j < numTokens; ++ j) {
                    ownerAsset.get(i).add(a.createAsset(genesisCycleRoot.value, address, j+1, ""));
                }
            } else {
                merchants.add(new Owner(Integer.toString(i)));
            }
        }

        for (int cycle = 1; cycle <= X; ++ cycle) {
            ArrayList<Pair<Integer, Integer>> pairs = new ArrayList<>();
            for (int i = 0; i < 5; ++ i) {
                int consumerIdx = rand.nextInt() % numConsumers;
                int merchantIdx = rand.nextInt() % numMerchants;
                Owner a = consumers.get(consumerIdx);
                Owner b = merchants.get(merchantIdx);
                a.transferAsset(cycleRoots.get(cycle-1), ownerPubKey.get(a), ownerAsset.get(consumerIdx).get(i), ownerPubKey.get(b));
                pairs.add(new Pair<Integer, Integer>(consumerIdx, merchantIdx));
                a.sendUpdates(cycleRoots.get(cycle-1), ownerPubKey.get(a));
            }
            MerkleTrie.TrieNode cycleRootNode = r.createCycleTrie();
            cycleRoots.add(cycleRootNode.value); // update cycle hash
            for (Pair<Integer, Integer> p: pairs) {
                Owner a = consumers.get(p.key);
                Owner b = merchants.get(p.value - numConsumers);
                String addressA = ownerPubKey.get(p.key);
                String addressB = ownerPubKey.get(p.value);
                POPSlice popSlice = r.getPOPSlice(addressA, cycleRoots.get(cycle));
                a.receivePOP(addressA, popSlice);
                for (int i = 0; i < 5; ++ i) {
                    Token asset = ownerAsset.get(p.key).get(i);
                    ArrayList<POPSlice> pop = a.getPOP(cycleRoots.get(cycle), addressA, asset);
                    if (!b.verifyPOP(pop, addressA, addressB, "")) {
                        throw new RuntimeException("Valid POP is not correctly verified!");
                    }
                }
            }
        }

    }

    public static void testSingleTransaction(int addressSize) {
        ArrayList<String> cycleRoots = new ArrayList<>();
        Relay r = new Relay();
        MerkleTrie.TrieNode genesisCycleRoot = TestUtils.createGenesisCycleTrie(r);
        cycleRoots.add(genesisCycleRoot.value);

        Owner a = new Owner("userA");
        a.relay = r;
        String addressA = TestUtils.getRandomXBitAddr(rand, addressSize);
        int d = 2;
        Token asset = a.createAsset(cycleRoots.get(0), addressA, d, ""); 
        // blind token.getFileId() and request signature for blinded version
        // unblind signature 
        String signature = "asdfghjkl";
        String destPk = TestUtils.getRandomXBitAddr(rand, addressSize);
        a.transferAsset(cycleRoots.get(0), addressA, asset, destPk);
        a.sendUpdates(cycleRoots.get(0), addressA);
        MerkleTrie.TrieNode cycleRootNode1 = r.createCycleTrie();
        cycleRoots.add(cycleRootNode1.value); // update1 cycle hash
        POPSlice popSlice1 = r.getPOPSlice(addressA, cycleRoots.get(1));
        a.receivePOP(addressA, popSlice1);
        System.out.println(r.cycleId.keySet());
        System.out.println(cycleRoots);
        ArrayList<POPSlice> pop = a.getPOP(cycleRoots.get(1), addressA, asset);
        

        Owner b = new Owner("userB");
        if (!b.verifyPOP(pop, addressA, destPk, signature)) {
            throw new RuntimeException("Valid POP is not correctly verified!");
        }
        b.receiveAsset(pop, addressA, destPk, signature, asset);
        ArrayList<Token> assetsB = b.assets.get(destPk);
        if (assetsB == null || !assetsB.get(assetsB.size()-1).getFileId().equals(asset.getFileId())) {
            throw new RuntimeException("Asset not correctly redeemed!");
        }
    }

    public static void main(String[] args) {
        testSingleTransaction(MerkleTrie.ADDRESS_SIZE);
        testMutipleTransactions(4, 2, 10, 3);
    }
}
