package dslab.transfer.tcp;

import dslab.entity.Message;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TransferServerDMTP implements Runnable {

    private Socket socket;
    private Socket clientSocketEarth;
    private Socket clientSocketUniver;
    private Config domains;
    private boolean begin;
    private DatagramSocket datagramSocket;
    private Config config;

    private Log logger = LogFactory.getLog(TransferServerDMTP.class);

    public TransferServerDMTP(Socket socket, Config domains, DatagramSocket datagramSocket, Config config) {
        this.socket = socket;
        this.domains = domains;
        this.datagramSocket = datagramSocket;
        this.config = config;
        begin = false;
    }

    @Override
    public void run() {
        logger.info("Transfer server dmtp is running");
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
                    reader.close();
                    writer.close();
                    socket.close();
                    break;
                }else if(!begin && !parts[0].equals("begin") && !parts[0].equals("quit")){
                    response = "<begin> command is not given yet!";
                } else if(commandValidator(parts)){
                    switch (parts[0]) {
                        case "to":
                            String[] recipients = parts[1].split(",");
                            message.setRecipient(Arrays.asList(recipients));
                            response = "ok " + (recipients.length);
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
                            connectToServer(message);
                            sendStatistic(message.getSender());
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

    private String[] domainLookup(String domain) {
        return domain.split(":");
    }

    private boolean sendMessage(Message message, Socket socket, String recipient) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream());
            reader.readLine();
            writer.println("begin");
            writer.flush();
            reader.readLine();
            writer.println("from " + message.getSender());
            writer.flush();
            reader.readLine();
            writer.println("to " + recipient);
            writer.flush();
            reader.readLine();
            writer.println("data " + message.getData());
            writer.flush();
            reader.readLine();
            writer.println("subject " + message.getSubject());
            writer.flush();
            reader.readLine();
            writer.println("send");
            writer.flush();
            if (reader.readLine().equals("ok")) {
                writer.close();
                reader.close();
                return true;
            }
            writer.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void connectToServer(Message message){
        String domainEarth = domains.getString("earth.planet");
        String domainUniver = domains.getString("univer.ze");
        String[] earth = domainLookup(domainEarth);
        String[] univer = domainLookup(domainUniver);
        boolean isSent;
        for (int i = 0; i < message.getRecipient().size(); i++) {
            isSent =  false;
            String[] currentRecipients = message.getRecipient().get(i).split("@");
            try {
                if (currentRecipients[1].equals("earth.planet")) {
                    clientSocketEarth = new Socket(earth[0], Integer.parseInt(earth[1]));
                    isSent = sendMessage(message, clientSocketEarth, message.getRecipient().get(i));
                    clientSocketEarth.close();
                }else if(currentRecipients[1].equals("univer.ze")) {
                    clientSocketUniver = new Socket(univer[0], Integer.parseInt(univer[1]));
                    isSent = sendMessage(message, clientSocketUniver, message.getRecipient().get(i));
                    clientSocketUniver.close();
                }
            } catch (SocketException e) {
                logger.error("Error while connecting socket " + e);
            } catch (IOException e) {
                logger.error("Error while interacting with socket " + e);
            }
            if (!isSent) {
                Message errorMessage = new Message();
                List<String> errorRecipient = new LinkedList<>();
                errorRecipient.add(message.getSender());
                errorMessage.setRecipient(errorRecipient);
                try {
                    errorMessage.setSender("mailer@[" + InetAddress.getLocalHost().getHostAddress() + "]");
                } catch (UnknownHostException e) {
                    logger.error("Error while identifying host " + e);
                }
                errorMessage.setSubject("error message delivery failed");
                errorMessage.setData("message could not be delivered to " + message.getRecipient().get(i));
                String[] errorMessageRecipient = message.getSender().split("@");
                if (errorMessageRecipient[1].equals("earth.planet")) {
                    try {
                        clientSocketEarth = new Socket(earth[0], Integer.parseInt(earth[1]));
                        sendMessage(errorMessage, clientSocketEarth, message.getSender());
                        clientSocketEarth.close();
                    } catch (SocketException e) {
                        logger.error("Error while closing socket " + e);
                    } catch (IOException e) {
                        logger.error("Error while interacting with socket " + e);
                    }
                }else if(errorMessageRecipient[1].equals("univer.ze")) {
                    try {
                        clientSocketUniver = new Socket(univer[0], Integer.parseInt(univer[1]));
                        sendMessage(errorMessage, clientSocketUniver, message.getSender());
                        clientSocketUniver.close();
                    } catch (SocketException e) {
                        logger.error("Error while closing socket " + e);
                    } catch (IOException e) {
                        logger.error("Error while interacting with socket " + e);
                    }
                }
            }
        }
    }

    public void sendStatistic(String sender) {
        byte[] buffer = new byte[0];
        try {
            buffer = (InetAddress.getLocalHost().getHostAddress() + ":" + socket.getLocalPort() + " " + sender).getBytes();
        } catch (UnknownHostException e) {
            logger.error("Error while identifying host " + e);
        }
        try {
            InetAddress address = InetAddress.getByName(config.getString("monitoring.host"));
            int port = config.getInt("monitoring.port");
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, address, port);
            datagramSocket.send(datagramPacket);
        } catch (IOException e) {
            logger.error("Error while interacting with socket " + e);
        }
    }
}
