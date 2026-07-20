package cz.humblej.squares.app;

import cz.humblej.squares.ui.Messages;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

final class NetworkAddressSupport {
    private NetworkAddressSupport() {
    }

    static NetworkAddress ask(JFrame frame) {
        List<NetworkAddress> addresses = localAddresses(false);
        if (addresses.isEmpty()) {
            return null;
        }
        if (addresses.size() == 1) {
            return addresses.get(0);
        }

        return (NetworkAddress) JOptionPane.showInputDialog(frame,
                Messages.ADAPTER_PROMPT, Messages.ADAPTER_TITLE, JOptionPane.PLAIN_MESSAGE,
                null, addresses.toArray(), addresses.get(0));
    }

    static List<NetworkAddress> localAddresses(boolean includeVirtual) {
        List<NetworkAddress> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback()
                        || (!includeVirtual && isVirtualLike(networkInterface))) {
                    continue;
                }

                Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
                while (interfaceAddresses.hasMoreElements()) {
                    InetAddress address = interfaceAddresses.nextElement();
                    String value = address.getHostAddress();
                    if (!address.isLoopbackAddress() && value.indexOf(':') < 0) {
                        addresses.add(new NetworkAddress(value, networkInterface.getDisplayName()));
                    }
                }
            }
        } catch (SocketException exception) {
            return addresses;
        }
        return addresses;
    }

    static void select(JComboBox<NetworkAddress> adapterBox, String address) {
        for (int index = 0; index < adapterBox.getItemCount(); index++) {
            if (adapterBox.getItemAt(index).address().equals(address)) {
                adapterBox.setSelectedIndex(index);
                return;
            }
        }
    }

    static boolean contains(List<NetworkAddress> addresses, String address) {
        for (NetworkAddress item : addresses) {
            if (item.address().equals(address)) {
                return true;
            }
        }
        return false;
    }

    static Integer parsePort(String value) {
        try {
            int port = Integer.parseInt(value.trim());
            return port >= 1 && port <= 65535 ? port : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isVirtualLike(NetworkInterface networkInterface) throws SocketException {
        if (networkInterface.isVirtual()) {
            return true;
        }

        String name = (networkInterface.getName() + " " + networkInterface.getDisplayName()).toLowerCase();
        return name.contains("virtual") || name.contains("vmware") || name.contains("virtualbox")
                || name.contains("hyper-v") || name.contains("vbox") || name.contains("vmnet")
                || name.contains("wsl") || name.contains("docker") || name.contains("tap")
                || name.contains("tun") || name.contains("vpn") || name.contains("loopback");
    }
}
