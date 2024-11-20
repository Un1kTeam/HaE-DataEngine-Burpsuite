package burp.utils;
import burp.BurpExtender;
import burp.IHttpRequestResponse;
import burp.Proto.Network;
import burp.classes.Config;
import com.google.protobuf.ByteString;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

public class Functions {
    public static void showLog(PrintWriter stdout,String level,String msg){
        switch (level){
            case "info":
                stdout.println("[*] "+msg);
                break;
            case "error":
                stdout.println("[x] "+msg);
                break;
            case "success":
                stdout.println("[+] "+msg);
                break;
        }
    }


    private static final List<String> STATIC_EXTENSIONS = Arrays.asList(
            "mp4", "mp3", "css", "jpg", "jpeg", "png", "gif", "woff", "woff2", "ttf", "ico", "eot", "otf"
    );

    public static void checkAndCreateConfigFile(BurpExtender BurpExtender, String fileName){

        String currentDirectory = System.getProperty("user.dir");
        File iniFile = new File(currentDirectory, fileName);

        // 检查文件是否存在
        if (!iniFile.exists()) {
            try {
                if (iniFile.createNewFile()) {
                    showLog(BurpExtender.stdout,"success",fileName+" does not exist, new file has been created");
                    writeDefaultConfig(BurpExtender, iniFile);
                }
            } catch (IOException e) {
                showLog(BurpExtender.stdout,"error","An error occurred while creating the default configuration");
            }
        } else {
            showLog(BurpExtender.stdout,"info",fileName+" file already exists.");
            showLog(BurpExtender.stdout,"info",fileName+" Path: "+iniFile.getAbsolutePath());
        }
    }

    private static void writeDefaultConfig(BurpExtender BurpExtender,File file) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("[Settings]\n");
            writer.write("NatsServerHost=0.0.0.0\n");
            writer.write("NatsServerPort=4222\n");
            writer.write("debug=true\n");
            showLog(BurpExtender.stdout,"success","The default configuration has been written to the file");
        } catch (IOException e) {
            showLog(BurpExtender.stdout,"error","Error occurred while writing file");
        }
    }



    public static Config parseIniFile(String fileName) throws IOException {
        Map<String, String> configMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";") || line.startsWith("#") || line.startsWith("[")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    configMap.put(key, value);
                }
            }
        }

        String natsServerHost = configMap.get("NatsServerHost");
        String natsServerPort = configMap.get("NatsServerPort");
        String debug = configMap.get("debug");

        return new Config(natsServerHost, natsServerPort, debug);
    }


    public static void chunkSend(IHttpRequestResponse messageInfo, int type,BurpExtender BurpExtender) throws IOException {

        byte[] data;
        UUID uuid = UUID.randomUUID();

        if(messageInfo.getComment().isEmpty()){
            messageInfo.setComment(uuid.toString());
        }

        Network.NetworkData.Builder networkDataBuilder = Network.NetworkData.newBuilder();

        //基础信息设置
        networkDataBuilder.setTraceID(String.valueOf(messageInfo.getComment()));
        networkDataBuilder.setServiceHost(messageInfo.getHttpService().getHost());
        networkDataBuilder.setServicePort(messageInfo.getHttpService().getPort());

        if(type == 1){
            data = messageInfo.getRequest();
        }else{
            data = messageInfo.getResponse();
        }
        int chunkSizeThreshold = 1000 * 1000 ;

        if (data.length > chunkSizeThreshold) {

            //设置标识
            networkDataBuilder.setIsChunked(true);
            int numberOfChunks = (data.length + chunkSizeThreshold - 1) / chunkSizeThreshold;

            for (int i = 0; i < numberOfChunks; i++) {
                int start = i * chunkSizeThreshold;
                int end = Math.min(start + chunkSizeThreshold, data.length);
                byte[] chunk = new byte[end - start];
                System.arraycopy(data, start, chunk, 0, end - start);

                //设置分块号以及数据,进行gzip压缩
                networkDataBuilder.setChunkNum((i + 1));
                networkDataBuilder.setRawData(ByteString.copyFrom(chunk));
                byte[] serializedData =  networkDataBuilder.build().toByteArray();
                BurpExtender.NC.publish("test", compress(serializedData));
                showLog(BurpExtender.stdout,"info","Large Messages Process: Published chunk " + (i + 1) + " of " + numberOfChunks);
            }

        }else{
            networkDataBuilder.setIsChunked(false);
            networkDataBuilder.setChunkNum(-1);
            networkDataBuilder.setRawData(ByteString.copyFrom(data));
            byte[] serializedData =  networkDataBuilder.build().toByteArray();
            BurpExtender.NC.publish("test", compress(serializedData));
        }
    }

    public static Boolean checkExt(String path) {

        if (path == null || path.isEmpty()) {
            return false;
        }

        int lastDotIndex = path.lastIndexOf('.');

        if (lastDotIndex == -1) {
            return false;
        }

        String extension = path.substring(lastDotIndex + 1).toLowerCase();

        return STATIC_EXTENSIONS.contains(extension);
    }


    public static byte[] compress(byte[] data) throws IOException {

        if (data == null || data.length == 0) {
            return new byte[0];
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream)) {
            gzipOutputStream.write(data);
            gzipOutputStream.finish();
            return byteArrayOutputStream.toByteArray();
        }
    }
}
