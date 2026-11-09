package com.bank.server.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkUtil {

    /**
     * Attempts to find a non-loopback IPv4 address for LAN/Wi-Fi.
     */
    public static String getLocalLanIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;

                // Prefer Wi-Fi or Ethernet
                String name = iface.getDisplayName().toLowerCase();
                boolean isVirtual = name.contains("vmnet") || name.contains("virtual") || name.contains("hyper-v") || name.contains("wsl");

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String hostAddress = addr.getHostAddress();
                        if (!isVirtual) {
                            return hostAddress; // Ideal physical LAN / Wi-Fi IP
                        }
                    }
                }
            }

            // Fallback: any non-loopback IPv4
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) continue;
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }

            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "localhost";
        }
    }
}
