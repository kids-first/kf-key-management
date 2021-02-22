package io.kidsfirst.config;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpConnectorConfig {
    @Value("${server.port}")
    private int httpsPort;

    @Value("${server.http.port}")
    private int httpPort;

    @Value("${server.http.force-ssl}")
    private boolean forceSSL;

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat =
                forceSSL ? new TomcatServletWebServerFactory() {
                                @Override
                                protected void postProcessContext(Context context) {
                                    SecurityConstraint securityConstraint = new SecurityConstraint();
                                    securityConstraint.setUserConstraint("CONFIDENTIAL");
                                    SecurityCollection collection = new SecurityCollection();
                                    collection.addPattern("/*");
                                    securityConstraint.addCollection(collection);
                                    context.addConstraint(securityConstraint);
                                }
                            } :
                            new TomcatServletWebServerFactory();

        tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        return tomcat;
    }

    private Connector createHttpConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        try {
            connector.setScheme("http");
            connector.setSecure(false);
            connector.setPort(httpPort);
            protocol.setSSLEnabled(false);
            connector.setRedirectPort(httpsPort);

            return connector;
        } catch (Exception ex) {
            throw new IllegalStateException("Fail to create http connector", ex);
        }
    }

}
