package bean;

import java.io.Serializable;

/**
 * All the message transfer in our p2p system must be "Message" type
 */
public class Message implements Serializable {

    /*
        Possible message types
    */
    public static final String CONNECT_REQUEST = "conn_request";
    public static final String TEST_TEXT = "test_text";
    public static final String PORT_YOUR = "port_your";
    public static final String PORT_SHARE = "port_share";
    public static final String DIR_SHARE = "dir_share";
    public static final String UPDATE_DHRT = "update_DHRT";
    public static final String RESOURCE_REQUEST = "resource_request"; //the request tag to server
    public static final String RESOURCE_REQUEST_AT_PEER = "resource_request_at_peer";   //the request tag to sharing peer
    public static final String ACK_TRANSFER_DONE = "ack_transfer_done";
    public static final String ACK_RESOURCE_GET = "ack_resource_get";
    public static final String ACK_PEER_LEAVE = "ack_peer_leave";
    public static final String FILE_FRAME = "file_frame";
    public static final String FILE_TRANSFER_STAR = "file_transfer_start";

    private String type;
    private Object content;

    public Message(String type, Object content) {
        this.type = type;
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }
}
