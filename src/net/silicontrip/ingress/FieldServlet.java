package net.silicontrip.ingress;

import net.silicontrip.*;
import org.json.*;
import com.google.common.geometry.*;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.*;
import javax.naming.*;

import javax.ejb.EJB;


public class FieldServlet extends HttpServlet {

	@EJB
	private FieldSessionBean fieldBean;

	@EJB
	private CellSessionBean cellBean;

	@EJB
	private MUSessionBean muBean;
	
	private final InitialContext ctx = null;
	//private MUCellDAO cdao = null;
	//private MUFieldDAO fdao = null;
	private final HashMap<S2CellId,UniformDistribution> cellmu = null;

/*
    @Override
	public void init () throws ServletException {
			System.out.println("Starting Field Servlet");
	}

    @Override
	public void destroy () {
		System.out.println("stopping Field Servlet");
	}
*/
	private JSONObject mu(String s) {
		//System.out.println("-> " + s);

		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		for (Object cobj : cells)
		{
			S2CellId cell = S2CellId.fromToken((String)cobj);
			UniformDistribution mu = muBean.getMU(cell);
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

		int known_mu = fieldBean.muKnownField(searchField);

		response.put("mu_known",known_mu);

		// getCellsForField
		S2Polygon s2Field = searchField.getS2Polygon();
		//S2CellUnion cellu = cellBean.getCellsForField(s2Field);

		// getIntersectionMU
		HashMap<S2CellId,AreaDistribution> area = fieldBean.getIntersectionMU(s2Field);

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
		//System.out.println("<- " + response.toString());
		return response;
	}
	// damn copy and paste from use()
	// will find common code and move to another method.
	private JSONObject dtReport(String s) throws NamingException {
//		System.out.println("useField -> " + s);

		JSONObject response = new JSONObject();
		JSONArray resarr = new JSONArray();

		JSONArray dt = new JSONArray(s);
		for (Object ent : dt) {
			JSONObject dtent = (JSONObject) ent;
			JSONObject entres = new JSONObject();

			if (dtent.getString("type").equals("polygon")) {
				JSONArray pts = dtent.getJSONArray("latLngs");

				Field searchField = new Field(
					pts.getJSONObject(0).getDouble("lat"),
					pts.getJSONObject(0).getDouble("lng"),
					pts.getJSONObject(1).getDouble("lat"),
					pts.getJSONObject(1).getDouble("lng"),
					pts.getJSONObject(2).getDouble("lat"),
					pts.getJSONObject(2).getDouble("lng"));

			// common with use
				int known_mu = fieldBean.muKnownField(searchField);

				response.put("mu_known",known_mu);

				// getCellsForField
				S2Polygon s2Field = searchField.getS2Polygon();
				//S2CellUnion cellu = cellBean.getCellsForField(s2Field);

				// getIntersectionMU
				HashMap<S2CellId,AreaDistribution> area = fieldBean.getIntersectionMU(s2Field);

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
				entres.put("mu_min", min_mu);
				entres.put("mu_max", max_mu);
				entres.put("cells", cells); // future expansion
		// common code ends
				//System.out.println("<- " + response.toString());
			}
			resarr.put(entres);
		}
		response.put("result",resarr); // despite the response being an array, wrap it in an object
						// incase of needing to report an error
		return response;
	}

        @Override
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
		if (apiKey!=null) apiKey = apiKey.toLowerCase();
		req.login(userName,apiKey);

		if (req.getParameter("mu") != null) {
		//	System.out.println("Field Servlet - mu request");
			jsonResponse = mu(req.getParameter("mu"));
		}
		else if (req.getParameter("use") != null) {
		//	System.out.println("Field Servlet - use request");
			jsonResponse = use(req.getParameter("use"));
		} else if (req.getParameter("dtreport") != null) {
			jsonResponse = dtReport(req.getParameter("dtreport"));
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
		} catch (IOException e2) {
			// :-P
			e2.printStackTrace();
		}
	} catch (IOException | NamingException | JSONException e) {
		JSONObject jsonResponse = new JSONObject();
		e.printStackTrace();
		jsonResponse.put("error",  e.getMessage());
		jsonResponse.put("errorType",  e.getClass().getName());
		try {
			resp.setStatus(500);
			PrintWriter writer = resp.getWriter();
			writer.println(jsonResponse.toString());
			writer.close();
		} catch (IOException e2) {
			// sending the error caused an error.
			// is there anything else we can do?
			e2.printStackTrace();
		}
	}
  }

        @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

