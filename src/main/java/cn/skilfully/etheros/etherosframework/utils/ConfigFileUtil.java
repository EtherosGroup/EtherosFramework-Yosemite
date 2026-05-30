package cn.skilfully.etheros.etherosframework.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigFileUtil {

    /**
     * 创建工作目录
     * @param pluginName 插件名
     * @return 插件工作目录，一般是 plugins/Etheros/{pluginName}。当创建失败时返回null
     */
    public static File createWorkDirectory(String pluginName) {
        String workDirectoryPath = "plugins/Etheros/" + pluginName;
        Path path = Paths.get(workDirectoryPath);
        File directory = path.toFile();
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                return null;
            }
        }
        return directory;
    }

    /**
     * 获取工作目录url
     * @param pluginName 插件名
     * @return 工作目录 e.g. plugins/Etheros/EtherosCore/
     */
    public static String getWorkDirectory(String pluginName) {
        return "plugins/Etheros/" + pluginName + "/";
    }

    /**
     * 从Jar包的resources目录提取文件到目标位置
     * @param clazz 插件本体，一般类型为JavaPlugin
     * @param resourcePath 资源文件路径
     * @param toFile 将文件保存到
     * @param overwrite 如果文件已存在是否覆盖
     * @return 文件复制成功后的对象
     * @throws IOException
     */
    public static File extractFileFromJarResources(
            Class<?> clazz,
            String resourcePath,
            File toFile,
            boolean overwrite
    ) throws IOException {
        if (toFile.exists() && !overwrite) {
            return toFile;
        }

        File parentDir = toFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                return null;
            }
        }

        try (InputStream in = clazz.getResourceAsStream("/" + resourcePath);
             OutputStream out = Files.newOutputStream(toFile.toPath())) {

            if (in == null) {
                return null;
            }

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            return toFile;
        }
    }

}
