import java.io.*;
import java.net.*;

public class NameNode {

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(9000);

        System.out.println("NameNode running...");

        while (true) {

            Socket socket = server.accept();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            String request = in.readUTF();

            if (request.equals("GET_DATANODE")) {

                // send datanode1 info
                out.writeUTF("10.58.132"); // change to your dn1 IP
                out.writeInt(9001); // port mentioned on DataNode.java file

            }

            socket.close();
        }
    }
}