package dslab.util;

import dslab.entity.Message;

import java.util.LinkedList;
import java.util.List;

public class DB {

    private List<Message> list;
    private String domain;
    private int availableId;

    public DB(String domain) {
        this.domain = domain;
        list = new LinkedList<>();
        availableId = 0;
    }

    synchronized public void addNewMessageToMailbox(Message message) {
        message.setId(availableId);
        list.add(message);
        availableId++;
    }

    synchronized public void deleteMessage(int id) {
        list.remove(showMessage(id));
    }

    public Message showMessage(int id) {
        for (Message message : list) {
            if (message.getId() == id) {
                return message;
            }
        }
        return null;
    }

    synchronized public List<Message> getAllMessagesForUser(String username) {
            List<Message> userMails = new LinkedList<>();
            for (Message message : list) {
                if (message.getRecipient().contains(username + "@" + domain)) {
                    userMails.add(message);
                }
            }
            return userMails;
    }
}
