// ==UserScript==
// @id             iitc-plugin-db-portal-grabber@silicontrip
// @name           IITC Plugin: DB Portal grabber
// @category       Misc
// @version        0.1.60
// @namespace      https://github.com/jonatkins/ingress-intel-total-conversion
// @description    Capture Portals.
// @updateURL      https://quadrant.silicontrip.net:8181/portalApi/db_portal_grabber.user.js
// @downloadURL    https://quadrant.silicontrip.net:8181/portalApi/db_portal_grabber.user.js
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
 * 0.0.2
 * 	- Moved to use portalAdd se we get every portal on every layer. :)
 * 0.0.3
 * 	- Added array length to sidebar so we can see that it's working.
 * 0.0.4
 * 	- Added check to ensure there is an array of portals before pushing
 * 	- Added a green bar to show that the portals have been pushed
 * 	- Removed green bar when a new refresh has been started
 * 0.0.5
 * 	- Set green bar even when no portals are found
 * 0.1.13 
 *      - Merged with DB portal display
 */

function wrapper(plugin_info) {
    
    // ensure plugin framework is there, even if iitc is not yet loaded
    if(typeof window.plugin !== 'function') window.plugin = function() {};
        
    //PLUGIN AUTHORS: writing a plugin outside of the IITC build environment? if so, delete these lines!!
    //(leaving them in place might break the 'About IITC' page or break update checks)
    plugin_info.buildName = 'hrabbit';
    plugin_info.dateTimeVersion = '20140925.0037';
    plugin_info.pluginId = 'dbPortalGrabber';
    //END PLUGIN AUTHORS NOTE
    
    // use own namespace for plugin
    //window.plugin.portalGrabber = function() {};

    window.plugin.dbPortalGrabber = {
        hz_submit_url: "https://quadrant.silicontrip.net:8181/portalApi/submitEntity",
        hz_get_url: "https://quadrant.silicontrip.net:8181/portalApi/getPortals",
        hz_portals: [],
		STROKE_STYLE: {
			stroke: true,
			opacity: 1,
			weight: 1.5,
			fill: false,
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
        
			window.plugin.dbPortalGrabber.layer = new window.L.LayerGroup();
            window.addLayerGroup('DB Portals', window.plugin.dbPortalGrabber.layer, true);
			window.plugin.dbPortalGrabber.STROKE_STYLE.color = localStorage.getItem("window.plugin.dbPortalGrabber.STROKE_STYLE.color");

            window.addHook('portalAdded', window.plugin.dbPortalGrabber.portalAdded);
            window.addHook('mapDataRefreshEnd', window.plugin.dbPortalGrabber.pushPortals);
            window.addHook('mapDataRefreshStart', window.plugin.dbPortalGrabber.mapDataRefreshStart);
            // want to catch enable db portals layer. perform redraw.
        
            window.$('#toolbox').append('<a onclick="window.plugin.dbPortalGrabber.portalColour();return false;" accesskey="C" >DB Colour</a>');
        
            window.$('#sidebar').append('<div id="hz_portal_grabber" style="padding: 5px; font-size: 12px">'
		    +'<strong>Portal post</strong> '
		    +'<a onclick="window.plugin.dbPortalGrabber.addKey();return false;">Update key</a>'
		    +' (<span id="hz_portals">0</span>)'
		    //+' <a onclick="window.plugin.portalGrabber.pushPortals();return false;">Push Portals</a>, '
		    +'</div>'
	        );
        },

        portalAdded: function(data)
        {
            var portal = data.portal.options.data;
            portal.guid = data.portal.options.guid;
                
            window.plugin.dbPortalGrabber.hz_portals.push(portal);
            window.$('#hz_portals').html(window.plugin.dbPortalGrabber.hz_portals.length);
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
			//console.log("event:");
			//console.log(evt);
			window.plugin.dbPortalGrabber.STROKE_STYLE.color = evt.srcElement.value;
			localStorage.setItem("window.plugin.dbPortalGrabber.STROKE_STYLE.color", window.plugin.dbPortalGrabber.STROKE_STYLE.color);
			window.plugin.dbPortalGrabber.portalLayer = {};
			window.plugin.dbPortalGrabber.drawPortals();
		},
        mapDataRefreshStart: function()
        {
                        console.log('dbPortalGrabber::mapDataRefreshStart');

            window.$('#hz_portal_grabber').css('background-color', '');
            if (!window.map.hasLayer(window.plugin.dbPortalGrabber.layer)) {
                return;
            }
                
			var bounds = window.map.getBounds();
			//console.log("Map Bounds");
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
            console.log("dbPortalGrabber::loadJSONPortals");
			window.plugin.dbPortalGrabber.dbPortals = dbPortals;
			window.plugin.dbPortalGrabber.drawPortals();
		},
		drawPortals: function(){
            console.log("dbPortalGrabber::drawPortals");

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
			//console.log(window.plugin.dbPortals.layer);
		},
    addKey: function()
    {
        var portalgrabber_key = prompt('portalGrabber - Access key');
        if(portalgrabber_key != null)
        {
		localStorage.setItem('window.plugin.dbPortalGrabber.portalgrabber_key', portalgrabber_key);
    		console.log("portalGrabber - Setting key: " + portalgrabber_key);
        }
    },
    getKey: function()
    {                 
	var portalgrabber_key = localStorage.getItem('window.plugin.dbPortalGrabber.portalgrabber_key');
        console.log("portalGrabber - Getting key: " + portalgrabber_key);
        return portalgrabber_key;
    },
    pushPortals: function() 
    {
        if(!window.plugin.dbPortalGrabber.hz_portals.length)
        {
            window.$('#hz_portal_grabber').css('background-color', 'green');
            return;
        }
        
        console.log('portalGrabber - Ensuring we have a key');
        var portalgrabber_key = window.plugin.dbPortalGrabber.getKey();
        if(portalgrabber_key == 'null')
        {
            console.log('dbPortalGrabber::pushPortals - No API key defined, skipping submissions');
            return;
        }
        // identify deleted portals
        // check we are zoom 15 or higher
        var deleted_portals =[];
	console.log("db_portal_grabber::pushPortals map zoom: "+window.map.getZoom());
        if (window.map.getZoom() >= 15)
        {
            for (var guid in window.plugin.dbPortalGrabber.dbPortals)
            {
                // does this guid exist?
                if (!window.portals.hasOwnProperty(guid)) //no?
                { deleted_portals.push({"delete": guid}); }
            }
            // add to deleted array
		console.log("db_portal_grabber::pushPortals deleted_portals: " +deleted_portals.length);
        }
		window.$('#hz_portals').html('Pushing.. '+window.plugin.dbPortalGrabber.hz_portals.length);
        console.log('portalGrabber - Pushing portals - ' + window.plugin.dbPortalGrabber.hz_portals.length);
        window.$.post(window.plugin.dbPortalGrabber.hz_submit_url, {apikey: portalgrabber_key, agent: window.PLAYER.nickname, portals: JSON.stringify(window.plugin.dbPortalGrabber.hz_portals), portals_deleted: JSON.stringify(deleted_portals)} )
            .fail(function() {
                alert("portalGrabber - Invalid key used for request");
        });
        window.plugin.dbPortalGrabber.hz_portals = [];
        window.$('#hz_portal_grabber').css('background-color', 'green');
        window.$('#hz_portals').html(window.plugin.dbPortalGrabber.hz_portals.length);
    }
};
    
    //window.plugin.entityGrabber.setupCSS();
    var setup = window.plugin.dbPortalGrabber.setup;
        
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


