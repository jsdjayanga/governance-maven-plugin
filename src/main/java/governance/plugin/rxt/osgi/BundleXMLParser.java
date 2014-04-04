package governance.plugin.rxt.osgi;

import com.google.inject.internal.util.$SourceProvider;
import governance.plugin.util.PathNameResolver;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
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
import java.util.*;

/**
 * Created by jayanga on 2/9/14.
 */
public class BundleXMLParser {
    public static List<Object> parse(File file) throws SAXException, IOException, ParserConfigurationException {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = null;

        List<Object> osgiServiceInfoList = new LinkedList<Object>();

        documentBuilder = factory.newDocumentBuilder();
        Document document = documentBuilder.parse(file);

        NodeList nodeList = document.getElementsByTagName("scr:component");

        for (int index = 0; index < nodeList.getLength(); index++){
            Node node = nodeList.item(index);
            if (node != null){
                Node nameNode = node.getAttributes().getNamedItem("name");

                String className = "";
                List<Map<String, String>> references = new ArrayList<Map<String, String>>();

                NodeList componentChildList = node.getChildNodes();
                for (int childIndex = 0; childIndex < componentChildList.getLength(); childIndex++){
                    Node componentChildNode = componentChildList.item(childIndex);
                    if (componentChildNode != null){
                        if (componentChildNode.getNodeName().equalsIgnoreCase("implementation")){

                            Node classNode = componentChildNode.getAttributes().getNamedItem("class");
                            if (classNode != null){
                                className = classNode.getTextContent();
                            }
                        }else if (componentChildNode.getNodeName().equalsIgnoreCase("property")){
                            // TODO - what are the properties needed.
                        }else if (componentChildNode.getNodeName().equalsIgnoreCase("reference")){

                            String referenceName = "";
                            String referenceInterface = "";

                            Map<String, String> refMap = new HashMap<String, String>();

                            Node referenceNameNode = componentChildNode.getAttributes().getNamedItem("name");
                            if (referenceNameNode != null){
                                referenceName = referenceNameNode.getTextContent();
                                refMap.put("name", referenceName);
                            }

                            Node referenceInterfaceNode = componentChildNode.getAttributes().getNamedItem("interface");
                            if (referenceInterfaceNode != null){
                                referenceInterface = referenceInterfaceNode.getTextContent();
                                refMap.put("interface", referenceInterface);
                            }

                            references.add(refMap);
                        }
                    }
                }

                Map<String, Object> osgiServiceInfo = new HashMap<String, Object>();
                osgiServiceInfo.put("className", className.trim());
                osgiServiceInfo.put("references", references);

                osgiServiceInfoList.add(osgiServiceInfo);
            }
        }

        return osgiServiceInfoList;
    }

    /*
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
    */
}
