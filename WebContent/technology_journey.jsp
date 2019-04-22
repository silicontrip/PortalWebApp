<%
try {
	response.setCharacterEncoding("UTF-8");
	response.setContentType("text/javascript");

	response.addHeader("Access-Control-Allow-Origin","https://intel.ingress.com");
	request.login(request.getParameter("agent"),request.getParameter("apikey"));

	String baseUrl = "https://quadrant.silicontrip.net/portalApi/";
	if (request.getRemoteAddr().startsWith("10.15.240.")) 
		baseUrl = "https://quadrant.silicontrip.net:8181/portalApi/";

%>
	window.plugin.dbPortalGrabber = {
		request_ip: "<%= request.getRemoteAddr() %>",
		hz_submit_url: "<%= baseUrl %>submitEntity",
		hz_get_url: "<%= baseUrl %>getPortals",
		hz_portals: [],
		STROKE_STYLE: {
			stroke: true,
			opacity: 1,
			weight: 1.5,
			fill: true,
			fillColor: null, //same as color by default
			fillOpacity: 0.2,
			clickable: true,
		},
		dbPortals: {},
		portalLayer: {},
		layer: null,
		setupCSS: function() {
			window.$("<style>").prop("type", "text/css").html(''
			+'#hz_portal_grabber a {'
			+'margin-left: 5px;'
			+'margin-right: 5px;'
			+'white-space: nowrap;'
			+'display: inline-block;'
			+'}'
			).appendTo("head");
		},
		setup: function() {
			console.log('dbPortalGrabber::setup');
			console.log('this is a change. this is a change');

			window.plugin.dbPortalGrabber.layer = new window.L.LayerGroup();
			window.addLayerGroup('DB Portals', window.plugin.dbPortalGrabber.layer, true);
			window.plugin.dbPortalGrabber.STROKE_STYLE.color = localStorage.getItem("window.plugin.dbPortalGrabber.STROKE_STYLE.color");

			window.addHook('portalAdded', window.plugin.dbPortalGrabber.portalAdded);
			window.addHook('mapDataRefreshEnd', window.plugin.dbPortalGrabber.pushPortals);
			window.addHook('mapDataRefreshStart', window.plugin.dbPortalGrabber.mapDataRefreshStart);
			// want to catch enable db portals layer. perform redraw.

			window.$('#toolbox').append('<a onclick="window.plugin.dbPortalGrabber.portalColour();return false;" accesskey="C" >DB Colour</a>');

		window.$('#hz_portal_grabber').html('<strong>Portal post</strong> '
					+'<a onclick="window.plugin.dbPortalGrabber.addKey();return false;">Update key</a>'
					+' (<span id="hz_portals">0</span>)');
		},
		portalAdded: function(data)
		{
			console.log (">>> portalAdded");
			var portal = data.portal.options.data;
			portal.guid = data.portal.options.guid;

			window.plugin.dbPortalGrabber.hz_portals.push(portal);
			window.$('#hz_portals').html(window.plugin.dbPortalGrabber.hz_portals.length);
			console.log ("<<< portalAdded");
		},
		selectPortal: function(e){
			window.renderPortalDetails (e.target.guid);
		},
		portalColour: function() {
			var html = '<div id="db_colour"><input id="colour_selector" type="color" name="color" value="' + window.plugin.dbPortalGrabber.STROKE_STYLE.color +'" /></div>' ;
						  window.dialog({
							html: html,
							id: 'plugin-dbportal-colour',
							dialogClass: 'ui-dialog-dbportal',
							title: 'DB Portal Colour'
						  });
						document.getElementById('colour_selector').addEventListener('change', window.plugin.dbPortalGrabber.changeColour, false);
		},
		changeColour: function(evt) {
			window.plugin.dbPortalGrabber.STROKE_STYLE.color = evt.srcElement.value;
			localStorage.setItem("window.plugin.dbPortalGrabber.STROKE_STYLE.color", window.plugin.dbPortalGrabber.STROKE_STYLE.color);
			window.plugin.dbPortalGrabber.portalLayer = {};
			window.plugin.dbPortalGrabber.drawPortals();
		},
		mapDataRefreshStart: function()
		{
			window.$('#hz_portal_grabber').css('background-color', '');
			if (!window.map.hasLayer(window.plugin.dbPortalGrabber.layer)) { return; }

			var bounds = window.map.getBounds();
			var alat = bounds._southWest.lat;
			var alng = bounds._southWest.lng;
			var blat = bounds._northEast.lat;
			var blng = bounds._northEast.lng;

			var query = "?ll=" + alat + "," + alng +"&l2=" + blat + "," + blng + "&agent=" + window.PLAYER.nickname ;
			var geturl = window.plugin.dbPortalGrabber.hz_get_url+query;
			//console.log(geturl);

			window.$.get(geturl, window.plugin.dbPortalGrabber.loadJSONPortals);
		},
		loadJSONPortals: function (dbPortals) {
			//console.log("dbPortalGrabber::loadJSONPortals");
			window.plugin.dbPortalGrabber.dbPortals = dbPortals;
			window.plugin.dbPortalGrabber.drawPortals();
		},
		drawPortals: function(){
			//console.log("dbPortalGrabber::drawPortals");
			// don't draw if all portals are visible.
			if(window.map.getZoom() < 15) {
				for (var pt in window.plugin.dbPortalGrabber.dbPortals)
				{
					//console.log(pt)
					var thisPortal = window.plugin.dbPortalGrabber.dbPortals[pt];
					if (! (pt in window.plugin.dbPortalGrabber.portalLayer)) {
						var ll = window.L.latLng(thisPortal.lat/1000000.0, thisPortal.lng/1000000.0);
						var circ = window.L.circle(ll,10,window.plugin.dbPortalGrabber.STROKE_STYLE);
						circ.on('click', window.plugin.dbPortalGrabber.selectPortal);
						circ.guid = pt;
						window.plugin.dbPortalGrabber.portalLayer[pt] = circ;
						circ.addTo(window.plugin.dbPortalGrabber.layer);
					}
				}
			}
			//console.log(window.plugin.dbPortals.layer);
		},
		pushPortals: function()
		{
			if(!window.plugin.dbPortalGrabber.hz_portals.length)
			{
				window.$('#hz_portal_grabber').css('background-color', 'green');
				return;
			}

			// identify deleted portals
			// check we are zoom 15 or higher
			var deleted_portals =[];
			//console.log("db_portal_grabber::pushPortals map zoom: "+window.map.getZoom());
			if (window.map.getZoom() >= 15)
			{
				for (var guid in window.plugin.dbPortalGrabber.dbPortals)
				{
					// does this guid exist?
					if (!window.portals.hasOwnProperty(guid)) //no?
					{ deleted_portals.push({"delete": guid}); }
				}
				// add to deleted array
			//console.log("db_portal_grabber::pushPortals deleted_portals: " +deleted_portals.length);
			}
			window.$('#hz_portals').html('Pushing.. '+window.plugin.dbPortalGrabber.hz_portals.length);
			//console.log('portalGrabber - Pushing portals - ' + window.plugin.dbPortalGrabber.hz_portals.length);
			window.$.post(window.plugin.dbPortalGrabber.hz_submit_url, {apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname, portals: JSON.stringify(window.plugin.dbPortalGrabber.hz_portals), portals_deleted: JSON.stringify(deleted_portals)} )
				.fail(function() {
					alert("Technology Journey - error submitting portals");
			});
			window.plugin.dbPortalGrabber.hz_portals = [];
			window.$('#hz_portal_grabber').css('background-color', 'green');
			window.$('#hz_portals').html(window.plugin.dbPortalGrabber.hz_portals.length);
		}
	};

	window.plugin.dbEdgeGrabber = {
		hz_url: "<%= baseUrl %>submitEntity",
		hz_get_url: "<%= baseUrl %>getLinks",
		hz_edges: [],
		setupCSS: function() {
			window.$("<style>").prop("type", "text/css").html(''+
				'#hz_edge_grabber a {' +
				'margin-left: 5px;' +
				'margin-right: 5px;' +
				'white-space: nowrap;' +
				'display: inline-block;'+
				'}'
			).appendTo("head");
		},
		setup: function() {
			console.log('dbEdgeGrabber::setup');

			window.addHook('linkAdded', window.plugin.dbEdgeGrabber.edgeAdded);
			window.addHook('mapDataRefreshEnd', window.plugin.dbEdgeGrabber.pushedges);
			window.addHook('mapDataRefreshStart', window.plugin.dbEdgeGrabber.mapDataRefreshStart);

			window.$('#sidebar').append('<div id="hz_edge_grabber" style="padding: 5px; font-size: 12px">'
				+'<strong>Edges: </strong> '
				+' (<span id="hz_edges">0</span>)'
				+'</div>'
			);
		},
		edgeAdded: function(data) {
			var edge = data.link.options.data;
			edge.guid = data.link.options.guid;

			window.plugin.dbEdgeGrabber.hz_edges.push(edge);
			window.$('#hz_edges').html(window.plugin.dbEdgeGrabber.hz_edges.length);
		},
		mapDataRefreshStart: function()
		{
			window.$('#hz_edge_grabber').css('background-color', '');
			var bounds = window.map.getBounds();
			//console.log("Map Bounds");
			var alat = bounds._southWest.lat;
			var alng = bounds._southWest.lng;
			var blat = bounds._northEast.lat;
			var blng = bounds._northEast.lng;

			var query = "?ll=" + alat + "," + alng +"&l2=" + blat + "," + blng + "&agent=" + window.PLAYER.nickname ;
			var geturl = window.plugin.dbEdgeGrabber.hz_get_url+query;
			//console.log(geturl);
			// maybe try this as post?
			window.$.get(geturl, window.plugin.dbEdgeGrabber.loadJSON);
		},
		loadJSON: function (json) {
			window.plugin.dbEdgeGrabber.dbLinks = json;
		},
		pushedges: function()
		{
			if(!window.plugin.dbEdgeGrabber.hz_edges.length)
			{
				window.$('#hz_edge_grabber').css('background-color', 'green');
				return;
			}

			// work out deleted links
			// read links DB for current view
			var deleted_links =[];
			var zoom = window.map.getZoom();
			for (var guid in window.plugin.dbEdgeGrabber.dbLinks)
			{
				// does this guid exist?
				if (!window.links.hasOwnProperty(guid)) //no?
				{
					deleted_links.push({"delete": true, "guid": guid, "zoom":zoom});
				}
			}
			window.$('#hz_edges').html('Pushing.. '+window.plugin.dbEdgeGrabber.hz_edges.length);
			//console.log('edgeGrabber - Pushing edges - ' + window.plugin.dbEdgeGrabber.hz_edges.length);

			// need to put map bounds in here, as intel submits more links than are visible
			// this causes DB constraint issues
			window.$.post(window.plugin.dbEdgeGrabber.hz_url, {apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname, bounds: JSON.stringify(window.map.getBounds()), edges: JSON.stringify(window.plugin.dbEdgeGrabber.hz_edges), edges_deleted: JSON.stringify(deleted_links)} )
				.fail(function(xhr, status, error) {
					alert("Technology Journey - Error submitting links");
					console.log(JSON.stringify(xhr) +" " + status + " " + error );
				});
			window.plugin.dbEdgeGrabber.hz_edges = [];
			window.$('#hz_edge_grabber').css('background-color', 'green');
			window.$('#hz_edges').html(window.plugin.dbEdgeGrabber.hz_edges.length);
		}
	};

window.plugin.muScraper = {
	plextLinkList: {},
	plextFieldList: {},
	fieldLayers: {},
	fieldList: {},
	cellMu: {},
	labelLayerGroup: null,
	fieldLayerGroup: null,
	cellsLayerGroup: null,
	clickLatLng: null,
	selectedField: null,
	muSubmit: true,
	submitKey: "",
	hz_fields: [],
	hz_url: '<%= baseUrl %>submitEntity',
	mu_url: '<%= baseUrl %>getMU',
	mu_use: '<%= baseUrl %>getMU',
	NAME_WIDTH: 60,
	NAME_HEIGHT: 12,
	FILL_STYLE: {
		fill: true,
		color: '#CC44CC',
		opacity: 1,
		fillColor: '#CC44CC',
		fillOpacity: 0.5,
		weight: 4,
		clickable: false
	},
	hsvtorgb: function(h, s, v) {
		//console.log("hsv: " + h + ", " + s +", " + v);
		var r, g, b, i, f, p, q, t;
		i = Math.floor(h * 6);
		f = h * 6 - i;
		p = v * (1 - s);
		q = v * (1 - f * s);
		t = v * (1 - (1 - f) * s);
		switch (i % 6) {
			case 0: r = v; g = t; b = p; break;
			case 1: r = q; g = v; b = p; break;
			case 2: r = p; g = v; b = t; break;
			case 3: r = p; g = q; b = v; break;
			case 4: r = t; g = p; b = v; break;
			case 5: r = v; g = p; b = q; break;
		}
		//console.log("rgb: " + r + ", " + g + ", " + b);
		var colour = Math.round(b * 255) + Math.round(g * 255) * 256 + Math.round(r * 255) * 65536;
		var c = ("000000" + colour.toString(16)).substr(-6);
		//console.log("hex: " + c);
		return "#" + c;
	},
	setup: function() {
		console.log("dbMUFieldsCells::setup");
		/// S2 Geometry functions
		// the regional scoreboard is based on a level 6 S2 Cell
		// - https://docs.google.com/presentation/d/1Hl4KapfAENAOf4gv-pSngKwvS_jwNVHRPZTTDzXXn6Q/view?pli=1#slide=id.i22
		// at the time of writing there's no actual API for the intel map to retrieve scoreboard data,
		// but it's still useful to plot the score cells on the intel map


		// the S2 geometry is based on projecting the earth sphere onto a cube, with some scaling of face coordinates to
		// keep things close to approximate equal area for adjacent cells
		// to convert a lat,lng into a cell id:
		// - convert lat,lng to x,y,z
		// - convert x,y,z into face,u,v
		// - u,v scaled to s,t with quadratic formula
		// - s,t converted to integer i,j offsets
		// - i,j converted to a position along a Hubbert space-filling curve
		// - combine face,position to get the cell id
		// - get cells based on a triangle polygon

		//NOTE: compared to the google S2 geometry library, we vary from their code in the following ways
		// - cell IDs: they combine face and the hilbert curve position into a single 64 bit number. this gives efficient space
		//             and speed. javascript doesn't have appropriate data types, and speed is not cricical, so we use
		//             as [face,[bitpair,bitpair,...]] instead
		// - i,j: they always use 30 bits, adjusting as needed. we use 0 to (1<<level)-1 instead
		//        (so GetSizeIJ for a cell is always 1)

		(function() {

			// class to hold the pre-calculated maths for a geodesic line
			var GeodesicLine = window.GeodesicLine = function(start,end) {
				var d2r = Math.PI/180.0;
				var r2d = 180.0/Math.PI;

				// maths based on http://williams.best.vwh.net/avform.htm#Int

				//    if (start.lng == end.lng) {
				//      throw 'Error: cannot calculate latitude for meridians';
				//    }

				// only the variables needed to calculate a latitude for a given longitude are stored in 'this'
				this.lat1 = start.lat * d2r;
				this.lat2 = end.lat * d2r;
				this.lng1 = start.lng * d2r;
				this.lng2 = end.lng * d2r;

				var dLng = this.lng1-this.lng2;

				var sinLat1 = Math.sin(this.lat1);
				var sinLat2 = Math.sin(this.lat2);
				var cosLat1 = Math.cos(this.lat1);
				var cosLat2 = Math.cos(this.lat2);

				this.sinLat1CosLat2 = sinLat1*cosLat2;
				this.sinLat2CosLat1 = sinLat2*cosLat1;

				this.cosLat1CosLat2SinDLng = cosLat1*cosLat2*Math.sin(dLng);
			};

			GeodesicLine.prototype.isMeridian = function() {
				return this.lng1 == this.lng2;
			};

			GeodesicLine.prototype.latAtLng = function(lng) {
				lng = lng * Math.PI / 180; //to radians

				var lat;
				// if we're testing the start/end point, return that directly rather than calculating
				// 1. this may be fractionally faster, no complex maths
				// 2. there's odd rounding issues that occur on some browsers (noticed on IITC MObile) for very short links - this may help
				if (lng == this.lng1) {
					lat = this.lat1;
				} else if (lng == this.lng2) {
					lat = this.lat2;
				} else {
					lat = Math.atan ( (this.sinLat1CosLat2*Math.sin(lng-this.lng2) - this.sinLat2CosLat1*Math.sin(lng-this.lng1))/this.cosLat1CosLat2SinDLng);
				}
				return lat * 180 / Math.PI; // return value in degrees
			};

			GeodesicLine.prototype.intersects = function(gdl) {
				// based on the formula at http://williams.best.vwh.net/avform.htm#Int

				// method:
				// check to ensure no line segment is zero length - if so, cannot cross
				// check to see if either of the lines start/end at the same point. if so, then they cannot cross
				// check to see if the line segments overlap in longitude. if not, no crossing
				// if overlap, clip each line to the overlapping longitudes, then see if latitudes cross

				// anti-meridian handling. this code will not sensibly handle a case where one point is
				// close to -180 degrees and the other +180 degrees. unwrap coordinates in this case, so one point
				// is beyond +-180 degrees. this is already true in IITC
				// FIXME? if the two lines have been 'unwrapped' differently - one positive, one negative - it will fail

				//Dimand: Lets fix the date line issue.
				//always work in the eastern hemisphere. so += 360


				var a0={};
				var a1={};
				var b0={};
				var b1={};
				a0.lng=this.lng1;
				a0.lat=this.lat1;
				a1.lng=this.lng2;
				a1.lat=this.lat2;
				b0.lng=gdl.lng1;
				b0.lat=gdl.lat1;
				b1.lng=gdl.lng2;
				b1.lat=gdl.lat2;
				//debugger;
				// zero length line tests
				if ((a0.lat==a1.lat)&&(a0.lng==a1.lng)) return false;
				if ((b0.lat==b1.lat)&&(b0.lng==b1.lng)) return false;

				// lines have a common point
				if ((a0.lat==b0.lat)&&(a0.lng==b0.lng)) return false;
				if ((a0.lat==b1.lat)&&(a0.lng==b1.lng)) return false;
				if ((a1.lat==b0.lat)&&(a1.lng==b0.lng)) return false;
				if ((a1.lat==b1.lat)&&(a1.lng==b1.lng)) return false;

				// a0.lng<=-90 && a1.lng>=90 dosent suffice... a link from -70 to 179 still crosses
				//if a0.lng-a1.lng >180 or <-180 there is a cross!
				var aCross = false;
				var bCross = false;
				//this is the real link
				if ((a0.lng-a1.lng)<-180 ||(a0.lng-a1.lng)>180)
				{//we have a dateline cross
					//console.log('DateLine Cross!');
					//move everything in the eastern hemisphere to the extended eastern one
					aCross = true;
					if (a0.lng <0)
					{
						a0.lng += 360;
					}
					if (a1.lng <0)
					{
						a1.lng += 360;
					}
				}
				//this is the arc
				if ((b0.lng-b1.lng)<-180 ||(b0.lng-b1.lng)>180)
				{
					//console.log('DateLine Cross!');
					bCross=true;
					if (b0.lng <0)
					{
						b0.lng += 360;
					}
					if (b1.lng <0)
					{
						b1.lng += 360;
					}
				}
				//now corrected both a and b for date line crosses.
				//now if link is entirely in the west we need to move it to the east.
				if (bCross && aCross)
				{
					//both got moved. all should be good.
					//do nothing
				}
				else if (aCross)
				{
					//now we need to move any links in the west of the main one
					if (Math.max(b0.lng,b1.lng)<Math.min(a0.lng,a1.lng))
					{
						//console.log('arc shift');
						b0.lng += 360;
						b1.lng += 360;
					}
				}
				else if (bCross)
				{
					//now we need to move any links in the west of the main one
					if (Math.max(a0.lng,a1.lng)<Math.min(b0.lng,b1.lng))
					{
						//console.log('link shift');
						a0.lng += 360;
						a1.lng += 360;
						//console.log(a0);
						//console.log(a1);
						//console.log(b0);
						//console.log(b1);
					}
				}


				// check for 'horizontal' overlap in longitude
				if (Math.min(a0.lng,a1.lng) > Math.max(b0.lng,b1.lng)) return false;
				if (Math.max(a0.lng,a1.lng) < Math.min(b0.lng,b1.lng)) return false;

				// calculate the longitude of the overlapping region
				var leftLng = Math.max( Math.min(a0.lng,a1.lng), Math.min(b0.lng,b1.lng) );
				var rightLng = Math.min( Math.max(a0.lng,a1.lng), Math.max(b0.lng,b1.lng) );
				//console.log(leftLng);
				//console.log(rightLng);

				// calculate the latitudes for each line at left + right longitudes
				// NOTE: need a special case for meridians - as GeodesicLine.latAtLng method is invalid in that case
				var aLeftLat, aRightLat;
				if (a0.lng == a1.lng) {
					// 'left' and 'right' now become 'top' and 'bottom' (in some order) - which is fine for the below intersection code
					aLeftLat = a0.lat;
					aRightLat = a1.lat;
				} else {
					var aGeo = new GeodesicLine(a0,a1);
					aLeftLat = aGeo.latAtLng(leftLng);
					aRightLat = aGeo.latAtLng(rightLng);
				}

				var bLeftLat, bRightLat;
				if (b0.lng == b1.lng) {
					// 'left' and 'right' now become 'top' and 'bottom' (in some order) - which is fine for the below intersection code
					bLeftLat = b0.lat;
					bRightLat = b1.lat;
				} else {
					var bGeo = new GeodesicLine(b0,b1);
					bLeftLat = bGeo.latAtLng(leftLng);
					bRightLat = bGeo.latAtLng(rightLng);
				}
				//console.log(aLeftLat);
				//console.log(aRightLat);
				//console.log(bLeftLat);
				//console.log(bRightLat);
				// if both a are less or greater than both b, then lines do not cross

				if (aLeftLat < bLeftLat && aRightLat < bRightLat) return false;
				if (aLeftLat > bLeftLat && aRightLat > bRightLat) return false;

				// latitudes cross between left and right - so geodesic lines cross
				//console.log('Xlink!');
				return true;
			};

			var S2 = window.S2 = {
				FACE_BITS: 3,
				MAX_LEVEL: 30,
				//  POS_BITS: (2 * S2.MAX_LEVEL) + 1, // 61 (60 bits of data, 1 bit lsb marker)
				POS_BITS: 61,
				fromFacePosLevel: function (faceN, posS, levelN) {
					// var Long = window.dcodeIO && window.dcodeIO.Long || require('long');
					var faceB;
					var posB;
					var bin;

					if (!levelN) {
						levelN = posS.length;
					}
					if (posS.length > levelN) {
						posS = posS.substr(0, levelN);
					}

					// 3-bit face value
					//  faceB = Long.fromString(faceN.toString(10), true, 10).toString(2);
					faceB = faceN.toString(2);
					while (faceB.length < S2.FACE_BITS) {
						faceB = '0' + faceB;
					}

					//console.log("FACE B: " + faceB);

					// 60-bit position value
					//  posB = Long.fromString(posS, true, 4).toString(2);
					posB = parseInt(posS,4).toString(2);
					while (posB.length < (2 * levelN)) {
						posB = '0' + posB;
					}
					// console.log("POS: " + posS + " B: " + posB);

					bin = faceB + posB;
					// 1-bit lsb marker
					bin += '1';
					// n-bit padding to 64-bits
					while (bin.length < (S2.FACE_BITS + S2.POS_BITS)) {
						bin += '0';
					}
					//console.log("BIN: " + bin);
					return parseInt(bin,2);
					//  return Long.fromString(bin, true, 2).toString(10);
				},
				toCellId:function (key) {
					var parts = key.split('/');

					return S2.fromFacePosLevel(parts[0], parts[1], parts[1].length);
				},
				LatLngToXYZ: function(latLng) {
					var d2r = Math.PI/180.0;

					var phi = latLng.lat*d2r;
					var theta = latLng.lng*d2r;

					var cosphi = Math.cos(phi);

					return [Math.cos(theta)*cosphi, Math.sin(theta)*cosphi, Math.sin(phi)];
				},

				XYZToLatLng: function(xyz) {
					var r2d = 180.0/Math.PI;

					var lat = Math.atan2(xyz[2], Math.sqrt(xyz[0]*xyz[0]+xyz[1]*xyz[1]));
					var lng = Math.atan2(xyz[1], xyz[0]);

					return window.L.latLng(lat*r2d, lng*r2d);
				},
				largestAbsComponent: function(xyz) {
					var temp = [Math.abs(xyz[0]), Math.abs(xyz[1]), Math.abs(xyz[2])];

					if (temp[0] > temp[1]) {
						if (temp[0] > temp[2]) {
							return 0;
						} else {
							return 2;
						}
					} else {
						if (temp[1] > temp[2]) {
							return 1;
						} else {
							return 2;
						}
					}

				},

				faceXYZToUV: function(face,xyz) {
					var u,v;

					switch (face) {
						case 0: u =  xyz[1]/xyz[0]; v =  xyz[2]/xyz[0]; break;
						case 1: u = -xyz[0]/xyz[1]; v =  xyz[2]/xyz[1]; break;
						case 2: u = -xyz[0]/xyz[2]; v = -xyz[1]/xyz[2]; break;
						case 3: u =  xyz[2]/xyz[0]; v =  xyz[1]/xyz[0]; break;
						case 4: u =  xyz[2]/xyz[1]; v = -xyz[0]/xyz[1]; break;
						case 5: u = -xyz[1]/xyz[2]; v = -xyz[0]/xyz[2]; break;
						default: throw {error: 'Invalid face'};
					}

					return [u,v];
				},

				XYZToFaceUV: function(xyz) {
					var face = S2.largestAbsComponent(xyz);

					if (xyz[face] < 0) {
						face += 3;
					}

					var uv = S2.faceXYZToUV (face,xyz);

					return [face, uv];
				},
				FaceUVToXYZ: function(face,uv) {
					var u = uv[0];
					var v = uv[1];

					switch (face) {
						case 0: return [ 1, u, v];
						case 1: return [-u, 1, v];
						case 2: return [-u,-v, 1];
						case 3: return [-1,-v,-u];
						case 4: return [ v,-1,-u];
						case 5: return [ v, u,-1];
						default: throw {error: 'Invalid face'};
					}
				},

				STToUV: function(st) {
					var singleSTtoUV = function(st) {
						if (st >= 0.5) {
							return (1/3.0) * (4*st*st - 1);
						} else {
							return (1/3.0) * (1 - (4*(1-st)*(1-st)));
						}
					};

					return [singleSTtoUV(st[0]), singleSTtoUV(st[1])];
				},
				UVToST: function(uv) {
					var singleUVtoST = function(uv) {
						if (uv >= 0) {
							return 0.5 * Math.sqrt (1 + 3*uv);
						} else {
							return 1 - 0.5 * Math.sqrt (1 - 3*uv);
						}
					};

					return [singleUVtoST(uv[0]), singleUVtoST(uv[1])];
				},

				STToIJ: function(st,order) {
					var maxSize = (1<<order);

					var singleSTtoIJ = function(st) {
						var ij = Math.floor(st * maxSize);
						return Math.max(0, Math.min(maxSize-1, ij));
					};

					return [singleSTtoIJ(st[0]), singleSTtoIJ(st[1])];
				},

				IJToST: function(ij,order,offsets) {
					var maxSize = (1<<order);

					return [
						(ij[0]+offsets[0])/maxSize,
						(ij[1]+offsets[1])/maxSize
					];
				},

				// hilbert space-filling curve
				// based on http://blog.notdot.net/2009/11/Damn-Cool-Algorithms-Spatial-indexing-with-Quadtrees-and-Hilbert-Curves
				// note: rather than calculating the final integer hilbert position, we just return the list of quads
				// this ensures no precision issues whth large orders (S3 cell IDs use up to 30), and is more
				// convenient for pulling out the individual bits as needed later
				pointToHilbertQuadList: function(x,y,order) {
					var hilbertMap = {
						'a': [ [0,'d'], [1,'a'], [3,'b'], [2,'a'] ],
						'b': [ [2,'b'], [1,'b'], [3,'a'], [0,'c'] ],
						'c': [ [2,'c'], [3,'d'], [1,'c'], [0,'b'] ],
						'd': [ [0,'a'], [3,'c'], [1,'d'], [2,'d'] ]
					};

					var currentSquare='a';
					var positions = [];

					for (var i=order-1; i>=0; i--) {
						var mask = 1<<i;
						var quad_x = x&mask ? 1 : 0;
						var quad_y = y&mask ? 1 : 0;

						var t = hilbertMap[currentSquare][quad_x*2+quad_y];

						positions.push(t[0]);

						currentSquare = t[1];
					}

					return positions;
				},
				CellRegionOverlap: function(cell,field)
				{
					// could optimize by checking if boundaries overlap.

					//console.log(cell);
					//console.log(field);

					var fpts = field.getLatLngs();
					var cpts = cell.getCornerLatLngs();

					//console.log(fpts);
					//console.log(cpts);

					var gfield = new google.maps.Polygon({geodesic:true,paths:fpts});
					var gcell  = new google.maps.Polygon({geodesic:true,paths:cpts});

					var ll;
					var glatlng;
					for (ll in cpts)
					{
						glatlng=new google.maps.LatLng(cpts[ll]);
						// catch this error and try again
						try {
							if(google.maps.geometry.poly.containsLocation(glatlng, gfield)) return true;
						} catch (err) {
							if(google.maps.geometry.poly.containsLocation(glatlng, gfield)) return true;
						}
					}

					for (ll in fpts)
					{
						glatlng=new google.maps.LatLng(fpts[ll]);
						if(google.maps.geometry.poly.containsLocation(glatlng, gcell)) return true;
					}

					// console.log("No contains Location");

					var fl1 = new GeodesicLine(fpts[0],fpts[1]);
					var fl2 = new GeodesicLine(fpts[1],fpts[2]);
					var fl3 = new GeodesicLine(fpts[2],fpts[0]);

					var cl1 = new GeodesicLine(cpts[0],cpts[1]);
					var cl2 = new GeodesicLine(cpts[1],cpts[2]);
					var cl3 = new GeodesicLine(cpts[2],cpts[3]);
					var cl4 = new GeodesicLine(cpts[3],cpts[0]);


					return fl1.intersects(cl1) ||
						fl1.intersects(cl2) ||
						fl1.intersects(cl3) ||
						fl1.intersects(cl4) ||
						fl2.intersects(cl1) ||
						fl2.intersects(cl2) ||
						fl2.intersects(cl3) ||
						fl2.intersects(cl4) ||
						fl3.intersects(cl1) ||
						fl3.intersects(cl2) ||
						fl3.intersects(cl3) ||
						fl3.intersects(cl4);

				},
				getCellsForRegion: function(field){

	var candidate = [
		S2.S2Cell.FromLatLng({'lat': 0, 'lng': -90} , 0),
		S2.S2Cell.FromLatLng({'lat': 0, 'lng': 0} , 0),
		S2.S2Cell.FromLatLng({'lat': 0, 'lng': 90} , 0),
		S2.S2Cell.FromLatLng({'lat': 0, 'lng': 180} , 0),
		S2.S2Cell.FromLatLng({'lat': 90, 'lng': 0} , 0),
		S2.S2Cell.FromLatLng({'lat': -90, 'lng': 0} , 0)
	];

					var pts = field.getLatLngs();
	// assuming the field is all on one face.
	// this won't be true for all fields but just for testing.
					/*
					var candidate = [
						S2.S2Cell.FromLatLng(pts[0],0)
					];
*/
					var altcandidate = [];
					var altcandidate2 = [];

					var count = 0;
					while (count < 32)
					{
						//altcandidate = candidate;
						altcandidate2 = S2.findCandidates(candidate,field);
						//console.log("presingle: " + altcandidate.length);
						altcandidate = S2.singleCandidates(altcandidate2,field);
						//console.log("postsingle: " + candidate.length);
						if (altcandidate.length < 20) candidate = altcandidate;
						count++;
					}

					console.log("" + candidate.length + " <-> " + altcandidate.length);

					return candidate;

				},
				singleCandidates: function(altcandidate,field)
				{
					var candidate = [];
					var cell;
					for (cell in altcandidate)
					{
						if (altcandidate[cell].level < 13) {
							var ccells = altcandidate[cell].subdivide();
							var count = 0;
							var ccell;
							for (ccell in ccells)
							{
								if(S2.CellRegionOverlap(ccells[ccell],field)) count++;
							}
							if (count==1)
							{
								for (ccell in ccells)
								{
									if(S2.CellRegionOverlap(ccells[ccell],field)) candidate.push(ccells[ccell]);
								}
							} else {
								candidate.push(altcandidate[cell]);
							}
						} else {
							candidate.push(altcandidate[cell]);
						}
					}
					return candidate;
				},
				findCandidates: function(altcandidate,field) {
					var candidate = [];
					//console.log(">>> findCandidates");
					var cell;
					for (cell in altcandidate)
					{
						// console.log(altcandidate[cell]);
						if (altcandidate[cell].level < 13) {
							var ccells = altcandidate[cell].subdivide();
							// console.log(ccells);
							var count = 0;
							var ccell;
							for (ccell in ccells)
							{
								if(S2.CellRegionOverlap(ccells[ccell],field)) count++;
							}
							// console.log("count: " + count);
							if (count==4 && altcandidate.length > 2) {
								// don't subdivide further
								// unless the children have 1 child as their children
								candidate.push(altcandidate[cell]);
							} else {
								if (count > 0) {
									for (ccell in ccells)
									{
										if(S2.CellRegionOverlap(ccells[ccell],field)) candidate.push(ccells[ccell]);
									}
								}
							}
						} else {
							candidate.push(altcandidate[cell]);
						}
					}
					return candidate;
				},
				mergeCells: function(cellcover) {
					var pcells = {};
					var parent;
					for (var cell in cellcover)
					{
						parent = cellcover[cell].getParent();
						if (parent.toId() in pcells) pcells[parent.toId()] ++;
						else pcells[parent.toId()] = 1;
					}
					// need to iterate multiple times...
					var ucellcover={};
					for (cell in cellcover)
					{
						parent = cellcover[cell].getParent();
						if (pcells[parent.toId()] == 4)
						{ ucellcover[parent.toId()] = parent; }
						else
						{ ucellcover[cell] = cellcover[cell]; }
					}
					return ucellcover;
				}

			};

			// S2Cell class

			S2.S2Cell = function(){};

			//static method to construct
			S2.S2Cell.FromLatLng = function(latLng,level) {

				var xyz = S2.LatLngToXYZ(latLng);

				var faceuv = S2.XYZToFaceUV(xyz);
				var st = S2.UVToST(faceuv[1]);

				var ij = S2.STToIJ(st,level);

				return S2.S2Cell.FromFaceIJ (faceuv[0], ij, level);
			};

			S2.S2Cell.FromFaceIJ = function(face,ij,level) {
				var cell = new S2.S2Cell();
				cell.face = face;
				cell.ij = ij;
				cell.level = level;

				return cell;
			};


			S2.S2Cell.prototype.toString = function() {
				return 'F'+this.face+'ij['+this.ij[0]+','+this.ij[1]+']@'+this.level;
			};

			S2.S2Cell.prototype.getLatLng = function() {
				var st = S2.IJToST(this.ij,this.level, [0.5,0.5]);
				var uv = S2.STToUV(st);
				var xyz = S2.FaceUVToXYZ(this.face, uv);

				return S2.XYZToLatLng(xyz);
			};

			S2.S2Cell.prototype.getParent = function() {
				if (this.level === 0) return this;
				return S2.S2Cell.FromLatLng(this.getLatLng(),this.level-1);
			};
			S2.S2Cell.prototype.subdivide = function() {
				//console.log(JSON.stringify([this.ij,this.level]));
				var children=[];

				if (this.level==30) return [this]; // is 30 correct?

				// I'm wondering if there is a correct order to these...
				children =[
					S2.S2Cell.FromFaceIJ(this.face,[this.ij[0]*2,this.ij[1]*2],this.level+1),
					S2.S2Cell.FromFaceIJ(this.face,[this.ij[0]*2+1,this.ij[1]*2],this.level+1),
					S2.S2Cell.FromFaceIJ(this.face,[this.ij[0]*2+1,this.ij[1]*2+1],this.level+1),
					S2.S2Cell.FromFaceIJ(this.face,[this.ij[0]*2,this.ij[1]*2+1],this.level+1)
				];

				return children;
			};

			S2.S2Cell.prototype.getCornerLatLngs = function() {
				var result = [];
				var offsets = [
					[ 0.0, 0.0 ],
					[ 0.0, 1.0 ],
					[ 1.0, 1.0 ],
					[ 1.0, 0.0 ]
				];

				for (var i=0; i<4; i++) {
					var st = S2.IJToST(this.ij, this.level, offsets[i]);
					var uv = S2.STToUV(st);
					var xyz = S2.FaceUVToXYZ(this.face, uv);

					result.push ( S2.XYZToLatLng(xyz) );
				}
				return result;
			};
			S2.S2Cell.prototype.getFaceAndQuads = function() {
				var quads = S2.pointToHilbertQuadList(this.ij[0], this.ij[1], this.level);
				return [this.face,quads];
			};

			S2.S2Cell.prototype.toId = function() {
				//console.log("I: " + this.ij[0] + " J: " +  this.ij[1] + " Level: " + this.level);
				var quads = S2.pointToHilbertQuadList(this.ij[1], this.ij[0], this.level);
				//console.log(quads);
				var id = S2.fromFacePosLevel(this.face,quads.join(""),this.level);
				id = id / (1 << 24);
				id = id / (1 << 8);

				return id.toString(16);
			};

			S2.S2Cell.prototype.getNeighbors = function() {

				var fromFaceIJWrap = function(face,ij,level) {
					var maxSize = (1<<level);
					if (ij[0]>=0 && ij[1]>=0 && ij[0]<maxSize && ij[1]<maxSize) {
						// no wrapping out of bounds
						return S2.S2Cell.FromFaceIJ(face,ij,level);
					} else {
						// the new i,j are out of range.
						// with the assumption that they're only a little past the borders we can just take the points as
						// just beyond the cube face, project to XYZ, then re-create FaceUV from the XYZ vector

						var st = S2.IJToST(ij,level,[0.5,0.5]);
						var uv = S2.STToUV(st);
						var xyz = S2.FaceUVToXYZ(face,uv);
						var faceuv = S2.XYZToFaceUV(xyz);
						face = faceuv[0];
						uv = faceuv[1];
						st = S2.UVToST(uv);
						ij = S2.STToIJ(st,level);
						return S2.S2Cell.FromFaceIJ (face, ij, level);
					}
				};

				var face = this.face;
				var i = this.ij[0];
				var j = this.ij[1];
				var level = this.level;

				return [
					fromFaceIJWrap(face, [i-1,j], level),
					fromFaceIJWrap(face, [i,j-1], level),
					fromFaceIJWrap(face, [i+1,j], level),
					fromFaceIJWrap(face, [i,j+1], level)
				];
			};
		})();

		//console.log(">>> muScraper::setup");

		window.plugin.muScraper.setupCSS();

		window.plugin.muScraper.labelLayerGroup = new window.L.LayerGroup();
		window.plugin.muScraper.fieldLayerGroup = new window.L.LayerGroup();
		window.plugin.muScraper.cellsLayerGroup = new window.L.LayerGroup();

		window.addLayerGroup('MU labels', window.plugin.muScraper.labelLayerGroup, true);
		window.addLayerGroup('MU Cells', window.plugin.muScraper.cellsLayerGroup, true);
		window.addLayerGroup('MU selected field', window.plugin.muScraper.fieldLayerGroup, true);

		window.map.on('moveend', window.plugin.muScraper.updateCells);

		window.addHook('publicChatDataAvailable',window.plugin.muScraper.searchComms);
		window.addHook('fieldAdded',window.plugin.muScraper.readField);
		window.addHook('mapDataRefreshEnd' , window.plugin.muScraper.matchFieldsAndComms);

		$('#toolbox').append('<a onclick="window.plugin.muScraper.layerReport();return false;" >Layer Report</a>');
		$('#toolbox').append('<a onclick="window.plugin.muScraper.findSuitableFields();return false;" >Find Suitable Fields</a>');
		$('#toolbox').append('<a onclick="window.plugin.muScraper.copyFields();return false;" >Copy Fields</a>');

		window.$('#sidebar').append('<div id="hz_field_grabber" style="padding: 5px; font-size: 12px">' +
								  '<strong>Fields: </strong> ' +
								  '(<span id="hz_field">0</span>)' +
		' <a onclick="window.plugin.muScraper.pushFields();return false;">Push Fields</a> ' +
								  '</div>'
								 );

		// init google maps geometry
		// this doesn't work.  still need to click on the first field twice.
		setTimeout( function() { window.plugin.muScraper.getFieldsContainingLatLng({'lat':0,'lng':0}); }, 1000);
		window.plugin.muScraper.readLoadedFields();

	},
	readLoadedFields: function()
	{
		$.each(window.fields, function(ind, fd) { window.plugin.muScraper.readField({field:fd}); });
	},
	searchComms: function(data) {
		//console.log(">>> searchComms");
		for (var entry of data.result)
		{
			var read = entry[2].plext.text.split(" ");
			var creator = read[0];
			if ( read[1] == "linked")
			{
				//console.log(entry[2].plext.text);
				var oPortal = entry[2].plext.markup[2][1];
				var dPortal = entry[2].plext.markup[4][1];
				window.plugin.muScraper.plextLinkList[entry[0]] = { guid: entry[0], ts: entry[1], oPortal: oPortal, dPortal: dPortal, creator: creator};
			}

			if ( read[1] == "created")
			{
				//console.log("search comms - created");
				//console.log(entry);
				oPortal = entry[2].plext.markup[2][1];
				var mu = entry[2].plext.markup[4][1].plain;

				window.plugin.muScraper.plextFieldList[entry[0]]= { guid: entry[0], ts: entry[1], oPortal: oPortal, mu: mu, creator: creator};
			}
		}
		window.plugin.muScraper.matchFieldsAndComms();
	},
	getFieldsByTimestamp: function(timestamp)
	{
		//console.log(">>> getFieldsByTimestamp");
		var ff = [];
		$.each(window.fields, function(ind, field) {
			if(field.options.timestamp===timestamp) ff.push(field);
		});
		return ff;
	},
	searchCommsForLayers: function(latlng)
	{
		//console.log(">>> searchCommsForLayers");
		var flayers = window.plugin.muScraper.getFieldsContainingLatLng(latlng);
		for (var i=0; i< flayers.length; i++) { window.plugin.muScraper.searchCommsForField(flayers[i]); }
	},
	searchCommsForFieldGuids: function (fdguidjson) {
		//console.log(">>> searchCommsForGUID Array");
		var fa = JSON.parse(fdguidjson);
		for (var fdguid in fa) {
			window.plugin.muScraper.searchCommsForFieldGuid(fdguid);
		}
	},
	searchCommsForFieldGuid: function (fdguid) {
		window.plugin.muScraper.searchCommsForField(window.fields[fdguid]);
	},
	searchCommsForField: function (fd) {
		var minLatE6;
		var maxLatE6;
		var minLngE6;
		var maxLngE6;

		var latlngs=fd.getLatLngs();
		//console.log(latlngs);
		var bounds=window.L.latLngBounds([latlngs[0],latlngs[1]]).extend(latlngs[2]);
		var fdList=window.plugin.muScraper.getFieldsByTimestamp(fd.options.timestamp);
		for(var i=0;i<fdList.length;i++)
		{
			latlngs=fdList[i].getLatLngs();
			bounds.extend(latlngs[0]);
			bounds.extend(latlngs[1]);
			bounds.extend(latlngs[2]);
			minLatE6=bounds.getSouth()*1E6;
			minLngE6=bounds.getWest()*1E6;
			maxLatE6=bounds.getNorth()*1E6;
			maxLngE6=bounds.getEast()*1E6;
		}
		var created=fd.options.timestamp;
		var minTsMs = created;
		var maxTsMs = created + 2000; // comm messages are delayed
		var d = {
			minLatE6: Math.round(minLatE6)-1,
			minLngE6: Math.round(minLngE6)-1,
			maxLatE6: Math.round(maxLatE6)+1,
			maxLngE6: Math.round(maxLngE6)+1,
			minTimestampMs: minTsMs,
			maxTimestampMs: maxTsMs,
			tab: 'all'
		};

		var r = window.postAjax(
			'getPlexts',
			d,
			function(data, textStatus, jqXHR) { window.plugin.muScraper.searchComms(data); },
			function() { console.log('mu comms request failed - latE6='+minLatE6+'x'+maxLatE6+', lngE6='+minLngE6+'x'+maxLngE6+', created='+created); }
		);
	},
	readField: function(data) {
	//	console.log(">>> readField");
		//console.log(data.field.options);
		data.field.options.clickable=true;
		var area=window.plugin.muScraper.getAngArea(data.field.getLatLngs()) * 6367.0 * 6367.0;
		window.plugin.muScraper.fieldList[data.field.options.guid]={area:area,options:data.field.options};
		data.field.addEventListener('click',window.plugin.muScraper.clickField);
	},
	clickField: function(event) {
		//console.log(">>> clickField");
		window.plugin.muScraper.clickLatLng = event.latlng;
		window.plugin.muScraper.displayField(event.target);
	},
	highlightField: function(field) {
		//console.log(">>> highlightField");
		if (!window.map.hasLayer(window.plugin.muScraper.fieldLayerGroup)) return;

		var cells = window.S2.getCellsForRegion(field);

		window.plugin.muScraper.fieldLayerGroup.clearLayers();
		var pt=field.getLatLngs();

		var dt = { 'type': 'polygon', 'latLngs': pt , 'color': '#aa44cc'};
		//console.log(JSON.stringify([dt]));

		var poly;
		poly = window.L.polygon(pt, window.plugin.muScraper.FILL_STYLE);
		poly.addTo(window.plugin.muScraper.fieldLayerGroup);

		for (var cell in cells)
		{
			//console.log(cells[cell]);
			poly = window.L.polygon(cells[cell].getCornerLatLngs(), window.plugin.muScraper.FILL_STYLE);
			poly.addTo(window.plugin.muScraper.fieldLayerGroup);
		}
	},
	displayFieldGuid: function(guid)
	{
		//console.log(">>> displayFieldGuid");
		//console.log("Select by guid: "+guid);
		window.plugin.muScraper.displayField(window.fields[guid]);
	},
	displayField: function(field) {
		//console.log(">>> displayField");
		//console.log(field);
		//console.log(JSON.stringify(field.options));
		//console.log(JSON.stringify(field.getLatLngs()));

	   // var dt = {type:"polygon", color: "#ffffff", latLngs: field.getLatLngs()};
	   //console.log(field);
		window.plugin.muScraper.highlightField(field);
		window.plugin.muScraper.selectedField = field;
		// only if a portal is selected.
		window.renderPortalDetails(null);

		setTimeout(function() {
			console.log("displayField::setTimeout Function");
			//var area=window.plugin.muScraper.getAngArea(field.getLatLngs()) * 6367.0 * 6367.0;

			var fd = window.plugin.muScraper.fieldList[field.options.guid];
			if (fd === undefined) { fd={}; }

			console.log("displayField::flayers");
			var flayers = [];
			if (window.plugin.muScraper.clickLatLng) { flayers = window.plugin.muScraper.getFieldsContainingLatLng(window.plugin.muScraper.clickLatLng); }

			var layersHtml='';
			layersHtml+='<select onchange="window.plugin.muScraper.displayFieldGuid(this.value)">';
			var totalmu = 0;
			var unknown = 0;

			//flayers.sort(function(a,b) {return (window.plugin.muScraper.getAngArea(a.getLatLngs()) > window.plugin.muScraper.getAngArea(b.getLatLngs())) ? 1 : ( (window.plugin.muScraper.getAngArea(b.getLatLngs()) > window.plugin.muScraper.getAngArea(a.getLatLngs())) ? -1 : 0);} );
			console.log("displayField::for j");

			for(var j=0;j<flayers.length;j++)
			{
				//console.log(flayers[j]);
				var tfd = window.plugin.muScraper.fieldList[flayers[j].options.guid];
				var fa = window.plugin.muScraper.getAngArea(flayers[j].getLatLngs()) * 6367.0 * 6367.0;
				var text = fa.toFixed(3) + 'km^2 ';
				if (tfd) {
					//console.log(tfd);
					//console.log("tfd.mu");
					if (tfd.mu) {
						text += tfd.mu + ' MU';
						totalmu += parseInt(tfd.mu);
					} else {
						unknown++;
					}
				}
				layersHtml+='<option value="'+flayers[j].options.guid+'"';
				if(field.options.guid===flayers[j].options.guid) layersHtml+=' selected';
				layersHtml+='>'+text+'</option>';
			}
			layersHtml+='</select>';

			//console.log(fd);
			//console.log("displayField::html");

			var html='<div style="padding:5px" id="mu_fd">';
			//console.log("!fd.mu");
			if (!fd.mu) { html += '<a onclick="window.plugin.muScraper.searchCommsForField(window.plugin.muScraper.selectedField);return false;">Get MU</a> '; }
			if (unknown>0) { html += '<a onclick="window.plugin.muScraper.searchCommsForLayers(window.plugin.muScraper.clickLatLng);return false;">Get Layers MU</a>'; }
			//console.log("displayField::html<table>");

			html+='<table>';
			var created=window.unixTimeToString(field.options.timestamp,true);
			var creator = "unknown";

			if (fd.creator) { creator = fd.creator; }

			// <mark class="nickname" style="cursor:pointer; color:#03DC03">zfonz</mark>
			html+='<tr><td><b>Creator</b></td><td><mark class="nickname" style="cursor:pointer; color:' + window.COLORS[window.teamStringToId(field.options.data.team)] + '">'+creator+'</mark></td></tr>';

			html+='<tr><td><b>Created</b></td><td>'+created+'</td></tr>';
			html+='<tr><td><b>Area</b></td><td>'+fd.area.toFixed(3)+' km^2</td></tr>';
						//console.log("displayField::html getPortalLink");

			html+='<tr><td><b>Anchor</b></td><td>' + window.plugin.muScraper.getPortalLink(field.options.ent[2][2][0][0]) + '</td></tr>';
			html+='<tr><td><b>Anchor</b></td><td>' + window.plugin.muScraper.getPortalLink(field.options.ent[2][2][1][0]) + '</td></tr>';
			html+='<tr><td><b>Anchor</b></td><td>' + window.plugin.muScraper.getPortalLink(field.options.ent[2][2][2][0]) + '</td></tr>';
			html+='<tr><td><b>Layers</b></td><td>' + flayers.length +' '+ layersHtml + '</td></tr>';
			if (totalmu>0) html+='<tr><td><b>Total MU</b></td><td>' + totalmu + ' MU</td><tr>';
			//html+='<tr><td>guid</td><td><tt>' + field.options.guid + '</tt></td></tr>';
			if (fd.split) {
				if (fd.split>1) html+='<tr><td><b>Split Field</b></td><td>' + fd.split + '</td></tr>';
			}
			html+='</table>';
			html+='</div>';

			//console.log("displayField::end html");

			var title = 'Mu: unknown';
			if (fd.mu) title = "Mu: " + fd.mu;

			// console.log("displayField::#portaldetails");

			$('#portaldetails')
				.attr('class', TEAM_TO_CSS[teamStringToId(field.options.data.team)])
				.html('')
				.append(
				$('<h3>').attr({class:'title'}).text(title),
				$('<span>').attr({
					class: 'close',
					title: 'Close [w]',
					onclick:'renderPortalDetails(null);',
					accesskey: 'w'
				}).text('X'),
				html
				);
			//console.log("displayField::post mu_use");
			if (title == 'Mu: unknown')
				$.post(
					window.plugin.muScraper.mu_use,
					{apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname, use: JSON.stringify(field.options)},
					function(dd) {
						//var dd = JSON.parse(data);
						var title = "Mu: unknown";
						if (dd.mu_known != -1)
						{
							var rmk = Math.round(dd.mu_known * 1000) / 1000;
							title="Mu: (" + rmk +")";
						}
						else
						{
							var rmn = Math.round(dd.mu_min * 1000) / 1000;
							var rmx = Math.round(dd.mu_max * 1000) / 1000;

							title = "Mu: ["+ rmn+","+rmx+"]";
						}
						$('#portaldetails')
							.attr('class', TEAM_TO_CSS[teamStringToId(field.options.data.team)])
							.html('')
							.append(
							$('<h3>').attr({class:'title'}).text(title),
							$('<span>').attr({
								class: 'close',
								title: 'Close [w]',
								onclick:'renderPortalDetails(null);',
								accesskey: 'w'
							}).text('X'),
							html
						);
					}
				);
		}, 1000);
	},
	muEstCB: function()
	{

	},
	pushFields: function()
	{
		if(!window.plugin.muScraper.hz_fields.length)
		{
			$('#hz_field_grabber').css('background-color', 'green');
			return;
		}
		$('#hz_field').html('Pushing.. '+window.plugin.muScraper.hz_fields.length);
		console.log('muGrabber - Pushing fields - ' + window.plugin.muScraper.hz_fields.length);
		//console.log(window.plugin.muScraper.hz_fields);
		$.post(window.plugin.muScraper.hz_url, {apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname, fields: JSON.stringify(window.plugin.muScraper.hz_fields)} )
			.fail(function() {
				alert("Technology Journey - error posting fields");
		});
		window.plugin.muScraper.hz_fields = [];
		$('#hz_field_grabber').css('background-color', 'green');
		$('#hz_field').html(window.plugin.muScraper.hz_fields.length);
	},
	labelSingle: function(fd,label)
	{
		// console.log("labelSingle: " + fd.guid+  ", " + JSON.stringify(fd.options.data.points) + ", " + label);
		window.plugin.muScraper.addLabel(fd.guid,fd.options.data.points,label + " mu");
		//fd.mu = label;
		if (window.plugin.muScraper.selectedField)
			if (window.plugin.muScraper.selectedField.options.guid == fd.guid)
				window.plugin.muScraper.displayField(window.plugin.muScraper.selectedField);
	},
	pushField: function(f) {
		console.log("push Field: " + JSON.stringify(f));
		window.plugin.muScraper.hz_fields.push(f);
		$('#hz_field').html(window.plugin.muScraper.hz_fields.length);
	},
	matchFieldsAndComms: function() {

		var commGuid;
		var comm;
		var fd;
	//	console.log(">>> matchFieldsAndComms");
		// match link created with field created.
		for (commGuid in window.plugin.muScraper.plextLinkList)
		{
			comm = window.plugin.muScraper.plextLinkList[commGuid];
			comm.fcomm=[];
			for (var fcommGuid in window.plugin.muScraper.plextFieldList)
			{
				var fcomm = window.plugin.muScraper.plextFieldList[fcommGuid];
				if (comm.ts == fcomm.ts) {
					// can create 2 fields with 1 link
					comm.fcomm.push(fcomm);
				}
			}
		}

		for (commGuid in window.plugin.muScraper.plextLinkList)
		{
			comm = window.plugin.muScraper.plextLinkList[commGuid];
			comm.field = [];
			for (var fdGuid in window.plugin.muScraper.fieldList)
			{
				fd = window.plugin.muScraper.fieldList[fdGuid];
				fd.guid = fdGuid;
				if (window.plugin.muScraper.compareLinkAndField([comm.oPortal,comm.dPortal],fd.options.data.points)) {
					var tsdiff = Math.abs(comm.ts - fd.options.timestamp);
					//console.log("TS diff: " + tsdiff);
					if (tsdiff < 2000) comm.field.push(fd);
				}
			}
		}
		for (commGuid in window.plugin.muScraper.plextLinkList)
		{
			comm = window.plugin.muScraper.plextLinkList[commGuid];
			if (comm.field.length >0 && comm.field.length == comm.fcomm.length )
			{
				var label = 'undef';
				fd = null;
			// put MU in array and let the server decide which is valid.
				if (comm.field.length == 1)
				{
					comm.field[0].mu = [comm.fcomm[0].mu];
					comm.field[0].creator = comm.fcomm[0].creator;
					window.plugin.muScraper.labelSingle(comm.field[0],comm.fcomm[0].mu);
					comm.field[0].mu = [comm.fcomm[0].mu];
					console.log("matchFieldsAndComms::pushfield single");
					window.plugin.muScraper.pushField(comm.field[0]);
				}
				else if (comm.field.length == 2)
				{
					var mu0 = parseInt(comm.fcomm[0].mu);
					var mu1 = parseInt(comm.fcomm[1].mu);
					comm.field[0].creator = comm.fcomm[0].creator;
					comm.field[1].creator = comm.fcomm[0].creator;
					// a really bad way of assigning MU to split fields.
					if (mu0 > mu1)
					{
						if (comm.field[0].area > comm.field[1].area)
						{
							//comm.field[0].mu=mu0; comm.field[1].mu=mu1;
							comm.field[0].mu=[mu0,mu1]; comm.field[1].mu=[mu1,mu0];
							window.plugin.muScraper.labelSingle(comm.field[0],mu0);
							window.plugin.muScraper.labelSingle(comm.field[1],mu1);
						} else {
							//comm.field[1].mu=mu0; comm.field[0].mu=mu1;
							comm.field[0].mu=[mu0,mu1]; comm.field[1].mu=[mu1,mu0];
							window.plugin.muScraper.labelSingle(comm.field[0],mu1);
							window.plugin.muScraper.labelSingle(comm.field[1],mu0);
						}
					} else {
						if (comm.field[0].area > comm.field[1].area)
						{
							//comm.field[1].mu=mu0; comm.field[0].mu=mu1;
							comm.field[0].mu=[mu0,mu1]; comm.field[1].mu=[mu1,mu0];
							window.plugin.muScraper.labelSingle(comm.field[0],mu1);
							window.plugin.muScraper.labelSingle(comm.field[1],mu0);
						} else {
							//comm.field[0].mu=mu0; comm.field[1].mu=mu1;
							comm.field[0].mu=[mu0,mu1]; comm.field[1].mu=[mu1,mu0];
							window.plugin.muScraper.labelSingle(comm.field[0],mu0);
							window.plugin.muScraper.labelSingle(comm.field[1],mu1);
						}
					}
					console.log("matchFieldsAndComms::pushfield split");

					window.plugin.muScraper.pushField(comm.field[0]);
					window.plugin.muScraper.pushField(comm.field[1]);

				}
				else
				{
					console.log("Searching for Split fields: more than 2 field created: " + comm.field.length);
					//console.log(comm);
				}
			} else {
				// field matching mismatch
				// console.log("field mismatch: " + comm.field.length + "!=" + comm.fcomm.length);
				//console.log(comm);
			}
		}

		window.plugin.muScraper.plextLinkList	={};
		window.plugin.muScraper.plextFieldList	={};
	},
	compareLinkAndField: function(link,field){
		//console.log(">>> compareLinkAndField");

		return ((link[0].latE6 == field[0].latE6 && link[0].lngE6 == field[0].lngE6) ||
			(link[0].latE6 == field[1].latE6 && link[0].lngE6 == field[1].lngE6) ||
			(link[0].latE6 == field[2].latE6 && link[0].lngE6 == field[2].lngE6) ) &&

			((link[1].latE6 == field[0].latE6 && link[1].lngE6 == field[0].lngE6) ||
			(link[1].latE6 == field[1].latE6 && link[1].lngE6 == field[1].lngE6) ||
			(link[1].latE6 == field[2].latE6 && link[1].lngE6 == field[2].lngE6) ) ;
	},
	setupCSS: function() {
		$("<style>").prop("type", "text/css").html('' +'.plugin-mu{' +'color:#FFFFBB;' +
			   'font-size:11px;line-height:12px;' +
			   'text-align:center;padding: 2px;' +
			   // padding needed so shadow doesn't clip
			   'overflow:hidden;' +
			   'text-shadow:1px 1px #000,1px -1px #000,-1px 1px #000,-1px -1px #000, 0 0 5px #000;' +
			   'pointer-events:none;'+
			   '}' ).appendTo("head");
		  $("<style>")
	.prop("type", "text/css")
	.html(".plugin-regions-name {" +
			 "font-size: 11px; "+
			 "font-family: monospace; "+
			 "color: white; "+
			 "opacity: 1.0; "+
			 "text-align: center; "+
			 "text-shadow: -1px -1px #000, 1px -1px #000, -1px 1px #000, 1px 1px #000, 0 0 2px #000; " +
			 "pointer-events: none;" +
		  "}").appendTo("head");
	},
	getPortalLink: function(guid) {

		if (guid === null)
			return null;

		var portal=window.portals[guid].options.data;

	//var portal = window.plugin.linksFields.portalCache[guid];
	// how do we have a GUID but no portal?

		if (portal === undefined) return null;


						var lat = portal.latE6 / 1000000;
						var lng = portal.lngE6 / 1000000;

						var latlng = [lat,lng].join();
						var jsSingleClick = 'window.renderPortalDetails(\''+guid+'\');return false';
						var jsDoubleClick = 'window.zoomToAndShowPortal(\''+guid+'\', ['+latlng+']);return false';
						var perma = '/intel?ll='+lat+','+lng+'&z=17&pll='+lat+','+lng;
						if (!portal.title)
						{
								portal.title='undefined';
								jsSingleClick = 'window.renderPortalDetails(\''+guid+'\');return false';
						}

						//Use Jquery to create the link, which escape characters in TITLE and ADDRESS of portal
						var a = $('<a>',{
								text: portal.title,
								href: perma,
								onClick: jsSingleClick,
								onDblClick: jsDoubleClick
						})[0].outerHTML;
						var div = '<div class="portalTitle">'+a+'</div>';
						return div;
				},
	addLabel: function(guid, points ,mu) {
		// console.log(">>> addLabel: "+ guid + ", " + JSON.stringify(points) + ", " + mu);

		var lat = (points[0].latE6 + points[1].latE6 + points[2].latE6)/3000000.0;
		var lng = (points[0].lngE6 + points[1].lngE6 + points[2].lngE6)/3000000.0;
		var latLng = window.L.latLng(lat,lng);
		// console.log("addLabel::latLng:" + lat + ", " + lng);

		var previousLayer = window.plugin.muScraper.fieldLayers[guid];
	   // console.log("addLabel::previousLayer");
	   // console.log(previousLayer);
		 if (!previousLayer) {
			//console.log("addLabel:: label: " + guid + " = " + mu );
			var label = window.L.marker(latLng, {
				icon: window.L.divIcon({
					className: 'plugin-mu',
					iconAnchor: [window.plugin.muScraper.NAME_WIDTH/2,0],
					iconSize: [window.plugin.muScraper.NAME_WIDTH,window.plugin.muScraper.NAME_HEIGHT],
					html: mu
				}),
				guid: guid,
			});
			//console.log(JSON.stringify(label));
			window.plugin.muScraper.fieldLayers[guid] = label;
			label.addTo(window.plugin.muScraper.labelLayerGroup);
		} else {
			console.log("has previousLayer addLabel:: " + mu + " " + guid);
		}
	},
	getFieldsContainingLatLng: function(latlng)
	{
		//console.log(">>> getFieldsContainingLatLng");
		var ff = [];
		$.each(window.fields, function(ind, fd) {
			var fll=fd.getLatLngs();
		/*
			var glatlngs=[];
			for (var i=0; i < 3; i++)
			{
				var gll = new google.maps.LatLng(fll[i]);
				glatlngs.push(gll);
			}
		*/
			var gpoly=new google.maps.Polygon({geodesic:true,paths:fll});
			var glatlng=new google.maps.LatLng(latlng);
			if(google.maps.geometry.poly.containsLocation(glatlng, gpoly))
				 ff.push(fd);
		});
// SORT HERE
		ff.sort(function(a,b) {return (window.plugin.muScraper.getAngArea(a.getLatLngs()) > window.plugin.muScraper.getAngArea(b.getLatLngs())) ? 1 : ( (window.plugin.muScraper.getAngArea(b.getLatLngs()) > window.plugin.muScraper.getAngArea(a.getLatLngs())) ? -1 : 0);} );
		return ff;
	},
	getAngDistance: function(ll1,ll2)
	{
		//console.log(">>> getAngDistance");
		// * Math.PI / 180;

		var lat = (ll1.lat - ll2.lat) * Math.PI / 180;
		var lng = (ll1.lng - ll2.lng) * Math.PI / 180;
		var olat = ll2.lat * Math.PI / 180;
		var dlat = ll1.lat * Math.PI / 180;

		var a = (Math.sin(lat / 2.0) * Math.sin(lat/2.0)) + Math.cos(dlat) * Math.sin(lng / 2.0) * Math.sin(lng/2.0);
		return 2.0 * Math.atan2(Math.sqrt(a),Math.sqrt(1-a));
		//console.log("<<< getAngDistance");
	},
	getAngArea: function(latlngs)
	{
		//console.log(">>> getAngArea");
		var a = window.plugin.muScraper.getAngDistance(latlngs[0],latlngs[1]);
		var b = window.plugin.muScraper.getAngDistance(latlngs[1],latlngs[2]);
		var c = window.plugin.muScraper.getAngDistance(latlngs[2],latlngs[0]);

		var s = (a + b + c) / 2;

		var e = Math.sqrt( Math.tan(s/2.0) * Math.tan((s-a)/2.0) * Math.tan((s-b)/2.0) * Math.tan((s-c)/2.0) );

		return 4 * Math.atan(e);
	},
	findSuitableFields: function() {
		//console.log(">>> findSuitableFields");

		var d = new Date();
		var n = d.getTime();

		//var fa =[];
		var html ="";
		var fdguida = {};
		//console.log(window.fields.keys);
		var countFields=0;
		var knownFields=0;
		var estFields=0;
		var unknownFields=0;

		html += '<p id="totalfields"></p>';
		html += '<p id="knownfields"></p>';
		html += '<p id="estfields"></p>';
		html += '<p id="unknownfields"></p>';
		window.dialog({
			html: '<div id="cellReport">' + html + '</div>',
			dialogClass: 'ui-dialog-layers',
			title: 'Find Fields Report',
			id: 'layer-report',
			width: 550
		});

		for (var fdguid in window.fields) {
			//console.log(fdguid);
			if (window.fields.hasOwnProperty(fdguid)) {
				//console.log("has own property");
				var layer = window.plugin.muScraper.fieldLayers[fdguid];
				if (!layer) {
					var fd = window.fields[fdguid];
					fd.guid = fdguid;
					if (!fd.mu) {
						let field = fd;
						$.post(
							window.plugin.muScraper.mu_use,
							{apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname, use: JSON.stringify(field.options)},
							function(dd) {
								 //console.log("findSuitableFields::post " + data);
								//var dd = JSON.parse(data);
								if (dd.mu_known == -1)
								{
									var rmn = Math.round(dd.mu_min);
									var rmx = Math.round(dd.mu_max);

									if (rmn == -1 || rmn != rmx) {
										//console.log("" + n + " : " + fd.options.timestamp + " : " + age);
										//console.log(JSON.stringify(fd.options));
										//console.log("findSuitableFields:: searchCommsForField " + field.guid);
										window.plugin.muScraper.searchCommsForFieldGuid(field.guid);
										unknownFields ++;
										$('#unknownfields').html("unknown " + unknownFields);
									} else {
										//console.log("findSuitableFields:: accurate MU EST " + field.guid);
										window.plugin.muScraper.addLabel(field.guid,field.options.data.points,"["+rmn+"]");
										estFields ++;
										$('#estfields').html("Estimate " + estFields);

									   // window.plugin.muScraper.labelSingle(fd,rmn);
									}
								} else {
									//console.log("findSuitableFields:: known MU " + field.guid);
									window.plugin.muScraper.addLabel(field.guid,field.options.data.points,"("+dd.mu_known+")");
									knownFields++;
									$('#knownfields').html("known " + knownFields);

									//window.plugin.muScraper.labelSingle(fd,dd.mu_known);
								}
							}
						);
					}
					countFields++;
					var completed = knownFields + estFields+ unknownFields;
									$('#totalfields').html("completed " +completed + "/" + countFields);

				}
			}
		}
		//console.log(JSON.stringify(fdguida));
		//	html += '<a onclick="window.plugin.muScraper.searchCommsForFieldGuids(\'' + JSON.stringify(fdguida) + '\'); return false;">Get ALL</a>';

	},
	copyFields: function()
	{
		var flayers = [];
		if (window.plugin.muScraper.clickLatLng) {
			flayers = window.plugin.muScraper.getFieldsContainingLatLng(window.plugin.muScraper.clickLatLng);
			var dt = [];
			for (var i=0; i<flayers.length; i++)
			{
				var points = flayers[i].options.data.points;
				var dtpoints = [];
				for (var pts=0; pts < 3; pts++) dtpoints.push({"lat": points[pts].latE6/1000000.0, "lng": points[pts].lngE6/1000000.0});
				dt.push({"type": "polygon", "color": "#a24ac3", "latLngs": dtpoints});
			}
			//console.log(dt);
			window.plugin.drawTools.import(dt);
			window.plugin.drawTools.save();
		}
	},
	layerReport: function()
	{
		var flayers = [];
		var tfd;
		var html = "<h1>No Layers Selected</h1>";
		if (window.plugin.muScraper.clickLatLng) {
			flayers = window.plugin.muScraper.getFieldsContainingLatLng(window.plugin.muScraper.clickLatLng);
			//console.log("Layers selected: " + flayers.length);
			//html="<table>";
			//html+="<tr><th>creator</th><th>created</th><th>Area</th><th>mu</th></tr>";
			html = "<pre>";
			var sflayers=[];
			var ttmu=0;
			for (var i=0; i<flayers.length; i++)
			{
				//console.log("Layer : " + i);

				tfd = window.plugin.muScraper.fieldList[flayers[i].options.guid];
								var fa = window.plugin.muScraper.getAngArea(flayers[i].getLatLngs()) * 6367.0 * 6367.0;
				var created=window.unixTimeToString(tfd.options.timestamp,true);


				sflayers.push({ 'creator': tfd.creator, 'created_str': created, 'created': tfd.options.timestamp, 'area': fa, 'mu': tfd.mu });

				//html+="<tr>";
				//html += "<td>" + tfd.creator + "</td>";
				//html += "<td>" + created + "</td>";
				//html += "<td>" + fa + "</td>";
				//html += "<td>" + tfd.mu + "</td>";
				//html += "</tr>";
			}
			//sflayers.sort(function(a,b) {return (a.created > b.created) ? 1 : ((b.created > a.created) ? -1 : 0);} );
			for (i=0; i<sflayers.length; i++)
			{
				tfd = sflayers[i];
				html += tfd.creator + " ";
				html += tfd.created_str + " ";
				html += tfd.area.toFixed(3) + "km ";
				html += tfd.mu + "mu\n";
				ttmu += parseInt(tfd.mu);

			}
			//html+="</table>";
			html +="Total: " + ttmu;
			html+="</pre>";
		}
		window.dialog({
			html: '<div id="layerReport">' + html + '</div>',
			dialogClass: 'ui-dialog-layers',
			title: 'Fields Report',
			id: 'layer-report',
			width: 550
		});
	},
	updateCells: function() {

		window.plugin.muScraper.cellsLayerGroup.clearLayers();
		var bounds = window.map.getBounds();

		var seenCells = {};

		var drawCellAndNeighbors = function(cell) {
			// generating a list of cells to draw

			var cellStr = cell.toId();


			if (!seenCells[cellStr]) {
				// cell not visited - flag it as visited now
				seenCells[cellStr] = cell;

				// is it on the screen?
				var corners = cell.getCornerLatLngs();
				var cellBounds = window.L.latLngBounds([corners[0],corners[1]]).extend(corners[2]).extend(corners[3]);

				if (cellBounds.intersects(bounds)) {
					// on screen - draw it
					//  window.plugin.muScraper.drawCell(cell);

					// and recurse to our neighbors
					var neighbors = cell.getNeighbors();
					for (var i=0; i<neighbors.length; i++) {
						drawCellAndNeighbors(neighbors[i]);
					}
				}
			}

		};

		// centre cell
		var zoom = window.map.getZoom();
		if (zoom >= 11) {
			var cell = window.S2.S2Cell.FromLatLng ( window.map.getCenter(), 13 );
			drawCellAndNeighbors(cell);
			// can do mu request pre-caching here.
			var reqMu={};
			for (var scell in seenCells) if (!window.plugin.muScraper.cellMu[scell]) reqMu[scell]=true;

			if (window.plugin.muScraper.muSubmit) {
				var celllist = JSON.stringify(Object.keys(reqMu));
				//console.log(celllist);
				$.post(window.plugin.muScraper.mu_url, {apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname, mu:celllist},
					   function(dd) {
					//console.log(data);
					//var dd = JSON.parse(data);
					var cell;
					for (cell in dd) window.plugin.muScraper.cellMu[cell] = dd[cell];
					for (var cellid in seenCells)
					{
						//console.log("cell: " + cellid);
						window.plugin.muScraper.drawCell(seenCells[cellid]);
					}
				});
			}
		}
	},
	drawCell: function(cell) {

		//TODO: move to function - then call for all cells on screen

		// corner points
		var corners = cell.getCornerLatLngs();

		// center point
		var center = cell.getLatLng();

		// DEBUG name
		var name = "";
		var marker;
		if (window.map.getZoom() >= 11)
		{
			name = cell.toId();
			// get mu and set the cell name
			var murange = window.plugin.muScraper.cellMu[name];
			//console.log(murange);
			var mus="undefined";
			var fc="#000000";
			if (murange) {
				//console.log(murange[0] + "..." + murange[1]);
				var mean = (murange[0] + murange[1]) / 2.0;
				var error = ((murange[1] - murange[0]) / 2.0) / mean * 100.0;
				mus = mean.toFixed(2) + " mu/km<br/>\u00b1" + error.toFixed(2)+ "%";
				fc = window.plugin.muScraper.hsvtorgb(mean/3600.0, 1.0 - error/100.0, 1.0 - error/100.0);
			}
			name += "<br/>" + mus;
			marker = window.L.marker(center, {
				icon: window.L.divIcon({
					className: 'plugin-regions-name',
					iconAnchor: [100,5],
					iconSize: [200,10],
					html: name,
				})
			});
			//console.log(data);
			if (window.map.getZoom() >= 14) window.plugin.muScraper.cellsLayerGroup.addLayer(marker);
			var region = window.L.geodesicPolyline([corners[0],corners[1],corners[2],corners[3]], {fill: true, opacity: 0, fillColor: fc , fillOpacity: 0.5, clickable: false });
			window.plugin.muScraper.cellsLayerGroup.addLayer(region);
		} else {
			marker = window.L.marker(center, {
				icon: window.L.divIcon({
					className: 'plugin-regions-name',
					iconAnchor: [100,5],
					iconSize: [200,10],
					html: name,
				})
			});
			window.plugin.muScraper.cellsLayerGroup.addLayer(marker);
		}
		if (window.map.getZoom() >= 12) {
			region = window.L.geodesicPolyline([corners[0],corners[1],corners[2]], {fill: false, color: 'white', opacity: 0.5, dashArray: [4,2], weight: 1, clickable: false });
			window.plugin.muScraper.cellsLayerGroup.addLayer(region);
		}
	}
};

//window.bootPlugins.push(window.plugin.dbPortalGrabber.setup);
//window.bootPlugins.push(window.plugin.dbEdgeGrabber.setup);
//window.bootPlugins.push(window.plugin.muScraper.setup);
window.plugin.dbPortalGrabber.setup();
window.plugin.dbEdgeGrabber.setup();
window.plugin.muScraper.setup();

<%
} catch (Exception e) {
%>
alert("<%= e.getMessage() %>");
<% } %>

