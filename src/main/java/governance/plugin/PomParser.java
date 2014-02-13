package governance.plugin;

import java.io.File;
import java.io.FileReader;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

/**
 * Parse a pom.xml file and build a Maven project model
 */
public class PomParser {

	public static Model parse(File file){
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
}
