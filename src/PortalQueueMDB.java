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
//		System.out.println(textMessage.getText());
		JSONObject pobj = new JSONObject (textMessage.getText());
		SQLPortalDAO pdao = new SQLPortalDAO();
		if (pobj.has("title")) 
		{
			System.out.println("" + pobj.getString("image").length() + " / " + pobj.getString("image"));
			pdao.writeFull(pobj.getString("guid"), pobj.getString("title"), pobj.getLong("latE6"), pobj.getLong("lngE6"),pobj.getString("team"),pobj.getInt("level"),pobj.getInt("resCount"),pobj.getInt("health"),pobj.getString("image"));
		}
		else 
			pdao.write(pobj.getString("guid"), pobj.getLong("latE6"), pobj.getLong("lngE6"),pobj.getString("team"));
			
        } catch (JMSException e) {
            System.out.println(
              "Error while trying to consume messages: " + e.getMessage());
        }
    }
}
