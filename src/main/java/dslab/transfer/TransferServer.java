package dslab.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.transfer.tcp.TransferServerListenerThread;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransferServer implements ITransferServer, Runnable {

    private Config config;
    private Config domains;
    private Shell shell;
    private ServerSocket serverSocketDMTP;
    private DatagramSocket datagramSocket;

    private Log logger = LogFactory.getLog(TransferServer.class);

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        this.domains = new Config("domains");
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    @Override
    public void run() {
        logger.info("Transfer server is started at port: " + config.getInt("tcp.port"));
        try {
            datagramSocket = new DatagramSocket();
            serverSocketDMTP = new ServerSocket(config.getInt("tcp.port"));
            new TransferServerListenerThread(serverSocketDMTP, domains, datagramSocket, config).start();
        } catch (IOException e){
            logger.error("Error while creating server socket " + e);
            throw new UncheckedIOException("Error while creating server socket", e);
        }
        shell.run();
    }

    @Command
    @Override
    public void shutdown() {
        if(serverSocketDMTP != null) {
            try {
                serverSocketDMTP.close();
            } catch (IOException e) {
                logger.error("Error while closing server socket " + e);
            }
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

}
