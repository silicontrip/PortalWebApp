package net.silicontrip.ingress;

import java.util.Map;
import java.util.HashMap;

import javax.jms.*;
import javax.ejb.*;

import org.json.*;
import com.google.common.geometry.*;



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

	@EJB
	private CellSessionBean cellBean;

	public void onMessage(Message message) {
		TextMessage textMessage = (TextMessage) message;
		String tm = "";
		try {
			tm= textMessage.getText();
//		System.out.println("field: " + tm);
			JSONObject pobj = new JSONObject (textMessage.getText());
			SQLMUFieldDAO dao = new SQLMUFieldDAO();
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
					System.out.println("exists: " + guid);
					return;
				}
				
				// check for validity
				// EJB???
				S2Polygon field = cellBean.getS2Polygon(p1.getLong("latE6"),p1.getLong("lngE6"), p2.getLong("latE6"),p2.getLong("lngE6"), p3.getLong("latE6"),p3.getLong("lngE6"));
				if (cellBean.fieldMUValid(field,pobj.getInt("mu")))
	
				// create invalid field. in table.	
				
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
