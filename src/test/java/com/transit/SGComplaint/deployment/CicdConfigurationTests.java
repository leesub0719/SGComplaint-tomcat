package com.transit.SGComplaint.deployment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CicdConfigurationTests {

    private final Path root = Path.of(System.getProperty("user.dir"));

    @Test
    void ciBuildsAndPublishesExternalTomcatWar() throws IOException {
        String workflow = read(".github/workflows/war-ci.yml");
        assertTrue(workflow.contains("clean test bootWar"));
        assertTrue(workflow.contains("WEB-INF/lib-provided/"));
        assertTrue(workflow.contains("actions/upload-artifact@v4"));
        assertTrue(workflow.contains("build/libs/sgcomplaint.war"));
    }

    @Test
    void ciDoesNotDeployOrExposeProductionSecrets() throws IOException {
        String workflow = read(".github/workflows/war-ci.yml");
        assertTrue(workflow.contains("workflow_dispatch:"));
        assertFalse(workflow.contains("systemctl"));
        assertFalse(workflow.contains("DB_PASSWORD"));
        assertFalse(workflow.contains("SOLAPI_API_SECRET"));
    }

    @Test
    void deploymentScriptsBuildBackupDeployAndHealthCheckWar() throws IOException {
        String linux = read("deploy/linux/deploy-tomcat.sh");
        assertTrue(linux.contains("clean test bootWar"));
        assertTrue(linux.contains("build/libs/sgcomplaint.war"));
        assertTrue(linux.contains("backup/tomcat"));
        assertTrue(linux.contains("actuator/health"));
        assertTrue(linux.contains("realpath"));
        assertFalse(linux.contains("eval "));
        assertFalse(linux.contains("source "));

        String windows = read("deploy/windows/deploy-tomcat.ps1");
        assertTrue(windows.contains("clean test bootWar"));
        assertTrue(windows.contains("build\\libs\\sgcomplaint.war"));
        assertTrue(windows.contains("backup\\tomcat"));
        assertTrue(windows.contains("actuator/health"));
        assertTrue(windows.contains("StartsWith($WebAppsFull"));
    }

    @Test
    void smsCredentialsComeOnlyFromEnvironment() throws IOException {
        for (String profile : new String[]{"local", "prod"}) {
            String properties = read("src/main/resources/application-" + profile + ".properties");
            assertTrue(properties.contains("sms.solapi.api-key=${SOLAPI_API_KEY"));
            assertTrue(properties.contains("sms.solapi.api-secret=${SOLAPI_API_SECRET"));
            assertTrue(properties.contains("sms.solapi.sender=${SOLAPI_SENDER"));
        }
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(root.resolve(relativePath));
    }
}
