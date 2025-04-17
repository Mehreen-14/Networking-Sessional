package Server;
import java.io.*;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.DoubleToIntFunction;



public class Utils extends Thread {
    Socket socket;
    FileOutputStream fileStream = null;
    SocketAddress socketAddress;

    String clientname;
    int online = 1;
    String current = null;
    long fileSize = 0;



    public Utils(Socket socket) {
        this.socket = socket;
        socketAddress = socket.getRemoteSocketAddress();
    }


    public void FileSend(File file, ObjectInputStream objectInputStream, ObjectOutputStream objectOutputStream){
        try {
            long file_len = file.length();
            objectOutputStream.writeObject(file_len);

            int chunkSize = Server.get_max_chunk_size();
            InputStream inputStream = new FileInputStream(file);
            byte[] buffer = new byte[chunkSize];
            int len = 0;
            int cnt = 0;

            while ((len = inputStream.read(buffer))!=-1){
                if (len == chunkSize){
                    objectOutputStream.writeObject(buffer);
                }
                else {
                    byte[] buffer2 = new byte[len];
                    System.arraycopy(buffer,0,buffer2,0,len);
                    objectOutputStream.writeObject(buffer2);
                }

                objectOutputStream.reset();
                cnt += 1;

            }

            objectOutputStream.writeObject("completed");
            inputStream.close();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public String  FileReceive(ObjectInputStream objectInputStream,ObjectOutputStream objectOutputStream,String privacy) {

        try {
            String filename = (String) objectInputStream.readObject();
            fileSize = (long) objectInputStream.readObject();

            boolean available_buffer = Server.BufferCheck(fileSize);
            objectOutputStream.writeObject(available_buffer);

            if (!available_buffer)
                return null;

            int chunksize = Server.get_chunk_size();
            objectOutputStream.writeObject(chunksize);
            current = "Directory/" + clientname + "/" + privacy + "/" + filename; //finding current user's file
            int fileID = Server.add_File(current);
            objectOutputStream.writeObject(fileID);
            System.out.println("File id will be " + fileID);

            File file = new File(current);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            fileStream = fileOutputStream;

            String ack = "";
            int terminate = 0;
            int bytes = 0;
            int total = 0;

            long rem = (((fileSize % chunksize) - 1) >> 31) ^ 1;

            System.out.println("R "+rem);
            long size_f = (fileSize / chunksize) + rem;
            long count = 0;

            while (count != size_f) {
                Object o = objectInputStream.readObject();
                if (o.getClass().equals(ack.getClass())) {
                    ack = (String) o;
                    terminate = 1;
                    break;
                }

                byte[] c = (byte[]) o;
                bytes = c.length;
                total += bytes;
                bufferedOutputStream.write(c, 0, bytes);

                System.out.println(total + " bytes reached out of " + fileSize);
                objectOutputStream.writeObject(total + " bytes reached out of " + fileSize);
                count += 1;
            }

            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            fileOutputStream.close();
            fileStream = null;


            if (terminate == 0) {
                ack = (String) objectInputStream.readObject();
            }

            if (ack.equalsIgnoreCase("completed")){
                Server.Buffer_Clear(total);
                if (total == fileSize){
                    objectOutputStream.writeObject("success");
                }
                else
                {
                    objectOutputStream.writeObject("failure");
                    File file1 = new File(current); //this file will be deleted
                    boolean deleted = file1.delete();
                    if (deleted == false){
                        System.out.println("file deletion failed");
                    }
                    else{
                        System.out.println("file deletion successful");
                    }
                }
            }

            else if (ack.equalsIgnoreCase("timeout")){
                File file1 = new File(current); //this file will be  deleted because of timeout
                boolean deleted = file1.delete();
                if (deleted == false){
                    System.out.println("file deletion failed");
                }
                else{
                    System.out.println("file deletion successful (timeout)");
                }
            }

            current = null;
            return filename;


        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    public void run(){
        try {
            ObjectInputStream is = new ObjectInputStream(this.socket.getInputStream());
            ObjectOutputStream os = new ObjectOutputStream(this.socket.getOutputStream());

            while (online==1){

                clientname = (String) is.readObject();
                boolean on = Server.login(clientname,socketAddress);
                os.writeObject(on);
                if (on==false){
                    continue;
                }

                while (true){
                    String op = (String) is.readObject();


                    //options
                    if (op.equalsIgnoreCase("1")){
                        System.out.println(clientname+" wants to see clients list");

                        ArrayList<String> onlineUsers = Server.get_Online_Users(); //online users
                        os.writeObject(onlineUsers);

                        //all users available in server
                        String[] AllUsers = Server.getClients();
                        os.writeObject(AllUsers);
                    }

                    //Looking up own files
                    else if(op.equalsIgnoreCase("2")){
                        //own files
                        String[] privacy = {"private" ,"public"};
                        for (int i=0;i<privacy.length;i++){
                            List<String> files = new ArrayList<>();
                            files.add(privacy[i] + ":");
                            File[] f = new File("Directory/"+clientname+"/"+privacy[i]+"/").listFiles();


                            for (File file : f){
                                if (file.isFile()){
                                    int fileID = Server.get_file_id("Directory/"+clientname+"/"+privacy[i]+"/"+file.getName());
                                    files.add("FileID :"+fileID+" FileName :"+file.getName());

                                }
                            }
                            os.writeObject(files);
                            files.clear();
                        }

                        String dwnld = (String) is.readObject();
                        //yes
                        if (dwnld.equalsIgnoreCase("1")){

                            int fileID;
                            try {
                                fileID = (int) is.readObject();
                                System.out.println(fileID+" is downloaded by "+clientname);
                            } catch (Exception e){
                                continue;
                            }

                            String filePath = Server.get_filePath(fileID);
                            System.out.println(fileID+" "+filePath);
                            if (filePath==null){
                                System.out.println("No such directory");
                                os.writeObject("no such directory");
                                continue;
                            }
                            if(filePath.startsWith("Directory/" + clientname + "/public/") || filePath.startsWith( "Directory/" + clientname + "/private/")){
                                os.writeObject("exist");
                                File file = new File(filePath);
                                os.writeObject(file.getName());
                                FileSend(file,is,os);
                            }
                            else {
                                System.out.println("File not exists");
                                os.writeObject("no such file");
                                continue;
                            }


                        }
                        //no
                        else if (dwnld.equalsIgnoreCase("2")){
                            System.out.println("Client doesn't want to download file");
                            continue;
                        }


                    }

                    else if (op.equalsIgnoreCase("3")){
                        // other
                        String name = (String) is.readObject();
                        String[] allUsers = Server.getClients();
                        boolean exists = false; // the client is available
                        for( int i = 0 ; i < allUsers.length ; i++ ){
                            if( allUsers[i].equalsIgnoreCase(name) ){
                                exists = true;
                                break;
                            }
                        }
                        if( exists == true ){
                            List<String> resultfiles = new ArrayList<String>();
                            File[] files = new File("Directory/" + name + "/public/" ).listFiles();

                            for (File file : files) {
                                if (file.isFile()) {
                                    resultfiles.add(file.getName());
                                }
                            }
                           //send
                            os.writeObject(resultfiles);
                            String download = (String) is.readObject();
                            //yes
                            if( download.equalsIgnoreCase("1") ){

                                String filename = (String) is.readObject();
                                File file = new File("Directory/" + name + "/public/" + filename);

                                if(!file.exists()){
                                    System.out.println("No such file with ");
                                    os.writeObject("No such file");
                                    continue;
                                }

                                os.writeObject("exist"); // file exists
                                FileSend(file, is, os);
                            }
                            //no
                            else if( download.equalsIgnoreCase("2") ){
                                continue;
                            }
                        }
                        else{
                            System.out.println(name + " doesn't exist");
                            os.writeObject(0);
                        }
                    }

                    else if (op.equalsIgnoreCase("4")){
                        //uploaded files
                        String privacy = (String) is.readObject();
                        int reqid = -1;
                        if (privacy.equalsIgnoreCase("1")){
                            privacy = "private";
                        }
                        else if(privacy.equalsIgnoreCase("2")){
                            privacy="public";
                        }
                        else if (privacy.equalsIgnoreCase("3")){
                            privacy = "public";
                            reqid = (int) is.readObject();
                            boolean exist = Server.check_request_id(reqid);
                            os.writeObject(exist);
                            if( exist == false ){
                                continue;
                            }
                        }

                        String file = FileReceive(is,os,privacy);
                        Server.Upload(clientname,"Directory/"+clientname+"/public/"+file,reqid);
                    }

                    else if(op.equalsIgnoreCase("5")){
                        //file request
                        System.out.println("File request");
                        String desc = (String) is.readObject();
                        Server.AddRequest(clientname,desc);
                    }

                    else if (op.equalsIgnoreCase("6")){
                        //Message check

                        ArrayList<ClientRequest> requests = Server.allRequests();

                        //own msg
                        ArrayList<String> own = new ArrayList<>();
                        ArrayList<String> other = new ArrayList<>();

                        for (int i=0;i<requests.size();i++){ //request of client
                            if (requests.get(i).getClientname().equalsIgnoreCase(clientname)){
                                ArrayList<GrantRequest> pairArrayList = requests.get(i).getUploads();


                                if (pairArrayList.size()==0){
                                    System.out.println("nothing");
                                    continue;
                                }

                                for (int j=0;j<pairArrayList.size();j++){
                                    String msg = requests.get(i).getReq_id()+" : "+pairArrayList.get(j).getClient()+" uploaded : "+pairArrayList.get(j).getFilepath();
                                    own.add(msg);
                                    //Server.UnreadMsg.put(pairArrayList.get(j).getClient(),msg);
                                    //os.writeObject(Server.UnreadMsg);

                                   if (!Server.UnreadMsg.containsKey(pairArrayList.get(j).getClient()) && !Server.UnreadMsg.containsValue(msg)){
                                       Server.UnreadMsg.put(pairArrayList.get(j). getClient(),msg);
                                   }
                                   else{
                                       Server.UnreadMsg.remove(pairArrayList.get(j).getClient(),msg);
                                       Server.ReadMsg.put(pairArrayList.get(j). getClient(),msg);
                                   }
                                }
                                Server.removeRequest(requests.get(i).getReq_id());
                            }
                            else {
                                String msg = requests.get(i).getReq_id()+" : "+requests.get(i).getClientname()+" requested "+requests.get(i).getDescription();
                                other.add(msg);
                                if (!Server.UnreadMsg.containsKey(requests.get(i).getClientname()) && !Server.UnreadMsg.containsValue(msg)){
                                    Server.UnreadMsg.put(requests.get(i). getClientname(),msg);
                                }
                                else{
                                    Server.UnreadMsg.remove(requests.get(i).getClientname(),msg);
                                   // Server.ReadMsg.put(requests.get(i). getClientname(),msg);
                                }


                            }
                        }


                        os.writeObject(own);
                        os.flush();
                        os.writeObject(other);
                        os.flush();

                        os.writeObject(Server.UnreadMsg);
                        os.flush();
                        //os.writeObject(Server.ReadMsg);

                    }

                    else if (op.equalsIgnoreCase("7")){
                        System.out.println("Logged out : "+clientname);
                        is.close();
                        os.close();
                        socket.close();
                        Server.logout(clientname);
                        online = 0;
                        break;
                    }
                }
            }
        } catch (Exception e){

            if (current != null){
                try {
                    if (fileStream != null){
                        fileStream.close();
                    }
                } catch (IOException exc){
                    ;
                }

                File deleted = new File(current);
                Server.Buffer_Clear(fileSize);
                boolean s = deleted.delete();
                if (s==true){
                    System.out.println("deleting file "+current+" as "+clientname+" is on offline");

                }
                else
                {
                    System.out.println("failed");
                }

                current=null;
            }
            Server.logout(clientname);
            System.out.println("offline....logged out");

        }
    }

}
