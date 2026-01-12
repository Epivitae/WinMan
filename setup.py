import os

# ================= 配置区域 =================
PROJECT_NAME = "WinMan"
GROUP_ID = "cn.ac.cns" 
ARTIFACT_ID = "WinMan"
VERSION = "0.1.0-SNAPSHOT"

# 个人信息
AUTHOR_NAME = "Kui Wang"
AUTHOR_EMAIL = "k@cns.ac.cn"
ORG_URL = "http://www.cns.ac.cn"
GITHUB_USER = "kuiwang" 

PACKAGE_DIR = os.path.join(*GROUP_ID.split("."), ARTIFACT_ID.lower()) 
JAVA_PACKAGE = f"{GROUP_ID}.{ARTIFACT_ID.lower()}" 

MAIN_CLASS = "WinMan"

# 基础路径
BASE_SRC = "src/main/java"
BASE_RES = "src/main/resources"
BASE_TEST = "src/test/java"

FULL_SRC_PATH = os.path.join(BASE_SRC, PACKAGE_DIR)
FULL_TEST_PATH = os.path.join(BASE_TEST, PACKAGE_DIR)

# ================= 文件内容模版 =================

# 1. POM.xml (🔥修复：依赖改为 net.imagej:ij，并强制 UTF-8)
POM_CONTENT = f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
    http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.scijava</groupId>
        <artifactId>pom-scijava</artifactId>
        <version>31.1.0</version>
        <relativePath />
    </parent>

    <groupId>{GROUP_ID}</groupId>
    <artifactId>{ARTIFACT_ID}</artifactId>
    <version>{VERSION}</version>

    <name>{PROJECT_NAME}</name>
    <description>A window manager utility for ImageJ.</description>
    <url>{ORG_URL}</url>
    <inceptionYear>2026</inceptionYear>

    <organization>
        <name>CNS</name>
        <url>{ORG_URL}</url>
    </organization>

    <licenses>
        <license>
            <name>Simplified BSD License</name>
            <url>https://opensource.org/licenses/BSD-2-Clause</url>
            <distribution>repo</distribution>
        </license>
    </licenses>

    <developers>
        <developer>
            <id>kuiwang</id>
            <name>{AUTHOR_NAME}</name>
            <email>{AUTHOR_EMAIL}</email>
            <url>{ORG_URL}</url>
            <roles>
                <role>maintainer</role>
                <role>developer</role>
            </roles>
            <timezone>+8</timezone>
        </developer>
    </developers>

    <contributors>
        <contributor><name>None</name></contributor>
    </contributors>

    <mailingLists>
        <mailingList>
            <name>Image.sc Forum</name>
            <archive>https://forum.image.sc/</archive>
        </mailingList>
    </mailingLists>

    <scm>
        <connection>scm:git:git://github.com/{GITHUB_USER}/{ARTIFACT_ID}</connection>
        <developerConnection>scm:git:git@github.com:{GITHUB_USER}/{ARTIFACT_ID}</developerConnection>
        <tag>HEAD</tag>
        <url>https://github.com/{GITHUB_USER}/{ARTIFACT_ID}</url>
    </scm>

    <issueManagement>
        <system>GitHub Issues</system>
        <url>https://github.com/{GITHUB_USER}/{ARTIFACT_ID}/issues</url>
    </issueManagement>

    <ciManagement>
        <system>GitHub Actions</system>
        <url>https://github.com/{GITHUB_USER}/{ARTIFACT_ID}/actions</url>
    </ciManagement>

    <properties>
        <package-name>{JAVA_PACKAGE}</package-name>
        <main-class>{JAVA_PACKAGE}.{MAIN_CLASS}</main-class>
        <license.licenseName>bsd_2</license.licenseName>
        <license.copyrightOwners>Kui Wang</license.copyrightOwners>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <repositories>
        <repository>
            <id>scijava.public</id>
            <url>https://maven.scijava.org/content/groups/public</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>net.imagej</groupId>
            <artifactId>ij</artifactId>
        </dependency>
    </dependencies>

    <build>
        <resources>
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
            </resource>
        </resources>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <encoding>UTF-8</encoding>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"""

# 2. version.properties
VERSION_PROP_CONTENT = """# Auto-injected by Maven
version=${project.version}
buildDate=${maven.build.timestamp}
author=${project.developers[0].name}
"""

# 3. plugins.config
CONFIG_CONTENT = f"""# Name: {PROJECT_NAME}
# Author: {AUTHOR_NAME}
# Date: 2026

# Menu path > Sub Menu, "Label", Class Name
Plugins > Biosensor Tool, "{PROJECT_NAME} Manager", {JAVA_PACKAGE}.{MAIN_CLASS}
"""

# 4. WinMan.java
JAVA_CLASS_CONTENT = f"""package {JAVA_PACKAGE};

import ij.IJ;
import ij.plugin.PlugIn;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * {PROJECT_NAME} - Main Entry Point
 * @author {AUTHOR_NAME} ({AUTHOR_EMAIL})
 */
public class {MAIN_CLASS} implements PlugIn {{

    @Override
    public void run(String arg) {{
        String version = getVersion();
        IJ.log("Starting {PROJECT_NAME} v" + version + " by {AUTHOR_NAME}...");
        // TODO: Show GUI here
    }}

    private String getVersion() {{
        String path = "/version.properties";
        try (InputStream stream = getClass().getResourceAsStream(path)) {{
            if (stream == null) return "UNKNOWN";
            Properties props = new Properties();
            props.load(stream);
            return props.getProperty("version", "UNKNOWN");
        }} catch (IOException e) {{
            return "ERROR";
        }}
    }}
}}
"""

# 5. Debug Wrapper
DEBUG_CLASS_CONTENT = f"""package {JAVA_PACKAGE};

import ij.IJ;
import ij.ImageJ;

public class {MAIN_CLASS}_Debug {{
    public static void main(final String... args) throws Exception {{
        new ImageJ();
        IJ.open("http://imagej.net/images/clown.jpg");
        IJ.runPlugIn({MAIN_CLASS}.class.getName(), "");
    }}
}}
"""

# 6. gitignore
GITIGNORE_CONTENT = """target/
.vscode/
*.class
*.jar
.DS_Store
.idea/
*.iml
"""

# ================= 执行构建 =================

def create_file(path, content):
    directory = os.path.dirname(path)
    if directory:
        os.makedirs(directory, exist_ok=True)
        
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"✅ Created: {path}")

def main():
    print(f"🚀 Initializing {PROJECT_NAME} for {AUTHOR_NAME}...")
    
    # 创建文件
    create_file("pom.xml", POM_CONTENT)
    create_file(".gitignore", GITIGNORE_CONTENT)
    create_file(os.path.join(BASE_RES, "version.properties"), VERSION_PROP_CONTENT)
    create_file(os.path.join(BASE_RES, "plugins.config"), CONFIG_CONTENT)
    create_file(os.path.join(FULL_SRC_PATH, f"{MAIN_CLASS}.java"), JAVA_CLASS_CONTENT)
    create_file(os.path.join(FULL_TEST_PATH, f"{MAIN_CLASS}_Debug.java"), DEBUG_CLASS_CONTENT)

    print("\n🎉 Setup Complete!")
    print("👉 Please run 'mvnd clean install' again.")

if __name__ == "__main__":
    main()