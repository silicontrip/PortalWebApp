package net.silicontrip.ingress;

import java.util.Enumeration;
import org.json.*;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import javax.naming.*;
import javax.jms.*;

public class JMSServlet extends HttpServlet {


  public void init () throws ServletException {
	;
  }

  public void destroy () {
	;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/html");
	//	resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");

	// authentication code
////		req.login(req.getParameter("agent"),req.getParameter("apikey"));
	// authentication code ends.  move to method? or catch login exception lower down?	

		
		PrintWriter writer = resp.getWriter();

		InitialContext ctx = new InitialContext();

		QueueConnectionFactory qcf = (QueueConnectionFactory) ctx.lookup("jms/QueueConnectionFactory");
		QueueConnection queueCon = qcf.createQueueConnection();

            // create queue session off the connection
            QueueSession queueSession = queueCon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
                       Queue  queue = (Queue)ctx.lookup("jms/portalQueue");
		queueCon.start();
	MessageConsumer receiver = queueSession.createConsumer(queue);
		Message msg = receiver.receiveNoWait();
		writer.println("<pre>");
		while (msg != null)
		{
			if (msg instanceof TextMessage) 
				writer.println(((TextMessage)msg).getText());
			msg = receiver.receiveNoWait();
		}
		writer.println("</pre>");
		writer.close(); 
	}
	catch (Exception e) {
		JSONObject jsonResponse = new JSONObject();
		e.printStackTrace();
		jsonResponse.put("error",  e.getMessage());
		jsonResponse.put("errorType",  e.getClass().getName());
		try {
			resp.setStatus(500);
			PrintWriter writer = resp.getWriter();
			writer.println(jsonResponse.toString());
			writer.close(); 
			//resp.sendError(500,jsonResponse.toString());  // I want this to be json valid
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

