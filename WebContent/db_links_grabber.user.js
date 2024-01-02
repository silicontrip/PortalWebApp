
	window.plugin.dbEdgeGrabber = {
		hz_url: "https://quadrant.silicontrip.net/portalApi/submitEntity",
		hz_get_url: "https://quadrant.silicontrip.net/portalApi/getLinks",
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
			console.log('edgeGrabber - setup');
			window.addHook('linkAdded', window.plugin.dbEdgeGrabber.edgeAdded);
			window.addHook('mapDataRefreshEnd', window.plugin.dbEdgeGrabber.pushedges);
			window.addHook('mapDataRefreshStart', window.plugin.dbEdgeGrabber.mapDataRefreshStart);
			window.$('#sidebar').append('<div id="hz_edge_grabber" style="padding: 5px; font-size: 12px">'+
				'<strong>Edges: </strong> '+
				'(<span id="hz_edges">0</span>)'+
				'</div>'
			);
		},
		edgeAdded: function(data) {
			var edge = data.link.options.data;
			edge.guid = data.link.options.guid;
			window.plugin.dbEdgeGrabber.hz_edges.push(edge);
			window.$('#hz_edges').html(window.plugin.dbEdgeGrabber.hz_edges.length);
		},
		mapDataRefreshStart: function() {
			window.$('#hz_edge_grabber').css('background-color', '');
			var bounds = window.map.getBounds();
            var alat = bounds._southWest.lat;
            var alng = bounds._southWest.lng;
            var blat = bounds._northEast.lat;
            var blng = bounds._northEast.lng;

            var query = "?ll=" + alat + "," + alng +"&l2=" + blat + "," + blng + "&agent=" + window.PLAYER.nickname ;
			var geturl = window.plugin.dbEdgeGrabber.hz_get_url+query;
            //console.log(geturl);

            window.$.get(geturl, window.plugin.dbEdgeGrabber.loadJSON);
		},
		loadJSON: function (json) {
			window.plugin.dbEdgeGrabber.dbLinks = json;
		},
		getKey: function() {
			var edgegrabber_key = localStorage.getItem('net.silicontrip.ingress.apikey');
			console.log("edgeGrabber - Getting key: " + edgegrabber_key);
			return edgegrabber_key;
		},
		pushedges: function() {
			if(!window.plugin.dbEdgeGrabber.hz_edges.length)
			{
				window.$('#hz_edge_grabber').css('background-color', 'green');
				return;
			}

			console.log('edgeGrabber - Ensuring we have a key');
			var edgegrabber_key = window.plugin.dbEdgeGrabber.getKey();
			if(edgegrabber_key == 'null')
			{
				console.log('edgeGrabber - No API key defined, skipping submissions');
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
			console.log('edgeGrabber - Pushing edges - ' + window.plugin.dbEdgeGrabber.hz_edges.length);

            window.$.post(window.plugin.dbEdgeGrabber.hz_url, {apikey: edgegrabber_key,
                agent: window.PLAYER.nickname,
                edges: JSON.stringify(window.plugin.dbEdgeGrabber.hz_edges),
                edges_deleted: JSON.stringify(deleted_links)} ).fail(
                    function(xhr, status, error) {
                        alert("edgeGrabber - Invalid key used for request: " + JSON.stringify(xhr) +" " + status + " " + error );
                    });
			window.plugin.dbEdgeGrabber.hz_edges = [];
			window.$('#hz_edge_grabber').css('background-color', 'green');
			window.$('#hz_edges').html(window.plugin.dbEdgeGrabber.hz_edges.length);
		}
		};

	//window.plugin.entityGrabber.setupCSS();
	var setup = plugin.dbEdgeGrabber.setup;

	setup.info = plugin_info; //add the script info data to the function as a property
	if(!window.bootPlugins) window.bootPlugins = [];
	window.bootPlugins.push(setup);
	// if IITC has already booted, immediately run the 'setup' function
	if(window.iitcLoaded && typeof setup === 'function') setup();
} // wrapper end
// inject code into site context
var script = document.createElement('script');
var info = {};
if (typeof GM_info !== 'undefined' && GM_info && GM_info.script) info.script = { version: GM_info.script.version, name: GM_info.script.name, description: GM_info.script.description };
script.appendChild(document.createTextNode('('+ wrapper +')('+JSON.stringify(info)+');'));
(document.body || document.head || document.documentElement).appendChild(script);
