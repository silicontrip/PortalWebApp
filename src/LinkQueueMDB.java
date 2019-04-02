package net.silicontrip.ingress;
import javax.jms.*;
import javax.ejb.*;

import org.json.*;


@MessageDriven(
mappedName = "jms/linkQueue",
activationConfig = { 
    @ActivationConfigProperty(
      propertyName = "acknowledgeMode", 
      propertyValue = "Auto-acknowledge"), 
    @ActivationConfigProperty(
      propertyName = "destinationType", 
      propertyValue = "javax.jms.Queue")
})

public class LinkQueueMDB implements MessageListener {
 
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
		String tm = "";
        try {
			tm= textMessage.getText();
			JSONObject pobj = new JSONObject (textMessage.getText());
			SQLLinkDAO dao = new SQLLinkDAO();
			if (pobj.has("delete"))
			{
				// check zoom 
				// get link length
				// if link length > zoom distance
				//	dao.delete(pobj.getString("guid"));
			} else if (pobj.has("team")) {
				//System.out.println("" + pobj.getString("image").length() + " / " + pobj.getString("image"));
				dao.insert(
					pobj.getString("guid"),
					pobj.getString("dguid"), pobj.getLong("dlatE6"), pobj.getLong("dlngE6"),
					pobj.getString("oguid"), pobj.getLong("olatE6"), pobj.getLong("olngE6"),
					pobj.getString("team"));
			} else if (pobj.has("purge")) {
				dao.purge();
			}
        } catch (JMSException e) {
            System.out.println(
              "Error while trying to consume messages: " + e.getMessage());
        } catch (Exception e) {
			System.out.println(tm);
			e.printStackTrace();
		}
    }
}
