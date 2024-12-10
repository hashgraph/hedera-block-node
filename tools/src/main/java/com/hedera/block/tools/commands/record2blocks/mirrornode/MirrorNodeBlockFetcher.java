package com.hedera.block.tools.commands.record2blocks.mirrornode;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MirrorNodeBlockFetcher {
    private static final String BASE_URL_PREFIX = "https://mainnet-public.mirrornode.hedera.com/api/v1/blocks?block.number=gt%3A";
    private static final String BASE_URL_POSTFIX = "&limit=100&order=asc";
    public static void main(String[] args) {
        String urlString = BASE_URL_PREFIX + "1200" + BASE_URL_POSTFIX;
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine);
                }
                in.close();

                // Parse JSON response with GSON
                Gson gson = new Gson();
                JsonObject jsonResponse = gson.fromJson(content.toString(), JsonObject.class);
                System.out.println("Parsed JSON: " + jsonResponse);
            } else {
                System.out.println("GET request failed. Response Code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
