## Node Application Overview

The node application can be considered an extension of the user application. In addition to having a wallet with the
previously described functionalities, it also performs the following tasks:

- **Accept Connections**: Establish connections with users or other nodes.
- **Validate and Add Transactions**: Validate incoming transactions from users or other nodes and add them to the
  blockchain.
- **Forward Transactions**: Relay received transactions to other nodes.
- **Broadcast New Wallets**: Inform nodes of new wallets joining the network.
- **Mining and Proof of Work**: Execute mining operations and perform proof of work.
- **Verify Mining Results**: Check if another node has correctly mined a block.

### Additional Features

From the node application, it is also possible to:

- **View the Full Blockchain**: Display the entire blockchain stored locally on disk. Each block can be inspected,
  showing all contained transactions, and for each transaction, the input and output details.
- **View UTXO Array**: Access the stored array of unspent transaction outputs (UTXOs).