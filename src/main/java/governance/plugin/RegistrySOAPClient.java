package governance.plugin;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/**
 * SOAP client to communicate with web services exposed by 
 * registry.
 */
public class RegistrySOAPClient {
	
	private static String username;
	private static String password;
	
	public static void setCredentials(String username, String password){
		RegistrySOAPClient.username = username;
		RegistrySOAPClient.password = password;
	}
	
	public static String sendMessage(String serviceUrl, String messageBody)
			throws MojoExecutionException{
		URL url = null;
		URLConnection connection = null;
		HttpURLConnection httpConn = null;
		String responseString = null;
		String response = null;
		ByteArrayOutputStream bout = null;
		OutputStream out = null;
		InputStreamReader isr = null;
		BufferedReader in = null;
		try 
		{
			url = new URL(serviceUrl);
			connection = url.openConnection();
			httpConn = (HttpURLConnection) connection;
			bout = new ByteArrayOutputStream();
			byte[] buffer = new byte[messageBody.length()];
			buffer = messageBody.getBytes();
			bout.write(buffer);
			
			byte[] b = bout.toByteArray();
			String SOAPAction = "";
			// Set the appropriate HTTP parameters.
			httpConn.setRequestProperty("Content-Length", String
					.valueOf(b.length));
			httpConn.setRequestProperty("Content-Type",
					"text/xml; charset=utf-8");
			byte [] authBytes = new String(RegistrySOAPClient.username + ":" + RegistrySOAPClient.password).getBytes();
			String encoded = Base64.encode(authBytes);
			
			httpConn.setRequestProperty("Authorization", "Basic "+encoded);
			httpConn.setRequestProperty("SOAPAction", SOAPAction);
			httpConn.setRequestMethod("GET");
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			
			out = httpConn.getOutputStream();
			out.write(b);
			out.close();
			
			// Read the response and write it to standard out.
			isr = new InputStreamReader(httpConn.getInputStream(), "UTF-8");
			in = new BufferedReader(isr);
			while ((responseString = in.readLine()) != null) 
			{
				response = response + responseString;
			}
			return response;
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getMessage());
		}
	}
	
	/**
	 * Erase leading characters before first '<' and trailing characters after last '>'
	 * in incoming SOAP message
	 */
	public static String stripMessage(String input){
		int startingIndex = input.indexOf('<');
		int lastIndex = input.lastIndexOf('>');
		
		return input.substring(startingIndex, lastIndex + 1);
	}
	
	public static Document parseXmlString(String xmlString) throws MojoExecutionException{
		DocumentBuilderFactory factory;
		DocumentBuilder builder;
		Document doc = null;
		try{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			
			doc = builder.parse(new ByteArrayInputStream(xmlString.getBytes()));
		}
		catch(Exception e){
			throw new MojoExecutionException(e.getMessage());
		}
		
		return doc;
	}
}
