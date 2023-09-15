import bean.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class MainServer extends JFrame implements ActionListener {

    //socket connection information
    public static final String MAIN_SERVER_IP = "127.0.0.1";
    public static final int MAIN_SERVER_PORT = 5000;

    //UHPT (peers)
    private Map<String, List<String>> uhpt;
    //UHRT (resources)
    private Map<String, List<String>> uhrt;

    //GUI
    private JTextArea mJtaMessageWindow;
    private JScrollPane mJspMessageWindowContainer;
    private JPanel mJpSendPanel;
    private  JTextField mJtfSendField;
    private JButton mJbSendBtn;
    private JPanel mJpShowingPanel;
    private JTextArea mJtaUHPT;
    private JTextArea mJtaUHRT;
    private JScrollPane mJspUHPTContainer;
    private JScrollPane mJspUHRTContainer;


    //IO
    private ObjectOutputStream oos;

    //random for generating the port numbers, put it here can make all the port number unique
    private final Random random = new Random();


    public static void main(String[] args) {
        new MainServer();
    }

    public MainServer(){
        //initialize the GUI
        initGUI();

        //initialize the server socket
        initServerSocket();

    }

    private void initServerSocket(){
        try {
            //create a server socket
            ServerSocket serverSocket = new ServerSocket(MAIN_SERVER_PORT);
            System.out.println("--- server started ---");
            showOnScreen("--- server started ---");

            //init the UHPT and UHRT
            uhpt = new HashMap<>();
            uhrt = new HashMap<>();

            //show UHPT and UHRT
            updateShowingUHPT();
            updateShowingUHRT();

            //keep receiving clients, for each one, we will start a thread to handle with it
            while (true){
                //receive a client
                Socket socket = serverSocket.accept();
                System.out.println("--- a client connected ---");
                showOnScreen("--- a client connected ---");


                //init the output stream
                oos = new ObjectOutputStream(socket.getOutputStream());

                //start a new thread for this handling this client
                new Thread(new PeerHandler(socket, oos)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initGUI(){
        /*
            init GUI
         */
        //Panel of UHPT
        mJtaUHPT = new JTextArea();
        mJtaUHPT.setEditable(false);
        mJspUHPTContainer = new JScrollPane(mJtaUHPT);
        
        //Panel of UHRT
        mJtaUHRT = new JTextArea();
        mJtaUHRT.setEditable(false);
        mJspUHRTContainer = new JScrollPane(mJtaUHRT);
        
        //Panel of information
        mJtaMessageWindow = new JTextArea();
        mJtaMessageWindow.setEditable(false);
        mJspMessageWindowContainer = new JScrollPane(mJtaMessageWindow);

        //Showing Panel, which contains above all
        mJpShowingPanel = new JPanel();
        mJpShowingPanel.setLayout(new GridLayout(0, 1, 0, 5));
        mJpShowingPanel.add(mJspUHPTContainer);
        mJpShowingPanel.add(mJspUHRTContainer);
        mJpShowingPanel.add(mJspMessageWindowContainer);

        //Panel of "send" module
        mJpSendPanel = new JPanel();
        mJtfSendField = new JTextField(10);
        mJbSendBtn =  new JButton("Send");
        mJpSendPanel.add(mJtfSendField);
        mJpSendPanel.add(mJbSendBtn);

        //add components into the window frame
        this.add(mJpShowingPanel, BorderLayout.CENTER);
        this.add(mJpSendPanel, BorderLayout.SOUTH);

        //setting the window frame
        this.setTitle("Main Server");
        this.setSize(600, 600);
        this.setLocation(800, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);


        //setting the action listener of buttons
        mJbSendBtn.addActionListener(this);
    }

    /**
     * show a specific text on the  JTextArea
     * @param text
     */
    private void showOnScreen(String text){
        mJtaMessageWindow.append(text + "\n");
    }

    /**
     * When send button clicked, write out
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        //get the text to be sent
        String text = mJtfSendField.getText();
        text = ">> server: " + text;

        //show text for self
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
    }

    private String getResNameByID(String rGUID){
        List<String> resInfo = uhrt.get(rGUID);
        if (resInfo != null){
            return resInfo.get(0);
        }else {
            return null;
        }
    }

    private String getPeerPortByID(String pGUID){
        List<String> peerInfo = uhpt.get(pGUID);
        if (peerInfo != null){
            return peerInfo.get(0);
        }else {
            return null;
        }
    }

    private void updateShowingUHPT(){
        //clean all first
        this.mJtaUHPT.setText("");
        //title
        mJtaUHPT.append("<<<<<<<<<<<<<<<<<<<<<<<<< UHPT >>>>>>>>>>>>>>>>>>>>>>>>> " + "\n");
        //table body
        for (String pGUID : uhpt.keySet()){
            String port = uhpt.get(pGUID).get(0);
            String routingMetric = uhpt.get(pGUID).get(1);
            List<String> resList = uhpt.get(pGUID).subList(3, uhpt.get(pGUID).size());
            mJtaUHPT.append("GUID: " + pGUID + "          ");
            mJtaUHPT.append("Port: " + port + "          ");
            mJtaUHPT.append("Routing Metric: " + routingMetric + "          ");
            mJtaUHPT.append("Holding Res: ");
            for (String rId : resList){
                String rName = getResNameByID(rId);
                mJtaUHPT.append(rName + " || ");
            }

            mJtaUHPT.append("\n");
        }
    }

    private void updateShowingUHRT(){
        //clean all first
        this.mJtaUHRT.setText("");
        //title
        mJtaUHRT.append("<<<<<<<<<<<<<<<<<<<<<<<<< UHRT >>>>>>>>>>>>>>>>>>>>>>>>> " + "\n");
        //table body
        for (String key : uhrt.keySet()){
            String filename = uhrt.get(key).get(0);
            List<String> holders = uhrt.get(key).subList(1, uhrt.get(key).size());
            mJtaUHRT.append("GUID: " + key + "          ");
            mJtaUHRT.append("File Name: " + filename + "          ");
            mJtaUHRT.append("Holders' ports: ");
            for (String pGUID : holders){
                String port = getPeerPortByID(pGUID);
                mJtaUHRT.append(port + ", ");
            }

            mJtaUHRT.append("\n");
        }

    }


    /**
     * (an inner class) for handling each connected server
     */
    class PeerHandler implements Runnable{

        //socket connection
        private Socket socket;
        //in and out put belong to each socket
        private ObjectInputStream ois;
        private ObjectOutputStream oos;
        //the port number for this peer
        private String port;
        //the GUID of this peer
        private String guid;
        //the routing metric of this peer
        private String routingMetric;
        //the sharing directory of this peer
        private String shareDir;
        //for every socket, when connecting, the peer should advertise the resources, this is what server gets form peer.
        private List<String> resourceNames;
        private List<String> resourceGUIDs = new ArrayList<>();
        //this is used to control the endless loop. if this peer leaves, this should be false
        private boolean isRunning = true;

        public PeerHandler(Socket socket, ObjectOutputStream oos){
            //init the socket
            this.socket = socket;
            //get the output stream obj from the main class
            this.oos = oos;
            //init the input stream according to socket
            try {
                ois = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            //generate the port number for this peer
            genPort();
            //generate the GUID for this peer
            genGUID();
            //generate the routing metric for this peer
            genRoutingMetric();
            //send back the port umber for this peer
            sendYourPort();
        }


        /**
         * for generating the GUID
         */
        private void genGUID(){
            this.guid = UUID.randomUUID().toString();
        }

        /**
         * generate the port number for this peer
         */
        private void genPort(){
            int port = random.nextInt(1000) + 2000;
            this.port = String.valueOf(port);
        }

        /**
         * According to our teacher, we should assume the routing metric for the peer.
         * Here we just generate it randomly range from 1 to 100
         */
        private void genRoutingMetric(){
            int routingMetric = random.nextInt(100) + 1;
            this.routingMetric = String.valueOf(routingMetric);
        }

        /**
         * insert a row of this peer's info into the UHPT table
         */
        private void updateUHPT(){
            //pack up the peer info into an arraylist
            List<String> peerInfos = new ArrayList<>();
            peerInfos.add(port);
            peerInfos.add(routingMetric);
            peerInfos.add(shareDir);
            peerInfos.addAll(resourceGUIDs);
            uhpt.put(guid, peerInfos);
        }

        /**
         * check to update the UHRT
         * case1: insert the user to the row of exist resource
         * case2: create a row of resource then add the user to this row
         */
        private void updateUHRT(){
            //get all the file names of the resources in the UHRT
            List<String> fileNameList = new ArrayList<>();
            for (List<String> infoList : uhrt.values()){
                if (infoList != null){
                    String filename = infoList.get(0);
                    fileNameList.add(filename);
                }
            }

            //loop over the list of resource names that the user advertised
            for (String rName : resourceNames){
                //for each resource, check whether it is already recorded
                if (fileNameList.contains(rName)){
                    /* if already recorded, we add the GUID of this peer into the value list of UHRT */

                    //get the info list and the GUID of this resource
                    List<String> infoList = null;
                    String rGUID = null;
                    for (String key : uhrt.keySet()){
                        if (key != null){
                            String filename = uhrt.get(key).get(0);
                            if (rName.equals(filename)){
                                infoList = uhrt.get(key);
                                rGUID = key;
                            }
                        }
                    }

                    //add the GUID of this peer in to UHRT map
                    infoList.add(guid);
                    uhrt.replace(rGUID, infoList);

                    //update the list, which stores the GUID of res of this peer
                    resourceGUIDs.add(rGUID);

                }else{
                    //if not recorded, we generate a GUID for this resource as the key, a list contains the GUID of this client as the value, put it into the UHRT map
                    //values(0) should be the file name, values(1~n) should be the GUID of peers
                    List<String> value = new ArrayList<>();
                    value.add(rName);
                    value.add(guid);
                    String rGUID = UUID.randomUUID().toString();
                    resourceGUIDs.add(rGUID);  //update the list, which stores the GUID of res of this peer
                    uhrt.put(rGUID, value);
                    fileNameList.add(rName);

                }

            }
        }

        /**
         * Add specific resource into the row of a specific peer in the UHPT
         * @param rGUID the new resource GUID
         * @param pGUID the peer GUID
         */
        private void addResToUHPT(String rGUID, String pGUID){
            //get the row by GUID of the peer
            List<String> peerInfo = uhpt.get(pGUID);
            //add the GUID of the resource in to the row
            peerInfo.add(rGUID);
            //update the UHPT
            uhpt.replace(pGUID, peerInfo);
        }

        /**
         * Add specific peer into the row of a specific resource in the UHRT
         * @param rGUID the new resource GUID
         * @param pGUID the peer GUID
         */
        private void addPeerToUHRT(String rGUID, String pGUID){
            //get the row by GUID of the resource
            List<String> resInfo = uhrt.get(rGUID);
            //add the GUID of the peer in to the row
            resInfo.add(pGUID);
            //update the UHRT
            uhrt.replace(rGUID, resInfo);

        }

        /**
         * Delete all the GUIDs of a peer in the UHRT
         * @param pGUID the GUID of the peer
         */
        private void deletePeerFromUHRT(String pGUID){
            //loop over every row in the UHR
            for (String key : uhrt.keySet()){
                //get a row
                List<String> resInfo = uhrt.get(key);
                //if the GUID of the peer in this row, we just delete it
                if (resInfo.contains(pGUID)){
                    //remove the peer GUID in this list
                    resInfo.remove(pGUID);
                    //update the UHRT
                    uhrt.replace(key, resInfo);

                    //if after that, no peer holds this resource, we will delete this row in UHRT
                    if (resInfo.size() <= 1){
                        uhrt.remove(key);
                    }
                }
            }


        }

        /**
         * Delete a row of a peer in the UHPT
         * @param pGUID the GUID of the peer
         */
        private void deletePeerFromUHPT(String pGUID){
            //remove the row of peer with this GUID in the UHPT hashmap
            uhpt.remove(pGUID);
        }

        /**
         * send the generated port number back to this peer
         */
        private void sendYourPort(){
            Message msg = new Message(Message.PORT_YOUR, port);
            try {
                oos.writeObject(msg);
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * send the current UHRT to the connected server, ask it to update its DHRT
         */
        private void sendCurrentUHRT(){
            Message msg = new Message(Message.UPDATE_DHRT, uhrt);
            try {
                oos.writeObject(msg);
                oos.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * send the port number of resource sharing peer to the resource requesting peer
         * @param port port number of resource sharing peer
         * @param isFound is there a selected peer, which means does the resource exist
         */
        private void sendSharingPort(String port, Boolean isFound){
            if (isFound){
                //send the port number of selected peer to the requesting peer
                Message msg = new Message(Message.PORT_SHARE, port);
                try {
                    oos.writeObject(msg);
                    oos.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }else{
                //if the resource does not exist

                //send indication of "resource not found"
                Message msg = new Message(Message.PORT_SHARE, "-1");
                try {
                    oos.writeObject(msg);
                    oos.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }

        /**
         * Find the proper sharing peer of the requested source, then
         *
         * @param rGUID the GUID of the requested source
         * @return the GUID of the selected peer
         */
        private String selectSharingPeer(String rGUID) {
            //the GUID of the selected peer
            String selectedPGUID = null;

            //if the resource exists
            if (uhrt.containsKey(rGUID)){
                //get a list of possible sharing peers who own this resource
                List<String> sharingPeers = uhrt.get(rGUID);
                String fileName = sharingPeers.get(0);
                sharingPeers = sharingPeers.subList(1, sharingPeers.size());

                showOnScreen(">> NOTICE: a peer is requesting resource: " + fileName);

                //record the routing metric of selected peer
                int selectedRm = 99999;

                //select one of the sharing peer who has the minimum routing metric
                for (String pGUID : sharingPeers){
                    if (uhpt.containsKey(pGUID)){
                        List<String> peerInfoList = uhpt.get(pGUID);
                        //get the routing metric
                        String routingMetricStr = peerInfoList.get(1);
                        int routingMetric = Integer.parseInt(routingMetricStr);
                        if (routingMetric < selectedRm){
                            selectedRm = routingMetric;
                            selectedPGUID = pGUID;
                        }
                    }
                }

                //return the sharing directory
                return selectedPGUID;

            }else{
                //no sharing peer found
                return null;
            }
        }

        /**
         * Advertise the sharing directory of the selected peer to the requesting peer
         * @param shareDir the sharing directory of the selected peer
         */
        private void sendSharingDir(String shareDir){
            Message msg = new Message(Message.DIR_SHARE, shareDir);
            try {
                oos.writeObject(msg);
                oos.flush();
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
                //read in the message
                Message msg = (Message) ois.readObject();
                String type = msg.getType();

                //check the type of message
                if (type.equals(Message.CONNECT_REQUEST)){
                    /*
                        if the type is "connecting to the server", the content must be arraylist of sharing directory and resources GUIDs
                        list(0)=shareDir, list(1, n)=rGUIDs
                     */

                    List<String> pack = (List<String>) msg.getContent();

                    //get the list of resources that the peer advertised
                    this.resourceNames = pack.subList(1, pack.size());

                    //get the sharing directory of this peer
                    this.shareDir = pack.get(0);

                    //update the UHRT according to the resources the peer advertised
                    updateUHRT();
                    //add the new peer info into the UHPT (we must do this here)
                    updateUHPT();

                    //after that we should tell the client to update their DHRT
                    sendCurrentUHRT();

                    //show message on screen
                    String show = ">> NOTICE: " + " this peer has resources: ";
                    for (String name : resourceNames){
                        show = show + ", " + name;
                    }
                    showOnScreen(show);

                    //update showing UHPT and UHRT
                    updateShowingUHPT();
                    updateShowingUHRT();

                    //for test--------------------------------------------------------------------------
                    for (String k : uhrt.keySet()){
                        String fileName = uhrt.get(k).get(0);
                        System.out.println("GUID: " + k + "  filename: " + fileName);
                    }
                    //--------------------------------------------------------------------------------


                }else if (type.equals(Message.RESOURCE_REQUEST)){
                    /*
                        if the type is "resource_request", the content must be a string of resource GUID
                     */

                    //get the requested resource GUID
                    String rGUID = (String) msg.getContent();

                    //select a sharing peer by the GUID of requested resource
                    String pGUID = selectSharingPeer(rGUID);
                    if (pGUID != null){
                        List<String> sharingPeerInfo = uhpt.get(pGUID);
                        String sharePort = sharingPeerInfo.get(0);
                        this.shareDir = sharingPeerInfo.get(2);

                        //advertise the sharing directory to the peer, (NOTICE!! this must be done before sending the port)
                        sendSharingDir(this.shareDir);

                        //send back the port number of sharing peer
                        sendSharingPort(sharePort, true);

                    }else{
                        //advertise the sharing directory to the peer, (NOTICE!! this must be done before sending the port)
                        sendSharingDir(shareDir);

                        //send back the port number of sharing peer
                        sendSharingPort("-1", false);
                    }

                    //send the current UHRT to the requesting peer for it to update its DHRT
                    sendCurrentUHRT();

                }else if (type.equals(Message.ACK_RESOURCE_GET)){
                    /*
                        if the type is "ack_resource_get", this means the requesting peer have gotten the resource successfully,
                        the content must be a String, which is the GUID of the gotten resource
                     */

                    String rGUID = (String) msg.getContent();

                    //here we should add this peer to the UHRT, in the row of this resource
                    addPeerToUHRT(rGUID, guid);
                    //also we should add this resource to the UHPT in the row of this peer
                    addResToUHPT(rGUID, guid);

                    //show on screen
                    showOnScreen(">> NOTICE: requesting peer get the resource, UHRT updated");

                    //update showing UHPT and UHRT
                    updateShowingUHPT();
                    updateShowingUHRT();

                }else if (type.equals(Message.ACK_PEER_LEAVE)){
                    /*
                        if the type is "ack_peer_leave", this means this peer is leaving, we should update our UHPT and UHRT
                     */
                    //delete the peer from the UHPT
                    deletePeerFromUHPT(guid);
                    //delete the peer from the UHRT
                    deletePeerFromUHRT(guid);

                    showOnScreen(">> NOTICE: a peer leaves, UHPT and UHRT are updated");

                    //stop the endless loop for listening to this peer
                    this.isRunning = false;

                    //update showing UHPT and UHRT
                    updateShowingUHPT();
                    updateShowingUHRT();

                }
                else if (type.equals(Message.TEST_TEXT)){
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

        @Override
        public void run() {
            while(isRunning){
                //keep reading from input (listening to this peer)
                readInMessage();
            }
        }

    }

}

