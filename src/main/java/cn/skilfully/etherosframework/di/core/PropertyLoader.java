package cn.skilfully.etherosframework.di.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {

    private static final String APPLICATION_PROPERTIES = "application.properties";
    private final Properties properties = new Properties();
    private boolean loaded;

    public PropertyLoader() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public PropertyLoader(ClassLoader classLoader) {
        try (InputStream in = classLoader.getResourceAsStream(APPLICATION_PROPERTIES)) {
            if (in != null) {
                properties.load(in);
                loaded = true;
            }
        } catch (IOException ignored) {
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public boolean isLoaded() {
        return loaded;
    }
}
