package edu.msudenver.cs.jdnss;

import java.util.Properties;
import java.io.InputStream;

// https://maven.apache.org/plugins/maven-resources-plugin/examples/filter.html
// http://stackoverflow.com/questions/3104617/what-is-the-path-to-resource-files-in-a-maven-project

class Version {
    public String getVersion() {
        try {
            try (InputStream in = getClass().getResourceAsStream("/version.properties")) {
                return readVersion(in);
            }
        } catch (java.io.IOException IOE) {
            return "unknown";
        }
    }

    static String readVersion(final InputStream in) throws java.io.IOException {
        if (in == null) {
            return "unknown";
        }

        Properties properties = new Properties();
        properties.load(in);
        return properties.getProperty("version", "unknown");
    }
}
