# COMP0031-Digital-Currency  

# Distributed Ledger Technology 
The Hyperledger Fabric network is an open source enterprise-grade permissioned distributed ledger technology (DLT) platform, 
designed for use in enterprise contexts. In this project, we utilise the network provided by Fabric to keep an immutable log of transactions, represented by the `cyclerootID` and `cyclerootHash` that are stored as key value pairs. 

### Installing Prerequisites
Before you begin, there are a number of prerequisites that need to be installed.

Install docker

`sudo apt install docker`

Install docker compose

`sudo apt install docker-compose`

### Deploying the test network

First, clone the repository

`git clone https://github.com/chadarean/COMP0031-Digital-Currency.git`

Next, set the working directory to 'test-network'

`cd COMP0031-Digital-Currency\DLT\fabric-samples\`

From inside the test-network directory, run the following command to remove any containers or artifacts from any previous runs:

`./network.sh down`

Then, bring up the network

`./network.sh up`

After your test network is deployed, you can examine its components. Run the following command to list all of Docker containers that are running on your machine. You should see the three nodes that were created by the `network.sh` script:

`docker ps -a`

Now that we have peer and orderer nodes running on our machine, we can use the script to create a Fabric channel for transactions between Org1 and Org2. Channels are a private layer of communication between specific network members. Channels can be used only by organizations that are invited to the channel, and are invisible to other members of the network. Each channel has a separate blockchain ledger. Organizations that have been invited “join” their peers to the channel to store the channel ledger and validate the transactions on the channel.

You can use the network.sh script to create a channel between Org1 and Org2 and join their peers to the channel. Run the following command to create a channel with the default name of mychannel:

`./network.sh createChannel`

After you have used the network.sh to create a channel, you can start a chaincode on the channel using the following command:

`./network.sh deployCC -ccn basic -ccp ../../code/chaincode-java -ccl java`

### Interacting with the network

Make sure that you are operating from the test-network directory. You can find the peer binaries in the bin folder of the fabric-samples repository. Use the following command to add those binaries to your CLI Path:

`export PATH=${PWD}/../bin:$PATH`

You also need to set the FABRIC_CFG_PATH to point to the core.yaml file in the fabric-samples repository:

`export FABRIC_CFG_PATH=$PWD/../config/`

You can now set the environment variables that allow you to operate the peer CLI as Org1:

`export CORE_PEER_TLS_ENABLED=true`

`export CORE_PEER_LOCALMSPID="Org1MSP"`

`export CORE_PEER_TLS_ROOTCERT_FILE=${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt`

`export CORE_PEER_MSPCONFIGPATH=${PWD}/organizations/peerOrganizations/org1.example.com/users/Admin@org1.example.com/msp`

`export CORE_PEER_ADDRESS=localhost:7051`

Run the following command to initialise the ledger with the assets

`peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C mychannel -n basic --peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses localhost:9051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"function":"InitLedger","Args":[]}'`

You can now query the ledger from your CLI. The following command can be used to get the list of assets that were added to your channel ledger:

`peer chaincode query -C mychannel -n basic -c '{"Args":["GetAllAssets"]}'`

Chaincodes are invoked when a network member wants to transfer or change an asset on the ledger. Use the following command to change the owner of an asset on the ledger by invoking the asset-transfer (basic) chaincode:

`peer chaincode invoke -o localhost:7050 --ordererTLSHostnameOverride orderer.example.com --tls --cafile ${PWD}/organizations/ordererOrganizations/example.com/orderers/orderer.example.com/msp/tlscacerts/tlsca.example.com-cert.pem -C mychannel -n basic --peerAddresses localhost:7051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt --peerAddresses localhost:9051 --tlsRootCertFiles ${PWD}/organizations/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tls/ca.crt -c '{"function":"TransferAsset","Args":["asset6","Christopher"]}'`

