package uk.ac.ucl.cs.digitalcurrency2021.dlt;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.identity.*;



public final class FabricGateway implements LedgerAPI {

    private static final String mspID = "Org1MSP";
    private static final String channelName = "mychannel";
    private static final String chaincodeName = "basic";

    // Path to crypto materials. Relative path from project repository root
    private static final Path cryptoPath = Paths.get("DLT",  "fabric-samples", "test-network",
            "organizations", "peerOrganizations", "org1.example.com");

    // Path to user certificate.
    private static final Path certPath = cryptoPath.resolve(Paths.get("users", "User1@org1.example.com", "msp", "signcerts", "User1@org1.example.com-cert.pem"));
    // Path to user private key directory.
    private static final Path keyDirPath = cryptoPath.resolve(Paths.get("users", "User1@org1.example.com", "msp", "keystore"));
    // Path to peer tls certificate
    private static final Path tlsCertPath = cryptoPath.resolve(Paths.get("peers", "peer0.org1.example.com", "tls", "ca.crt"));

     // Gateway peer end point.
    private static final String peerEndpoint = "localhost:7051";
    private static final String overrideAuth = "peer0.org1.example.com";

    private ManagedChannel channel;
    private Contract contract;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();


    FabricGateway() {
    }


    public void connect() throws Exception {
        // The gRPC client connection should be shared by all Gateway connections to
        // this endpoint.
        channel = newGrpcConnection();

        Gateway gateway = Gateway.newInstance().identity(newIdentity()).signer(newSigner()).connection(channel)
                // Default timeouts for different gRPC calls
                .evaluateOptions(CallOption.deadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(CallOption.deadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(CallOption.deadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(CallOption.deadlineAfter(1, TimeUnit.MINUTES))
                .connect();

        // Get a network instance representing the channel where the smart contract is deployed.
        Network network = gateway.getNetwork(channelName);
        // Get the smart contract from the network.
        contract = network.getContract(chaincodeName);
    }

    public void shutdown() throws Exception {
        this.channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }

    private static ManagedChannel newGrpcConnection() throws IOException, CertificateException {
        Reader tlsCertReader = Files.newBufferedReader(tlsCertPath);
        X509Certificate tlsCert = Identities.readX509Certificate(tlsCertReader);

        return NettyChannelBuilder.forTarget(peerEndpoint)
                .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build()).overrideAuthority(overrideAuth)
                .build();
    }

    private static Identity newIdentity() throws IOException, CertificateException {
        Reader certReader = Files.newBufferedReader(certPath);
        X509Certificate certificate = Identities.readX509Certificate(certReader);

        return new X509Identity(mspID, certificate);
    }

    private static Signer newSigner() throws IOException, InvalidKeyException {
        Path keyPath = Files.list(keyDirPath)
                .findFirst()
                .orElseThrow();
        Reader keyReader = Files.newBufferedReader(keyPath);
        PrivateKey privateKey = Identities.readPrivateKey(keyReader);

        return Signers.newPrivateKeySigner(privateKey);
    }

    /**
     * For testing only
     */
    String getAllAssets() throws GatewayException {

        byte[] result = contract.evaluateTransaction("GetAllAssets");

        return jsonBytesToString(result);
    }


    private String jsonBytesToString(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        JsonElement parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }


    public void writeHash(String cycleRootID, String hash) throws Exception {

        contract.submitTransaction("CreateAsset",  cycleRootID, hash);
    }


    public boolean hasHash(String cycleRootID) throws Exception {

        byte[] evaluateResult = contract.evaluateTransaction("ReadAsset", cycleRootID);
        return !Objects.isNull(JsonParser.parseString(jsonBytesToString(evaluateResult))
                .getAsJsonObject().get("cycleRootHash"));
    }


    public String getHash(String cycleRootID) throws Exception {

        byte[] evaluateResult = contract.evaluateTransaction("ReadAsset", cycleRootID);

        return JsonParser.parseString(jsonBytesToString(evaluateResult))
                .getAsJsonObject().get("cycleRootHash").getAsString();
    }


}

