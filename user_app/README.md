## User Application

The user application consists of a wallet that allows users to send and receive transactions. Upon the first launch, a
key pair is generated and saved to a file in secondary storage, which will be loaded on subsequent application startups.
After loading the file with the keys, users are prompted to enter the IP address of the node they wish to connect to. If
the connection is successful, a menu is displayed to interact with the node.

### Available Operations

- **Create a New Transaction**:
  Users are prompted to specify the recipient and the amount to transfer. The amount must be positive and above a set
  threshold. The recipient must be chosen from a list displayed on-screen that includes all wallet addresses that have
  connected to the network at least once.

- **Check Current Balance**:
  View the current balance of the wallet.

- **View Transaction Details**:
  Display information about both sent and received transactions.

- **Disconnect**:
  End the current connection with the node.