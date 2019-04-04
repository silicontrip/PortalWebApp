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

	private int submit (Queue sq, JSONArray ea,String userName) throws JMSException
	{
		int count =0;
		if (sq != null)
		{
			QueueSender sender = queueSession.createSender(sq);
			for (Object ent : ea)
			{
				JSONObject jsonEnt = (JSONObject) ent;
				jsonEnt.put("agent",userName); // I'd like to know who's submitting fields (and maybe portals)
				//System.out.println(jsonEnt.toString());
				Message msg = queueSession.createTextMessage(jsonEnt.toString());
				sender.send(msg);
				count++;
			}
			sender.close();
		}
		return count;
	}

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/json");
		resp.setCharacterEncoding("UTF-8");
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
			jsonResponse.put("portals_submitted", submit (submitQueue, new JSONArray(req.getParameter("portals")),userName));
			jsonResponse.put("portals_deleted", submit (submitQueue, new JSONArray(req.getParameter("portals_deleted")),userName));
		}
		if (req.getParameter("edges") != null)
		{
			//System.out.println("Submit Edges");
			submitQueue = (Queue)ctx.lookup("jms/linkQueue");
			// due to links being destroyed and recreated with new GUIDs, old links must be deleted before new ones added.
			// as the old links will cause DB constraint failures.
			jsonResponse.put("edges_deleted",submit(submitQueue, new JSONArray(req.getParameter("edges_deleted")),userName));
			jsonResponse.put("edges_submitted",submit(submitQueue, new JSONArray(req.getParameter("edges")),userName));
		}
		if (req.getParameter("fields") != null)
		{
			// want to put the agent name into the field here.
			jsonResponse.put("fields_submitted",submit((Queue)ctx.lookup("jms/fieldQueue"),new JSONArray(req.getParameter("fields")),userName));
		}

		System.out.println(jsonResponse.toString());
		
		writer.println(jsonResponse.toString());
		writer.close(); 
	}
	catch (Exception e) {
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

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

