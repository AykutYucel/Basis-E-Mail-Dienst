package dslab.mailbox;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.mailbox.tcp.MailboxServerListenerThread;
import dslab.util.Config;
import dslab.util.DB;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MailboxServer implements IMailboxServer, Runnable {

    private Config config;
    private Shell shell;
    private ServerSocket serverSocketDMAP;
    private ServerSocket serverSocketDMTP;
    private DB database;

    private Log logger = LogFactory.getLog(MailboxServer.class);

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
        database = new DB(config.getString("domain"));
    }

    @Override
    public void run() {
        logger.info("Mailbox server is started at ports: " + config.getInt("dmtp.tcp.port") + " and " + config.getInt("dmap.tcp.port"));
        try {
            serverSocketDMTP = new ServerSocket(config.getInt("dmtp.tcp.port"));
            serverSocketDMAP = new ServerSocket(config.getInt("dmap.tcp.port"));
            new MailboxServerListenerThread(serverSocketDMTP, config, database).start();
            new MailboxServerListenerThread(serverSocketDMAP, config, database).start();
        } catch (IOException e){
            logger.error("Error while creating server socket " + e);
            throw new UncheckedIOException("Error while creating server socket", e);
        }
        shell.run();
    }

    @Command
    @Override
    public void shutdown() {
        if (serverSocketDMAP != null) {
            try {
                serverSocketDMAP.close();
            } catch (IOException e) {
                logger.error("Error while closing server socket " + e);
            }
        }
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
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run();
    }
}
