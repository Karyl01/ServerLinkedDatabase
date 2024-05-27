package org.example.Network.client;

import javax.sound.sampled.*;
import java.io.*;

public class AudioRecorder {

    // 音频文件保存路径
    private static final String FILE_PATH = "C:\\Users\\admin\\Desktop\\ServerLinkedDatabase\\src\\main\\java\\org\\example\\Network\\client\\recorded_audio.wav";

    public static void main(String[] args) {
        // 创建音频格式
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                44100, 16, 2, 4, 44100, false);

        // 创建音频输入
        TargetDataLine line;
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
            System.exit(0);
        }

        try {
            // 打开音频输入
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            // 创建录音文件
            File audioFile = new File(FILE_PATH);

            // 创建线程，实时录音并写入文件
            Thread recordingThread = new Thread(() -> {
                AudioInputStream ais = new AudioInputStream(line);
                try {
                    AudioSystem.write(ais, AudioFileFormat.Type.WAVE, audioFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            // 开始录音
            recordingThread.start();

            // 录制 10 秒钟
            Thread.sleep(10000);

            // 停止录音
            line.stop();
            line.close();
            recordingThread.join();

            System.out.println("Recording completed!");

        } catch (LineUnavailableException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
