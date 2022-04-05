package com.groot.server.chat.util;

import com.groot.server.chat.database.ChatServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class ConfigFileReader {
    public static List<ChatServer> read(String filename) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(filename));
        List<ChatServer> serverConfigurations = new ArrayList<>();
        while (scanner.hasNextLine()) {
            String[] line = scanner.nextLine().split("\\s+");
            serverConfigurations.add(new ChatServer(line[0], line[1], Integer.parseInt(line[2]), Integer.parseInt(line[3])));
        }
        scanner.close();
        return serverConfigurations;
    }
}
