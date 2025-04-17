package Server;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.util.*;

public class Server {
    public static HashMap<String, SocketAddress> Clients;
    private static HashMap<Integer,String> Files;
    private static HashMap<Integer,Boolean> Unreadmsg;

    private static int fileid = 0;
    private static ArrayList<ClientRequest> requests = new ArrayList<ClientRequest>();

    private static int req_id = 0;
    private static long MAX_BUFFER_SIZE = 100*1024;
    private static int MIN_CHUNK_SIZE = 1024;
    private static int MAX_CHUNK_SIZE = 10*1024;

    private static long buffer = 0;

    public static boolean BufferCheck(long file_size){
        if (buffer + file_size > MAX_BUFFER_SIZE){
            return false;
        }
        buffer = buffer + file_size;
        return true;

    }

    public static void Buffer_Clear(long file_size){
        buffer -= file_size;
    }

    public static int get_chunk_size(){
        return (int)(Math.random()*(MAX_CHUNK_SIZE - MIN_CHUNK_SIZE + 1) + MIN_CHUNK_SIZE);

    }

    public static int get_max_chunk_size(){
        return MAX_CHUNK_SIZE;
    }



    //login
    public static boolean login(String clientname,SocketAddress socketAddress){
        if (Clients.containsKey(clientname) == true){
            System.out.println("Client exists");
            return false;
        }
        Clients.put(clientname,socketAddress);
        String dir = "Directory/"+clientname+"/";
        File directory = new File(dir);

        if ((!directory.exists())){
            directory.mkdir();
            File pu = new File(dir+"public/");
            pu.mkdir();
            File pr = new File(dir+"private/");
            pr.mkdir();
        }
        System.out.println("Logged in "+clientname);
        return true;

    }

    //logout
    public static void logout(String client){
        Clients.remove(client);
    }


    //All Users
    public static ArrayList<String> get_Online_Users(){
        Set<String> keys = Clients.keySet();
        ArrayList<String> o_users = new ArrayList<String>(keys);
        return o_users;
    }

    public static String[] getClients(){
        File paths = new File("Directory");
        return paths.list();
    }
    //


    //upload files

    public static void AddRequest(String clientname, String des){
        requests.add(new ClientRequest(req_id,clientname,des));
        req_id++;
        System.out.println(req_id);
        System.out.println("Request no: "+req_id);
    }



    public static int add_File(String filepath){
        fileid++;
        System.out.println("File ID "+fileid);
        Files.put(fileid, filepath);
        return fileid;
    }

    public static String get_filePath(int file_id){
        return Files.get(file_id);
    }

    public static Integer get_file_id(String filepath){
        for (Map.Entry<Integer, String> entry : Files.entrySet()) {
            if (entry.getValue().equals(filepath)) {
                System.out.println("entry.getKey "+entry.getKey());
                return entry.getKey();
            }
        }
        return 0;
    }


    //granting request
    public static ArrayList<ClientRequest> allRequests(){
        return requests;
    }

    public static void Upload(String client,String path,int req_id){
        for (int i=0;i<requests.size();i++){
            if (requests.get(i).getReq_id() == req_id){
                requests.get(i).acceptRequest(client,path);
                break;
            }
        }
    }

    public static void removeRequest(int request_id){
        for (int i=0;i<requests.size();i++){
            if (requests.get(i).getReq_id() == req_id){
                requests.remove(i);
                break;
            }
        }
    }

    public static boolean check_request_id(int request_id){
        for( int i = 0 ; i < requests.size() ; i++ ){
            if(requests.get(i).getReq_id() == request_id){
                return true;
            }
        }
        return false;
    }


    //read message list //unread message list

    public static HashMap<String,String> ReadMsg = new HashMap<>();
    public static HashMap<String,String> UnreadMsg = new HashMap<>();



    public static void main(String[] args) throws Exception {
        String path = "Directory/";
        File dir = new File(path);
        if (!dir.exists()){
            dir.mkdir();
        }
        Clients = new HashMap<>();
        Files = new HashMap<>();
        ServerSocket serverSocket = new ServerSocket(8000);
       try {
           while (true){
               Socket socket = serverSocket.accept();
               System.out.println("Established");
               Thread thread = new Utils(socket);
               thread.start();
           }
       } catch (Exception e){
           e.printStackTrace();
       }


    }

}
