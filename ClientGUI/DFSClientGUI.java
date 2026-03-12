import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

public class DFSClientGUI extends JFrame {

    JTextField namenodeIP = new JTextField("10.58.1.4");
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

    void uploadFile(){

        try{

            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);

            if(res != JFileChooser.APPROVE_OPTION)
                return;

            File file = chooser.getSelectedFile();

            Socket socket = new Socket(datanodeIP,datanodePort);

            DataOutputStream out =
                    new DataOutputStream(socket.getOutputStream());

            FileInputStream fis = new FileInputStream(file);

            out.writeUTF("UPLOAD");
            out.writeUTF(file.getName());
            out.writeLong(file.length());

            byte[] buffer = new byte[4096];
            int bytes;

            while((bytes = fis.read(buffer)) > 0){
                out.write(buffer,0,bytes);
            }

            fis.close();
            socket.close();

            log("Uploaded file: " + file.getName());
        }
        catch(Exception ex){
            log("Upload error: " + ex.getMessage());
        }
    }

    void downloadFile(){

        try{

            String filename = filePath.getText();

            Socket socket = new Socket(datanodeIP,datanodePort);

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

            while(size > 0 &&
                    (bytes = in.read(buffer,0,
                            (int)Math.min(buffer.length,size))) > 0){

                fos.write(buffer,0,bytes);
                size -= bytes;
            }

            fos.close();
            socket.close();

            log("Downloaded file: " + filename);
        }
        catch(Exception ex){
            log("Download error: " + ex.getMessage());
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