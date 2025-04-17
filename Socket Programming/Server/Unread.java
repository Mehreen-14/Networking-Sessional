package Server;

public class Unread {
    private String  client;
    private boolean isread;

    public Unread(String client){
        this.client = client;
        this.isread = false;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public boolean isIsread() {
        return isread;
    }

    public void setIsread(boolean isread) {
        this.isread = isread;
    }
}
