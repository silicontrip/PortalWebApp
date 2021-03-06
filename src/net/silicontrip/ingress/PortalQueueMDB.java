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
 
		@EJB
		private	SQLEntityDAO dao;
	
		@Override
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
	String tm = "";
        try {
		tm= textMessage.getText();
		// System.out.println("PortalQUEUE: " + tm);
		JSONObject pobj = new JSONObject (textMessage.getText());
		//SQLEntityDAO dao = new SQLEntityDAO();
		if (pobj.has("delete"))
		{
			dao.deletePortal(pobj.getString("delete"));
		} else {
			if (pobj.has("level"))  // seems like the title "undefined" shows up in some titles
			{
				// grr sometimes these contain nulls.  
				// have to go over each with a fine grade if statement.
				//System.out.println("" + pobj.getString("image").length() + " / " + pobj.getString("image"));
				//Object image = pobj.getObject("image");
				if (!pobj.isNull("image"))
					dao.writePortalFull(pobj.getString("guid"), pobj.getString("title"), pobj.getLong("latE6"), pobj.getLong("lngE6"),pobj.getString("team"),pobj.getInt("level"),pobj.getInt("resCount"),pobj.getInt("health"),pobj.getString("image"));
				else
					dao.writePortalFull(pobj.getString("guid"), pobj.getString("title"), pobj.getLong("latE6"), pobj.getLong("lngE6"),pobj.getString("team"),pobj.getInt("level"),pobj.getInt("resCount"),pobj.getInt("health"),"");	
			}
			else 
				dao.writePortal(pobj.getString("guid"), pobj.getLong("latE6"), pobj.getLong("lngE6"),pobj.getString("team"));
		}
			
        } catch (JMSException e) {
            System.out.println(
              "Error while trying to consume messages: " + e.getMessage());
        } catch (Exception e) {
		System.out.println("PortalQueueMDB::"+tm);
		e.printStackTrace();
	}
		
    }
}
