package net.silicontrip.ingress;
import javax.jms.*;
import javax.ejb.*;

import org.json.*;


@MessageDriven(
mappedName = "jms/portalQueue",
activationConfig = { 
    @ActivationConfigProperty(
      propertyName = "acknowledgeMode", 
      propertyValue = "Auto-acknowledge"), 
    @ActivationConfigProperty(
      propertyName = "destinationType", 
      propertyValue = "javax.jms.Queue")
})

public class PortalQueueMDB implements MessageListener {
 
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
        try {
		JSONObject pobj = new JSONObject (textMessage.getText());
		SQLPortalDAO pdao = new SQLPortalDAO();
		if (pobj.has("title")) 
			pdao.insert(pobj.getString("guid"), pobj.getString("title"), pobj.getLong("latE6"), pobj.getLong("lngE6"));
		else 
			pdao.insert(pobj.getString("guid"), "" , pobj.getLong("latE6"), pobj.getLong("lngE6"));
        } catch (JMSException e) {
            System.out.println(
              "Error while trying to consume messages: " + e.getMessage());
        }
    }
}
