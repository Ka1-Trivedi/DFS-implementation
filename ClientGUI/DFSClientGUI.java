import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class DFSClientGUI extends JFrame {

    JTextField namenodeIP = new JTextField("10.19.225.4");
    JTextField namenodePort = new JTextField("9000");
    JTextField filePath = new JTextField();

    JTextArea logArea = new JTextArea(12,40);

    String datanodeIP;
    int datanodePort;

    public DFSClientGUI() {

        setTitle("Mini DFS Client");
        setSize(500,400);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel top = new JPanel(new GridLayout(3,2));

        top.add(new JLabel("NameNode IP"));
        top.add(namenodeIP);

        top.add(new JLabel("NameNode Port"));
        top.add(namenodePort);

        top.add(new JLabel("DFS File Path"));
        top.add(filePath);

        add(top,BorderLayout.NORTH);

        JPanel buttons = new JPanel();

        JButton connect = new JButton("Connect NameNode");
        JButton upload = new JButton("Upload File");
        JButton download = new JButton("Download File");

        buttons.add(connect);
        buttons.add(upload);
        buttons.add(download);

        add(buttons,BorderLayout.CENTER);

        logArea.setEditable(false);
        add(new JScrollPane(logArea),BorderLayout.SOUTH);

        connect.addActionListener(e -> connectNameNode());
        upload.addActionListener(e -> uploadFile());
        download.addActionListener(e -> downloadFile());
    }

    void connectNameNode(){

        try{

            Socket socket = new Socket(
                    namenodeIP.getText(),
                    Integer.parseInt(namenodePort.getText())
            );

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            DataInputStream in =
                    new DataInputStream(socket.getInputStream());

            out.writeUTF("GET_DATANODE");

            datanodeIP = in.readUTF();
            datanodePort = in.readInt();

            log("Connected to DataNode: "
                    + datanodeIP + ":" + datanodePort);

            socket.close();

        }
        catch(Exception ex){
            log("Connection error: " + ex.getMessage());
        }
    }

    void uploadFile() {
        try {

            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
                return;

            File file = chooser.getSelectedFile();

            Socket nn = new Socket(namenodeIP.getText(),
                    Integer.parseInt(namenodePort.getText()));

            DataOutputStream nnOut = new DataOutputStream(nn.getOutputStream());
            DataInputStream nnIn = new DataInputStream(nn.getInputStream());

            nnOut.writeUTF("UPLOAD_FILE");
            nnOut.writeUTF(file.getName());

            int count = nnIn.readInt();
//            System.out.println("Nodes received: " + count);

            if (count == 0) {
                log("No nodes available");
                return;
            }

            String primaryIP = nnIn.readUTF();
            int primaryPort = nnIn.readInt();

            String[] replicaIPs = new String[count - 1];
            int[] replicaPorts = new int[count - 1];

            for (int i = 1; i < count; i++) {
                replicaIPs[i - 1] = nnIn.readUTF();
                replicaPorts[i - 1] = nnIn.readInt();
            }

            nn.close();

            Socket socket = new Socket(primaryIP, primaryPort);

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            FileInputStream fis = new FileInputStream(file);

            out.writeUTF("UPLOAD");
            out.writeUTF(file.getName());
            out.writeLong(file.length());

            out.writeInt(replicaIPs.length);

            for (int i = 0; i < replicaIPs.length; i++) {
                out.writeUTF(replicaIPs[i]);
                out.writeInt(replicaPorts[i]);
            }

            byte[] buffer = new byte[4096];
            int bytes;

            while ((bytes = fis.read(buffer)) > 0) {
                out.write(buffer, 0, bytes);
            }

            fis.close();
            socket.close();

            log("Uploaded: " + file.getName());

        } catch (Exception e) {
            log("Upload error: " + e.getMessage());
        }
    }

    void downloadFile() {

        try {

            String filename = filePath.getText();

            Socket nn = new Socket(namenodeIP.getText(),
                    Integer.parseInt(namenodePort.getText()));

            DataOutputStream nnOut = new DataOutputStream(nn.getOutputStream());
            DataInputStream nnIn = new DataInputStream(nn.getInputStream());

            nnOut.writeUTF("READ_FILE");
            nnOut.writeUTF(filename);

            int count = nnIn.readInt();
//            System.out.println("Nodes received: " + count);

            if (count == 0) {
                log("File not found");
                return;
            }

            String[] nodes = new String[count];
            int[] ports = new int[count];

            for (int i = 0; i < count; i++) {
                nodes[i] = nnIn.readUTF();
                ports[i] = nnIn.readInt();
            }

            nn.close();

            int idx = new java.util.Random().nextInt(count);

            Socket socket = new Socket(nodes[idx], ports[idx]);

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            DataInputStream in =
                    new DataInputStream(socket.getInputStream());

            out.writeUTF("READ");
            out.writeUTF(filename);

            long size = in.readLong();

            FileOutputStream fos =
                    new FileOutputStream("download_" + filename);

            byte[] buffer = new byte[4096];
            int bytes;

            while (size > 0 &&
                    (bytes = in.read(buffer, 0,
                            (int)Math.min(buffer.length, size))) > 0) {

                fos.write(buffer, 0, bytes);
                size -= bytes;
            }

            fos.close();
            socket.close();

            log("Downloaded: " + filename);

        } catch (Exception e) {
            log("Download error: " + e.getMessage());
        }
    }

    void log(String msg){
        logArea.append(msg + "\n");
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() ->
                new DFSClientGUI().setVisible(true)
        );
    }
}
