package governance.plugin.license;

import governance.plugin.util.CommandExecuter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public abstract class AbstractLicenseDetailReader {

	private String outputFile;
	private String perPomcommand;
	private String fromRootPomCommand;
	Map<String, String> information = new HashMap<String, String>();
	Log logger;
	
	public AbstractLicenseDetailReader(String outputFileName, String perPomcommand, String fromRootPomCommand, Log logger){
		this.logger = logger;
		this.outputFile = outputFileName;
		this.perPomcommand = perPomcommand;
		this.fromRootPomCommand = fromRootPomCommand;
	}
	
	public void executePerPomCommand(File target) throws MojoExecutionException{
		logger.debug("Executing " + perPomcommand + " on " + target.getAbsolutePath());
		CommandExecuter.executeCommand(perPomcommand, target, false);
	}
	
	public void executeFromRootPomCommand(File root) throws MojoExecutionException{
		logger.debug("Executing " + fromRootPomCommand + " on root pom " +root.getAbsolutePath());
		CommandExecuter.executeCommand(fromRootPomCommand, root, false);
	}
	
	public Map<String, String> getInformation(MavenProject project) throws IOException{
		information.clear();
		readInformationFromFile(project);
		return this.information;
	}
	
	public void readInformationFromFile(MavenProject project) throws IOException{
		BufferedReader inputBuffer;
    	File outputFile = new File(project.getBasedir().getAbsolutePath() + File.separatorChar + this.outputFile);
        inputBuffer = new BufferedReader(new FileReader(outputFile));
        while (inputBuffer.ready()) {
  		  String inputLine = inputBuffer.readLine();
  		  decodeAndPopulateInfo(inputLine);
  		}
  		inputBuffer.close();
  		outputFile.delete();
	}
	
	public void deleteOutputFile(MavenProject project){
		File outputFile = new File(project.getBasedir().getAbsolutePath() + File.separatorChar + this.outputFile);
		if (outputFile.exists()){
			outputFile.delete();
		}
	}
	
	public abstract void decodeAndPopulateInfo(String informationLine);
    
}
