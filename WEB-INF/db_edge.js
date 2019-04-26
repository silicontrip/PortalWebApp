/* global window */
	window.plugin.dbEdgeGrabber = {
		hz_url: window.silicontrip_ingress_url+"submitEntity",
		hz_get_url: window.silicontrip_ingress_url+"getLinks",
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

window.plugin.dbEdgeGrabber.setup();
