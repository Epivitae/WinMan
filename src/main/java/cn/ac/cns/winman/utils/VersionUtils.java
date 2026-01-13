// Placeholder for VersionUtils.java
package cn.ac.cns.winman.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class VersionUtils {
    
    public static String getVersion(Class<?> cls) {
        try (InputStream s = cls.getResourceAsStream("/version.properties")) {
            if (s == null) return "dev";
            Properties p = new Properties();
            p.load(s);
            return p.getProperty("version", "dev");
        } catch (IOException e) {
            return "err";
        }
    }
}