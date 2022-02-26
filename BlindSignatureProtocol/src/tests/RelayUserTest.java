package tests;

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
         * Transaction workflow: 
         * 1. user creates a fileDetail for each transaction
         * 2. user creates the fileTrie
         * 3. user blinds the fileTrie root and requests s(b(F0), I_d) from integrity (not tested here)
         * 4. user sends the txpx to the relay  via sendUpdate(addr, hash)
         * 5a. user requests POP from relay requestPOP(String address, String G_k, String G_n)
         * 5b. user receives POP from relay sendPOP(String address, String G_k, String G_n)
         * 6. sender sends POP to receiver
         * 7. receiver verifies POP (not properly tested here)
         * 
         * */
        
    }
}
