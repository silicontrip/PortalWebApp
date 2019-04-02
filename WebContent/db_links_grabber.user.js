// ==UserScript==
// @id             iitc-plugin-edges-grabber@silicontrip
// @name           IITC Plugin: db edge grabber
// @category       Chat
// @version        0.0.2
// @namespace      https://github.com/jonatkins/ingress-intel-total-conversion
// @description    Capture edges.
// @updateURL      https://quadrant.silicontrip.net:8181/portalApi/db_link_grabber.user.js
// @downloadURL    https://quadrant.silicontrip.net:8181/portalApi/db_link_grabber.user.js
// @include        https://*.ingress.com/intel*
// @include        http://*.ingress.com/intel*
// @match          https://*.ingress.com/intel*
// @match          http://*.ingress.com/intel*
// @grant          none
// ==/UserScript==

/*
 * CHANGELOG
 *
 * 0.0.1
 * 	- Initial
 */

function wrapper(plugin_info) {

    
    // ensure plugin framework is there, even if iitc is not yet loaded
    if(typeof window.plugin !== 'function') window.plugin = function() {};
        
    //PLUGIN AUTHORS: writing a plugin outside of the IITC build environment? if so, delete these lines!!
    //(leaving them in place might break the 'About IITC' page or break update checks)
    plugin_info.buildName = 'hrabbit';
    plugin_info.dateTimeVersion = '20140925.0037';
    plugin_info.pluginId = 'edgeGrabber';
    //END PLUGIN AUTHORS NOTE
    
    // use own namespace for plugin
    window.plugin.dbEdgeGrabber = {
		hz_url: "https://quadrant.silicontrip.net:8181/portalApi/submitEntity",
		hz_get_url: "https://quadrant.silicontrip.net:8181/portalApi/getLinks",
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
			if(!window.plugin.dbPortalGrabber)
			{
				console.log('This plugin requires portal grabber in order to work');
				alert('edge Grabber requires portal Grabber');
				return;
			}
        
			window.addHook('linkAdded', window.plugin.dbEdgeGrabber.edgeAdded);
			window.addHook('mapDataRefreshEnd', window.plugin.dbEdgeGrabber.pushedges);
			window.addHook('mapDataRefreshStart', window.plugin.dbEdgeGrabber.mapDataRefreshStart);
        
			window.$('#sidebar').append('<div id="hz_edge_grabber" style="padding: 5px; font-size: 12px">'
				+'<strong>Edges: </strong> '
				//+'<a onclick="window.plugin.edgeGrabber.addKey();return false;">Update key</a>'
				+' (<span id="hz_edges">0</span>)'
				//+' <a onclick="window.plugin.edgeGrabber.pushedges();return false;">Push edges</a>, '
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

                        window.$.get(geturl, window.plugin.dbEdgeGrabber.loadJSON);
		},
		loadJSON: function (json) {
			window.plugin.dbEdgeGrabber.dbLinks = json;
		},
     
    /*
    window.plugin.edgeGrabber.addKey = function()
    {
        var edgegrabber_key = prompt('edgeGrabber - Access key');
        if(edgegrabber_key != null)
        {
		localStorage.setItem('edgegrabber_key', edgegrabber_key);
    		console.log("edgeGrabber - Setting key: " + edgegrabber_key);
        }
    };
    */
    
		getKey: function() {                 
			var edgegrabber_key = localStorage.getItem('portalgrabber_key');
			console.log("edgeGrabber - Getting key: " + edgegrabber_key);
			return edgegrabber_key;
		},
		pushedges: function() 
		{
			var hz_edge_min_zoom = 1;
        
			//console.log('edgeGrabber - Restricting zoom to no less than ' +  hz_edge_min_zoom);
        
			if(window.map.getZoom() < hz_edge_min_zoom)
			{
				console.log('Zoom out of range, not submitting');
				window.plugin.dbEdgeGrabber.hz_edges = [];
				window.$('#hz_edge_grabber').css('background-color', 'blue');
				window.$('#hz_edges').html(window.plugin.dbEdgeGrabber.hz_edges.length);
				return;
			}
        
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

			window.$.post(window.plugin.dbEdgeGrabber.hz_url, {apikey: edgegrabber_key, agent: window.PLAYER.nickname, edges: JSON.stringify(window.plugin.dbEdgeGrabber.hz_edges), edges_deleted: JSON.stringify(deleted_links)} )
				.fail(function(xhr, status, error) {
					alert("edgeGrabber - Invalid key used for request: " + JSON.stringify(xhr) +" / " + status + " / " + error );
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


