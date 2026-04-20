import java.io.*;
import java.net.*;

public class DataNode {

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(9001);
        System.out.println("DataNode running...");

        // 💓 Heartbeat Thread
        new Thread(() -> {
            try {
                while (true) {
                    Socket hb = new Socket("10.19.225.4", 9000);

                    DataOutputStream out =
                            new DataOutputStream(hb.getOutputStream());

                    out.writeUTF("HEARTBEAT");
                    hb.close();

                    Thread.sleep(5000);
                }
            } catch (Exception e) {
                System.out.println("Heartbeat error");
            }
        }).start();

        while (true) {

            Socket socket = server.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());

            String cmd = in.readUTF();

            if (cmd.equals("UPLOAD")) {

                String filename = in.readUTF();
                long size = in.readLong();

                int replicaCount = in.readInt();

                String[] ips = new String[replicaCount];
                int[] ports = new int[replicaCount];

                for (int i = 0; i < replicaCount; i++) {
                    ips[i] = in.readUTF();
                    ports[i] = in.readInt();
                }

                FileOutputStream fos =
                        new FileOutputStream("storage/" + filename);

                ByteArrayOutputStream bufferStore = new ByteArrayOutputStream();

                byte[] buffer = new byte[4096];
                int bytes;

                while (size > 0 &&
                        (bytes = in.read(buffer, 0,
                                (int)Math.min(buffer.length, size))) > 0) {

                    fos.write(buffer, 0, bytes);
                    bufferStore.write(buffer, 0, bytes);
                    size -= bytes;
                }

                fos.close();
                System.out.println("Stored: " + filename);

                byte[] fileData = bufferStore.toByteArray();

                // 🔁 Replication
                for (int i = 0; i < replicaCount; i++) {

                    String ip = ips[i];
                    int port = ports[i];

                    new Thread(() -> {
                        try {
                            Socket s = new Socket(ip, port);

                            DataOutputStream out =
                                    new DataOutputStream(s.getOutputStream());

                            out.writeUTF("UPLOAD");
                            out.writeUTF(filename);
                            out.writeLong(fileData.length);

                            out.writeInt(0); // no further replication
                            out.write(fileData);

                            s.close();

                            System.out.println("Replicated to: " + ip);

                        } catch (Exception e) {
                            System.out.println("Replication failed: " + ip);
                        }
                    }).start();
                }
            }

            else if (cmd.equals("REPLICATE")) {

                String filename = in.readUTF();
                String targetIP = in.readUTF();
                int targetPort = in.readInt();

                File file = new File("storage/" + filename);

                if (!file.exists()) return;

                new Thread(() -> {
                    try {

                        Socket s = new Socket(targetIP, targetPort);

                        DataOutputStream out =
                                new DataOutputStream(s.getOutputStream());

                        FileInputStream fis = new FileInputStream(file);

                        out.writeUTF("UPLOAD");
                        out.writeUTF(filename);
                        out.writeLong(file.length());

                        out.writeInt(0); // no further replication

                        byte[] buffer = new byte[4096];
                        int bytes;

                        while ((bytes = fis.read(buffer)) > 0) {
                            out.write(buffer, 0, bytes);
                        }

                        fis.close();
                        s.close();

                        System.out.println("Recovered file to: " + targetIP);

                    } catch (Exception e) {
                        System.out.println("Recovery send failed");
                    }
                }).start();
            }

            else if (cmd.equals("READ")) {

                String filename = in.readUTF();

                File file = new File("storage/" + filename);

                DataOutputStream out =
                        new DataOutputStream(socket.getOutputStream());

                if (!file.exists()) {
                    out.writeLong(0);
                    socket.close();
                    continue;
                }

                FileInputStream fis = new FileInputStream(file);

                out.writeLong(file.length());

                byte[] buffer = new byte[4096];
                int bytes;

                while ((bytes = fis.read(buffer)) > 0) {
                    out.write(buffer, 0, bytes);
                }

                fis.close();
                System.out.println("Sent: " + filename);
            }

            socket.close();
        }
    }
}
