import bean.Message;
import jdk.nashorn.internal.runtime.regexp.joni.ast.StringNode;
import sun.util.resources.cldr.agq.CurrencyNames_agq;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


public class Peer extends JFrame implements ActionListener {

    //DHRT (resources)
    private Map<String, List<String>> dhrt;

    //the port of this peer
    private int myPort;

    //the GUID of wanted resource
    private String wantedRGUID;

    //the directory for receiving the downloaded resources
    private String receiveDir;

    /*
        This is a sharing directory of the sharing peer, which should be advertised to the sharing peer every time when 2 Peers are get connected.
        We need to keep this here, because the sharing directory are assumed before running the program

        At the beginning, every peer have to advertise there sharing directory to the main server as soon as connect to it.
        The main serer will store this dir in the UHPT
        If a peer is requesting a resource, and a sharing peer is selected by main server,
        the main server will also pass the shareDir of the sharing peer to the requesting peer.

        And the sharing directory can be advertised automatically to the sharing peer as soon as the P2P connection is established.
        (We should do this because the dirs are assumed, so that each peer is unable to store their sharing dir locally, rather store these in the UHPT in main server)
    */
    private String shareDir;

    //GUI
    private JTextArea mJtaMessageWindow;
    private JScrollPane mJspMessageWindowContainer;
    private JPanel mJpSendPanel;
    private JPanel mJpGetResPanel;
    private JTextField mJtfSendField;
    private JTextField mJtfGetResField;
    private JTextField mJtfDownloadDirField;
    private JButton mJbSendBtn;     //the button for sending test text
    private JButton mJbGetRes;     //the button for resource requesting to main server
    private JPanel mJpInputPanel;
    private JPanel mJpShowingPanel;
    private JTextArea mJtaDHRT;
    private JScrollPane mJspDHRTContainer;

    //IO stream of this peer --> mainserver
    private ObjectOutputStream oos;
    private ObjectInputStream ois;


    public static void main(String[] args) {
        new Peer();
    }

    public Peer(){
        //initialize the GUI
        initGUI();

        //initialize the server socket
        initClientSocket();
    }

    private void initGUI(){
        /*
            init GUI
         */
        //Panel of DHRT
        mJtaDHRT = new JTextArea();
        mJtaDHRT.setEditable(false);
        mJspDHRTContainer = new JScrollPane(mJtaDHRT);

        //Panel of information
        mJtaMessageWindow = new JTextArea();
        mJtaMessageWindow.setEditable(false);
        mJspMessageWindowContainer = new JScrollPane(mJtaMessageWindow);

        //Showing Panel, which contains above all
        mJpShowingPanel = new JPanel();
        mJpShowingPanel.setLayout(new GridLayout(0, 1, 0, 5));
        mJpShowingPanel.add(mJspDHRTContainer);
        mJpShowingPanel.add(mJspMessageWindowContainer);

        //Panel of "send" module
        mJpSendPanel = new JPanel();
        mJtfSendField = new JTextField(10);
        mJbSendBtn =  new JButton("Send");
        mJpSendPanel.add(mJtfSendField);
        mJpSendPanel.add(mJbSendBtn);

        //Panel of "getRes" module
        mJpGetResPanel = new JPanel();
        mJtfGetResField = new JTextField(10);
        mJtfDownloadDirField = new JTextField(10);
        mJbGetRes = new JButton("Get Resource");
        mJpGetResPanel.add(mJtfGetResField);
        mJpGetResPanel.add(mJtfDownloadDirField);
        mJpGetResPanel.add(mJbGetRes);

        //a input panel contains all the input modules
        mJpInputPanel = new JPanel();
        mJpInputPanel.setLayout(new GridLayout(0, 1));
        mJpInputPanel.add(mJpSendPanel);
        mJpInputPanel.add(mJpGetResPanel);

        //add components into the window frame
        this.add(mJpShowingPanel, BorderLayout.CENTER);
        this.add(mJpInputPanel, BorderLayout.SOUTH);

        //setting the window frame
        this.setTitle("Peer");
        this.setSize(500, 500);
        this.setLocation(800, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);

        //setting the action listener of buttons
        mJbSendBtn.addActionListener(this);
        mJbGetRes.addActionListener(this);

    }

