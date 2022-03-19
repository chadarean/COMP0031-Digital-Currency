package com.mycompany.app.test;

import com.google.gson.Gson;
import com.mycompany.app.POP.Token;
import com.mycompany.app.TODA.MerkleTrie;
import com.mycompany.app.TODA.Relay;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;

public class TestUtils {
    public static Random rand = new Random();

    public static int randState = 0;

    public static int maxRandNumbers = 10000000;

    public static ArrayList<Integer> randNumbers;

    public static void resetState() {
        randState = 0;
    }

    public static void setRandomNumbers() {
        randNumbers = new ArrayList<>();
        for (int i = 0; i < maxRandNumbers; ++ i) {
            randNumbers.add(rand.nextInt());
        }
    }

    public static int getNextInt() {
        randState += 1;
        return randNumbers.get(randState-1);
    }

    public static String getRandomXBitAddr(Random rand, int addrSize) {
        StringBuilder addr = new StringBuilder();
        addr.append(0);
        for (int j = 1; j < MerkleTrie.ADDRESS_SIZE; ++ j) {
            addr.append(Integer.toString(rand.nextInt(2)));
        }
        return addr.toString();
    }

    public static MerkleTrie.TrieNode createRandomCycleTrie(Relay r) throws IOException {
        HttpGet request = new HttpGet("http://localhost:8090/Relay/addUpdateFromDownstream/"+TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE)+"/"+Token.getHashOfString(TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE)));
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);

        request = new HttpGet("http://localhost:8090/Relay/createCycleTrie");
        client = HttpClients.createDefault();
        response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String merkleTrieString = EntityUtils.toString(entity);
        return new Gson().fromJson(merkleTrieString, MerkleTrie.TrieNode.class);

    }

    public static MerkleTrie.TrieNode createRandomCycleTrie(Relay r, int nUpdates) throws IOException {
        for (int i = 0; i < nUpdates; ++ i) {

            HttpGet request = new HttpGet("http://localhost:8090/Relay/addUpdateFromDownstream/"+TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE)+"/"+Token.getHashOfString(TestUtils.getRandomXBitAddr(rand, MerkleTrie.ADDRESS_SIZE)));
            CloseableHttpClient client = HttpClients.createDefault();
            CloseableHttpResponse response = client.execute(request);


        }
        HttpGet request = new HttpGet("http://localhost:8090/Relay/createCycleTrie");
        CloseableHttpClient client = HttpClients.createDefault();
        CloseableHttpResponse response = client.execute(request);
        HttpEntity entity = response.getEntity();
        String merkleTrieString = EntityUtils.toString(entity);
        return new Gson().fromJson(merkleTrieString, MerkleTrie.TrieNode.class);
    }


}
