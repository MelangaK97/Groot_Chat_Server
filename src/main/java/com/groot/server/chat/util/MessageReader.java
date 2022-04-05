package com.groot.server.chat.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author Melanga Kasun
 * @date (Fri) 18-Feb-2022
 */
public class MessageReader {
    private static final JSONParser JSON_PARSER = new JSONParser();
    public static JSONObject read(InputStream stream) throws IOException, ParseException {
        return (JSONObject) JSON_PARSER.parse(new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).readLine());
    }
}
