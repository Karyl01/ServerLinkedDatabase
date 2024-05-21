package org.example.Network;
import org.example.Network.DatabaseUtil;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;

public class SimpleHttpServer {
    private static final int PORT = 8080;
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/upload", new UploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/userUploadImage", new UserUploadImageHandler()); // 新增的处理程序
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port " + PORT);
    }


    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // Parse multipart form data
                Map<String, String> formData = parseMultipartFormData(exchange);
                String userIdStr = formData.get("userId");
                String userPassword = formData.get("userPassword");
                String fileName = formData.get("fileName");
                byte[] fileBytes = Base64.getDecoder().decode(formData.get("fileBytes"));

                if (userIdStr == null || userPassword == null || fileName == null || fileBytes == null) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    return;
                }

                int userId = Integer.parseInt(userIdStr);

                // Define cache file path
                Path cacheDir = Paths.get("src/main/java/org/example/Network/ImageCache");
                if (!Files.exists(cacheDir)) {
                    Files.createDirectories(cacheDir);
                }
                Path filePath = cacheDir.resolve(fileName);

                try {
                    if (!DatabaseUtil.userExists(userId, userPassword)) {
                        String response = "User does not exist.";
                        exchange.sendResponseHeaders(403, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }

                    // Save file to cache directory
                    Files.write(filePath, fileBytes);

                    // Get file metadata
                    String imageType = getFileExtension(fileName);
                    long imageSize = Files.size(filePath);

                    // Record file information in database
                    String result = DatabaseUtil.sendUserImage(userId, userPassword, fileName, (int) imageSize, imageType);
                    if (result != null) {
                        String response = "File uploaded and recorded successfully.";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                    } else {
                        // If database operation fails, delete the cache file
                        Files.delete(filePath);
                        exchange.sendResponseHeaders(500, -1); // Internal Server Error
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    // If exception occurs, delete the cache file
                    Files.delete(filePath);
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                } catch (IOException e) {
                    e.printStackTrace();
                    // If exception occurs, delete the cache file
                    Files.delete(filePath);
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }



        private Map<String, String> parseMultipartFormData(HttpExchange exchange) throws IOException {
            Map<String, String> formData = new HashMap<>();
            InputStream is = exchange.getRequestBody();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String body = sb.toString();
            String[] parts = body.split("--");

            for (String part : parts) {
                if (part.contains("Content-Disposition: form-data")) {
                    String[] lines = part.split("\r\n");
                    String name = null;
                    String value = null;
                    for (String l : lines) {
                        if (l.contains("name=\"")) {
                            int start = l.indexOf("name=\"") + 6;
                            int end = l.indexOf("\"", start);
                            name = l.substring(start, end);
                        } else if (!l.isEmpty() && !l.contains("Content-Disposition") && !l.contains("Content-Type")) {
                            value = l;
                        }
                    }
                    if (name != null && value != null) {
                        if (name.equals("file")) {
                            formData.put("fileBytes", value);
                        } else {
                            formData.put(name, value);
                        }
                    }
                }
            }
            return formData;
        }

        private String getFileExtension(String fileName) {
            int lastIndexOfDot = fileName.lastIndexOf('.');
            if (lastIndexOfDot > 0) {
                return fileName.substring(lastIndexOfDot + 1);
            }
            return "";
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


    static class UserUploadImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // 读取请求数据
                    BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                    String requestData = reader.readLine();

                    // 解析请求数据
                    String[] params = requestData.split("&");
                    int userId = Integer.parseInt(params[0].split("=")[1]);
                    String userPassword = URLDecoder.decode(params[1].split("=")[1], StandardCharsets.UTF_8);
                    String localImagePath = URLDecoder.decode(params[2].split("=")[1], StandardCharsets.UTF_8);

                    // 在此处调用 FileServer 中的逻辑处理
                    String imagePath = handleUserUploadImage(userId, userPassword, localImagePath);

                    // 返回结果给客户端
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(imagePath.getBytes());
                    outputStream.flush();

                    // 关闭流
                    outputStream.close();
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }

        private String handleUserUploadImage(int userId, String userPassword, String localImagePath) {
            try {
                // 读取本地图片文件信息并上传
                File imageFile = new File(localImagePath);
                String imageName = imageFile.getName();
                byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
                int imageSize = fileBytes.length;
                String imageType = imageName.substring(imageName.lastIndexOf('.') + 1);

                // 调用 sendUserImage 方法
                String imagePath = DatabaseUtil.sendUserImage(userId, userPassword, imageName, imageSize, imageType);
                return imagePath != null ? imagePath : ""; // 返回服务器返回的图片路径
            } catch (IOException | SQLException e) {
                e.printStackTrace();
                return "";
            }
        }
    }






















}


