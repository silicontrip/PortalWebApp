package net.silicontrip.ingress;

import java.util.Map;
import java.util.HashMap;

import javax.jms.*;
import javax.ejb.*;

import org.json.*;



@MessageDriven(
mappedName = "jms/fieldQueue",
activationConfig = { 
    @ActivationConfigProperty(
      propertyName = "acknowledgeMode", 
      propertyValue = "Auto-acknowledge"), 
    @ActivationConfigProperty(
      propertyName = "destinationType", 
      propertyValue = "javax.jms.Queue")
})

public class FieldQueueMDB implements MessageListener {

	public void onMessage(Message message) {
		TextMessage textMessage = (TextMessage) message;
		String tm = "";
		try {
			tm= textMessage.getText();
//		System.out.println("field: " + tm);
			JSONObject pobj = new JSONObject (textMessage.getText());
			SQLMUFieldDAO dao = new SQLMUFieldDAO();
//{"apikey": "eae5a26b-1951-45bd-9abd-be41a0167bcc", "creator": "DeadlyErnest", "timestamp": 1501240660772, "agent": "silicontrip", "mu": 3, "ent": ["1419262fcbd64f92b8236b19876b73f0.b", 1501240660772, ["r", "R", [["bda4dd77e0be4bf0978c22b6ea087170.16", -37778790, 145017573], ["fd6c3f43e1fa4fc2abbf9da72d6815e9.16", -37778760, 145018062], ["c8654a70c1ed4a47b898accf98ee6ad1.16", -37779288, 145008148]]]], "_id": {"$oid": "597c4d9dd64e974e3e2fb6d3"}, "data": {"points": [{"lngE6": 145017573, "guid": "bda4dd77e0be4bf0978c22b6ea087170.16", "latE6": -37778790}, {"lngE6": 145018062, "guid": "fd6c3f43e1fa4fc2abbf9da72d6815e9.16", "latE6": -37778760}, {"lngE6": 145008148, "guid": "c8654a70c1ed4a47b898accf98ee6ad1.16", "latE6": -37779288}], "team": "R"}}
//{"area":1.4674022845382035,"creator":"ororamate","options":{"noClip":false,"color":"#0033ff","fillOpacity":0.25,"data":{"team":"E","points":[{"lngE6":145286933,"guid":"268f78fa265a4e2bb06db8eff2270d6a.16","latE6":-37844978},{"lngE6":145266871,"guid":"ae6b36c8271a4443a39fa35b9c7ee94c.16","latE6":-37860118},{"lngE6":145284453,"guid":"331b43227e354933a626bf000cb11145.11","latE6":-37860182}]},"clickable":true,"smoothFactor":1,"weight":5,"lineJoin":null,"ent":["8fb733c6b1a54d4bb1376cbe48eab9cf.b",1554173047045,["r","E",[["268f78fa265a4e2bb06db8eff2270d6a.16",-37844978,145286933],["ae6b36c8271a4443a39fa35b9c7ee94c.16",-37860118,145266871],["331b43227e354933a626bf000cb11145.11",-37860182,145284453]]]],"team":2,"fill":true,"stroke":false,"fillColor":"#03DC03","lineCap":null,"guid":"8fb733c6b1a54d4bb1376cbe48eab9cf.b","dashArray":null,"opacity":0.5,"timestamp":1554173047045},"mu":"1384","guid":"8fb733c6b1a54d4bb1376cbe48eab9cf.b"}
			if (pobj.has("mu")) {

				// unwrap the JSON message

				JSONObject options = pobj.getJSONObject("options");
				JSONArray ent = options.getJSONArray("ent");
				JSONArray entPoints = ent.getJSONArray(2);
				JSONArray points = options.getJSONObject("data").getJSONArray("points");
				JSONObject p1 = points.getJSONObject(0);
				JSONObject p2 = points.getJSONObject(1);
				JSONObject p3 = points.getJSONObject(2);

				String guid = pobj.getString("guid");

				// perform business logic...
				// check for duplicates
				if (dao.exists(guid))
				{
					System.out.println("exists" + guid);
					return;
				}
				
				// check for validity
				// EJB???

				dao.insert(
					pobj.getString("creator"),
					pobj.getString("agent"),
					pobj.getInt("mu"),
					guid,
					options.getInt("timestamp"),
					entPoints.getString(1),
					p1.getString("guid"),p1.getLong("latE6"),p1.getLong("lngE6"),
					p2.getString("guid"),p2.getLong("latE6"),p2.getLong("lngE6"),
					p3.getString("guid"),p3.getLong("latE6"),p3.getLong("lngE6")
				);
			} 
		} catch (JMSException e) {
			System.out.println( "Error while trying to consume messages: " + e.getMessage());
		} catch (Exception e) {
			System.out.println(tm);
			e.printStackTrace();
		}
    }
}
