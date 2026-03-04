package org.jenkinsci.plugins.githubautostatus;

import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.model.CNode;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;
import org.junit.jupiter.api.Test;

import static io.jenkins.plugins.casc.misc.Util.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@WithJenkins
public class ConfigurationAsCodeTest {

    @Test
    public void should_support_configuration_as_code(JenkinsRule j) throws Exception {
        String yaml = toStringFromYamlFile(this, "configuration-as-code.yml");
        ConfigurationAsCode.get().configure(yaml);

        BuildStatusConfig config = ExtensionList.lookupSingleton(BuildStatusConfig.class);
        assertNotNull(config);

        assertEquals("test-creds", config.getCredentialsId());
        assertEquals(2, config.getDbVersion().intValue());
        assertTrue(config.getEnableGithub());
        assertTrue(config.getEnableHttp());
        assertTrue(config.getEnableInfluxDb());
        assertTrue(config.getEnableStatsd());
        assertEquals("test-http-creds", config.getHttpCredentialsId());
        assertEquals("http://localhost:8088", config.getHttpEndpoint());
        assertTrue(config.getHttpVerifySSL());
        assertTrue(config.getIgnoreSendingTestCoverageToInflux());
        assertTrue(config.getIgnoreSendingTestResultsToInflux());
        assertEquals("jenkins", config.getInfluxDbDatabase());
        assertEquals("http://localhost:8086", config.getInfluxDbUrl());
        assertEquals("bucket", config.getStatsdBucket());
        assertEquals("http://localhost:8087", config.getStatsdHost());
        assertEquals("1400", config.getStatsdMaxSize());
        assertEquals("8125", config.getStatsdPort());
    }

    @Test
    public void should_support_configuration_export(JenkinsRule j) throws Exception {
        String yaml = toStringFromYamlFile(this, "configuration-as-code.yml");
        ConfigurationAsCode.get().configure(yaml);

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        CNode buildStatusConfigAttribute = getUnclassifiedRoot(context).get("buildStatusConfig");

        String exported = toYamlString(buildStatusConfigAttribute);
        String expected = toStringFromYamlFile(this, "configuration-as-code-expected.yml");

        assertThat(exported, is(expected));
    }
}
