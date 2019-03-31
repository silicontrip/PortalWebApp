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
			jsonResponse.put("agent", userName);
			jsonResponse.put("apikey", apiKey);

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
			submitQueue = (Queue)ctx.lookup("jms/portalQueue");
			entityArray = new JSONArray(req.getParameter("portals"));
		}
		if (req.getParameter("edges") != null)
		{
			submitQueue = (Queue)ctx.lookup("jms/linkQueue");
			entityArray = new JSONArray(req.getParameter("edges"));
		}
		if (req.getParameter("fields") != null)
		{
			submitQueue = (Queue)ctx.lookup("jms/fieldQueue");
			entityArray = new JSONArray(req.getParameter("fields"));
		}

		//System.out.println(entityArray);
		
		if (submitQueue != null)
		{
			QueueSender sender = queueSession.createSender(submitQueue);
			for (Object ent : entityArray)
			{
				JSONObject jsonEnt = (JSONObject) ent;
				//System.out.println(jsonEnt.toString());
				Message msg = queueSession.createTextMessage(jsonEnt.toString());
				sender.send(msg);
			}

			jsonResponse.put("status",  "ok");
		}
		else
		{
			jsonResponse.put("status",  "invalidsubmit");
		}
		
		writer.println(jsonResponse.toString());
		writer.close(); 
	}
	catch (Exception e) {
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("error",  e.getMessage());
		try {
			resp.sendError(500,jsonResponse.toString());
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

