package com.tai.workflow.domain.util;

import lombok.extern.slf4j.Slf4j;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author zhanghaolong1989@163.com
 */
@Slf4j
public class IpUtils {
    private static final Integer DNS_PORT = 10002;

    public static Optional<String> getLocalIp4Address() {
        final List<Inet4Address> ipByNi = getLocalIp4AddressFromNetworkInterface();
        if (ipByNi.size() != 1) {
            final Optional<Inet4Address> ipBySocketOpt = getIpBySocket();
            if (ipBySocketOpt.isPresent()) {
                return ipBySocketOpt.map(Inet4Address::getHostAddress);
            } else {
                return ipByNi.isEmpty() ? Optional.empty() : Optional.of(ipByNi.get(0)).map(Inet4Address::getHostAddress);
            }
        }
        return Optional.of(ipByNi.get(0)).map(Inet4Address::getHostAddress);
    }

    public static List<Inet4Address> getLocalIp4AddressFromNetworkInterface() {
        List<Inet4Address> addresses = new ArrayList<>(1);
        Enumeration<NetworkInterface> networkInterfaceEnumeration = null;
        try {
            networkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            log.error("getLocalIp4AddressFromNetworkInterface met with exception", ex);
        }
        if (Objects.isNull(networkInterfaceEnumeration)) {
            return addresses;
        }

        while (networkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaceEnumeration.nextElement();
            try {
                if (!isValidInterface(networkInterface)) {
                    continue;
                }
            } catch (SocketException ex) {
                log.error("getLocalIp4AddressFromNetworkInterface isValidInterface met with exception", ex);
                continue;
            }

            Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses();
            while (inetAddressEnumeration.hasMoreElements()) {
                InetAddress inetAddress = inetAddressEnumeration.nextElement();
                if (isValidAddress(inetAddress)) {
                    addresses.add((Inet4Address) inetAddress);
                }
            }
        }
        return addresses;
    }

    /**
     * 过滤回环网卡、非活动网卡、虚拟网卡并要求网卡名字是eth或ens开头
     *
     * @param ni 网卡
     * @return 如果满足要求则true，否则false
     */
    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && ni.isUp() && !ni.isVirtual() && (ni.getName().startsWith("eth") || ni.getName()
                .startsWith("ens") || ni.getName().startsWith("en") || ni.getName().startsWith("utun"));
    }

    /**
     * 判断是否是IPv4，并且内网地址并过滤回环地址.
     */
    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress();
    }

    private static Optional<Inet4Address> getIpBySocket() {
        try (DatagramSocket socket = new DatagramSocket()) {
            log.info("getIpBySocket use communication with 8.8.8.8 to get local ip");
            socket.connect(InetAddress.getByName("8.8.8.8"), DNS_PORT);
            if (socket.getLocalAddress() instanceof Inet4Address) {
                return Optional.of((Inet4Address) socket.getLocalAddress());
            }
        } catch (UnknownHostException | SocketException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }
}
