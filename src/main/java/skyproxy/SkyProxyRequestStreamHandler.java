package skyproxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import config.Config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Acts as a pass through from Lambda endpoint to a web service
 */
public final class SkyProxyRequestStreamHandler implements RequestStreamHandler {
    // replace this with the remote url of your local server and servlet name
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;) {
            int ch = inputStream.read();
            if (ch == -1) break;
            baos.write(ch);
        }
        byte[] inbuf = baos.toByteArray();
        try {
            disableCertificateValidation();
        }
        catch (Exception e) {
        }
        System.out.println(new String(inbuf));
        byte[] output = makeRequest(inbuf);
        outputStream.write(output);
    }
    
    private byte[] makeRequest(byte[] body) 
    		throws IOException {
        Config.loadProperties();
        URL serviceURL = new URL(Config.SERVLET_URL);
        System.out.println(serviceURL.toString());
        HttpsURLConnection con = (HttpsURLConnection) serviceURL.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Accept-Charset", "utf-8");
        con.setRequestProperty("Content-Length", String.valueOf(body.length));
        OutputStream os = con.getOutputStream();
        os.write(body);
        os.close();
        InputStream is = con.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (;;) {
            int ch = is.read();
            if (ch == -1) break;
            baos.write(ch);
        }
        is.close();
        byte[] data = baos.toByteArray();
        return data;
    }
    
    private void disableCertificateValidation() 
    		throws Exception {
    	// create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { 
			new X509TrustManager() {     
				public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
					return new X509Certificate[0];
				} 
				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				} 
				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			} 
		};

		// install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL"); 
		sc.init(null, trustAllCerts, new java.security.SecureRandom()); 
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}
}
