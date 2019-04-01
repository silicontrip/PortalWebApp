package net.silicontrip.ingress;

import java.util.Enumeration;
import org.json.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import javax.naming.*;
import javax.jms.*;

public class EntityServlet extends HttpServlet {

	private QueueConnectionFactory qcf = null;
	private InitialContext ctx = null;
	private QueueConnection queueCon = null;
	private QueueSession queueSession = null;
	private Queue submitQueue = null;


	public void init () throws ServletException {
		try {
			ctx = new InitialContext();
			qcf = (QueueConnectionFactory) ctx.lookup("jms/QueueConnectionFactory");	
			queueCon = qcf.createQueueConnection();
			queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		} catch (Exception e) {
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

	private int submit (Queue sq, JSONArray ea) throws JMSException
	{
		int count =0;
		if (sq != null)
		{
			QueueSender sender = queueSession.createSender(sq);
			for (Object ent : ea)
			{
				JSONObject jsonEnt = (JSONObject) ent;
				//System.out.println(jsonEnt.toString());
				Message msg = queueSession.createTextMessage(jsonEnt.toString());
				sender.send(msg);
				count++;
			}
		}
		return count;
	}

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/json");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");

	// authentication code
		JSONObject jsonResponse = new JSONObject();
		String userName = req.getParameter("agent");		
		String apiKey = req.getParameter("apikey");		
		try {
			req.login(userName,apiKey);
		} catch (ServletException e) {
			jsonResponse.put("error",  e.getMessage());
			jsonResponse.put("errorType",  e.getClass().getName());
			//jsonResponse.put("agent", userName);
			//jsonResponse.put("apikey", apiKey);

			try {
				resp.setStatus(403);
				PrintWriter writer = resp.getWriter();
				writer.println(jsonResponse.toString());
				writer.close();
			} catch (Exception e2) {
				// :-P
			}
			return;
		}
	// authentication code ends.  move to method? or catch login exception lower down?	

		PrintWriter writer = resp.getWriter();



            // create queue session off the connection

		JSONArray entityArray = null;
		if (req.getParameter("portals") != null)
		{
			jsonResponse.put("portals_submitted", submit ((Queue)ctx.lookup("jms/portalQueue"), new JSONArray(req.getParameter("portals"))));
			jsonResponse.put("portals_deleted", submit ((Queue)ctx.lookup("jms/portalQueue"), new JSONArray(req.getParameter("deleted_portals"))));
		}
		if (req.getParameter("edges") != null)
		{
			jsonResponse.put("edges_submitted",submit((Queue)ctx.lookup("jms/linkQueue"), new JSONArray(req.getParameter("edges"))));
			jsonResponse.put("edges_deleted",submit((Queue)ctx.lookup("jms/linkQueue"), new JSONArray(req.getParameter("deleted_edges"))));
		}
		if (req.getParameter("fields") != null)
		{
			jsonResponse.put("fields_submitted",submit((Queue)ctx.lookup("jms/fieldQueue"),new JSONArray(req.getParameter("fields"))));
		}

		//System.out.println(entityArray);
		
		writer.println(jsonResponse.toString());
		writer.close(); 
	}
	catch (Exception e) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error",  e.getMessage());
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

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

