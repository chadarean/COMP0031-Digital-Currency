package com.mycompany.app.test;

import com.google.gson.Gson;
import com.mycompany.app.BlindSignature;
import com.mycompany.app.MSB.MSB;
import com.mycompany.app.POP.POPSlice;
import com.mycompany.app.POP.Token;
import com.mycompany.app.StandardResponse;
import com.mycompany.app.StatusResponse;
import com.mycompany.app.TODA.MerkleTrie;
import com.mycompany.app.TODA.Owner;
import com.mycompany.app.TODA.Relay;
import com.mycompany.app.TODA.Utils;
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
import java.util.ArrayList;
import java.util.Random;

import static spark.Spark.port;

public class Experiment3Part2 {
    public static Random rand = new Random();

    static ArrayList<POPSlice> pop;
    static String addressA;
    static String destPk;
    static String signature;
    static Token asset;

    private static void setup() throws IOException {
        MSB msb = new MSB();
        ArrayList<String> cycleRoots = new ArrayList<>();
        Relay r = new Relay();
        MerkleTrie.TrieNode genesisCycleRoot = TestUtils.createRandomCycleTrie(r);
        cycleRoots.add(genesisCycleRoot.value);
        Owner a = new Owner("userA");
        a.relay = r;
        Wallet w = new Wallet("userA");
        addressA = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
        int d = 2;
        // the wallet will call the createAsset method which will call createAsset from the Owner.java class
        asset = a.createAsset(cycleRoots.get(0), addressA, d, "");

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
        signature = new String(unblindedSigBymsb, StandardCharsets.UTF_8); // unblinded bsig for asset.fileKernel
        asset.addSignature(signature);

        destPk = TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE);
        a.transferAsset(cycleRoots.get(0), addressA, asset, destPk);
        a.sendUpdates(cycleRoots.get(0), addressA);


        //MerkleTrie.TrieNode cycleRootNode1 = r.createCycleTrie();
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
        request = new HttpGet("http://localhost:8090/Relay/getPOPSlice/"+addressA+"/"+Integer.toString(offset));
        client = HttpClients.createDefault();
        response = client.execute(request);
        entity = response.getEntity();
        String popSliceString = EntityUtils.toString(entity);

        POPSlice popSlice_t = new Gson().fromJson(popSliceString, POPSlice.class);
        a.receivePOP(addressA, popSlice_t);


        pop = a.getPOPUsingCache(cycleRoots.get(1), addressA, asset);

        r.closeConnection();
    }

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
    public static void testValidatingPOP(int addressSize) throws IOException {


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

    public static void main(String[] args) throws IOException {
        //testSingleTransaction(MerkleTrie.ADDRESS_SIZE);
        //System.out.println("Tests passed!");

        setup();

        port(3456);
        Spark.get("/Experiment3b", (request, response) -> {
            try{
                testValidatingPOP(MerkleTrie.ADDRESS_SIZE);
                return new Gson().toJson(new StandardResponse(StatusResponse.SUCCESS,"Passed"));
            }catch(Exception e){
                return null;
            }
        });
    }
}
