package dslab.monitoring.udp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.HashMap;

public class MonitoringServerListenerThread extends Thread {

    private DatagramSocket datagramSocket;
    private HashMap<String, Integer> serverMap;
    private HashMap<String, Integer> addressMap;

    private Log logger = LogFactory.getLog(MonitoringServerListenerThread.class);

    public MonitoringServerListenerThread(DatagramSocket datagramSocket, HashMap<String,Integer> serverMap, HashMap<String, Integer> addressMap) {
        this.datagramSocket = datagramSocket;
        this.serverMap = serverMap;
        this.addressMap = addressMap;
    }

    public void run() {

        byte[] buffer;
        DatagramPacket datagramPacket;
        try {
            while (true) {
                buffer = new byte[1024];
                datagramPacket = new DatagramPacket(buffer, buffer.length);
                datagramSocket.receive(datagramPacket);
                String request = new String(datagramPacket.getData());

                String[] parts = request.trim().split("\\s");

                if (parts.length == 2) {
                    addServer(parts[0]);
                    addAddress(parts[1]);
                }
            }
        } catch (SocketException e) {
            logger.error("Error while waiting for packets " + e);
        } catch (IOException e) {
            logger.error("Error while interacting with socket " + e);
        } finally {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        }
    }

    private void addAddress(String address) {
        if (addressMap.containsKey(address)) {
            addressMap.put(address, (addressMap.get(address) + 1));
        }else {
            addressMap.put(address, 1);
        }
    }

    private void addServer(String server) {
        if (serverMap.containsKey(server)) {
            serverMap.put(server, (serverMap.get(server) + 1));
        }else {
            serverMap.put(server, 1);
        }
    }
}
