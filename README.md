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

`./network.sh deployCC -ccn basic -ccp -ccl java`

