package net.silicontrip.ingress;

import net.silicontrip.UniformDistribution;
import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;
import javax.naming.*;

import javax.ejb.EJB;


public class FieldServlet extends HttpServlet {

	@EJB
	private CellSessionBean cellBean;

	private InitialContext ctx = null;
	private MUCellDAO cdao = null;
	private MUFieldDAO fdao = null;
	private HashMap<S2CellId,UniformDistribution> cellmu = null;

	public void init () throws ServletException {
		cdao = new SQLMUCellDAO();
		fdao = new SQLMUFieldDAO();
/*
		try {
			ctx = new InitialContext();
		} catch (NamingException e) {
			throw new ServletException(e.getMessage());
		}
*/
	}

	public void destroy () {
	;
	}
	
	private long[] e6Points (String s)
	{
		JSONObject iitc_field = new JSONObject(s);
		JSONObject data = iitc_field.getJSONObject("data");
                JSONArray pts = data.getJSONArray("points");

		long[] points = new long[6];
	
		points[0] = pts.getJSONObject(0).getLong("latE6");
		points[1] = pts.getJSONObject(0).getLong("lngE6");
		points[2] = pts.getJSONObject(1).getLong("latE6");
		points[3] = pts.getJSONObject(1).getLong("lngE6");
		points[4] = pts.getJSONObject(2).getLong("latE6");
		points[5] = pts.getJSONObject(2).getLong("lngE6");

		return points;
	}
		

	private JSONObject cells(String s) {  // looks like USE provides same functionality
		// getCellsForField
		// getIntersectionMU
		return new JSONObject(); 
	}
	private JSONObject mu(String s) { 
		//System.out.println("-> " + s);

		
		// get MU for Cells
		JSONObject response = new JSONObject();
		if (cellmu == null)
			cellmu =cdao.getAll();
		JSONArray cells = new JSONArray(s);
		for (Object cobj : cells)
		{
			S2CellId cell = S2CellId.fromToken((String)cobj);
			UniformDistribution mu = cellmu.get(cell);
			JSONArray jmu = new JSONArray();
			if (mu != null)
			{
				jmu.put(mu.getLower());
				jmu.put(mu.getUpper());
			} else {
				jmu.put(-1);
				jmu.put(-1);
			}
			response.put((String)cobj,jmu);	
		}	
		//System.out.println("<- " + response.toString());
		return response;
	}
	private JSONObject use(String s) throws NamingException { 
		System.out.println("useField -> " + s);

		JSONObject iitc_field = new JSONObject(s);
		JSONObject data = iitc_field.getJSONObject("data");
                JSONArray pts = data.getJSONArray("points");
		long[] points = new long[6];

		points[0] = pts.getJSONObject(0).getLong("latE6");
		points[1] = pts.getJSONObject(0).getLong("lngE6");
		points[2] = pts.getJSONObject(1).getLong("latE6");
		points[3] = pts.getJSONObject(1).getLong("lngE6");
		points[4] = pts.getJSONObject(2).getLong("latE6");
		points[5] = pts.getJSONObject(2).getLong("lngE6");

		// known_mu = findField
		ArrayList<Field> fa = fdao.findField(points);
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

