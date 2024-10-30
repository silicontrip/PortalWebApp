package net.silicontrip.ingress;

import net.silicontrip.*;
import org.json.*;
import com.google.common.geometry.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.*;
import javax.naming.*;

import jakarta.ejb.EJB;

import java.util.logging.Level;
import java.util.logging.Logger;

// set MU for guid
// delete guid
// delete cellid
// process guid

public class FieldServlet extends HttpServlet {

	@EJB
	private FieldSessionBean fieldBean;

	@EJB
	private CellSessionBean cellBean;

	@EJB
	private MUSessionBean muBean;

	@EJB
	private FieldsCellsBean fieldsCellsBean;

	@EJB
	private SQLEntityDAO dao;

	@EJB
	private FieldProcessCache fpCache;
	
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

	private String hsv2rgb (double h, double s, double v)
	{
		double r=0;
		double g=0;
		double b=0;
		double c = v * s;

		while (h<0) { h += 360; }
		while (h>=360) { h -= 360; }

		double h1 = h / 60.0;
		double x = c * ( 1 - Math.abs(h1 % 2 - 1));
	
		if (h1 >=0 && h1 < 1) { r = c; g = x; b = 0; }
		if (h1 >=1 && h1 < 2) { r = x; g = c; b = 0; }
		if (h1 >=2 && h1 < 3) { r = 0; g = c; b = x; }
		if (h1 >=3 && h1 < 4) { r = 0; g = x; b = c; }
		if (h1 >=4 && h1 < 5) { r = x; g = 0; b = c; }
		if (h1 >=5 && h1 < 6) { r = c; g = 0; b = x; }

		double m = v - c;
		r+=m; g+=m; b+=m;

		long ri, gi, bi;

		ri = Math.round(255 * r);
		gi = Math.round(255 * g);
		bi = Math.round(255 * b);

		return String.format("#%02x%02x%02x",ri,gi,bi);
	}

	private JSONObject processCell(String s) {
		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		JSONArray fieldGuids = new JSONArray();
		
		for (Object cobj : cells)
		{
			S2CellId cell = S2CellId.fromToken((String)cobj);
			ArrayList<String> fields = fieldsCellsBean.fieldGuidsForCell(cell);
			for (String fi : fields) {
				fieldGuids.put(fi);
				fpCache.addFieldGuid(fi);
			}
		}
		fieldBean.beginProcessing();	

		response.put("fieldGuids",fieldGuids);
		return response;
	}

	// processes fields that only have this cell.
	
	private JSONObject processCellExact(String s) {
		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		JSONArray fieldGuids = new JSONArray();
		
		for (Object cobj : cells)
		{

			S2CellId cell = S2CellId.fromToken((String)cobj);
			ArrayList<String> fields = fieldsCellsBean.fieldGuidsForCell(cell);
			//Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO,"searching cell id: " + cobj + " found " + fields.size() + "fields.");

			for (String fis : fields) {

				try {
					Field fi = dao.getField(fis);
					boolean match = true;
					for (S2CellId s2c : fi.getCells())
					{
						boolean innerMatch = false;
						for (Object sobj : cells)
						{
							//Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO,"field: " + fis + " compare " + sobj + " : " + s2c.toToken());

							if (s2c.toToken().equals((String)sobj))
								innerMatch = true;
						}
						match = match && innerMatch;
					}
					if (match)
					{
						Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO,"processing field guid: " + fis);
						fieldBean.processField(fi.getGuid());
						fieldGuids.put(fi.getGuid());
						//fpCache.addFieldGuid(fi.getGuid());
					}
				} catch (Exception e) {
					Logger.getLogger(FieldServlet.class.getName()).log(Level.WARNING, null, e);
				}
			}
		}
		//fieldBean.beginProcessing();	

