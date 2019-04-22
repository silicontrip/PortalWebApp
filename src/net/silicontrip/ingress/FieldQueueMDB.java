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
	private FieldSessionBean fieldBean;

        @Override
	public void onMessage(Message message) {
		TextMessage textMessage = (TextMessage) message;
		String tm = "";
		try {
			tm= textMessage.getText();
			System.out.println("FieldQueue: " + tm);
			JSONObject pobj = new JSONObject (textMessage.getText());
//			SQLMUFieldDAO dao = new SQLMUFieldDAO();
			if (pobj.has("mu")) {
			//System.out.println("field has mu");

				// unwrap the JSON message

				JSONObject options = pobj.getJSONObject("options");
				JSONArray ent = options.getJSONArray("ent");
				JSONArray entPoints = ent.getJSONArray(2);
				JSONArray points = options.getJSONObject("data").getJSONArray("points");
				JSONObject p1 = points.getJSONObject(0);
				JSONObject p2 = points.getJSONObject(1);
				JSONObject p3 = points.getJSONObject(2);

				String guid = pobj.getString("guid");
				
				// PLUGIN isn't sending arrays yet.
				//JSONArray mu =  new JSONArray();
				//mu.put(pobj.getInt("mu"));
				// or is it?
				JSONArray mu = pobj.getJSONArray("mu");

				// perform business logic...
				// check for duplicates

				//System.out.println("Check for duplicates");
				
				if (fieldBean.hasFieldGuid(guid))
				{
					//System.out.println("field exists: " + guid);
					return;
				} 
				
				//	System.out.println("new Field: " + guid);

				Field fi = new Field (
					pobj.getString("creator"),
					pobj.getString("agent"),
					-1, // place holder
					guid,
					options.getInt("timestamp"),
					entPoints.getString(1),
					p1.getString("guid"),p1.getLong("latE6"),p1.getLong("lngE6"),
					p2.getString("guid"),p2.getLong("latE6"),p2.getLong("lngE6"),
					p3.getString("guid"),p3.getLong("latE6"),p3.getLong("lngE6")
				);
				
				// check for validity
				// EJB???
				S2Polygon S2Field = fi.getS2Polygon();

				boolean[] valid = new boolean[mu.length()];
				
				for (int i =0; i < mu.length(); i++)
					valid[i] = fieldBean.muFieldValid(S2Field,mu.getInt(i));

			//what to do if different MU are valid?
			// which one is more accurate?
			// really need split field handler.
			// problem is we don't know the matching split field.

			// some business logic, while I work out the code;
			// valid[0], valid[1]
			// false, false  -> don't know which to submit, safest is not to submit
			// false, true -> submit 1
			// true, false -> submit 0
			// true, true -> don't know
			// true -> submit 0
			// false -> submit 0

		//System.out.println ("" + guid + " valid: " + valid);


			// we don't want to actually submit a field twice.
				for (int i =0; i < mu.length(); i++)
				{
					//System.out.println ("submit field");
					fi.setMU(mu.getInt(i));
					if (mu.length() == 1 || (valid[i] && (valid[0] ^ valid[1]))) // above business logic decribed in 1 if statement
						fieldBean.submitField(fi,valid[i]);
					else
						if (mu.length()==2)
							System.out.println ("not Submitting field: " + guid + "MU: [" + mu.getLong(0) +", "+mu.getLong(1));
						
				}

				
			} 
		} catch (JMSException e) {
			System.out.println( "Error while trying to consume messages: " + e.getMessage());
		} catch (Exception e) {
			System.out.println(tm);
			e.printStackTrace();
		}
	}
}
