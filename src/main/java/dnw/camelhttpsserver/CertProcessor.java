package dnw.camelhttpsserver;

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CertProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		HttpServletRequest request = exchange.getIn().getBody(HttpServletRequest.class);
		X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		for (int i = 0; i < certs.length; i++) {
			// retrieve DN
			String DN = certs[i].getSubjectDN().getName();
			log.debug("DN="+DN);
		}
	}

}