		response.put("fieldGuids",fieldGuids);
		return response;
	}
	private JSONObject getFields(String s) {
		JSONArray fields = new JSONArray();
		JSONObject response = new JSONObject();
		JSONArray guids = new JSONArray(s);

		for (Object cobj : guids) 
		{
			JSONObject jsonField = new JSONObject();
			try {
				Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO,"getField for guid: " + cobj);

				Field fi = dao.getField((String)cobj);

				if (fi != null) {
					jsonField.put("creator",fi.getCreator());
					jsonField.put("agent",fi.getAgent());
					jsonField.put("mu",fi.getMU());
					jsonField.put("guid",fi.getGuid());
					jsonField.put("timestamp",fi.getTimestamp());
					jsonField.put("team",fi.getTeam());
					jsonField.put("pguid1",fi.getPGuid1()); jsonField.put("plat1",fi.getPLat1()); jsonField.put("plng1",fi.getPLng1());
					jsonField.put("pguid2",fi.getPGuid2()); jsonField.put("plat2",fi.getPLat2()); jsonField.put("plng2",fi.getPLng2());
					jsonField.put("pguid3",fi.getPGuid3()); jsonField.put("plat3",fi.getPLat3()); jsonField.put("plng3",fi.getPLng3());
				}
			} catch (Exception e) {
				Logger.getLogger(FieldServlet.class.getName()).log(Level.WARNING, null, e);
				;
			}

			fields.put(jsonField);
		}
		response.put("fields",fields);
		return response;
	}

	private JSONObject findSplit(String s)
	{
		JSONObject response = new JSONObject();
		JSONArray guids = new JSONArray();
		try {
			Field fi = dao.getField(s);

			guids.put(fi.getGuid());

			Link l1 = new Link("", "", fi.getPLat1(), fi.getPLng1(), "", fi.getPLat2(), fi.getPLng2(), "");
			Link l2 = new Link("", "", fi.getPLat2(), fi.getPLng2(), "", fi.getPLat3(), fi.getPLng3(), "");
			Link l3 = new Link("", "", fi.getPLat3(), fi.getPLng3(), "", fi.getPLat1(), fi.getPLng1(), "");

			ArrayList<Field>f1 = dao.findField(l1);
			ArrayList<Field>f2 = dao.findField(l2);
			ArrayList<Field>f3 = dao.findField(l3);

			for (Field f: f1)
			{
				JSONObject fid = new JSONObject();
				fid.put ("mu",f.getMU());
				JSONObject muest = getEstMu(f);
				fid.put ("muest", muest);
				response.put(f.getGuid(),fid);
			}
			for (Field f: f2)
			{
				JSONObject fid = new JSONObject();
				fid.put ("mu",f.getMU());
				JSONObject muest = getEstMu(f);
				fid.put ("muest", muest);
				response.put(f.getGuid(),fid);
			}
			for (Field f: f3)
			{
				JSONObject fid = new JSONObject();
				fid.put ("mu",f.getMU());
				JSONObject muest = getEstMu(f);
				fid.put ("muest", muest);
				response.put(f.getGuid(),fid);
			}


		} catch (Exception e) {
			Logger.getLogger(FieldServlet.class.getName()).log(Level.WARNING, null, e);
			;
		}
		return response;	
	}

	private JSONObject fields(String s) {
		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		JSONArray fieldGuids = new JSONArray();
		
		for (Object cobj : cells)
		{
			S2CellId cell = S2CellId.fromToken((String)cobj);
			ArrayList<String> fields = fieldsCellsBean.fieldGuidsForCell(cell);
			for (String fi : fields) {
				fieldGuids.put(fi);
			}
		}
		response.put("fieldGuids",fieldGuids);
		return response;
	}
	private String celldiag(String s) {
		StringBuilder response = new StringBuilder();
		JSONArray cells = new JSONArray(s);
		//JSONObject response = new JSONObject();
		JSONArray dt = new JSONArray();
		
		response.append("<pre>\n");
		for (Object cobj : cells)
		{
			S2CellId cell = S2CellId.fromToken((String)cobj);
			HashMap<String,UniformDistribution> fields = fieldBean.fieldMUCell(cell);
			for (Map.Entry<String,UniformDistribution> fis : fields.entrySet()) {
				try {
					Field fi = dao.getField(fis.getKey());
					if (fi != null) {
						response.append(fis.getKey());
						response.append(" ");
						response.append(fi.getMU());
						response.append(" ");
						response.append(fis.getValue());
						response.append(" ");

						S2Polygon fiPoly = fi.getS2Polygon();
						double fiArea = fiPoly.getArea() * 6367 * 6367 ;

						double mukm = fi.getMU() / fiArea;
						response.append("[");
						response.append(dtpolygon(fiPoly.loop(0),hsv2rgb(mukm/10,1.0,1.0)));
						response.append("]");
						response.append("\n");
					}
				} catch (EntityDAOException e) {
					Logger.getLogger(FieldServlet.class.getName()).log(Level.WARNING, "cannot find field guid: " + fis);
				}

			}
		}
		response.append("</pre>\n");
		return response.toString();
	}
	private JSONObject mu(String s) {
		//System.out.println("-> " + s);

		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		for (Object cobj : cells)
		{
			// S2CellId cell = S2CellId.fromToken((String)cobj);
			UniformDistribution mu = muBean.getMU((String)cobj);
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
	private JSONObject deleteCells(String s) {
		//System.out.println("-> " + s);

		JSONArray cells = new JSONArray(s);
		JSONObject response = new JSONObject();
		for (Object cobj : cells)
		{
			Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO, "Delete cell: " + cobj);

			//S2CellId cell = S2CellId.fromToken((String)cobj);
			muBean.deleteMU((String)cobj);
			//muBean.deleteMUEntity(mucell);
		}
		//System.out.println("<- " + response.toString());
		return response;
	}

	private JSONObject getEstMu(Field searchField) {
		HashMap<S2CellId,AreaDistribution> area = fieldBean.getIntersectionMU(searchField);
		JSONObject response = new JSONObject();
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
			areaDist.put("cell",entry.getKey().toToken());
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

		response = getEstMu(searchField);

		response.put("mu_known",known_mu);
		return response;
	}

	private JSONObject dtpolygon(S2Loop l, String colour)
	{
		JSONObject dtobj = new JSONObject();
		dtobj.put("type","polygon");
		dtobj.put("color", colour); // :-P

		//S2Loop l = p.loop(0); // these should be simple single polygons
		JSONArray latLngs = new JSONArray();
		for (int i =0; i < l.numVertices(); i++)
		{
			S2LatLng ll = new S2LatLng(l.vertex(i));
			JSONObject jpt = new JSONObject();
			jpt.put("lat",ll.latDegrees());
			jpt.put("lng",ll.lngDegrees());
			latLngs.put(jpt);
		}
		dtobj.put("latLngs",latLngs);
		return dtobj;
	}

	private String diag(String s) {
		StringBuilder response = new StringBuilder();
		try {
			Field fi = dao.getField(s);
			if (fi != null) {
				HashMap<S2CellId,AreaDistribution> area = fieldBean.getIntersectionMU(fi);
				response.append("<pre>");
				response.append("\n");
				response.append("mu: ");
				response.append(fi.getMU());
				response.append("\n");
				S2Polygon fiPoly = fi.getS2Polygon();
				Double fiArea = fiPoly.getArea() * 6367 * 6367 ;
				response.append("area: ");
				response.append(fiArea);
				response.append("\n");
				response.append("mu/km: ");
				UniformDistribution muud = new UniformDistribution(fi.getMU(),0.5);
				response.append( muud.div(fiArea).toString());
				response.append("\n");
				response.append("\n");
				for (Map.Entry<S2CellId, AreaDistribution> entry : area.entrySet()) {
					AreaDistribution mu = entry.getValue();
					response.append(entry.getKey().toToken());
					response.append(": ");
					response.append("[");
					if (mu.mu != null) {
						response.append(mu.mu.getLower());
						response.append(",");
						response.append(mu.mu.getUpper());
					} else {
						response.append("undefined");
					}
					response.append("]");
					response.append(" x ");
					response.append(mu.area);
					response.append(" = ");
					response.append("[");
					if (mu.mu != null) {
						response.append(mu.mu.getLower() * mu.area);
						response.append(",");
						response.append(mu.mu.getUpper() * mu.area);
					} else {
						response.append("undefined");
					}
					response.append("]");
					response.append("\n");
				}
				// dt for field
				JSONArray dtField = new JSONArray();
				dtField.put(dtpolygon(fiPoly.loop(0),"#CC4444"));
				response.append(dtField.toString());
				response.append("\n");
			
				// dt for cells
				JSONArray dtCells = new JSONArray();
				for (Map.Entry<S2CellId, AreaDistribution> entry : area.entrySet()) {
					S2Cell cell = new S2Cell(entry.getKey());
					dtCells.put(dtpolygon(new S2Loop(cell),"#FFFFFF"));
				}
				response.append(dtCells.toString());
				response.append("\n");

				HashSet<String> intFields = new HashSet<String>();
				for (Map.Entry<S2CellId, AreaDistribution> entry : area.entrySet()) 
				{
					//S2CellId cell = S2CellId.fromToken((String)cobj);
					ArrayList<String> fields = fieldsCellsBean.fieldGuidsForCell(entry.getKey());
					for (String fiGuid : fields) {
						intFields.add(fiGuid);

					}
				}
				for (Object fiGuid : intFields.toArray())
				{
					String fiGuidString = (String)fiGuid;
					response.append(fiGuidString);
					response.append(" ");
					Field fid = dao.getField(fiGuidString);
					response.append (fid.getMU());
					response.append(" ");
					// cells
					for (S2CellId sid : fid.getCells())
					{
						response.append(sid.toToken());
						response.append(" ");
					}	
					// mu/km
					S2Polygon fidPoly = fid.getS2Polygon();
					Double fidArea = fidPoly.getArea() * 6367 * 6367 ;
					UniformDistribution mud = new UniformDistribution(fid.getMU(),0.5);
					response.append(mud.div(fidArea).toString());
					response.append("\n");
				}


				response.append("</pre>");		
			}
		} catch (EntityDAOException e) {
			response.append(e.getMessage());
		}
		return response.toString();
	}
	// damn copy and paste from use()
	// will find common code and move to another method.
	private JSONObject dtReport(String s) throws NamingException {
		System.out.println("dtreport -> " + s);

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
				entres = getEstMu(searchField);

				entres.put("mu_known",known_mu);

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

                //Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO, "username: " + userName + " apikey: " + apiKey);

	//	req.login(userName,apiKey);

		if (req.getParameter("mu") != null) {
		//	System.out.println("Field Servlet - mu request");
			Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO, "request mu for: " + req.getParameter("mu"));
			jsonResponse = mu(req.getParameter("mu"));
		}
		else if (req.getParameter("use") != null) {
		//	System.out.println("Field Servlet - use request");
			//Logger.getLogger(FieldServlet.class.getName()).log(Level.INFO, "use field for: " + req.getParameter("use"));
			jsonResponse = use(req.getParameter("use"));
		} else if (req.getParameter("dtreport") != null) {
			jsonResponse = dtReport(req.getParameter("dtreport"));
		} else if (req.getParameter("fields") != null) {
			jsonResponse = fields(req.getParameter("fields"));
		} else if (req.getParameter("getfields") != null) {
			jsonResponse = getFields(req.getParameter("getfields"));
		} else if (req.getParameter("process") != null) {
			jsonResponse = processCell(req.getParameter("process"));
		} else if (req.getParameter("processexact") != null) {
			jsonResponse = processCellExact(req.getParameter("processexact"));
		} else if (req.getParameter("deletecell") != null) {
			jsonResponse = deleteCells(req.getParameter("deletecell"));
		} else if (req.getParameter("diag") != null) {
			jsonResponse = new JSONObject();
			String textResponse = diag(req.getParameter("diag"));
			writer.println(textResponse);
		} else if (req.getParameter("celldiag") != null) {
			jsonResponse = new JSONObject();
			String textResponse = celldiag(req.getParameter("celldiag"));
			writer.println(textResponse);
		} else if (req.getParameter("findsplit") != null) {
			jsonResponse = findSplit(req.getParameter("findsplit"));
		} else {
			System.out.println("Field Servlet - invalid request");
			resp.setStatus(400);
			jsonResponse = new JSONObject();
			jsonResponse.put("error",  "invalid request");
		}

		writer.println(jsonResponse.toString());
		writer.close();
/*
	} catch (ServletException e) {
		Logger.getLogger(FieldServlet.class.getName()).log(Level.SEVERE, null, e);

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
*/
	} catch (IOException | NamingException | JSONException e) {
		Logger.getLogger(FieldServlet.class.getName()).log(Level.SEVERE, null, e);

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

