import java.io.*;
import java.net.*;

public class DataNode {

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(9001);

        System.out.println("DataNode running...");

        while (true) {

            Socket socket = server.accept();

            DataInputStream in = new DataInputStream(socket.getInputStream());

            String command = in.readUTF();

            if (command.equals("UPLOAD")) {

                String filename = in.readUTF();
                long size = in.readLong();

                FileOutputStream fos = new FileOutputStream("dfs/storage/" + filename);

                byte[] buffer = new byte[4096];
                int bytes;

                while (size > 0 && (bytes = in.read(buffer, 0, (int) Math.min(buffer.length, size))) > 0) {
                    fos.write(buffer, 0, bytes);
                    size -= bytes;
                }

                fos.close();
                System.out.println("File stored: " + filename);
            }

            socket.close();
        }
    }
}