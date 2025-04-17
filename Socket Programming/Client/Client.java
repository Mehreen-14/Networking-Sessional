package Client;

import Server.Server;
import Server.Unread;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.List;

import static java.lang.Integer.parseInt;

public class Client {
    private static Socket socket;

    public static void FileSend(ObjectInputStream is, ObjectOutputStream os){
        //upload files
        try {
            FileDialog fileDialog = new FileDialog((Frame) null,"Opening file ");
            fileDialog.setMode(FileDialog.LOAD);
            fileDialog.setVisible(true);
            System.out.println(fileDialog.getFile()+" is here");
            File up = new File(fileDialog.getDirectory()+fileDialog.getFile());

            System.out.println("Filename : ");
            Scanner scanner = new Scanner(System.in);
            String filename = scanner.nextLine();
            os.writeObject(filename);

            //size
            long length = up.length();
            os.writeObject(length);

            boolean av = (boolean) is.readObject();
            if (av==false){
                System.out.println("Limit exceeded (buffer)");
                return;
            }

            int chunksize = (int) is.readObject();
            System.out.println("Chunk Size"+chunksize);

            //fileID
            int fileID = (int) is.readObject();
            InputStream inputStream = new FileInputStream(up);
            byte[] buffer = new byte[chunksize];

            int len = 0;
            int cnt=0;

            socket.setSoTimeout(30000);
            while ((len = inputStream.read(buffer))!=-1){
                if (len == chunksize){
                    os.writeObject(buffer);
                }
                else {
                    byte[] buffer2 = new byte[len];
                    System.arraycopy(buffer,0,buffer2,0,len);
                    os.writeObject(buffer2);
                }

                os.reset();
                String confirm = (String) is.readObject();
                Thread.sleep(1000);
                cnt+=1;


            }

            os.writeObject("completed");
            inputStream.close();

            String chk = (String) is.readObject();
            if (chk.equalsIgnoreCase("success")){
                System.out.println("Transfer Successful");
            }
            else if (chk.equalsIgnoreCase("failure")){
                System.out.println("Transfer failed");
            }
        } catch (SocketTimeoutException socketTimeoutException){
            try {
                os.writeObject("timeout");
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }



    public static void FileReceive(String filename,ObjectInputStream is,ObjectOutputStream os){
        try {
            //Downloads

            String filepath = "Downloads/";
            File dir = new File(filepath);

            if (!dir.exists()){
                dir.mkdir();
                System.out.println(filepath+" created");
            }

            long fileSize = (long) is.readObject();
            System.out.println("Downloading "+filename);
            File file = new File("Downloads/"+filename);

            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

            String ack = "";
            int bytes = 0;
            int total = 0;

            while (true){
                Object o = is.readObject();
                if (o.getClass().equals(ack.getClass())){
                    ack = (String) o;
                    break;
                }

                byte[] c = (byte[]) o;
                bytes = c.length;
                total+=bytes;

                bufferedOutputStream.write(c,0,bytes);
            }

            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            fileOutputStream.close();

            if (ack.equalsIgnoreCase("completed")){
                System.out.println("File downloaded");
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException,ClassNotFoundException{
        Scanner sc = new Scanner(System.in);

        socket = new Socket("localhost",8000);


        ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());

        System.out.println("Client side : connected");
        ObjectInputStream is = new ObjectInputStream(socket.getInputStream());

        String clientname;

        // System.out.println("here");
        while (true){
            System.out.println("Enter your name:");
            clientname = sc.nextLine();
            os.writeObject(clientname);

            boolean log = (boolean) is.readObject();
            if (log==true){
                System.out.println("Login "+clientname);
                break;
            }

            System.out.println("already logged in");

        }

        while (true){
            System.out.println("1. Looking up clients list  ");
            System.out.println("2. Looking up own files and Download");
            System.out.println("3. Looking up others public files and Download");
            System.out.println("4. Upload");
            System.out.println("5. File Request");
            System.out.println("6. View All Messages");
            System.out.println("7. Logout");

            String op = sc.nextLine();

            os.writeObject(op);

            if (op.equalsIgnoreCase("1")){

                System.out.println("All Users' List: ");
                ArrayList<String> onlne_users = (ArrayList<String>) is.readObject();

                String[] allusers = (String[]) is.readObject();

                for (int i=0;i< allusers.length;i++){
                    String user = allusers[i];
                    if (onlne_users.contains(user)){
                        user+="(active now)";
                    }
                    System.out.println(user);
                }
            }

            else if (op.equalsIgnoreCase("2")){
                for (int i=0;i<=1;i++){
                    //i = 0 private
                    //i = 1 public

                    ArrayList<String> publicFiles = (ArrayList<String>) is.readObject();
                    for (int j=0;j< publicFiles.size();j++){
                        System.out.println(publicFiles.get(j));
                    }
                }

                System.out.println("Download any file? Press 1 for yes and 2 for no");

                String down = sc.nextLine(); // yes or no
                os.writeObject(down);

                if (down.equalsIgnoreCase("1")){
                    System.out.println("Enter fileID:");
                    String filename = sc.nextLine();
                    try {
                        int fileID;
                        fileID = parseInt(filename);
                        os.writeObject(fileID);
                    } catch (Exception e){
                        os.writeObject("failed");
                        continue;
                    }

                    String avail = (String) is.readObject();
                    if (avail.equalsIgnoreCase("exist")){
                        filename = (String) is.readObject();
                        FileReceive(filename,is,os);
                    }
                    else {
                        System.out.println("No file exists "+filename);
                    }
                }

                else if (down.equalsIgnoreCase("2")){
                    continue;
                }
            }


            //Looking up others' files

            else if (op.equalsIgnoreCase("3")){
                System.out.println("Enter Client's name:");

                String name = sc.nextLine();
                os.writeObject(name);
                Object o = is.readObject();
                Integer chkclient = 0;
                //not existance of the client
                if (o.getClass().equals(chkclient.getClass())){
                    chkclient = (Integer) o;
                    if (chkclient == 0){
                        System.out.println("No client with this name "+name);
                        continue;
                    }
                }

                //files of mentioned client

                List<String> resultfiles = (List<String>) o;
                for (int i=0;i<resultfiles.size();i++){
                    System.out.println(resultfiles.get(i));
                }

                System.out.println("Do you want to download files? 1. yes or 2.no");
                String down = sc.nextLine();

                os.writeObject(down);
                if (down.equalsIgnoreCase("1")){

                    System.out.println("File name : ");
                    String filename = sc.nextLine();

                    os.writeObject(filename);
                    String avail = (String) is.readObject();
                    System.out.println("Client's availability "+avail);
                    if (avail.equalsIgnoreCase("exist")){
                        System.out.println(avail);
                        FileReceive(filename,is,os);
                    }
                    else {
                        System.out.println("No such file named "+filename);
                    }
                }

                else if (down.equalsIgnoreCase("2")){
                    //no
                    continue;
                }

            }

            else if (op.equalsIgnoreCase("4")){
                //upload

                System.out.println("1.Private");
                System.out.println("2.Public");
                System.out.println("3.Request");

                String privacy = sc.nextLine();

                os.writeObject(privacy);
                if (privacy.equalsIgnoreCase("3")){
                    System.out.println("Enter request id ");
                    int req_id = sc.nextInt();
                    os.writeObject( req_id );
                    boolean check = (boolean) is.readObject();
                    if( check == false ){
                        System.out.println("No such request id exists");
                        continue;
                    }
                }

                if (privacy.equalsIgnoreCase("1") | privacy.equalsIgnoreCase("2") | privacy.equalsIgnoreCase("3")){
                    FileSend(is,os);
                }
            }

            else if(op.equalsIgnoreCase("5")){
                //file request
                System.out.println("Enter Description: ");
                String desc = sc.nextLine();
                os.writeObject(desc);

            }

            else if (op.equalsIgnoreCase("6")){
                System.out.println("Messages : ");

                //own
                System.out.println("Uploads:");
                ArrayList<String> up = (ArrayList<String>) is.readObject();
                for (String s: up){
                        System.out.println("ID : " + s);
                }


                //other
                System.out.println("Requests:");
                ArrayList<String> other = (ArrayList<String>) is.readObject();

                for (String s : other) {
                        System.out.println("ID : " + s);

                }


                //unread
                System.out.println("Unread:");
                HashMap<String,String> unread = (HashMap<String, String>) is.readObject();
                for (Map.Entry<String, String> entry : unread.entrySet()){
                    System.out.println("Client :"+entry.getKey()+" Message :"+entry.getValue());
                }

//                System.out.println("Read:");
//                HashMap<String,String> read = (HashMap<String, String>) is.readObject();
//                for (Map.Entry<String,String>entry:read.entrySet()){
//                    System.out.println("Client :"+entry.getKey()+" Message :"+entry.getValue());
//                }
            }

            else if (op.equalsIgnoreCase("7")){
                System.out.println("Log out.........");
                is.close();
                os.close();
                socket.close();
                System.exit(0);
            }

        }

    }
}
