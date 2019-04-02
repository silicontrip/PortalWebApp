package net.silicontrip.ingress;

import java.util.Map;
import java.util.HashMap;

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

    private static final Map<Integer, Integer> zoomDistance = new HashMap<Integer, Integer>() {
    {
        put( 3, 200000);
        put( 4, 200000);
        put( 5, 60000);
        put( 6, 60000);
        put( 7, 10000);
        put( 8, 5000);
        put( 9, 2500);
	put(10, 2500);
	put(11, 800);
	put(12, 300);
	put(13, 0);
	put(14, 0);
	put(15, 0);
	put(16, 0);
	put(17, 0);
	put(18, 0);
	put(19, 0);
	put(20, 0);
	put(21, 0);
	put(22, 0);
	put(23, 0);
	put(24, 0);
    } 
	};
 
    public void onMessage(Message message) {
        TextMessage textMessage = (TextMessage) message;
		String tm = "";
        try {
			tm= textMessage.getText();
		//	System.out.println(tm);
			JSONObject pobj = new JSONObject (textMessage.getText());
			SQLLinkDAO dao = new SQLLinkDAO();
			if (pobj.has("delete"))
			{
				// check zoom 
				Link l = dao.getGuid(pobj.getString("guid"));
				double length = l.getAngle() * 6367000;
				// get link length
				if ( length > zoomDistance.get(pobj.getInt("zoom")))
					dao.delete(pobj.getString("guid"));
			} else if (pobj.has("team")) {
				//System.out.println("" + pobj.getString("image").length() + " / " + pobj.getString("image"));
//{"dLngE6":145176878,"oGuid":"704dbc67aed54b73b153bbb5be3a9fed.16","oLatE6":-37818041,"guid":"e11e884cdb0e438e9c25c6bc0b909b14.9","team":"E","oLngE6":145157441,"dLatE6":-37802085,"dGuid":"5f7ef86ee21f4c83809527b917cad585.16"}
				dao.insert(
					pobj.getString("guid"),
					pobj.getString("dGuid"), pobj.getLong("dLatE6"), pobj.getLong("dLngE6"),
					pobj.getString("oGuid"), pobj.getLong("oLatE6"), pobj.getLong("oLngE6"),
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
