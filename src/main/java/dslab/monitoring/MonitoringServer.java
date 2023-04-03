package dslab.monitoring;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.monitoring.udp.MonitoringServerListenerThread;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MonitoringServer implements IMonitoringServer {

    private Config config;
    private DatagramSocket datagramSocket;
    private Shell shell;
    private HashMap<String, Integer> serverMap;
    private HashMap<String, Integer> addressMap;

    private Log logger = LogFactory.getLog(MonitoringServer.class);

    /**
     * Creates a new server instance.
     *
     * @param componentId the id of the component that corresponds to the Config resource
     * @param config the component config
     * @param in the input stream to read console input from
     * @param out the output stream to write console output to
     */
    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
        serverMap = new HashMap<>();
        addressMap = new HashMap<>();
    }

    @Override
    public void run() {
        logger.info("Monitoring server is started at port: " + config.getInt("udp.port"));
        try {
            datagramSocket = new DatagramSocket(config.getInt("udp.port"));

            new MonitoringServerListenerThread(datagramSocket, serverMap, addressMap).start();
        } catch (SocketException e) {
            logger.error("Error while creating server socket " + e);
            throw new UncheckedIOException("Error while creating server socket", e);
        }
        shell.run();
    }

    @Command
    @Override
    public void addresses() {
        if (!addressMap.isEmpty()) {
            for (Map.Entry<String, Integer> address : addressMap.entrySet()) {
                shell.out().println(address.getKey() + " " + address.getValue());
            }
        }
    }

    @Command
    @Override
    public void servers() {
        if (!serverMap.isEmpty()) {
            for (Map.Entry<String, Integer> server : serverMap.entrySet()) {
                shell.out().println(server.getKey() + " " + server.getValue());
            }
        }
    }

    @Command
    @Override
    public void shutdown() {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
        throw new StopShellException();
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

}
