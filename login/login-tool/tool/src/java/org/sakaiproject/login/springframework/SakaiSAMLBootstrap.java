/**
 * Copyright (c) 2003-2026 The Apereo Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://opensource.org/licenses/ecl2
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.login.springframework;

import java.io.InputStream;

import org.apache.xml.security.Init;
import org.opensaml.Configuration;
import org.opensaml.ESAPISecurityConfig;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLConfigurator;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.DefaultSecurityConfigurationBootstrap;
import org.opensaml.xml.security.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory;
import org.owasp.esapi.ESAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.security.saml.SAMLConstants;

/**
 * Initializes OpenSAML without registering its legacy Commons HttpClient 3 HTTPS protocol.
 */
public class SakaiSAMLBootstrap implements BeanFactoryPostProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(SakaiSAMLBootstrap.class);

    private static final String[] XML_TOOLING_CONFIGS = {
        "/default-config.xml",
        "/schema-config.xml",
        "/signature-config.xml",
        "/signature-validation-config.xml",
        "/encryption-config.xml",
        "/encryption-validation-config.xml",
        "/saml2-assertion-config.xml",
        "/saml2-protocol-config.xml",
        "/saml2-core-validation-config.xml",
        "/saml2-metadata-config.xml",
        "/saml2-metadata-validation-config.xml",
        "/saml2-protocol-aslo-config.xml",
    };

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        try {
            bootstrap();
            setMetadataKeyInfoGenerator();
        } catch (ConfigurationException e) {
            throw new FatalBeanException("Error invoking OpenSAML bootstrap", e);
        }
    }

    protected void bootstrap() throws ConfigurationException {
        initializeXMLSecurity();
        initializeXMLTooling(XML_TOOLING_CONFIGS);
        initializeGlobalSecurityConfiguration();
        initializeParserPool();
        initializeESAPI();
    }

    protected void setMetadataKeyInfoGenerator() {
        NamedKeyInfoGeneratorManager manager = Configuration.getGlobalSecurityConfiguration().getKeyInfoGeneratorManager();
        X509KeyInfoGeneratorFactory generator = new X509KeyInfoGeneratorFactory();
        generator.setEmitEntityCertificate(true);
        generator.setEmitEntityCertificateChain(true);
        manager.registerFactory(SAMLConstants.SAML_METADATA_KEY_INFO_GENERATOR, generator);
    }

    private void initializeXMLSecurity() throws ConfigurationException {
        String lineBreakPropName = "org.apache.xml.security.ignoreLineBreaks";
        if (System.getProperty(lineBreakPropName) == null) {
            System.setProperty(lineBreakPropName, "true");
        }
        if (!Init.isInitialized()) {
            LOG.debug("Initializing Apache XMLSecurity library");
            Init.init();
        }
    }

    private void initializeXMLTooling(String[] providerConfigs) throws ConfigurationException {
        XMLConfigurator configurator = new XMLConfigurator();

        for (String config : providerConfigs) {
            LOG.debug("Loading XMLTooling configuration {}", config);
            try (InputStream inputStream = Configuration.class.getResourceAsStream(config)) {
                if (inputStream == null) {
                    throw new ConfigurationException("OpenSAML XMLTooling configuration not found: " + config);
                }
                configurator.load(inputStream);
            } catch (java.io.IOException e) {
                throw new ConfigurationException("Error closing OpenSAML XMLTooling configuration: " + config, e);
            }
        }
    }

    private void initializeGlobalSecurityConfiguration() {
        Configuration.setGlobalSecurityConfiguration(DefaultSecurityConfigurationBootstrap.buildDefaultConfig());
    }

    private void initializeParserPool() throws ConfigurationException {
        SakaiSamlParserPool parserPool = new SakaiSamlParserPool();
        try {
            parserPool.initialize();
        } catch (XMLParserException e) {
            throw new ConfigurationException("Error initializing parser pool", e);
        }
        Configuration.setParserPool(parserPool);
    }

    private void initializeESAPI() {
        String systemPropertyKey = "org.owasp.esapi.SecurityConfiguration";
        String opensamlConfigImpl = ESAPISecurityConfig.class.getName();

        String currentValue = System.getProperty(systemPropertyKey);
        if (currentValue == null || currentValue.isEmpty()) {
            LOG.debug("Setting ESAPI SecurityConfiguration impl to OpenSAML internal class: {}", opensamlConfigImpl);
            System.setProperty(systemPropertyKey, opensamlConfigImpl);
            ESAPI.initialize(opensamlConfigImpl);
        } else {
            LOG.debug("ESAPI SecurityConfiguration impl was already set via system property: {}", currentValue);
        }
    }
}
