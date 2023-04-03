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
import java.util.LinkedList;
import java.util.List;

public class MailboxServerDMTP implements Runnable {

    private Socket socket;
    private boolean begin;
    private DB database;
    private Config config;
    private Config users;

    private Log logger = LogFactory.getLog(MailboxServerDMTP.class);

    public MailboxServerDMTP(Socket socket , Config config, DB database) {
        this.socket = socket;
        this.database = database;
        this.config = config;
        this.begin = false;
    }

    @Override
    public void run() {
        logger.info("Mailbox server dmtp is running");
        this.users = new Config(config.getString("users.config"));
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());

            writer.println("ok DMTP");
            writer.flush();

            String request;
            String response;
            Message message = new Message();
            while((request = reader.readLine()) != null) {
                String[] parts = request.split("\\s");
                response = "Invalid command!";
                if (protocolError(parts)) {
                    response = "error protocol error";
                    writer.println(response);
                    writer.flush();
                    socket.close();
                    break;
                }else if(!begin && !parts[0].equals("begin") && !parts[0].equals("quit")){
                    response = "<begin> command is not given yet!";
                } else if(commandValidator(parts)){
                    switch (parts[0]) {
                        case "to":
                            String[] recipients = parts[1].split(",");
                            List<String> checkedRecipients = new LinkedList<>();
                            for (int i = 0; i < recipients.length; i++) {
                                String[] currentRecipient = recipients[i].split("@");
                                if(domainValidation(currentRecipient[1])) {
                                    if(recipientValidation(currentRecipient[0])) {
                                        checkedRecipients.add(recipients[i]);
                                    }
                                    else {
                                        response = "error unknown recipient " + currentRecipient[0];
                                        break;
                                    }
                                }
                            }
                            if(!response.contains("error unknown recipient") || checkedRecipients.size() != 0) {
                                message.setRecipient(checkedRecipients);
                                response = "ok " + (checkedRecipients.size());
                            }
                            break;
                        case "begin":
                            response = "ok";
                            break;
                        case "from":
                            message.setSender(parts[1]);
                            response = "ok";
                            break;
                        case "subject":
                            message.setSubject(request.substring(8));
                            response = "ok";
                            break;
                        case "data":
                            message.setData(request.substring(5));
                            response = "ok";
                            break;
                        case "send":
                            if(message.getRecipient() == null || message.getRecipient().isEmpty()){
                                response = "error! No recipient is specified!";
                                break;
                            }
                            if(message.getSender() == null || message.getSender().equals("")){
                                response = "error! No sender is specified!";
                                break;
                            }
                            response = "ok";
                            database.addNewMessageToMailbox(message);
                            message = new Message();
                            begin = false;
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

                writer.println(response);
                writer.flush();
            }
        } catch (SocketException e) {
            logger.error("Error while closing socket " + e);
        } catch (IOException e) {
            logger.error("Error while interacting with socket " + e);
        }
    }

    private boolean recipientValidation(String recipient) {
        if(users.containsKey(recipient)){
            return true;
        }
        return false;
    }

    private boolean domainValidation(String domain) {
        if(config.getString("domain").equals(domain)) {
            return true;
        }
        return false;
    }

    private boolean commandValidator(String[] parts){
        switch (parts[0]) {
            case "begin":
                begin = true;
            case "send":
                if(!begin){
                    return false;
                }
            case "quit":
                if(parts.length == 1){
                    return true;
                }
                break;
            case "to":
                if(!begin){
                    return false;
                }
                if(parts.length >= 2){
                    return true;
                }
                break;
            case "from":
                if(!begin){
                    return false;
                }
                if(parts.length == 2){
                    return true;
                }
                break;
            case "data":
            case "subject":
                if(!begin){
                    return false;
                }
                return true;
        }
        return false;
    }

    private boolean protocolError(String[] parts){
        if (parts[0].equals("begin") || parts[0].equals("to") || parts[0].equals("from") || parts[0].equals("subject") || parts[0].equals("data") || parts[0].equals("send") || parts[0].equals("quit")) {
            return false;
        }
        return true;
    }
}
