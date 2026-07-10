# Mini Distributed File System (DFS) 🚀

A lightweight, fully functional Distributed File System built entirely in **Java** using **TCP Socket Programming**. This project is designed to be deployed on a virtual machine (VM) cluster, mimicking the core behavior of large-scale distributed file systems like Hadoop HDFS. 

## 🌟 What is this project?

This Mini DFS implements a decentralized approach to file storage. Instead of storing a file on a single machine where it could be lost due to hardware failure, the system distributes and replicates files across multiple storage nodes. It consists of three core components:

- 🧠 **NameNode**: The central coordinator that manages file metadata, tracking which files are stored on which DataNodes. It also monitors the health of DataNodes.
- 💾 **DataNode**: The worker nodes responsible for storing the actual file chunks (replicas) and handling client requests for data.
- 🖥️ **ClientGUI**: A user-friendly Java Swing graphical application that allows users to interact with the DFS (uploading/downloading files) without using the command line.

## 💡 What problem does it solve?

Traditional centralized file storage is vulnerable to **Single Points of Failure (SPOF)**. If the hard drive crashes, the data is lost forever. Furthermore, as data grows, a single server cannot hold everything or serve it fast enough. 

This DFS solves these problems by providing:
1. **Fault Tolerance**: If a node crashes, your data is safe because redundant copies exist on other nodes.
2. **High Availability**: You can still download your file from alternative DataNodes if the primary one goes down.
3. **Decentralized Data Transfer**: The NameNode only handles metadata, while heavy data transfers happen directly between the Client and DataNodes, preventing bottlenecks.

## ⚙️ Key Functionalities Implemented

- **File Upload & Replication**: When a file is uploaded, it is automatically replicated across multiple DataNodes (Replication Factor = 2) to ensure redundancy.
- **File Download**: Clients dynamically fetch active DataNode IPs from the NameNode and download the file directly from an available DataNode.
- **Heartbeat Mechanism**: DataNodes continuously send heartbeat signals to the NameNode every 5 seconds to prove they are alive.
- **Auto-Recovery**: A background thread running on the NameNode checks node health every 8 seconds. If a DataNode goes offline and a file loses its replicas, the NameNode automatically commands remaining DataNodes to replicate the data elsewhere to maintain the replication factor.
- **Metadata Persistence**: The NameNode saves file locations in a `metadata.txt` file, ensuring the system can recover metadata upon restart.

## 🧠 Under-the-Hood Engineering Details

While not immediately visible to the end-user, this project implements several core distributed systems concepts to ensure performance and reliability:

- **4KB Data Chunking / Streaming**: Network data transfers (upload/download/replication) use a 4096-byte chunking buffer (`byte[] buffer = new byte[4096]`). This streams data incrementally instead of loading the entire file into memory at once, preventing `OutOfMemory` errors when handling large files.
- **Daisy-Chain / Parallel Replication**: To preserve the client's network bandwidth, the client only sends the file to the *primary* DataNode. The primary DataNode then dynamically spawns concurrent background threads to replicate the data in parallel to all other assigned DataNodes.
- **Client-Side Load Balancing**: When downloading a file, the ClientGUI receives a list of all DataNodes holding the replica. It then randomly selects one of them (`new java.util.Random().nextInt(count)`). This prevents a bottleneck where all clients hammer the exact same node for popular files.
- **Randomized Data Distribution**: When the NameNode assigns storage nodes for a new upload, it shuffles the list of alive DataNodes (`Collections.shuffle(alive)`). This ensures files are evenly distributed across the cluster rather than consistently filling up the first few nodes in the list.
- **Multithreaded Architecture**: The system heavily utilizes Java multithreading. The `NameNode` spawns separate `ClientHandler` threads for every incoming TCP connection to serve multiple clients simultaneously without blocking, while maintaining isolated background daemon threads for continuous heartbeat monitoring and auto-recovery checks.

---

## 🛠️ Setup & Getting Started

> **Important Setup Guide:** Refer to this chat link for an in-depth guide on how to set up the environment: [ChatGPT Setup Guide](https://chatgpt.com/share/69b294ab-9d3c-8003-9e84-b6f54b135f7f)

**Note:** I might have made some changes in the code, so make sure to copy the latest code from GitHub. Also, as originally noted, the download button might not work in all older versions because `DataNode.java` did not contain the script for it initially, leaving only the upload functionality. *(Recent code updates in this directory do include download/READ functionality, but keep this in mind for older iterations).*

### Steps to Perform in GUI:
1. Fill in the **IP of your Data Node / Name Node**.
2. The **Port** is correct by default, but if you changed it in the code, change it here too.
3. *Note on folders:* The chat guide says to name the folder `mini-dfs`, but I have named it `dfs`. Keep in mind to name it `dfs` because it is also used in the `DataNode.java` file. (Also, DataNodes store files in a `storage/` directory locally).
4. Keep the **3rd field (DFS File Path) empty** for upload because the storage path is hardcoded in `DataNode.java`.
5. Press the **'Connect NameNode'** button.
6. Then press **'Upload File'** to select and distribute your file.

---
*For any other queries, feel free to call me!*
