package org.example.Network.client;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ChatAPIClient {

    public static void main(String[] args) {
        try {
            // 访问凭证 access_token
            String accessToken = "<24.81df0362b36f5e8212409f8f8d6de8f6.2592000.1718954425.282335-73505887>";
            // 构建API请求地址
            String apiUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/yi_34b_chat?access_token=" + accessToken;
            // 构建请求参数
            String requestBody = "{\"messages\": [{\"role\": \"user\",\"content\": \"你好\"}],\"temperature\": 0.8,\"top_p\": 0.8}";
            // 构建HTTP请求
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            // 发送请求
            OutputStream os = conn.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();

            // 处理响应
            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            String output;
            StringBuilder response = new StringBuilder();
            while ((output = br.readLine()) != null) {
                response.append(output);
            }

            // 输出响应
            System.out.println("Response from API: " + response.toString());

            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
