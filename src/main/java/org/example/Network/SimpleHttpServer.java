package org.example.Network;

import org.example.Network.DatabaseUtil;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;

public class SimpleHttpServer {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/register", new RegisterHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port 8080");
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // 获取上传的文件流
                InputStream is = exchange.getRequestBody();
                FileOutputStream fos = new FileOutputStream("uploaded_1.jpg");

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }

                fos.close();
                is.close();

                String response = "File uploaded successfully.";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
                String fileName = params.get("file");
                if (fileName == null || fileName.isEmpty()) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    return;
                }

                File file = new File(fileName); // Ensure this file path is correct
                if (file.exists()) {
                    exchange.sendResponseHeaders(200, file.length());
                    OutputStream os = exchange.getResponseBody();
                    FileInputStream fis = new FileInputStream(file);
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    fis.close();
                    os.close();
                } else {
                    exchange.sendResponseHeaders(404, -1); // Not Found
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Read request body
                InputStream is = exchange.getRequestBody();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                is.close();

                // Parse form data
                Map<String, String> params = parseFormData(sb.toString());
                String userName = params.get("userName");
                String userPassword = params.get("userPassword");

                String response;
                if (userName == null || userPassword == null || userName.isEmpty() || userPassword.isEmpty()) {
                    response = "Username and password must be provided.";
                    exchange.sendResponseHeaders(400, response.getBytes().length);
                } else {
                    try {
                        boolean isInserted = DatabaseUtil.insertUser(userName, userPassword);
                        if (isInserted) {
                            response = "User registered successfully.";
                            exchange.sendResponseHeaders(200, response.getBytes().length);
                        } else {
                            response = "User registration failed.";
                            exchange.sendResponseHeaders(500, response.getBytes().length);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                        response = "Database error occurred: " + e.getMessage();
                        exchange.sendResponseHeaders(500, response.getBytes().length);
                    }
                }

                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }

        private Map<String, String> parseFormData(String formData) {
            Map<String, String> result = new HashMap<>();
            for (String param : formData.split("&")) {
                String[] entry = param.split("=");
                if (entry.length > 1) {
                    result.put(entry[0], entry[1]);
                } else {
                    result.put(entry[0], "");
                }
            }
            return result;
        }
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) {
            return result;
        }
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}
