package governance.plugin.rxt.webapp;

import governance.plugin.util.PathNameResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by jayanga on 2/11/14.
 */
public class WebXMLParser {
    public static List<Object> parse(File file) throws SAXException, IOException, ParserConfigurationException{
        List<Object> serviceInfoList = new LinkedList<Object>();

        Document document = parseFile(file);
        if (document != null){
            NodeList servletNodeList = document.getElementsByTagName("servlet");
            if (servletNodeList != null){

                for (int index = 0; index < servletNodeList.getLength(); index++){
                    Node servletNode = servletNodeList.item(index);
                    if (servletNode != null){
                        NodeList servletChildNodeList = servletNode.getChildNodes();
                        if (servletChildNodeList != null){

                            String servletName = getServletName(servletChildNodeList, "servlet-name");
                            String servletDisplayName = getServletName(servletChildNodeList, "display-name");
                            String servletClassQName = getServletName(servletChildNodeList, "servlet-class");
                            String version = "1.0.0";
                            String description = "";

                            servletName = servletName.trim();
                            servletDisplayName = servletDisplayName.trim();
                            servletClassQName = servletClassQName.trim();

                            int dotOffSet = servletClassQName.lastIndexOf('.') + 1;

                            String namespace = PathNameResolver.PackageToNamespace(servletClassQName.substring(0, dotOffSet));
                            namespace = namespace.toLowerCase();

                            //String serviceClassName = servletClassQName.substring(dotOffSet);

                            Map<String, String> serviceInfo = new HashMap<String, String>();
                            serviceInfo.put("name", servletName.trim());
                            serviceInfo.put("namespace", namespace.trim());
                            serviceInfo.put("serviceclass", servletClassQName.trim());
                            serviceInfo.put("displayname", servletDisplayName.trim());
                            serviceInfo.put("version", version);
                            serviceInfo.put("description", description);

                            serviceInfoList.add(serviceInfo);
                        }
                    }
                }
            }
        }

        return serviceInfoList;
    }

    private static Document parseFile(File file) throws SAXException, IOException, ParserConfigurationException{
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;

        dBuilder = dbFactory.newDocumentBuilder();

        doc = dBuilder.parse(file);
        doc.getDocumentElement().normalize();

        return doc;
    }

    private static String getServletName(NodeList nodeList, String name){

        for (int index = 0; index < nodeList.getLength(); index++){

            Node node = nodeList.item(index);
            if (node != null){
                if (node.getNodeName().equals(name)){
                    return node.getTextContent();
                }
            }
        }

        return "";
    }
}