    private void initWindowListener(){
        //set the window listener
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowClosing(WindowEvent e) {
                //when close the window, the connection of the server stopped,
                //this peer should advertise the leaving to the main server
                sendACKPeerLeave();
            }

            @Override
            public void windowOpened(WindowEvent e) {
            }
            @Override
            public void windowClosed(WindowEvent e) {
            }
            @Override
            public void windowIconified(WindowEvent e) {

            }
            @Override
            public void windowDeiconified(WindowEvent e) {

            }
            @Override
            public void windowActivated(WindowEvent e) {

            }
            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });
    }

    private void initClientSocket(){

        try {
            //create a server socket
            Socket socket = new Socket(MainServer.MAIN_SERVER_IP, MainServer.MAIN_SERVER_PORT);
            System.out.println("--- connected to main server ---");
            showOnScreen("--- connected to main server ---");

            ois = new ObjectInputStream(socket.getInputStream());
            oos = new ObjectOutputStream(socket.getOutputStream());

            //init the DHRT
            dhrt = new HashMap<>();

            //after connecting, the peer should advertise the resources
            advertiseRes();

            //initialize the window listener
            initWindowListener();

            //update showing DHRT
            updateShowingDHRT();

            //read input messages
            while (true){
                readInMessage();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * show a specific text on the  JTextArea
     * @param text
     */
    private void showOnScreen(String text){
        mJtaMessageWindow.append(text + "\n");
    }

    /**
     * Tell the main server, this peer is leaving
     */
    private void sendACKPeerLeave(){
        Message msg = new Message(Message.ACK_PEER_LEAVE, "leave");
        try {
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * read in the messages from client,
     * and deal with different type message with different way
     */
    private void readInMessage(){
        try {
            Message msg = (Message) ois.readObject();
            String type = msg.getType();
            if (type.equals(Message.PORT_YOUR)){
                /*
                    if the type is "port_your", the content must be a String of port for this client to be a server
                    This message can only be gotten a single time at the beginning of the connection to server
                 */
                String port = (String) msg.getContent();
                myPort = Integer.parseInt(port);            //initialize the "myPort"
                showOnScreen(">> my port: " + port);

                //Everytime connected to the server,
                //we let the peer start another thread to be a server (sharing peer) according to the port number,
                //waiting for another peer to connect with
                new Thread(new SharingHandler()).start();


            }else if (type.equals(Message.UPDATE_DHRT)){
                /*
                    if the type is "update_DHRT", the content must be a MAP of UHRT
                 */
                Map<String, List<String>> uhrt = (Map<String, List<String>>) msg.getContent();
                //update the DHRT by the UHRT we have got
                dhrt = uhrt;

                //update showing DHRT
                updateShowingDHRT();

                showOnScreen(">> NOTICE: DHRT updated");

            }else if (type.equals(Message.PORT_SHARE)){
                /*
                    if the type is "port_share", the content must be a string of the port of the sharing peer
                 */
                String sharingPortStr = (String) msg.getContent();
                int sharingPort = Integer.parseInt(sharingPortStr);

                //if the resource exists
                if (sharingPort != -1){
                    showOnScreen(">> NOTICE: Sharing Port is " + sharingPortStr);
                    //start a new thread to connect to the sharing peer
                    showOnScreen(">> NOTICE: connecting to the sharing peer...");
                    new Thread(new ConnectPeerHandler(sharingPort)).start();

                }else{
                    //if the resource does not exist
                    //show source not fund on the screen
                    showOnScreen(">> server: Resource not found!");
                }

            }else if (type.equals(Message.DIR_SHARE)){
                /*
                    if the type is "dir_share", the content must be a string of the sharing directory of the sharing peer
                    We should tell this to the sharing peer when requesting the resource
                 */
                String dir = (String) msg.getContent();
                this.shareDir = dir;


            }else if (type.equals(Message.TEST_TEXT)){
                /*
                    if the type is "test_text", the content must be a string
                 */
                String text = (String) msg.getContent();
                showOnScreen(text);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * after connecting, the peer should advertise the resources he has to the server immediately
     */
    private void advertiseRes(){
        try {
            //assume a list of resource names
            List<String> advPack = new ArrayList<>();
            /*
                Assume the initial resource of each peer

                For a single advertise package, first one is the sharing directory of this peer
                then the rest of the things in the list are resource names.
            */
            //resourcesDirs.peer1
//            advPack.add("src/resourcesDirs/peer1/");
//            advPack.add("resource1.txt");
//            advPack.add("resource2.txt");
            //resourcesDirs.peer2
//            advPack.add("src/resourcesDirs/peer2/");
//            advPack.add("resource1.txt");
//            advPack.add("resource3.txt");
            //resourcesDirs.peer3
            advPack.add("src/resourcesDirs/peer3/");
            advPack.add("resource2.txt");
            // resourcesDirs.peer4
//            advPack.add("src/resourcesDirs/peer4/");
//            advPack.add("resource4.txt");

            //send the message to the server
            Message msg = new Message(Message.CONNECT_REQUEST, advPack);
            oos.writeObject(msg);
            oos.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * send the resource request to the main server
     */
    private void getRes(String rGUID){
        try {
            Message msg = new Message(Message.RESOURCE_REQUEST, rGUID);
            oos.writeObject(msg);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * When buttons clicked
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == mJbSendBtn){
            /*
                when the button for sending test text is clicked
             */
            //get the text to be sent
            String text = mJtfSendField.getText();
            text = ">> Peer: " + text;

            //show for self
            showOnScreen(text);

            //write out
            try {
                Message msg = new Message(Message.TEST_TEXT, text);
                oos.writeObject(msg);
                oos.flush();
                mJtfSendField.setText("");

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }else if (e.getSource() == mJbGetRes){
            /*
                when the button for getting resource is clicked
             */
            String rGUID = mJtfGetResField.getText();
            this.receiveDir = mJtfDownloadDirField.getText();
            this.wantedRGUID = rGUID;   //update the wanted GUID in this class
            showOnScreen(">> --- requesting the resource(" + rGUID + ")... ---");
            getRes(rGUID);
            mJtfGetResField.setText("");
            mJtfDownloadDirField.setText("");

        }
    }

    private void updateShowingDHRT(){
        //clean all first
        this.mJtaDHRT.setText("");
        //title
        mJtaDHRT.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< DHRT >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> " + "\n");
        //table body
        for (String key : dhrt.keySet()){
            String filename = dhrt.get(key).get(0);
            List<String> holders = dhrt.get(key).subList(1, dhrt.get(key).size());
            mJtaDHRT.append("GUID: " + key + "          ");
            mJtaDHRT.append("File Name: " + filename + "          ");
            mJtaDHRT.append("Holders' GUID: ");
            for (String pGUID : holders){
                mJtaDHRT.append(pGUID + ",         ");
            }

            mJtaDHRT.append("\n");
        }
    }

    /**
     * An inner class for becoming a client to request resources from the sharing peer
     * (a thread for connecting to the sharing peer, acting as a client)
     */
    class ConnectPeerHandler implements Runnable{

        //the socket of the request peer
        private Socket requestSocket;

        //the port number of sharing peer
        private int sharePort;

        //filename of the wanted resource
        private String wantedFileName;

        //IO streams with this peer
        private ObjectInputStream ois;
        private ObjectOutputStream oos;

        //this is used to control the endless loop. if the sharing done, this should be false
        private boolean isRunning = true;

        //calculate the time cost
        private long start;
        private long end;

        /**
         * as soon as the handler created we will do the following things
         */
        public ConnectPeerHandler(int sharePort) {
            //init the port number
            this.sharePort = sharePort;

            try {
                //init the socket connection
                this.requestSocket = new Socket(MainServer.MAIN_SERVER_IP, this.sharePort);

                //init the IO streams according to the socket
                ois = new ObjectInputStream(requestSocket.getInputStream());
                oos = new ObjectOutputStream(requestSocket.getOutputStream());

                //screen notice
                showOnScreen(">> NOTICE: connected to the sharing peer successfully!");

            } catch (IOException e) {
                e.printStackTrace();
            }

            //send the guid of wanted resource to the sharing peer
            System.out.println("--- sending request to the sharing peer ---");
            showOnScreen(">> --- sending request to the sharing peer...");
            sendResRequest();
        }

        /**
         * read in the messages from peer,
         * and deal with different type message with different way
         */
        private void readInMessage(){
            try {
                //get the message and type
                Message msg = (Message) this.ois.readObject();
                String type = msg.getType();

                if (type.equals(Message.TEST_TEXT)){
                    /*
                        if the type is "test_text", the content must be a string
                    */
                    String text = (String) msg.getContent();
                    showOnScreen(text);

                }else if (type.equals(Message.FILE_TRANSFER_STAR)){
                    /*
                        if the type is "file_transfer_start", the content must be a string of file name
                    */
                    //get the filename
                    String filename = (String) msg.getContent();
                    this.wantedFileName = filename;

                    showOnScreen(">> NOTICE: ---------> start to download resource...");

                    //calculate the time cost (begin time)
                    this.start = System.currentTimeMillis();

                }else if (type.equals(Message.FILE_FRAME)){
                    /*
                        if the type is "file_frame", the content must be a string of a line in the file
                    */
                    //get the frame
                    String frame = (String) msg.getContent();
                    downloadFileAsFrames(receiveDir + "/" + this.wantedFileName, frame);

                }else if (type.equals(Message.ACK_TRANSFER_DONE)){
                    /*
                        if the type is "ack", the content must be a string say the file sent successfully
                    */
                    showOnScreen(">> NOTICE: ---------> File is downloaded successfully!");

                    //calculate the time cost (end time)
                    this.end = System.currentTimeMillis();

                    //calculate the time cost (end-start)
                    long timeCost = end - start;
                    showOnScreen(">> TIME COST: " + timeCost/1000.00f + " s");

                    //tell the main server, I have downloaded the file successfully
                    sendACKToServer();

                    //sharing done, stop listening to the sharing peer
                    isRunning = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Tell the main server, I have downloaded the file successfully.
         * This should send the GUID of the resource just have gotten to the main server as an acknowledgment of the getting of the resource.
         */
        private void sendACKToServer(){
            //send back the GUID
            Message msg = new Message(Message.ACK_RESOURCE_GET, wantedRGUID);
            try {
                //Attention, we should use the stream to server, not the stream to sharing peer
                Peer.this.oos.writeObject(msg);
                Peer.this.oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * send the receiving directory and GUID of wanted resource to the sharing peer
         * requestPack(0): the receiver directory
         * requestPack(1): GUID of requested resource
         * requestPack(2): advertise the sharing directory to the sharing peer (We need to do this because, those directories are assumed when running the program)
         */
        private void sendResRequest(){
            //pack up the self directory and the wanted resource GUID into an arraylist
            List<String> requestPack = new ArrayList<>();
            requestPack.add(receiveDir);
            requestPack.add(wantedRGUID);
            requestPack.add(shareDir);
            //send the package to the requesting peer
            Message msg = new Message(Message.RESOURCE_REQUEST_AT_PEER, requestPack);
            try {
                oos.writeObject(msg);
                oos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Receive the file from sharing peer and write into the local copy frame by frame.
         */
        private void downloadFileAsFrames(String fileDir, String frame){
            try(BufferedWriter bw = new BufferedWriter(new FileWriter(fileDir, true))) {
                //write the frame (line) into the file
                bw.write(frame + "\n");

//                System.out.println("----------frame download");
//                showOnScreen(">> DOWNLOAD PROGRESS: resource frame get!");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * as soon as the thread created we will do the following things
         */
        @Override
        public void run() {
            //keep reading the input until the sharing is done
            while(isRunning){
                readInMessage();
            }
        }
    }

    /**
     * An inner class for becoming a server to share resources with other peers
     * (a thread for waiting connections, acting as a server)
     */
    class SharingHandler implements Runnable{

        private ObjectOutputStream oosServerPeer;

        @Override
        public void run() {
            try {
                ServerSocket serverSocket = new ServerSocket(myPort);

                //keep receiving other peers, for each one, we will start a thread to handle with it
                while(true){
                    //receiving a requesting peer
                    Socket socket = serverSocket.accept();

                    //init the output stream
                    /* Attention!! We must let the oos be initialized here and let ois be initialized inside the new thread, otherwise, the statement of getting the streams will be blocked */
                    oosServerPeer = new ObjectOutputStream(socket.getOutputStream());

                    //show notice on screen
                    showOnScreen(">> NOTICE: a resource-requesting peer connected");

                    //start a new thread to deal with this peer
                    new Thread(new RequestHandler(socket, oosServerPeer)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * An inner class for dealing with the resource request
         * Define how to deal with each single requesting peer
         * (a thread for dealing with a connection)
         */
        class RequestHandler implements Runnable{

            //a socket of requesting peer
            private Socket requestPeerSocket;

            //IO streams with this peer
            private ObjectInputStream ois;
            private ObjectOutputStream oos;

            //this is used to control the endless loop. if the sharing done, this should be false
            private boolean isRunning = true;

            public RequestHandler(Socket requestPeerSocket, ObjectOutputStream oos) {
                //init the socket
                this.requestPeerSocket = requestPeerSocket;
                //init the output stream from parameter
                this.oos = oos;
                //init the input stream
                try {
                    this.ois = new ObjectInputStream(this.requestPeerSocket.getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * read in the messages from peer,
             * and deal with different type message with different way
             */
            private void readInMessage(){
                try {
                    //get the message and type
                    Message msg = (Message) this.ois.readObject();
                    String type = msg.getType();

                    if (type.equals(Message.TEST_TEXT)){
                        /*
                            if the type is "test_text", the content must be a string
                        */
                        String text = (String) msg.getContent();
                        showOnScreen(text);

                    }else if (type.equals(Message.RESOURCE_REQUEST_AT_PEER)){
                        /*
                            if the type is "resource_request", the content must be a List, which contains the receiving directory and the requested GUID
                        */
                        //get the package of requesting
                        List<String> pack = (List<String>) msg.getContent();

                        //unpack the information
                        String toDir = pack.get(0);
                        String rGUID = pack.get(1);
                        String shareDir = pack.get(2);

                        //get the file name
                        String rName = getFileNameById(rGUID);

                        //show it on the screen
                        showOnScreen(">> NOTICE: a peer is requesting resource " + rName + ", with GUID: " + rGUID);

                        /*
                            send the resource to the requesting peer
                         */
                        showOnScreen(">> NOTICE: ---------> start to send the file...");

                        //send the notification of the starting of the transfer, also send the file name here
                        sendTransferStartNotification(rName);

                        //send file
                        sendFileAsFrames(shareDir + rName);

                        //show indication
                        showOnScreen(">> NOTICE: ---------> file has been sent successfully!");

                        //Tell the requesting peer, the file has been sent successfully
                        sendACKFileSent();

                        //stop listening from the requesting peer
                        isRunning = false;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * send the notification of the starting of the transfer to the requiring peer, also send the file name here
             * @param fileName the file name
             */
            private void sendTransferStartNotification(String fileName){
                //pack up the filename and send
                Message msg = new Message(Message.FILE_TRANSFER_STAR, fileName);
                try {
                    oos.writeObject(msg);
                    oos.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * Read then send the file frame by frame
             */
            private void sendFileAsFrames(String fileDir){
                try(BufferedReader br = new BufferedReader(new FileReader(fileDir))) {
                    //each time read a line from the file
                    String line = br.readLine();
                    while(line != null){
                        //pack up the line then send to the requesting peer
                        Message msg = new Message(Message.FILE_FRAME, line);
                        oos.writeObject(msg);
                        oos.flush();

//                        Thread.sleep(1000);
//                        System.out.println("----------frame sent");
//                        showOnScreen(">> SENDING PROGRESS: resource frame send!");

                        //read next line
                        line = br.readLine();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * Tell the requesting peer, the file has been sent successfully
             */
            private void sendACKFileSent(){
                Message msg = new Message(Message.ACK_TRANSFER_DONE, "file has been sent successfully");
                try {
                    oos.writeObject(msg);
                    oos.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /**
             * For a given GUID of resource, return the name of the resource
             * @param rGUID the GUID of a resource
             * @return  name of the resource
             */
            private String getFileNameById(String rGUID){
                return dhrt.get(rGUID).get(0);
            }

            @Override
            public void run() {
                //keep listening the messages from requesting peer, until finish the resource sending
                while(isRunning){
                    readInMessage();
                }
            }

        }
    }
}
