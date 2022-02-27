package tests;

import src.TODA.*;

import java.lang.reflect.Array;
import java.util.*;

import src.POP.*;

public class RelayUserTest {
    public void test() {
        /** 
         * step 1: create k owners, (TODO: integrate balances)
         * for each owner create a number of assets using User.createAsset(creator_addr, addr, signature)
         * USer.createAsset():
         * 1. request (s(d, I_d), I) and crt cycle root for relay from the integrity provider (check user balance)
         * 2. call Token.creatAsset() which returns the Token to the user
         * 3. The user adds the list of files for the address 
         * 
         * create a set of senders and receivers, update the number of assets held by users and add/remove them from the sets accordingly
         * generate x simultaneous random transactions (a, b) by selecting a \in senders and b \in receivers
         * 
         * Transaction workflow1: 
         * 1. user creates a fileDetail for each transaction
         * 2. user creates the fileTrie
         * 3. user blinds the fileTrie root and requests s(b(F0), I_d) from integrity (not tested here)
         * 4. user sends the txpx to the relay  via sendUpdate(addr, hash)
         * 5a. user requests POP from relay requestPOP(String address, String G_k, String G_n)
         * 5b. user receives POP from relay sendPOP(String address, String G_k, String G_n)
         * 6. sender sends POP to receiver
         * 7. receiver verifies POP (not properly tested here)
         * 
         * Transaction workflow2:
         * 1. user requests signature on the blinded file identifier
         * 2. user creates the fileDetail representing an asset transfer
         * 3. user sends the file_identifier and the update to the relay
         * 4. the user gets the POP for the file_identifier from the relay
         * 5. the user sends the signature on the file_identifier, and the POP to receiver to verify 
         * 6. receiver verifies that the signature matches S(file_identifer, I_d) and that the POP is valid
         * 
         * */
    }

    public void testSingleTransaction() {
        ArrayList<String> cycleRoots = new ArrayList<>((List<String>)Arrays.asList({"C_0", "C_1", "C_2", "C_3", "C_4"}));
        Owner a = new Owner("userA");
        String addressA = "01010101010101011";
        int d = 2;
        Token asset = a.createAsset(addressA, d); 
        // blind token.getFileId() and request signature for blinded version
        // unblind signature 
        String signature = "asdfghjkl";
        String destPk = "00000110101010101";
        a.transferAsset(cycleRoots.get(1), addressA, assetId, destPk);
        a.sendUpdates(cycleRoots.get(1), address);
        ArrayList<POPSlice> popSlices1 = relay.sendPOP(addressA, cycleRoots.get(2), cycleRoots.get(2));
        ArrayList<POPSlice> popSlices2 = relay.sendPOP(addressA, cycleRoots.get(2), cycleRoots.get(4));
        ArrayList<POPSlice> popSlices3 = relay.sendPOP(addressA, cycleRoots.get(0), cycleRoots.get(1));
        Owner b = new Owner("userB");
        if (!b.verifyPOP(popSlices1, addressA, destPk, signature)) {
            throw new RuntimeException("Valid POP is not correctly verified!");
        }
        if (!b.verifyPOP(popSlices2, addressA, destPk, signature)) {
            throw new RuntimeException("Valid POP is not correctly verified!");
        }
        if (b.verifyPOP(popSlices3, addressA, destPk, signature)) {
            throw new RuntimeException("Invalid POP is considered valid!");
        }
    }
}
