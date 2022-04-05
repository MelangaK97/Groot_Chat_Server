package com.groot.server.chat.util;

import org.json.simple.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class MessageSender {
    public static void send(OutputStream stream, JSONObject message) throws IOException {
        DataOutputStream response = new DataOutputStream(stream);
        response.write((message.toJSONString() + "\n").getBytes(StandardCharsets.UTF_8));
        response.flush();
    }
}
