package com.mycompany.app.test;

import com.google.gson.Gson;
import com.mycompany.app.BlindSignature;
import com.mycompany.app.MSB.MSB;
import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.StandardResponse;
import com.mycompany.app.StatusResponse;
import com.mycompany.app.TODA.*;
import com.mycompany.app.Wallet;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import spark.Spark;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static spark.Spark.port;

public class Experiment3 {
    public static Random rand = new Random();

    public static String getMostRecentCycle() throws IOException {
        HttpGet request = new HttpGet("http://localhost:8090/Relay/getMostRecentCycleTrieNode");
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String crtCycleString = EntityUtils.toString(entity);
        MerkleTrie.TrieNode crtCycle = new Gson().fromJson(crtCycleString, MerkleTrie.TrieNode.class);
        return crtCycle.value;
    }

    // Create and Transfer Asset
    public static void testSingleTransaction(int addressSize) throws IOException {
        MSB msb = new MSB();
        ArrayList<String> cycleRoots = new ArrayList<>();
        Relay r = new Relay();
        MerkleTrie.TrieNode genesisCycleRoot = TestUtils.createRandomCycleTrie(r);
        cycleRoots.add(genesisCycleRoot.value);
        Owner a = new Owner("userA");
        a.relay = r;
        Wallet w = new Wallet("userA");
        String addressA = TestUtils.getRandomXBitAddr(rand, addressSize);
        int d = 2;
        // the wallet will call the createAsset method which will call createAsset from the Owner.java class
        Token asset = a.createAsset(cycleRoots.get(0), addressA, d, "");

        // blind token.getFileId() and request signature for blinded version
        byte[] msg = w.convert_token_to_byte(asset);
        msb.generate_keypairs(256);
        w.get_issuer_publickey();
        w.setBlindingFactor();
        byte[] blinded_msg = w.blind_message(msg);

        HttpGet request = new HttpGet("http://localhost:3080/requestSign/"+ Utils.getStringFromByte(blinded_msg, blinded_msg.length));
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String signedMessage = EntityUtils.toString(entity);
        byte[] sigBymsb = signedMessage.getBytes(StandardCharsets.UTF_8);
        byte[] unblindedSigBymsb = BlindSignature.unblind(w.issuer_public_key, w.blindingFactor, sigBymsb);
        String signature = new String(unblindedSigBymsb, StandardCharsets.UTF_8); // unblinded bsig for asset.fileKernel
        asset.addSignature(signature);

        String destPk = TestUtils.getRandomXBitAddr(rand, addressSize);
        a.transferAsset(cycleRoots.get(0), addressA, asset, destPk);
        a.sendUpdates(cycleRoots.get(0), addressA);


        //MerkleTrie.TrieNode cycleRootNode1 = r.createCycleTrie();
        request = new HttpGet("http://localhost:8090/Relay/createCycleTrie");
        client = HttpClients.createDefault();
        response = client.execute(request);
        entity = response.getEntity();
        String merkleTrieString = EntityUtils.toString(entity);

        MerkleTrie.TrieNode cycleRootNode1 = new Gson().fromJson(merkleTrieString, MerkleTrie.TrieNode.class);



        cycleRoots.add(cycleRootNode1.value); // update1 cycle hash


        //POPSlice popSlice1 = r.getPOPSlice(addressA, cycleRoots.get(1));
        //a.receivePOP(addressA, popSlice1);

        int offset = TestUtils.getCycleId(getMostRecentCycle());
        System.out.println(offset);

        request = new HttpGet("http://localhost:8090/Relay/getPOPSlice/"+addressA+"/"+Integer.toString(offset));
        client = HttpClients.createDefault();
        response = client.execute(request);
        entity = response.getEntity();
        String popSliceString = EntityUtils.toString(entity);

        POPSlice popSlice_t = new Gson().fromJson(popSliceString, POPSlice.class);
        a.receivePOP(addressA, popSlice_t);


        ArrayList<POPSlice> pop = a.getPOPUsingCache(cycleRoots.get(1), addressA, asset);
        

        Owner b = new Owner("userB");
        if (!b.verifyPOP(pop, addressA, destPk, signature)) {
            throw new RuntimeException("Valid POP is not correctly verified!");
        }
        b.receiveAsset(pop, addressA, destPk, signature, asset);
        ArrayList<Token> assetsB = b.assets.get(destPk);
        if (assetsB == null || !assetsB.get(assetsB.size()-1).getFileId().equals(asset.getFileId())) {
            throw new RuntimeException("Asset not correctly redeemed!");
        }

        r.closeConnection();
    }

    public static void main(String[] args) throws IOException {
        //testSingleTransaction(MerkleTrie.ADDRESS_SIZE);
        //System.out.println("Tests passed!");

        port(3456);
        Spark.get("/Experiment3", (request, response) -> {
            try{
                testSingleTransaction(MerkleTrie.ADDRESS_SIZE);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,"Passed"));
            }catch(Exception e){
                return null;
            }
        });
    }
}
