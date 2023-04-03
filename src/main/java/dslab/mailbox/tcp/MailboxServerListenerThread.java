package dslab.mailbox.tcp;

import dslab.util.Config;
import dslab.util.DB;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MailboxServerListenerThread extends Thread {

    private ServerSocket serverSocket;
    private Config config;
    private ExecutorService executorService;
    private List<Socket> socketList;
    private DB database;

    private Log logger = LogFactory.getLog(MailboxServerListenerThread.class);

    public MailboxServerListenerThread(ServerSocket serverSocket, Config config, DB database) {
        this.serverSocket = serverSocket;
        this.config = config;
        executorService = Executors.newCachedThreadPool();
        socketList = new LinkedList<>();
        this.database = database;
    }

    public void run() {
        logger.info("Mailbox server listener thread is running");
        while (true){
            Socket socket;
            try {
                socket = serverSocket.accept();
                socketList.add(socket);
                if (serverSocket.getLocalPort() == config.getInt("dmap.tcp.port")) {
                    executorService.execute(new MailboxServerDMAP(socket, config, database));
                }else if (serverSocket.getLocalPort() == config.getInt("dmtp.tcp.port")) {
                    executorService.execute(new MailboxServerDMTP(socket, config, database));
                }
            } catch (IOException e) {
                shutdown();
                return;
            }
        }
    }

    public void shutdown() {
        executorService.shutdown();
        try {
            for (Socket socket : socketList) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error while closing socket " + e);
        }
    }
}
