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

public class LinkServlet extends HttpServlet {

	LinkDAO dao = null;

  public void init () throws ServletException {
	dao = new SQLLinkDAO();
  }

  public void destroy () {
	;
  }

  private S2LatLng fromE6String(String s)
  {
	if (s==null)
		return null;
	if (s.matches("(\\+|-)?([0-9]+(\\.[0-9]+)),(\\+|-)?([0-9]+(\\.[0-9]+))"))
	{
		String[] ll = s.split(",");
		Double lat = Double.parseDouble(ll[0]);
		Double lng = Double.parseDouble(ll[1]);
		long latE6 = Math.round(lat * 1000000);
		long lngE6 = Math.round(lng * 1000000);  
		return S2LatLng.fromE6(latE6,lngE6);
	}
	return null;
  }

  public void doPost(HttpServletRequest req, HttpServletResponse resp){
	try {
		resp.setContentType("text/json");
		resp.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");
		PrintWriter writer = resp.getWriter();


	//	PortalDAO pdao = new SQLPortalDAO();

		S2LatLng p1 = fromE6String(req.getParameter("ll"));
		S2LatLng p2 = fromE6String(req.getParameter("l2"));
		
		S2LatLngRect searchRegion = null;

		if (p1 !=null && p2 !=null)
			searchRegion = S2LatLngRect.fromPointPair(p1,p2);

		ArrayList<Link> linkList; // no not that kind of link list

		if (searchRegion != null) 
			linkList = dao.getInRect(searchRegion);
		else
			linkList = dao.getAll();
			
		JSONArray jsonResponse = new JSONArray();
		for (Link li : linkList)
			{
				JSONObject jsonLink = new JSONObject();
				jsonLink.put("guid",li.getGuid());
				jsonLink.put("team",li.getTeam());
				jsonLink.put("dguid",li.getdGuid());
				jsonLink.put("dlat",li.getdLatE6()); // would love to move these to the E6 naming
				jsonLink.put("dlng",li.getdLngE6()); // E6

				jsonLink.put("oguid",li.getoGuid());
				jsonLink.put("olat",li.getoLatE6()); // would love to move these to the E6 naming
				jsonLink.put("olng",li.getoLngE6()); // E6
				jsonResponse.put(jsonLink);	
			}
			writer.println(jsonResponse.toString());
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
			e2.printStackTrace();
		}
	}  
  }

  public void doGet(HttpServletRequest req, HttpServletResponse resp){
	doPost(req,resp);
  }
}

