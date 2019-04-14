package net.silicontrip.ingress;

import net.silicontrip.*;
import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
	//private MUCellDAO cdao = null;
	//private MUFieldDAO fdao = null;
	private HashMap<S2CellId,UniformDistribution> cellmu = null;

	public void init () throws ServletException {
		//cdao = new SQLMUCellDAO();
		//fdao = new SQLMUFieldDAO();
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

	private JSONObject mu(String s) {
		//System.out.println("-> " + s);

		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		for (Object cobj : cells)
		{
			S2CellId cell = S2CellId.fromToken((String)cobj);
			UniformDistribution mu = cellBean.getMU(cell);
			JSONArray jmu = new JSONArray();
			if (mu != null)
			{
				jmu.put(mu.getLower());
				jmu.put(mu.getUpper());
				response.put((String)cobj,jmu);
			}
		}
		//System.out.println("<- " + response.toString());
		return response;
	}
	private JSONObject use(String s) throws NamingException {
//		System.out.println("useField -> " + s);

		JSONObject response = new JSONObject();

		JSONObject iitc_field = new JSONObject(s);
		JSONArray pts = iitc_field.getJSONObject("data").getJSONArray("points");

		Field searchField = new Field(
			pts.getJSONObject(0).getLong("latE6"),
			pts.getJSONObject(0).getLong("lngE6"),
			pts.getJSONObject(1).getLong("latE6"),
			pts.getJSONObject(1).getLong("lngE6"),
			pts.getJSONObject(2).getLong("latE6"),
			pts.getJSONObject(2).getLong("lngE6"));

		int known_mu = cellBean.muKnownField(searchField);

		response.put("mu_known",known_mu);

		// getCellsForField
		S2Polygon s2Field = searchField.getS2Polygon();
		//S2CellUnion cellu = cellBean.getCellsForField(s2Field);

		// getIntersectionMU
		HashMap<S2CellId,AreaDistribution> area = cellBean.getIntersectionMU(s2Field);

		// calc mu
		double min_mu = 0;
		double max_mu = 0;
		boolean undefined = false;
		JSONObject cells = new JSONObject();
		for (Map.Entry<S2CellId, AreaDistribution> entry : area.entrySet()) {
			//System.out.println (entry.getKey().toToken() + ": " + entry.getValue().toString());
			AreaDistribution mu = entry.getValue();
			JSONObject areaDist = new JSONObject();
			areaDist.put("area",mu.area);
			if (mu.mu != null)
			{
				areaDist.put("min",mu.mu.getLower());
				areaDist.put("max",mu.mu.getUpper());
				min_mu += mu.area * mu.mu.getLower();
				max_mu += mu.area * mu.mu.getUpper();
			} else {
				undefined=true;
				areaDist.put("lower",-1);
				areaDist.put("upper",-1);
			}
			cells.put(entry.getKey().toToken(),areaDist);
		}
		if (undefined)
		{
			min_mu=-1;
			max_mu=-1;
		}
		response.put("mu_min", min_mu);
		response.put("mu_max", max_mu);
		//response.put("cells", cells); // future expansion
		System.out.println("<- " + response.toString());
		return response;
	}

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/json");
		resp.setCharacterEncoding("UTF-8");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");

		//System.out.println("FieldServlet::doPost("+req.getQueryString()+")");

		PrintWriter writer = resp.getWriter();
		JSONObject jsonResponse;

				String userName = req.getParameter("agent");
				String apiKey = req.getParameter("apikey");
		req.login(userName,apiKey);

		if (req.getParameter("mu") != null)
		{
		//	System.out.println("Field Servlet - mu request");
			jsonResponse = mu(req.getParameter("mu"));
		}
		else if (req.getParameter("use") != null)
		{
		//	System.out.println("Field Servlet - use request");
			jsonResponse = use(req.getParameter("use"));
		} else {
			System.out.println("Field Servlet - invalid request");
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

