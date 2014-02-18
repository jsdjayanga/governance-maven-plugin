package governance.plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.w3c.dom.Document;

/**
 * Parse a pom.xml file and build a Maven project model
 */
public class XmlParser {

	public static Model parsePom(File file){
		Model model = null;
		FileReader reader = null;
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();

		try {
		     reader = new FileReader(file);
		     model = mavenreader.read(reader);
		     model.setPomFile(file);
		}catch(Exception ex){
		     ex.printStackTrace();
		}
		
		return model;
	}
	
	public static Model parsePomFromXmlString(String xmlString){
		Model model = null;
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();

		try {
		     model = mavenreader.read(new StringReader(xmlString));
		}catch(Exception ex){
		     ex.printStackTrace();
		}
		
		return model;
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
	
	public static Document parseXmlFile(File xmlFile) throws MojoExecutionException{
		DocumentBuilderFactory factory;
		DocumentBuilder builder;
		Document doc = null;
		try{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			
			doc = builder.parse(xmlFile);
		}
		catch(Exception e){
			throw new MojoExecutionException(e.getMessage());
		}
		
		return doc;
	}
}
