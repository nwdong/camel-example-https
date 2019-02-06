package dnw.camelhttpsserver;

import java.util.Arrays;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

import org.apache.camel.CamelContext;
import org.apache.camel.component.jetty.JettyHttpComponent;
import org.apache.camel.spring.SpringRouteBuilder;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.SSLContextServerParameters;
import org.apache.camel.util.jsse.SecureSocketProtocolsParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HttpsServerRoute extends SpringRouteBuilder {

	@Autowired
	private CamelContext camelContext;
	
	@Autowired
	private CertProcessor certProcessor;

	@Override
	public void configure() throws Exception {
		configRestAndSSL();

		rest("/dnwtest/{info}").get().to("direct:start");
		
		from("direct:start")
			//uncomment below piece of code to retrieve DN from certificates passed in through request
			//it can be used to implement DN white list function (retrieve DN and check if it's in the white list)
//			.process(certProcessor)   
			.setBody(simple("your input info is ${header.info}"));
		
		log.debug("HttpsServerRoute started");
	}

	protected void configRestAndSSL() throws Exception {
		
		String keystoreFilename = "src/main/resources/dnw-server.jks";
		String keystorePassword = "password";

		// keystore
		KeyStoreParameters ksp = new KeyStoreParameters();
		ksp.setCamelContext(camelContext);
		ksp.setResource(keystoreFilename);
		ksp.setPassword(keystorePassword);

		KeyManagersParameters kmp = new KeyManagersParameters();
		kmp.setKeyPassword(keystorePassword);
		kmp.setKeyStore(ksp);

		SSLContextParameters scp = new SSLContextParameters();
		scp.setKeyManagers(kmp);

		// truststore (here use the same keystore)
		TrustManagersParameters tmp = new TrustManagersParameters();
		tmp.setKeyStore(ksp);
		scp.setTrustManagers(tmp);

		SSLContextServerParameters scsp = new SSLContextServerParameters();
		// if REQUIRE, it means client certificate authentication is required, which turns on mutual authentication
		// if NONE, then no client authentication
		scsp.setClientAuthentication("NONE");  
		scp.setServerParameters(scsp);

		// set allowed protocol list
		List<String> supportedSslProtocols = Arrays.asList("TLSv1.2");
		SecureSocketProtocolsParameters protocolsParameters = new SecureSocketProtocolsParameters();
		protocolsParameters.setSecureSocketProtocol(supportedSslProtocols);
		scp.setSecureSocketProtocols(protocolsParameters);

		// for https
		camelContext.setSSLContextParameters(scp);
		HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
		restConfiguration().component("jetty").host("0.0.0.0").scheme("https").port("8888");

		// for http, comment out above https stuff and uncomment below line if using http
//		restConfiguration().component("jetty").host("0.0.0.0").scheme("http").port("8888");

		JettyHttpComponent restletComponent = camelContext.getComponent("jetty", JettyHttpComponent.class);
		restletComponent.setCamelContext(camelContext);
		restletComponent.setUseGlobalSslContextParameters(true);
	}

}
