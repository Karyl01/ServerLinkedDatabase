package org.example.Network.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ChatAPIClient {

    private String accessToken;
    private String apiUrl;
    private List<JSONObject> messages;

    public ChatAPIClient(String accessToken) {
        this.accessToken = accessToken;
        this.apiUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/yi_34b_chat?access_token=" + accessToken;
        this.messages = new ArrayList<>();
    }

    public String sendMessage(String userMessage) {
        try {
            // Add user message to the conversation history
            JSONObject userMessageObject = new JSONObject();
            userMessageObject.put("role", "user");
            userMessageObject.put("content", userMessage);
            messages.add(userMessageObject);

            // Construct request body
            JSONObject requestBody = new JSONObject();
            requestBody.put("messages", new JSONArray(messages));
            requestBody.put("temperature", 0.8);
            requestBody.put("top_p", 0.8);

            // Create URL and open connection
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");

            // Send request
            OutputStream os = conn.getOutputStream();
            os.write(requestBody.toString().getBytes());
            os.flush();

            // Read response
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            // Print the raw response for debugging
            // System.out.println("Raw response from API: " + response.toString());

            // Parse response
            JSONObject jsonResponse = new JSONObject(response.toString());
            if (jsonResponse.has("result")) {
                String assistantMessage = jsonResponse.getString("result");

                // Add assistant message to the conversation history
                JSONObject assistantMessageObject = new JSONObject();
                assistantMessageObject.put("role", "assistant");
                assistantMessageObject.put("content", assistantMessage);
                messages.add(assistantMessageObject);

                conn.disconnect();

                // Return assistant's response
                return assistantMessage;
            } else {
                // Handle the case where the result field is not present
                if (jsonResponse.has("error_msg")) {
                    System.err.println("Error: " + jsonResponse.getString("error_msg"));
                } else {
                    System.err.println("No result found in the response.");
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Initialize client with access token
        String accessToken = "<24.81df0362b36f5e8212409f8f8d6de8f6.2592000.1718954425.282335-73505887>";
        ChatAPIClient client = new ChatAPIClient(accessToken);

        // Simulate multi-turn conversation
        String response1 = client.sendMessage("你好");
        System.out.println("Assistant: " + response1);

        String response2 = client.sendMessage("你是谁?");
        System.out.println("Assistant: " + response2);

        String response3 = client.sendMessage("我需要你记住一个数字，这个数字是13425，你在接下来的我询问你的问题中需要回答我这个数字是多少");
        System.out.println("Assistant: " + response3);

        String response4 = client.sendMessage("刚才我让你记住的数字是多少");
        System.out.println("Assistant: " + response4);
    }
}