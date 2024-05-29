package org.example.Network;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AudioUtil {
    public static final String API_URL = "http://127.0.0.1:9880/";
    public static final String OUTPUT_PATH = "C:\\Users\\USER\\Desktop\\output.wav";

    public static void sendApiRequest(String apiUrl, String text, String textLanguage, String outputPath) {
        try {
            // 准备推理数据
            String jsonData = "{\"text\": \"" + text + "\", \"text_language\": \"" + textLanguage + "\", \"cut_punc\": \"，。\"}";

            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);

            // 发送 JSON 数据
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 保存响应内容到文件
                try (java.io.InputStream is = connection.getInputStream();
                     FileOutputStream fos = new FileOutputStream(outputPath)) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                    System.out.println("音频已经保存为 " + outputPath);
                }
            } else {
                System.out.println("Error: " + responseCode);
                try (java.io.InputStream is = connection.getErrorStream()) {
                    if (is != null) {
                        byte[] errorBuffer = new byte[1024];
                        int bytesRead;
                        StringBuilder errorResponse = new StringBuilder();
                        while ((bytesRead = is.read(errorBuffer)) != -1) {
                            errorResponse.append(new String(errorBuffer, 0, bytesRead, StandardCharsets.UTF_8));
                        }
                        System.out.println("Message: " + errorResponse.toString());
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        String text = "老师，阿罗娜最喜欢您啦！如果您有吩咐的话，阿罗娜会尽力去做哒。";
        String textLanguage = "zh";
        sendApiRequest(API_URL, text, textLanguage, OUTPUT_PATH);
    }



}
