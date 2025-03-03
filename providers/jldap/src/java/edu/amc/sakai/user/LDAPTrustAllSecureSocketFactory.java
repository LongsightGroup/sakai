package edu.amc.sakai.user;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.novell.ldap.LDAPSocketFactory;

/**
 * This is a {@link #javax.net.SocketFactory} that trusts any X509 certificate.
 * It currently supports SSL or TLS encryption protocols.
 * 
 * @author Earle Nietzel (earle.nietzel@gmail.com)
 *
 */
public class LDAPTrustAllSecureSocketFactory implements LDAPSocketFactory,
        org.ietf.ldap.LDAPSocketFactory {

	public static final String PROTOCOL_SSL = "SSL";
    public static final String PROTOCOL_TLS = "TLS";
    
    private final Log log = LogFactory.getLog(LDAPTrustAllSecureSocketFactory.class);

    private SocketFactory socketFactory;
    private String protocol;

    public LDAPTrustAllSecureSocketFactory() {
    	this(PROTOCOL_SSL);
	}

    public LDAPTrustAllSecureSocketFactory(String protocol) {
        if (!(PROTOCOL_SSL.equals(protocol) || PROTOCOL_TLS.equals(protocol))) {
            protocol = PROTOCOL_SSL;
        }
        this.protocol = protocol;

        try {
            SSLContext context = SSLContext.getInstance(protocol);
            context.init(null, getAllowAllTrustManager(), new SecureRandom());
            socketFactory = context.getSocketFactory();
        } catch (Exception e) {
            log.error("LDAP SSL excception creating socket factory: " + e.getMessage(), e);
        }
    }

    @Override
    public Socket createSocket(String host, int port) 
            throws IOException, UnknownHostException {
        
        return socketFactory.createSocket(host, port);
    }
    
    private TrustManager[] getAllowAllTrustManager() {
        return new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                // Allow all
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                // Allow all
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                // Not needed
                return null;
            }
         } };
    }

    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
