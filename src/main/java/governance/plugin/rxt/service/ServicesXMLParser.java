package governance.plugin.rxt.service;

import governance.plugin.util.PathNameResolver;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
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
 * Created by jayanga on 2/9/14.
 */
public class ServicesXMLParser {
    public static List<Object> parse(File file) throws SAXException, IOException, ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder documentBuilder = null;

        List<Object> serviceInfoList = new LinkedList<Object>();

        try {
            documentBuilder = factory.newDocumentBuilder();
            try {
                Document document = documentBuilder.parse(file);
                NodeList nodeList = document.getElementsByTagName("service");

                for (int index = 0; index < nodeList.getLength(); index++){
                    Node node = nodeList.item(index);
                    if (node != null){
                        Node nameNode = node.getAttributes().getNamedItem("name");
                        Node versionNode = node.getAttributes().getNamedItem("version");
                        Node descriptionNode = node.getAttributes().getNamedItem("description");

                        String name = (nameNode != null && nameNode.getTextContent() != "")? nameNode.getTextContent(): "un-named";
                        String version = (versionNode != null && versionNode.getTextContent() != "")? versionNode.getTextContent(): "1.0.0";
                        String description = (descriptionNode != null && descriptionNode.getTextContent() != "")? descriptionNode.getTextContent(): "";
                        String namespace = getNamespace(node);

                        //String[] serviceInfo = {name, namespace, version, "Axis2", description};
                        Map<String, String> serviceInfo = new HashMap<String, String>();
                        serviceInfo.put("name", name.trim());
                        serviceInfo.put("namespace", namespace.trim());
                        serviceInfo.put("version", version);
                        serviceInfo.put("type", "Axis2");
                        serviceInfo.put("description", description);

                        serviceInfoList.add(serviceInfo);
                    }
                }
            } catch (SAXException e) {
                //System.out.println("Failed to parse the file. SAXException (" + file.getPath() + ")");
                throw new SAXException("Failed to parse the file. SAXException (" + file.getPath() + ")", e);
            } catch (IOException e) {
                //System.out.println("Failed to parse the file. IOException (" + file.getPath() + ")");
                throw new SAXException("Failed to parse the file. IOException (" + file.getPath() + ")", e);
            }
        } catch (ParserConfigurationException e) {
            //System.out.println("Failed to parse the file. ParserConfigurationException (" + file.getPath() + ")");
            throw e;
        }

        return serviceInfoList;
    }

    private static String getNamespace(Node node){

        Node namespaceNode = node.getAttributes().getNamedItem("targetNamespace");
        if (namespaceNode != null && namespaceNode.getTextContent() != ""){
            return namespaceNode.getTextContent();
        }

        NodeList nodeList = node.getChildNodes();
        if (nodeList != null){
            for (int index = 0; index < nodeList.getLength(); index++){
                Node cnode = nodeList.item(index);
                if (cnode != null){
                    if (cnode.getNodeName().equals("parameter")){
                        NamedNodeMap namedNodeMap = cnode.getAttributes();
                        if (namedNodeMap != null){
                            Node nameNode = namedNodeMap.getNamedItem("name");
                            if (nameNode != null && nameNode.getTextContent().equals("ServiceClass")){
                                String serviceClassName = cnode.getTextContent().trim();
                                int dotOffSet = serviceClassName.lastIndexOf('.');
                                if (dotOffSet == -1){
                                    dotOffSet = serviceClassName.length();
                                }
                                String namespace = PathNameResolver.PackageToNamespace(serviceClassName.substring(0, dotOffSet));
                                return namespace;
                            }
                        }
                    }
                }
            }
        }

        return "http://defaultpackage";
    }
}
