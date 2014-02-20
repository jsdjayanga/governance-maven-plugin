package governance.plugin.webapp;

import governance.plugin.GovernanceSOAPMessageCreator;

/**
 * Created by jayanga on 2/11/14.
 */
public class WebAppGovernanceSOAPMessageCreator {

    public static final int TIME_STRING_LENGTH = 20;

    public static String  createAddWebAppRequest(String name, String namespace, String serviceclass, String displayname, String version, String description){
        StringBuffer soapRequest = new StringBuffer();
        soapRequest.append("<soapenv:Envelope xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/' ");
        soapRequest.append("xmlns:ser='http://services.add.webapp.governance.carbon.wso2.org'>");
        soapRequest.append("<soapenv:Header/>");
        soapRequest.append("<soapenv:Body>");
        soapRequest.append("<ser:addWebapp>");
        soapRequest.append("<ser:info><![CDATA[<metadata xmlns='http://www.wso2.org/governance/metadata'><overview><name>");
        soapRequest.append(name);
        soapRequest.append("</name>");
        soapRequest.append("<namespace>");
        soapRequest.append(namespace);
        soapRequest.append("</namespace>");
        soapRequest.append("<serviceclass>");
        soapRequest.append(serviceclass);
        soapRequest.append("</serviceclass>");
        soapRequest.append("<displayname>");
        soapRequest.append(displayname);
        soapRequest.append("</displayname>");
        soapRequest.append("<version>");
        soapRequest.append(version);
        soapRequest.append("</version>");
        soapRequest.append("<description>");
        soapRequest.append(description);
        soapRequest.append("</description>");
        soapRequest.append(WebAppGovernanceSOAPMessageCreator.getMetaDataTags());
        soapRequest.append("</overview>");
        soapRequest.append(WebAppGovernanceSOAPMessageCreator.getImageTags());
        soapRequest.append("</metadata>]]></ser:info>");
        soapRequest.append("</ser:addWebapp>");
        soapRequest.append("</soapenv:Body>");
        soapRequest.append("</soapenv:Envelope>");
        return soapRequest.toString();
    }

    public static String  getCurrentTime(){
        long unformatedTime = System.currentTimeMillis();
        String time  = String.valueOf(unformatedTime);

        StringBuffer leadingZeros = new StringBuffer();
        for (int i = (TIME_STRING_LENGTH - time.length()); i == 0; i--){
            leadingZeros.append("0");
        }

        return (leadingZeros.toString() + time);
    }

    public static String getMetaDataTags(){
        StringBuffer metadataTags = new StringBuffer();
        metadataTags.append("<createdtime>");
        metadataTags.append(GovernanceSOAPMessageCreator.getCurrentTime());
        metadataTags.append("</createdtime>");
        metadataTags.append("<provider>governance-maven-plugin</provider>");
        return metadataTags.toString();
    }

    private static String getImageTags() {
        StringBuffer imageTags = new StringBuffer();
        imageTags.append("<images>");
        imageTags.append("<thumbnail>/publisher/config/defaults/img/thumbnail.jpg</thumbnail>");
        imageTags.append("<banner>/publisher/config/defaults/img/banner.jpg</banner>");
        imageTags.append("</images>");
        return imageTags.toString();
    }
}
