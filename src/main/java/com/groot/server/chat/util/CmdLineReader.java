package com.groot.server.chat.util;

import org.kohsuke.args4j.Option;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class CmdLineReader {
    @Option(required = true, name = "-i", aliases = "--serverid", usage = "Server ID")
    private String serverId;

    @Option(required = true, name = "-c", aliases = "--servers_conf", usage = "Server Configuration File")
    private String serversConfiguration;

    public String getServerId() {
        return serverId;
    }

    public String getServersConfiguration() {
        return serversConfiguration;
    }
}
