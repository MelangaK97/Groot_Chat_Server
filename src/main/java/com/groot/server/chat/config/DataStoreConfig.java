package com.groot.server.chat.config;

import com.groot.server.chat.database.DataStore;

import java.util.Objects;

/**
 * @author Melanga Kasun
 * @date (Sun) 20-Feb-2022
 */
public class DataStoreConfig {
    private static DataStore store;

    private DataStoreConfig() {
    }

    public static DataStore getInstance() {
        if (Objects.isNull(store)) {
            synchronized (DataStore.class) {
                if (Objects.isNull(store)) {
                    store = new DataStore();
                }
            }
        }
        return store;
    }
}
