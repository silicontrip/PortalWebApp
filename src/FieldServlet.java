package net.silicontrip.ingress;

import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;

public class FieldServlet extends HttpServlet {

	MUCellDAO dao = null;

  public void init () throws ServletException {
	dao = new SQLMUCellDAO();
  }

  public void destroy () {
	;
  }

	private JSONObject cells(String s) { 
		// getCellsForField
		// getIntersectionMU
		return new JSONObject(); 
	}
	private JSONObject mu(String s) { 
		// get MU for Cells
		return new JSONObject(); 
	}
	private JSONObject use(String s) { 
		// known_mu = findField
		// getCellsForField
		// getIntersectionMU
		// calc mu
		return new JSONObject(); 
	}

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/json");
		resp.setCharacterEncoding("UTF-8");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");

		PrintWriter writer = resp.getWriter();
		JSONObject jsonResponse;

                String userName = req.getParameter("agent");
                String apiKey = req.getParameter("apikey");
		req.login(userName,apiKey);

		if (req.getParameter("cells") != null)
		{
			jsonResponse = cells(req.getParameter("cells"));
		}
		else if (req.getParameter("mu") != null)
		{
			jsonResponse = mu(req.getParameter("mu"));
		}
		else if (req.getParameter("use") != null)
		{
			jsonResponse = use(req.getParameter("use"));
		} else {
			resp.setStatus(400);
			jsonResponse = new JSONObject();
			jsonResponse.put("error",  "invalid request");
		}
			

		writer.println(jsonResponse.toString());
		writer.close(); 
		
	} catch (ServletException e) {
		JSONObject jsonResponse = new JSONObject();
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
	} catch (Exception e) {
		JSONObject jsonResponse = new JSONObject();
		e.printStackTrace();
		jsonResponse.put("error",  e.getMessage());
		jsonResponse.put("errorType",  e.getClass().getName());
		try {
			resp.setStatus(500);
			PrintWriter writer = resp.getWriter();
			writer.println(jsonResponse.toString());
			writer.close(); 
		} catch (Exception e2) {
			// sending the error caused an error.
			// is there anything else we can do?
			e2.printStackTrace();
		}
	}  
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

