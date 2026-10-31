package mage.remote;

import mage.MageException;
import mage.utils.MageVersion;

/**
 * @author BetaSteward_at_googlemail.com
 */
public class MageVersionException extends MageException {

    private final MageVersion clientVersion;
    private final MageVersion serverVersion;

    public MageVersionException(MageVersion clientVersion, MageVersion serverVersion) {
        super("Wrong client version."
                + "<br/>Your version: " + clientVersion
                + "<br/>Server version: " + (serverVersion == null ? "unknown" : serverVersion)
                + "<br/>App download: http://xmage.today");
        this.clientVersion = clientVersion;
        this.serverVersion = serverVersion;
    }

    public MageVersion getClientVersion() {
        return clientVersion;
    }

    public MageVersion getServerVersion() {
        return serverVersion;
    }
}
