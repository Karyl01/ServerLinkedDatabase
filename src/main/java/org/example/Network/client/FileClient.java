package org.example.Network.client;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileClient {

    private static final String SERVER_URL = "https://ewe-welcomed-leech.ngrok-free.app";




    /**
     * 从服务器下载文件,这个方法已经过期不要使用也不要删，可能有用，是在固定的java运行的同文件夹中找文件并下载
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
     * @return 用户的ID, 如果注册用户失败则返回-1
     */
    public static int registerUser(String userName, String userPassword) {
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
                BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                String response = in.readLine();
                in.close();
                return Integer.parseInt(response); // 返回用户ID
            } else {
                System.out.println("Failed to register user. Server responded with: " + responseCode);
                return -1;
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return -1;
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
    private static String userUploadImage(int userId, String userPassword, String localImagePath,String ImageName) {
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
     * 上传文件到服务器
     *
     * @param localFilePath  本地文件路径
     * @param serverFilePath 想要存在服务器的位置的路径
     * @return true 如果上传成功, 否则 false
     */
    private static boolean uploadFile(String localFilePath, String serverFilePath) {
        String targetUrl = SERVER_URL + "/upload?path=" + URLEncoder.encode(serverFilePath, StandardCharsets.UTF_8);
        File file = new File(localFilePath);

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
     * 上传本地的文件到服务器的合并方法
     *
     * @param userId         发送图片的user的Id
     * @param userPassword   user的密码
     * @param localImagePath 本地需要上传图片的信息的路径
     * @param ImageName 用户想要指定的上传图片的名字
     * @return true 如果上传成功, 否则 false
     */
    public static boolean upLoadFilesToServer(int userId, String userPassword, String localImagePath,String ImageName){
        String targetPath = userUploadImage(userId, userPassword, localImagePath, ImageName);
        return uploadFile(localImagePath, targetPath);
    }



    /**
     * 用户根据图片名称查询图片存储地址的方法，中间用，隔开
     * @param imageName 用户想要指定的上传图片的名字
     * @return String，所有查询到的图片的存储路径用“,”分割
     */
    public static String userSearchByName(String imageName) {
        try {
            // 构建请求URL，并对图片名称进行URL编码
            URL url = new URL(SERVER_URL + "/userSearchByName?imageName=" + URLEncoder.encode(imageName, StandardCharsets.UTF_8));

            // 打开连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // 获取响应
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 读取响应数据
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // 返回响应
                return response.toString();
            } else {
                System.out.println("Server returned error code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }




    /**
     * 下载单个文件并放到指定目录
     *
     * @param filePath     服务器上的文件路径
     * @param downloadDir  本地保存路径
     * @return true 如果下载成功, 否则 false
     */
    public static boolean downloadSingleFile(String filePath, String downloadDir) {
        String fileURL = SERVER_URL + "/downloadSingle?file=" + URLEncoder.encode(filePath, StandardCharsets.UTF_8);

        try {
            URL url = new URL(fileURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 确定文件在本地保存的路径
                Path outputFile = Paths.get(downloadDir, Paths.get(filePath).getFileName().toString());
                try (InputStream inputStream = httpConn.getInputStream();
                     FileOutputStream outputStream = new FileOutputStream(outputFile.toFile())) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                System.out.println("Success download file to local dir path: " + downloadDir);
                return true;
            } else {
                System.out.println("Failed to download file. Server responded with: " + responseCode);
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }





    /**
     * 根据对应的用户给定的文件名称ImageName不是文件的文件名下载所有同名文件到指定的本地保存文件夹中
     *
     * @param imageName 想下载的文件名
     * @param downloadDir   本地保存路径
     * @return true 如果下载成功, 否则 false
     */
    public static void downloadAccordingImageName(String imageName, String downloadDir){
        String text = userSearchByName(imageName);
        String[] downLoadPaths = text.split(",");
        for (int i = 0; i < downLoadPaths.length; i++) {
            System.out.println("要下载的文件地址是："+ downLoadPaths[i]);
            downloadSingleFile(downLoadPaths[i], downloadDir);
        }
    }


    /**
     * 测试与服务器的连接
     *
     * @return true 如果连接成功, 否则 false
     */
    public static boolean testConnection() {
        String testURL = SERVER_URL + "/testConnection";

        try {
            URL url = new URL(testURL);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");
            int responseCode = httpConn.getResponseCode();

            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }



    /**
     * 测试输入的用户id和password在数据库中有没有对应的数据
     * @param userId: 用户返回的id
     * @param username: 这里使用的是用户的userPassword，变量名写错了不想改了
     * @return true 如果连接成功, 否则 false
     */
    public static boolean userExists(int userId, String username) {
        try {
            String requestURL = SERVER_URL + "/userExists"; // 修改成服务器的URL

            // Create URL connection
            URL url = new URL(requestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            // Write request body
            String requestBody = "userId=" + userId + "&username=" + URLEncoder.encode(username, StandardCharsets.UTF_8);
            OutputStream os = conn.getOutputStream();
            os.write(requestBody.getBytes());
            os.flush();
            os.close();

            // Get response
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String response = reader.readLine();
                reader.close();
                return Boolean.parseBoolean(response);
            } else {
                // Handle error
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }






    /**
     * 请求服务器上的所有图片信息
     *
     * @return 包含所有图片信息的字符串，如果请求失败则返回空字符串
     */
    public static String getAllImagesInfo() {
        String targetUrl = SERVER_URL + "/getAllImagesInfo";

        try {
            URL url = new URL(targetUrl);
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");

            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                in.close();
                return response.toString();
            } else {
                System.out.println("Failed to get images info. Server responded with: " + responseCode);
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }




    /**
     * 根据ImageId获取图片的路径和类型
     *
     * @param imageId 要查询的图片ID
     * @return 包含图片路径和类型的字符串，格式为 "ImagePath: imagePath, ImageType: imageType"，如果查询失败则返回空字符串
     */
    private static String getImagePathAndType(int imageId) {
        String targetUrl = SERVER_URL + "/getImagePathAndType";

        try {
            // 构建请求URL和参数
            URL url = new URL(targetUrl + "?imageId=" + URLEncoder.encode(String.valueOf(imageId), StandardCharsets.UTF_8));

            // 打开连接
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("GET");

            // 获取响应
            int responseCode = httpConn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                in.close();
                return response.toString();
            } else {
                System.out.println("Failed to get image path and type. Server responded with: " + responseCode);
                return "";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }














    /**
     * 输入一个代表服务器我需要下载的文件的ImageId下载对应的文件
     *
     *  @param ImageId 想下载的文件名
     *  @param downloadDir   本地保存路径
     *  @return 在给定的downloadDir下载对应imageId的文件
     */
    public static boolean downloadAccordingImageId(String ImageId, String downloadDir){
        String ImagePathAndImageType = getImagePathAndType(Integer.parseInt(ImageId));
        String[] ImagePathDivided = ImagePathAndImageType.split(" ");
        System.out.println(ImagePathDivided[0]);
        return downloadSingleFile(ImagePathDivided[0], downloadDir);
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
//        System.out.println("User upload image should be stored at: "+ userUploadImage(1,"sensei", "C:\\Users\\admin\\Desktop\\2.png", "Hina"));
//        uploadFile("C:\\Users\\admin\\Desktop\\2.png","src/main/java/com/example/network/Server/Images/5.png");
//
//        upLoadFilesToServer(1, "sensei", "C:\\\\Users\\\\admin\\\\Desktop\\\\2.png", "Hina为师的大可爱！！！！！" );
//        System.out.println(userSearchByName("Hina"));
//        String downloadText = "src/main/java/com/example/network/Server/Images/5.png";
//        downloadSingleFile(downloadText, "src/main/java/org/example/Network/client");
//        downloadAccordingImageName("Hina", "src/main/java/org/example/Network/client");

//        System.out.println("Database connection: " + testConnection());
//        System.out.println("User exists: "+userExists(1, "ensei"));
//        System.out.println("newly registered UserId: "+registerUser("blackSuit", "blackSuit"));
//        System.out.println(getAllImagesInfo());
        //upLoadFilesToServer(1, "sensei", "C:\\Users\\admin\\Desktop\\n.png", "若藻" );
//        downloadAccordingImageId("8", "C:\\Users\\admin\\Desktop");
//        downloadAccordingImageName("", "C:\\\\Users\\\\admin\\\\Desktop");


        /*
        * 第二次测试所有已经存在的需要放在gui中使用的代码，注释的是已经测试完成的方法可以正常使用，请保证服务器结构正确
        *  */

//        System.out.println(registerUser("sensei", "sensei"));
//        System.out.println(userExists(1, "sensei"));
//        upLoadFilesToServer(1, "sensei", "C:\\Users\\admin\\Desktop\\N.png", "若藻大狐狸");
//        System.out.println(testConnection());
//        System.out.println("测试下载方法下载到桌面端：");
//        downloadAccordingImageName("若藻大狐狸", "C:\\Users\\admin\\Desktop");
//        System.out.println(userSearchByName("若藻大狐狸"));
//        downloadAccordingImageName("若藻大狐狸", "C:\\Users\\admin\\Desktop");
//        downloadAccordingImageId("1", "C:\\Users\\admin\\Desktop");
//        System.out.println(registerUser("sensei", "空崎日奈是我老婆"));
//        System.out.println(userExists(3, "sensei"));
//        System.out.println(userExists(3, "空崎日奈是我老婆"));
//        System.out.println(getAllImagesInfo());
        //上面测试的代码包括重置数据库，注册用户，测试用户是否存在，测试用户上传图片,
        //测试用户根据Imagename下载图片,用户根据ImageId下载指定图片,用户返回所有图片信息的"id name"一个字符串中间用逗号隔开







    }





}
