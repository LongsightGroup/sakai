package org.sakaiproject.login.springframework;

import org.opensaml.Configuration;
import org.opensaml.xml.security.BasicSecurityConfiguration;
import org.opensaml.xml.signature.SignatureConstants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public final class CustomADFSSAMLBootstrap extends SakaiSAMLBootstrap {
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);
        BasicSecurityConfiguration config = (BasicSecurityConfiguration) Configuration.getGlobalSecurityConfiguration();
        config.registerSignatureAlgorithmURI("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        config.setSignatureReferenceDigestMethod(SignatureConstants.ALGO_ID_DIGEST_SHA256);
    }
}
