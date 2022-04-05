package com.groot.server.chat.service;

import java.util.List;

/**
 * @author Melanga Kasun
 * @date (Tue) 22-Mar-2022
 */

public interface FastBullyElectionService {
    void recoverFromFailure();
    boolean startElection();
    void broadcastToLowerNeighbours();
    void updateView(List<String> ids);
    List<String> compareViews(List<String> ids, List<String> neighbours);
}
