import java.io.*;
import java.net.*;
import java.util.*;

public class NameNode {

    static int REPLICATION_FACTOR = 2;
    static final String METADATA_FILE = "metadata.txt";
    static HashMap<String, List<String>> fileMap = new HashMap<>();
    static HashMap<String, Long> heartbeatMap = new HashMap<>();

    static List<String> dataNodes = new ArrayList<>();

    static synchronized void saveMetadata() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(METADATA_FILE))) {

            for (String file : fileMap.keySet()) {
                List<String> nodes = fileMap.get(file);

                writer.write(file + ":" + String.join(",", nodes));
                writer.newLine();
            }

            System.out.println("Metadata saved");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void loadMetadata() {
        File file = new File(METADATA_FILE);

        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;

            while ((line = reader.readLine()) != null) {

                String[] parts = line.split(":");

                String filename = parts[0];
                List<String> nodes = Arrays.asList(parts[1].split(","));

                fileMap.put(filename, new ArrayList<>(nodes));
            }

            System.out.println("Metadata loaded");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static boolean isAlive(String ip) {
        if (!heartbeatMap.containsKey(ip)) return false;
        return (System.currentTimeMillis() - heartbeatMap.get(ip)) < 10000;
    }

    static List<String> getAliveNodes() {
        List<String> alive = new ArrayList<>();
        for (String dn : dataNodes) {
            if (isAlive(dn)) alive.add(dn);
        }
        return alive;
    }

    static void checkAndRecover() {

        for (String file : fileMap.keySet()) {

            List<String> nodes = fileMap.get(file);

            List<String> alive = new ArrayList<>();

            for (String dn : nodes) {
                if (isAlive(dn)) {
                    alive.add(dn);
                }
            }

            // if replication broken
            if (alive.size() < REPLICATION_FACTOR) {

                System.out.println("⚠ Recovering file: " + file);

                // find target node
                for (String candidate : dataNodes) {

                    if (!alive.contains(candidate) && isAlive(candidate)) {

                        String source = alive.get(0);
                        String target = candidate;

                        replicateFile(source, target, file);

                        // update metadata
                        nodes.add(target);
                        saveMetadata();

                        break;
                    }
                }
            }


        }
    }

    static void replicateFile(String sourceIP, String targetIP, String filename) {

        try {
            Socket socket = new Socket(sourceIP, 9001);

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            out.writeUTF("REPLICATE");
            out.writeUTF(filename);
            out.writeUTF(targetIP);
            out.writeInt(9001);

            socket.close();

            System.out.println("Recovery: " + filename +
                    " from " + sourceIP + " -> " + targetIP);

        } catch (Exception e) {
            System.out.println("Recovery failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(9000);
        System.out.println("NameNode running...");
        loadMetadata();
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(8000); // every 8 sec
                    checkAndRecover();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        while (true) {
            Socket socket = server.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    static class ClientHandler implements Runnable {

        Socket socket;

        ClientHandler(Socket s) {
            socket = s;
        }

        public void run() {
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                String req = in.readUTF();

                switch (req) {

                    case "UPLOAD_FILE" -> {
                        String filename = in.readUTF();

                        List<String> alive = getAliveNodes();

                        if (alive.size() < REPLICATION_FACTOR) {
                            out.writeInt(0);
                            return;
                        }

                        Collections.shuffle(alive);
                        List<String> selected = alive.subList(0, REPLICATION_FACTOR);

                        fileMap.put(filename, new ArrayList<>(selected));
                        saveMetadata();

                        out.writeInt(selected.size());

                        for (String dn : selected) {
                            out.writeUTF(dn);
                            out.writeInt(9001);
                        }

                        System.out.println("Upload: " + filename + " -> " + selected);
                    }

                    case "READ_FILE" -> {
                        String filename = in.readUTF();

                        List<String> nodes = fileMap.get(filename);

                        if (nodes == null) {
                            out.writeInt(0);
                            return;
                        }

                        List<String> alive = new ArrayList<>();

                        for (String dn : nodes) {
                            if (isAlive(dn)) alive.add(dn);
                        }

                        if (alive.isEmpty()) {
                            out.writeInt(0);
                            return;
                        }

                        out.writeInt(alive.size());

                        for (String dn : alive) {
                            out.writeUTF(dn);
                            out.writeInt(9001);
                        }

                        System.out.println("Read: " + filename + " -> " + alive);
                    }

                    case "HEARTBEAT" -> {
                        String ip = socket.getInetAddress().getHostAddress();
                        heartbeatMap.put(ip, System.currentTimeMillis());

                        if (!dataNodes.contains(ip)) {
                            dataNodes.add(ip);
                            System.out.println("New DataNode registered: " + ip);
                        }
                    }
                }

                socket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
