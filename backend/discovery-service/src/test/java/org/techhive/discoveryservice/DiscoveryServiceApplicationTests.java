package org.techhive.discoveryservice;

import com.netflix.appinfo.ApplicationInfoManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.cloud.netflix.eureka.server.EurekaServerAutoConfiguration",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class DiscoveryServiceApplicationTests {

    @MockBean
    private ApplicationInfoManager applicationInfoManager;

    @Test
    void contextLoads() {
    }

}
