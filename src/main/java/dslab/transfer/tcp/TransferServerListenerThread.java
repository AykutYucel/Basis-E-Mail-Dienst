package dslab.transfer.tcp;

import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TransferServerListenerThread extends Thread {

    private ServerSocket serverSocket;
    private Config domains;
    private ExecutorService executorService;
    private List<Socket> socketList;
    private DatagramSocket datagramSocket;
    private Config config;

    private Log logger = LogFactory.getLog(TransferServerListenerThread.class);

    public TransferServerListenerThread(ServerSocket serverSocket, Config domains, DatagramSocket datagramSocket, Config config) {
        this.serverSocket = serverSocket;
        this.domains = domains;
        executorService = Executors.newCachedThreadPool();
        socketList = new LinkedList<>();
        this.datagramSocket = datagramSocket;
        this.config = config;
    }

    public void run() {
        logger.info("Transfer server listener thread is running");
        while (true){
            Socket socket;
            try {
                socket = serverSocket.accept();
                socketList.add(socket);
                executorService.execute(new TransferServerDMTP(socket, domains, datagramSocket, config));
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
