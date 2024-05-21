package org.example.Network.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class FileClient {

    private static final String SERVER_URL = "https://b791-2001-da8-201d-1102-1dd3-a1b7-e709-cae7.ngrok-free.app";




    /**
     * 上传文件到服务器
     *
     * @param filePath 本地文件路径
     * @return true 如果上传成功, 否则 false
     */
    public static boolean uploadFile(String filePath) {
        String targetUrl = SERVER_URL + "/upload";
        File file = new File(filePath);

        try {
            // 打开连接
            URL url = new URL(targetUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            // 设置请求方法为POST
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");

            // 设置请求头
            httpConn.setRequestProperty("Content-Type", "application/octet-stream");
            httpConn.setRequestProperty("Content-Length", String.valueOf(file.length()));

            // 发送文件
            FileInputStream fileInputStream = new FileInputStream(file);
            OutputStream outputStream = httpConn.getOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            fileInputStream.close();

            // 获取响应
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("File uploaded successfully.");
                return true;
            } else {
                System.out.println("Failed to upload file. Server responded with: " + responseCode);
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }




    /**
     * 从服务器下载文件
     *
     * @param fileName       要下载的文件名
     * @param localSavePath  本地保存路径
     * @return true 如果下载成功, 否则 false
     */
    public static boolean downloadFile(String fileName, String localSavePath) {
        String fileURL = SERVER_URL + "/download?file=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        try {
            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            // Always check HTTP response code first
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("Content-Type = " + httpConn.getContentType());
                System.out.println("Content-Length = " + httpConn.getContentLength());

                // 打开输入流
                InputStream inputStream = httpConn.getInputStream();

                // 打开输出流保存文件
                FileOutputStream outputStream = new FileOutputStream(localSavePath);

                int bytesRead = -1;
                byte[] buffer = new byte[4096];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                System.out.println("File downloaded");
                return true;
            } else {
                System.out.println("No file to download. Server replied HTTP code: " + responseCode);
                return false;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }


    /**
     * 发送userName和userPassword给服务器用来注册用户
     *
     * @param userName 注册用户名
     * @param userPassword 注册用户的密码
     * @return true 如果注册用户成功则返回true, 否则 false
     */
    public static boolean registerUser(String userName, String userPassword) {
        String targetUrl = SERVER_URL + "/register";

        try {
            // 构建表单数据
            String formData = "userName=" + URLEncoder.encode(userName, StandardCharsets.UTF_8)
                    + "&userPassword=" + URLEncoder.encode(userPassword, StandardCharsets.UTF_8);

            // 打开连接
            URL url = new URL(targetUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            // 设置请求方法为POST
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");

            // 设置请求头
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", String.valueOf(formData.getBytes().length));

            // 发送表单数据
            OutputStream outputStream = httpConn.getOutputStream();
            outputStream.write(formData.getBytes());
            outputStream.flush();
            outputStream.close();

            // 获取响应
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("User registered successfully.");
                return true;
            } else {
                System.out.println("Failed to register user. Server responded with: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * 发送user的信息用来确认身份，之后上传一个本地在localImagePath路径的图像文件的相关信息
     *
     * @param userId         发送图片的user的Id
     * @param userPassword   user的密码
     * @param localImagePath 本地需要上传图片的信息的路径
     * @param ImageName 用户想要指定的上传图片的名字
     * @return 如果上传图片成功则返回服务器返回的字符串，否则返回空字符串
     */
    public static String userUploadImage(int userId, String userPassword, String localImagePath,String ImageName) {
        String targetUrl = SERVER_URL + "/userUploadImage"; // 服务器端处理上传的URL
        File file = new File(localImagePath);

        try {
            // 构建表单数据
            String formData = "userId=" + userId
                    + "&userPassword=" + URLEncoder.encode(userPassword, StandardCharsets.UTF_8)
                    + "&imageName=" + ImageName
                    + "&imageSize=" + file.length()
                    + "&imageType=" + getImageType(file);

            // 打开连接
            URL url = new URL(targetUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            // 设置请求方法为POST
            httpConn.setDoOutput(true);
            httpConn.setRequestMethod("POST");

            // 设置请求头
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestProperty("Content-Length", String.valueOf(formData.getBytes().length));

            // 发送表单数据
            OutputStream outputStream = httpConn.getOutputStream();
            outputStream.write(formData.getBytes());
            outputStream.flush();
            outputStream.close();

            // 获取响应
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取服务器返回的字符串（存储路径或错误信息）
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                String response = reader.readLine();
                reader.close();

                System.out.println("Server response: " + response);
                return response;
            } else {
                System.out.println("Failed to upload image. Server responded with: " + responseCode);
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
    private static String getImageType(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex != -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }





    /**
     * 发送user的信息用来确认身份，之后上传一个本地在localImagePath路径的图像文件的相关信息
     *
     * @param localFilePath    发送文件的本地路径
     * @param serverFilePath   在服务器端想要把这个文件存在什么地方的指定路径
     * @return boolean 成功是true
     */
    public static boolean uploadFile(String localFilePath, String serverFilePath) {
        try {
            File file = new File(localFilePath);
            FileInputStream fis = new FileInputStream(file);
            URL url = new URL("http://localhost:8080/upload?path=" + serverFilePath);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Content-Length", String.valueOf(file.length()));

            try (OutputStream os = connection.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }



    public static void main(String[] args) {
//        String uploadFilePath = "C:\\Users\\admin\\Desktop\\2.png"; // 本地图片路径
//        String downloadFileName = "uploaded_1.jpg"; // 要下载的文件名
//        String downloadSavePath = "C:\\Users\\admin\\Desktop\\" + downloadFileName; // 本地保存路径
//        // 上传文件
//        boolean uploadSuccess = uploadFile(uploadFilePath);
//        System.out.println("Upload success: " + uploadSuccess);
//        // 下载文件
//        boolean downloadSuccess = downloadFile(downloadFileName, downloadSavePath);
//        System.out.println("Download success: " + downloadSuccess);

//        registerUser("s", "000000");
//        userUploadImage(1, "0", "C:\\Users\\admin\\Desktop\\1.png");
//        userUploadImage(1,"sensei", "C:\\Users\\admin\\Desktop\\1.png");
        System.out.println("User upload image should be stored at: "+ userUploadImage(1,"sensei", "C:\\Users\\admin\\Desktop\\2.png", "Hina"));

    }









}
