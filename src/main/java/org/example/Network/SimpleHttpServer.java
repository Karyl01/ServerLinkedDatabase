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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.example.Network.DatabaseUtil.userExists;

public class SimpleHttpServer {
    private static final int PORT = 8080;
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/upload", new FileUploadHandler());
        server.createContext("/download", new DownloadHandler());
        server.createContext("/register", new RegisterHandler());
        server.createContext("/userUploadImage", new UserUploadImageHandler());
        server.createContext("/userSearchByName", new UserSearchByNameHandler());
        server.createContext("/downloadSingle", new SingleFileDownloadHandler());
        server.createContext("/testConnection", new ConnectionTestHandler());
        server.createContext("/userExists", new UserExistsHandler());
        server.createContext("/getAllImagesInfo", new GetAllImagesHandler());
        server.createContext("/getImagePathAndType", new GetImagePathAndTypeHandler());
        server.createContext("/receiveChatMessage", new ChatMessageHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
        System.out.println("Server started on port " + PORT);
    }


    //实现从指定路径下载文件的服务类
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


    //接受注册用户信息的服务类
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
                        int userId = DatabaseUtil.insertUser(userName, userPassword);
                        if (userId > 0) {
                            response = String.valueOf(userId);
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








    //用户上载图片信息到数据库之后返回应该保存的位置的方法，和上载文件的服务类一起使用
    static class UserUploadImageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // 读取请求数据
                    BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                    String requestData = reader.readLine();

                    // 解析请求数据
                    Map<String, String> params = parseFormData(requestData);
                    int userId = Integer.parseInt(params.get("userId"));
                    String userPassword = URLDecoder.decode(params.get("userPassword"), StandardCharsets.UTF_8);
                    String imageName = URLDecoder.decode(params.get("imageName"), StandardCharsets.UTF_8);
                    int imageSize = Integer.parseInt(params.get("imageSize"));
                    String imageType = URLDecoder.decode(params.get("imageType"), StandardCharsets.UTF_8);

                    // 在此处调用数据库操作方法，保存图片相关信息
                    String response = DatabaseUtil.sendUserImage(userId, userPassword, imageName, imageSize, imageType);

                    // 返回结果给客户端
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(response.getBytes());
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



    //上载文件到指定位置的方法，和上面的服务类一起使用构成用户上传文件并且放到规定位置并且在数据库中添加指定信息的类
    static class FileUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String serverFilePath = null;
                if (query != null) {
                    String[] params = query.split("=");
                    if (params.length == 2 && "path".equals(params[0])) {
                        serverFilePath = URLDecoder.decode(params[1], "UTF-8");
                    }
                }

                if (serverFilePath != null) {
                    // 创建指定路径的文件
                    File file = new File(serverFilePath);
                    if (!file.exists()) {
                        file.getParentFile().mkdirs(); // 创建目录及其父目录
                        file.createNewFile(); // 创建文件
                    }

                    try (InputStream is = exchange.getRequestBody();
                         FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }

                        String response = "File uploaded successfully.";
                        exchange.sendResponseHeaders(200, response.length());
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(response.getBytes());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.sendResponseHeaders(500, -1);
                    }
                } else {
                    exchange.sendResponseHeaders(400, -1); // 400 Bad Request
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // 405 Method Not Allowed
            }
        }
    }





    //用户根据UserName查询一个写着所有的相同name的image信息的id的接收器
    static class UserSearchByNameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String imageName = exchange.getRequestURI().getQuery().split("=")[1]; // 获取查询参数中的图片名称
                try {
                    List<String> imagePaths = DatabaseUtil.findImagesByName(imageName); // 查询图片 ID
                    String response = String.join(",", imagePaths.stream().map(String::valueOf).toArray(String[]::new)); // 将图片地址 转换为逗号分隔的字符串
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(response.getBytes());
                    outputStream.flush();
                    outputStream.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }



    //用户根据输入的一个服务器上文件的地址之后通过服务类发送给客户端
    static class SingleFileDownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.startsWith("file=")) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    return;
                }

                String fileParam = URLDecoder.decode(query.substring(5), "UTF-8");
                Path filePath = Paths.get(fileParam);

                if (Files.exists(filePath)) {
                    exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
                    exchange.sendResponseHeaders(200, Files.size(filePath));

                    try (OutputStream os = exchange.getResponseBody();
                         InputStream is = Files.newInputStream(filePath)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            os.write(buffer, 0, length);
                        }
                    }
                } else {
                    exchange.sendResponseHeaders(404, -1); // Not Found
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }


    //测试链接服务器是否成功的方法
    static class ConnectionTestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String response = "Connection Successful";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }


    //测试用户是否存在的服务类
    static class UserExistsHandler implements HttpHandler {
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
                int userId = Integer.parseInt(params.get("userId"));
                String username = params.get("username");

                // Check if user exists
                boolean userExists = false;
                try {
                    userExists = userExists(userId, username);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                // Send response to client
                String response = String.valueOf(userExists);
                exchange.sendResponseHeaders(200, response.getBytes().length);
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


    // 获取所有图片信息的服务器端处理器
    static class GetAllImagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    String response = DatabaseUtil.getAllImagesInfo();
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }







    static class GetImagePathAndTypeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                if (query == null || !query.startsWith("imageId=")) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    return;
                }

                String imageIdParam = URLDecoder.decode(query.substring(8), StandardCharsets.UTF_8);
                int imageId;
                try {
                    imageId = Integer.parseInt(imageIdParam);
                } catch (NumberFormatException e) {
                    exchange.sendResponseHeaders(400, -1); // Bad Request
                    return;
                }

                try {
                    String result = DatabaseUtil.getImagePathAndTypeById(imageId);
                    if (result != null) {
                        exchange.sendResponseHeaders(200, result.getBytes().length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(result.getBytes());
                        }
                    } else {
                        exchange.sendResponseHeaders(404, -1); // Not Found
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }
    }







    //接收客户端的chat方法发送过来的string并且调用sovits模型进行语音生成，语音生成的固定位置是桌面的output.wav
    static class ChatMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()));
                    String requestData = reader.readLine();

                    Map<String, String> params = parseFormData(requestData);
                    String content = URLDecoder.decode(params.get("content"), StandardCharsets.UTF_8);

                    handleChatMessage(content);

                    exchange.sendResponseHeaders(200, -1); // OK
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(500, -1); // Internal Server Error
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        }

        private void handleChatMessage(String content) {
            AudioUtil.sendApiRequest(AudioUtil.API_URL, content, "zh", AudioUtil.OUTPUT_PATH);
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





















}


