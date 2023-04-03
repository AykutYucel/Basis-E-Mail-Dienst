package dslab.mailbox.tcp;

import dslab.entity.Message;
import dslab.util.Config;
import dslab.util.DB;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class MailboxServerDMAP implements Runnable {

    private Socket socket;
    private boolean loggedIn;
    private Config config;
    private Config users;
    private DB database;
    private String currentUser;

    private Log logger = LogFactory.getLog(MailboxServerDMAP.class);

    public MailboxServerDMAP(Socket socket, Config config, DB database) {
        this.socket = socket;
        this.config = config;
        this.database = database;
        currentUser = null;
    }

    @Override
    public void run() {
        logger.info("Mailbox server dmap is running");
        this.users = new Config(config.getString("users.config"));

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            writer.println("ok DMAP");
            writer.flush();

            String request;
            String response;
            while((request = reader.readLine()) != null) {
                String[] parts = request.split("\\s");
                response = "Invalid command!";
                if (protocolError(parts)) {
                    response = "error protocol error";
                    writer.println(response);
                    writer.flush();
                    socket.close();
                    break;
                }else if(!loggedIn && !parts[0].equals("login") && !parts[0].equals("quit")){
                    response = "error not logged in";
                }else if(commandValidator(parts)){
                    switch (parts[0]) {
                        case "login":
                            if(passwordCheck(parts)){
                                currentUser = parts[1];
                                response = "ok";
                                break;
                            }else {
                                if (users.containsKey(parts[1])) {
                                    response = "error wrong password";
                                }
                                else {
                                    response = "error unknown user";
                                }
                                break;
                            }
                        case "list":
                            response = "";
                            listMessages(writer);
                            break;
                        case "show":
                            response = "";
                            showMessage(writer, Integer.parseInt(parts[1]));
                            break;
                        case "delete":
                            if(Integer.parseInt(parts[1]) > database.getAllMessagesForUser(currentUser).size() || Integer.parseInt(parts[1]) < 1){
                                response = "error unknown message id!";
                            }else {
                                Message message = database.getAllMessagesForUser(currentUser).get(Integer.parseInt(parts[1])-1);
                                database.deleteMessage(message.getId());
                                response = "ok";
                            }
                            break;
                        case "logout":
                            response = "ok";
                            break;
                        case "quit":
                            response = "ok bye";
                            writer.println(response);
                            writer.flush();
                            reader.close();
                            writer.close();
                            socket.close();
                            break;
                    }
                }
                if (response != ""){
                    writer.println(response);
                    writer.flush();
                }
            }
        } catch (SocketException e) {
            logger.error("Error while closing socket " + e);
        } catch (IOException e){
            logger.error("Error while interacting with socket " + e);
        }
    }

    private boolean commandValidator(String[] parts) {
        switch (parts[0]) {
            case "login":
                if (parts.length == 3) {
                    return true;
                }else {
                    return false;
                }
            case "quit":
            case "list":
                if (parts.length == 1) {
                    return true;
                }
                break;
            case "show":
            case "delete":
                if (parts.length == 2) {
                    return true;
                }
                break;
            case "logout":
                loggedIn = false;
                currentUser = null;
                return true;
        }
        return false;
    }

    private boolean passwordCheck(String[] parts){
        if(users.containsKey(parts[1])){
            if(users.getString(parts[1]).equals(parts[2])){
                loggedIn = true;
                return true;
            }
        }
        return false;
    }

    private void listMessages(PrintWriter writer){
        database.getAllMessagesForUser(currentUser);
        int i = 1;
        for (Message message : database.getAllMessagesForUser(currentUser)){
            writer.println(i + " " + message.getSender() + " " + message.getSubject());
            writer.flush();
            i++;
        }
    }

    private void showMessage(PrintWriter writer, int id){
        if(id > database.getAllMessagesForUser(currentUser).size() || id < 1){
            writer.println("error unknown message id!");
        }else{
            Message message = database.getAllMessagesForUser(currentUser).get(id-1);
            writer.println("from " + message.getSender());
            writer.flush();
            writer.println("to " + String.join(",", message.getRecipient()));
            writer.flush();
            writer.println("subject " + message.getSubject());
            writer.flush();
            writer.println("data " + message.getData());
        }
        writer.flush();
    }

    private boolean protocolError(String[] parts){
        if (parts[0].equals("login") || parts[0].equals("list") || parts[0].equals("show") || parts[0].equals("delete") || parts[0].equals("logout") || parts[0].equals("quit")) {
            return false;
        }
        return true;
    }

}
