package com.mycompany.app.test;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mycompany.app.BlindSignature;
import com.mycompany.app.MSB.MSB;
import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.TODA.MerkleProof;
import com.mycompany.app.TODA.MerkleTrie;
import com.mycompany.app.TODA.Owner;
import com.mycompany.app.TODA.Utils;
import com.mycompany.app.Wallet;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static java.lang.Thread.sleep;

public class OwnerThread implements Runnable{
    private final MSB msb;
    Owner owner;
    Wallet wallet;
    HashMap<String, ArrayList<Token>> tokensForAddr = new HashMap<>();
    String pkeyA;
    String pkeyB;
    Integer waitingCycles = 8;

    // The run method will call createTokens , sleep w seconds, then call transferAsset, sleep 1 second, then ask for POP

    OwnerThread(String userId) {
        this.owner = new Owner(userId);
        this.wallet = new Wallet(userId);
        this.msb = new MSB();
    }

    public void run() {
        setAddresses();
        try {
            createTokens(TestUtils.getMostRecentCycle(), pkeyA);
            try {
                sleep(1000L * (waitingCycles+1));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String tranferCycle = TestUtils.getMostRecentCycle();
            transferTokens(pkeyA, pkeyB, tranferCycle);
            try {
                sleep(2000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            String nextCycle = TestUtils.getMostRecentCycle();
            while (tranferCycle.compareTo(nextCycle) == 0) {
                nextCycle = TestUtils.getMostRecentCycle();
            }

            String crtCycleRoot = TestUtils.getMostRecentCycle();
            getVerifyPOP(pkeyA, crtCycleRoot, TestUtils.getCycleId(crtCycleRoot));
        } catch (IOException e) {
            throw new RuntimeException("IO Exception!");
        }
    }

    void setAddresses() {
        Random rand = new Random();
        pkeyA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
        pkeyB = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
    }

    void createTokens(String cycleRoot, String addressA) throws IOException {
        int tokens_i = 1; // only 1 token per address
        tokensForAddr.put(addressA, new ArrayList<Token>());
        for (int j = 0; j < tokens_i; ++j) {
            String certSignature = "s((I_d, d), I)"; // certificate signature s((I_d, d), I)
            // create asset for addressA, issuance cycle root C_.get(c), denominator j+1 and certSignature
            Token asset = owner.createAsset(cycleRoot, addressA, j + 1, certSignature);
            byte[] msg = wallet.convert_token_to_byte(asset);
            msb.generate_keypairs(256);

            wallet.get_issuer_publickey();
            wallet.setBlindingFactor();
            byte[] blinded_msg = wallet.blind_message(msg);

            HttpGet request = new HttpGet("http://localhost:3080/requestSign/"+ Utils.getStringFromByte(blinded_msg, blinded_msg.length));
            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String signedMessage = EntityUtils.toString(entity);
            byte[] sigBymsb = signedMessage.getBytes(StandardCharsets.UTF_8);
            byte[] unblindedSigBymsb = BlindSignature.unblind(wallet.issuer_public_key, wallet.blindingFactor, sigBymsb);
            String signature = new String(unblindedSigBymsb, StandardCharsets.UTF_8); // unblinded bsig for asset.fileKernel
            asset.addSignature(signature);
            tokensForAddr.get(addressA).add(asset);
        }
    }

    void transferTokens(String addressA, String addressB, String cycleRoot) throws IOException {
        ArrayList<Token> crtTokens = tokensForAddr.get(addressA);
        for (Token asset: crtTokens) {
            owner.transferAsset(cycleRoot, addressA, asset, addressB);
        }
        owner.sendUpdates(cycleRoot, addressA);
    }

    void getVerifyPOP(String addressA, String cycleRoot, int cycleRootId) throws IOException {
        POPSlice popSlice_t = null;
        int cycleId = cycleRootId;
        while (cycleId > 0) {
            HttpGet request = new HttpGet("http://localhost:8090/Relay/getPOPSlice/"+addressA+"/"+Integer.toString(cycleId));
            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String popSliceString = EntityUtils.toString(entity);
            try {
                popSlice_t = new Gson().fromJson(popSliceString, POPSlice.class);
            } catch (JsonSyntaxException e) {
                System.out.println(popSliceString);
                throw new RuntimeException("JSON ex");
            }

            if (!popSlice_t.addressProof.null_proof) {
                break;
            }
            -- cycleId;
        }
        if (cycleId == 0) {
            throw new RuntimeException("Why?");
        }
        owner.receivePOP(addressA, popSlice_t);

        ArrayList<Token> tokens_i = tokensForAddr.get(addressA);
        for (Token token_j : tokens_i) {
            ArrayList<POPSlice> pop;
            pop = owner.getPOPUsingCache(cycleRoot, addressA, token_j);
            MerkleProof proof = owner.getFileProof(cycleRoot, addressA, token_j.getFileId());

            String fileDetailHash = token_j.getFileDetail();

            if (!proof.verify(token_j.getFileId(), fileDetailHash)) {
                throw new RuntimeException("Incorrect proof created!");
            }
        }
    }
}
