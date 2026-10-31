
package mage.remote;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import mage.players.net.UserData;

/**
 *
 * @author BetaSteward_at_googlemail.com
 */
public class Connection {

    private String host;
    private int port;
    private String username;
    private String password;
    private String email;
    private String authToken;
    private String adminPassword;
    private ProxyType proxyType;
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private int clientCardDatabaseVersion;
    private String userIdStr;
    private int socketWriteTimeout;

    private UserData userData;

    private static final String serialization = "?serializationtype=java";
    private static final String transport = "bisocket";
    // two independent fixes for jboss oneway pools on java 9+, see issue #6514:
    // - onewayThreadPool replaces a broken pool of ServerInvoker (incoming oneway invocations);
    // - maxNumThreadsOneway limits a pool of jboss Client inside ServerInvokerCallbackHandler;
    // current usage: in async mode it create one stable thread per user session to send async callbacks from server to client
    // see ASYNC_MESSAGES
    private static final String threadpool = "onewayThreadPool=mage.remote.CustomThreadPool&maxNumThreadsOneway=1";

    private final String parameter;

    public Connection() {
        this("");
    }

    public Connection(String parameter) {
        this.parameter = parameter;
        socketWriteTimeout = 10000;
    }

    @Override
    public int hashCode() {
        return (transport + host + Integer.toString(port) + proxyType.toString()).hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Connection)) {
            return false;
        }
        Connection otherConnection = (Connection) object;
        return hashCode() == otherConnection.hashCode();
    }

    @Override
    public String toString() {
        return host + ':' + Integer.toString(port) + '/' + serialization + parameter;
    }

    public String getURI() {
        if (host.equals("localhost") || host.equals("127.0.0.1")) {
            return transport + "://127.0.0.1:" + port + '/' + serialization + "&" + threadpool + parameter;
        }
        return transport + "://" + host + ':' + port + '/' + serialization + "&" + threadpool + parameter;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public void setProxyType(ProxyType proxyType) {
        this.proxyType = proxyType;
    }

    public enum ProxyType {

        SOCKS("Socks"), HTTP("HTTP"), NONE("None");

        private final String text;

        ProxyType(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static ProxyType valueByText(String value) {
            for (ProxyType type : values()) {
                if (type.text.equals(value)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserIdStr() {
        return userIdStr;
    }

    public void setUserIdStr(String userIdStr) {
        this.userIdStr = userIdStr;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public static InetAddress getLocalAddress() throws SocketException {
        InetAddress fallback = null;
        for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
            NetworkInterface iface = interfaces.nextElement();
            if (iface.isLoopback() || !iface.isUp() || iface.isVirtual()) {
                continue;
            }
            String name = (iface.getName() + " " + iface.getDisplayName()).toLowerCase();
            // Ignore disconnected or virtual VPN tap adapters
            if (name.contains("tap") || name.contains("vpn") || name.contains("wintun") || name.contains("tunnel")) {
                continue;
            }
            for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
                if (addr != null) {
                    InetAddress iaddr = addr.getAddress();
                    if (iaddr instanceof Inet4Address) {
                        if (!iaddr.isLoopbackAddress() && !iaddr.isLinkLocalAddress()) {
                            if (iaddr.isSiteLocalAddress()) {
                                return iaddr;
                            }
                            if (fallback == null) {
                                fallback = iaddr;
                            }
                        }
                    }
                }
            }
        }
        if (fallback != null) {
            return fallback;
        }
        return InetAddress.getLoopbackAddress();
    }

    public static String getMAC() throws SocketException {
        StringBuilder allMACs = new StringBuilder();
        for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces.hasMoreElements();) {
            NetworkInterface iface = interfaces.nextElement();
            byte[] mac = iface.getHardwareAddress();

            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                sb.append(';');
                allMACs.append(sb.toString());
            }
        }
        return allMACs.toString();
    }

    public void setUserData(UserData userData) {
        this.userData = userData;
    }

    public UserData getUserData() {
        return userData;
    }

    public int getSocketWriteTimeout() {
        return socketWriteTimeout;
    }
}
