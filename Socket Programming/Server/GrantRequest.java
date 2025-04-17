package Server;
public class GrantRequest {
    String client;
    String filepath;
    public GrantRequest(String client, String filepath) {
        this.client = client;
        this.filepath = filepath;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }
}
