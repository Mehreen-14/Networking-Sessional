package Server;
import java.util.ArrayList;


public class ClientRequest {
    private int req_id;
    private String clientname;
    private String description;
    private ArrayList<GrantRequest> uploads;

    public ClientRequest(int req_id, String clientname, String description){
        this.req_id = req_id;
        this.clientname = clientname;
        this.description = description;
        uploads = new ArrayList<GrantRequest>();
    }

    public int getReq_id() {
        return req_id;
    }

    public void setReq_id(int req_id) {
        this.req_id = req_id;
    }

    public String getClientname() {
        return clientname;
    }

    public void setClientname(String clientname) {
        this.clientname = clientname;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArrayList<GrantRequest> getUploads() {
        return uploads;
    }

    public void setUploads(ArrayList<GrantRequest> uploads) {
        this.uploads = uploads;
    }

    public void acceptRequest(String granter, String filepath){
        GrantRequest p = new GrantRequest(granter, filepath);
        uploads.add(p);
    }



}