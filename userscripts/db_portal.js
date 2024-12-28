	window.plugin.dbPortalGrabber = {
		hz_submit_url: window.silicontrip_ingress_url+"submitEntity",
		hz_get_url: window.silicontrip_ingress_url+"getPortals",
		hz_portals: [],
		STROKE_STYLE: {
			stroke: true,
			opacity: 1,
			weight: 0.5,
			fill: true,
			fillColor: null, //same as color by default
			fillOpacity: 0.4,
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
		//	console.log (">>> portalAdded");

			//var portal = data.portal.options.data;
			var portal={};
			var elements =  Object.getOwnPropertyNames(data.portal.options.data);
			for (var ekey in elements)
			{
				var key = elements[ekey];
				// console.log( key + ": " + data.portal.options.data[key]);
				portal[key] = data.portal.options.data[key];
			}
			portal["guid"] = data.portal.options.guid;

			window.plugin.dbPortalGrabber.hz_portals.push(portal);
			window.$('#hz_portals').html(window.plugin.dbPortalGrabber.hz_portals.length);
			//console.log ("<<< portalAdded");
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
		chunkArray: function(array, size) {
			var chunks = [];
			for (var i = 0; i < array.length; i += size) {
				chunks.push(array.slice(i, i + size));
			}
			return chunks;
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
			console.log('portalGrabber - Pushing portals - ' + window.plugin.dbPortalGrabber.hz_portals.length);
			//console.log(window.plugin.dbPortalGrabber.hz_portals);

			// Define chunk size
			const chunkSize = 100; // You can adjust this value as needed

			// Chunk the edges and deleted_links arrays
			var portalChunks = window.plugin.dbPortalGrabber.chunkArray(window.plugin.dbPortalGrabber.hz_portals, chunkSize);
			var deletedPortalChunks = window.plugin.dbPortalGrabber.chunkArray(deleted_portals, chunkSize);

			for (let chunk=0; chunk < Math.max(portalChunks.length,deletedPortalChunks.length); chunk++)
			{
				let post_data = {
					apikey: window.PLAYER.apikey, 
					agent: window.PLAYER.nickname, 
				};
				if (chunk < portalChunks.length)
					post_data["portals"] = JSON.stringify(portalChunks[chunk]);
				else
					post_data["portals"] = "[]";

				if (chunk < deletedPortalChunks.length)
					post_data["portals_deleted"] = JSON.stringify(deletedPortalChunks[chunk]);
				else
					post_data["portals_deleted"] = "[]";
				
				console.log('portalGrabber - Pushing portals chunk - ' + chunk + " of " + Math.max(portalChunks.length,deletedPortalChunks.length));

				window.$.post(window.plugin.dbPortalGrabber.hz_submit_url, post_data ).fail(function(xhr, status, error) {
						alert("Technology Journey - error submitting portals");
						console.log(JSON.stringify(xhr) +" " + status + " " + error );
				});
			}
			window.plugin.dbPortalGrabber.hz_portals = [];
			window.$('#hz_portal_grabber').css('background-color', 'green');
			window.$('#hz_portals').html(window.plugin.dbPortalGrabber.hz_portals.length);
		}
	};

window.plugin.dbPortalGrabber.setup();
