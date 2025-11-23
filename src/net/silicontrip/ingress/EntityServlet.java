package net.silicontrip.ingress;

import java.util.Enumeration;
import org.json.*;

import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;
import java.util.Collection;
import java.util.HashSet;
import javax.naming.*;
import jakarta.jms.*;

import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityServlet extends HttpServlet {

	private QueueConnectionFactory qcf = null;
	private InitialContext ctx = null;
	private QueueConnection queueCon = null;
	private QueueSession queueSession = null;
	// private Queue submitQueue = null;

	public void init () throws ServletException {
		try {
			ctx = new InitialContext();
			qcf = (QueueConnectionFactory) ctx.lookup("jms/QueueConnectionFactory");
			queueCon = qcf.createQueueConnection();
			queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (Exception e) {
			Logger.getLogger(EntityServlet.class.getName()).log(Level.SEVERE, null, e);
			throw new ServletException(e.getMessage());
		}
	}

	public void destroy () {
		try {
			queueCon.close();
			queueSession.close();
		} catch (Exception e) {
		// not much we can do here as we can't throw anything.
			e.printStackTrace();
		}
	}

	private void insertAgent (JSONArray ea,String userName)
	{
		for (Object ent : ea)
			((JSONObject) ent).put("agent",userName);
	}
	
	private void insertBounds (JSONArray ea,JSONObject b)
	{
		for (Object ent : ea)
			((JSONObject) ent).put("bounds",b);
	}
	
	private int submit (Queue sq, JSONArray ea) throws JMSException
	{
		int count =0;
		if (sq != null)
		{
		//	HashSet<String> submittedGuid = new HashSet<>();
			QueueSender sender = queueSession.createSender(sq);
			for (Object ent : ea)
			{
				try {
				JSONObject jsonEnt = (JSONObject) ent;
				String guid = null;
				if (jsonEnt.has("guid"))
						guid = jsonEnt.getString("guid");
				// We seem to get duplicates in the single submission.
				// psuedo portals and real portals are added to the hz_portals array
				// as the psuedo is always first we never get the real portal.
					//if (guid==null || !submittedGuid.contains(guid))
					//{
						Message msg = queueSession.createTextMessage(jsonEnt.toString());
						sender.send(msg);
						count++;
						//if (guid!=null)
							//submittedGuid.add(guid);
					//}
				} catch (Exception e) {
					Logger.getLogger(EntityServlet.class.getName()).log(Level.SEVERE, null, e);
					System.out.println(e.getMessage() + ": " + ((JSONObject)ent));
				}
			}
			sender.close();
		}
		return count;
	}
		

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp){
	
		String userName = req.getParameter("agent");
		String apiKey = req.getParameter("apikey");
		if (apiKey!=null) apiKey = apiKey.toLowerCase();
		
		try {
			resp.setContentType("text/json");
			resp.setCharacterEncoding("UTF-8");
			resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");

			JSONObject jsonResponse = new JSONObject();
			// authentication code
			req.login(userName,apiKey);
			// authentication code ends.  move to method? or catch login exception lower down?

			PrintWriter writer = resp.getWriter();

			// create queue session off the connection

			JSONArray entityArray = null;
			if (req.getParameter("portals") != null)
			{
				Queue submitQueue = (Queue)ctx.lookup("jms/portalQueue");
				//System.out.println("Submit Portals");
				//System.out.println(req.getParameter("portals"));
				jsonResponse.put("portals_deleted", submit (submitQueue, new JSONArray(req.getParameter("portals_deleted"))));
				jsonResponse.put("portals_submitted", submit (submitQueue, new JSONArray(req.getParameter("portals"))));

			}
			if (req.getParameter("edges") != null)
			{
				//System.out.println("Submit Edges");
				Queue submitQueue = (Queue)ctx.lookup("jms/linkQueue");
				// due to links being destroyed and recreated with new GUIDs, old links must be deleted before new ones added.
				// as the old links will cause DB constraint failures.
				System.out.println("bounds: " + req.getParameter("bounds"));
				JSONObject bounds = new JSONObject(req.getParameter("bounds"));
				//System.out.println("edges bounds: " + bounds.toString());
				
				JSONArray ed = new JSONArray(req.getParameter("edges_deleted"));
				JSONArray ea =new JSONArray(req.getParameter("edges"));

				insertBounds(ea,bounds);
				jsonResponse.put("edges_deleted",submit(submitQueue, ed));
				jsonResponse.put("edges_submitted",submit(submitQueue, ea));
			}
			if (req.getParameter("fields") != null)
			{
				// want to put the agent name into the field here.
				JSONArray fa = new JSONArray(req.getParameter("fields"));
				insertAgent(fa,userName);
				jsonResponse.put("fields_submitted",submit((Queue)ctx.lookup("jms/fieldQueue"),fa));
			}
			if (req.getParameter("refields") != null)
			{
				JSONArray fa = new JSONArray(req.getParameter("refields"));
				insertAgent(fa,userName);
				for (Object ent : fa)
					((JSONObject) ent).put("refield",true);
				jsonResponse.put("fields_resubmitted",submit((Queue)ctx.lookup("jms/fieldQueue"),fa));
			}
			

			//System.out.println(jsonResponse.toString());

			writer.println(jsonResponse.toString());
			writer.close();
		} catch (ServletException e) {
			Logger.getLogger(EntityServlet.class.getName()).log(Level.SEVERE, null, e);

			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error",  e.getMessage());
			jsonResponse.put("errorType",  e.getClass().getName());
			jsonResponse.put("agent", userName);
			jsonResponse.put("apikey", apiKey);

			try {
				resp.setStatus(401);
				PrintWriter writer = resp.getWriter();
				writer.println(jsonResponse.toString());
				writer.close();
			} catch (IOException e2) {
				// :-P
			}
		} catch (Exception e) {
			Logger.getLogger(EntityServlet.class.getName()).log(Level.SEVERE, null, e);

			JSONObject jsonResponse = new JSONObject();
			jsonResponse.put("error",  e.getMessage());
			e.printStackTrace();
			try {
				resp.setStatus(500);
				PrintWriter writer = resp.getWriter();
				writer.println(jsonResponse.toString());
				writer.close();
			} catch (Exception e2) {
				// sending the error caused an error.
				// is there anything else we can do?
				;
			}
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		doPost(req,resp);
	}
}
