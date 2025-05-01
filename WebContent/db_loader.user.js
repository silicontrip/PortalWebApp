// ==UserScript==
// @id             iitc-plugin-db-loader@silicontrip
// @name           IITC Plugin: DB technology journey
// @category       layer
// @version        0.1.2
// @namespace      https://github.com/jonatkins/ingress-intel-total-conversion
// @description
// @updateURL      https://quadrant.silicontrip.net:8181/portalApi/db_loader.user.js
// @downloadURL    https://quadrant.silicontrip.net:8181/portalApi/db_loader.user.js
// @include        https://intel.ingress.com/*
// @include        http://intel.ingress.com/*
// @match          https://intel.ingress.com/*
// @match          http://intel.ingress.com/*
// @grant          none
// ==/UserScript==

function wrapper(plugin_info) {

	// ensure plugin framework is there, even if iitc is not yet loaded
	if(typeof window.plugin !== 'function') window.plugin = function() {};

	//PLUGIN AUTHORS: writing a plugin outside of the IITC build environment? if so, delete these lines!!
	//(leaving them in place might break the 'About IITC' page or break update checks)
	plugin_info.buildName = 'hrabbit';
	plugin_info.dateTimeVersion = '20140925.0037';
	plugin_info.pluginId = 'db_loader';
	//END PLUGIN AUTHORS NOTE

	// use own namespace for plugin

	window.plugin.dbLoader = {
		hz_loader_url: "http://localhost:8080/portalApi/loader",
        alertLock: false,
		setupCSS: function() {
			window.$("<style>").prop("type", "text/css").html(''+
				'#hz_portal_grabber a {'+
				'margin-left: 5px;'+
				'margin-right: 5px;'+
				'white-space: nowrap;'+
				'display: inline-block;'+
				'}'
			).appendTo("head");
		},
		setup: function() {
			console.log('dbLoader::setup');

            window.PLAYER.apikey = window.plugin.dbLoader.getKey();
            var keyMessage="Update Key";
            var hasKey = true;
            if (window.PLAYER.apikey == null) {
                keyMessage="Enter Your API Key";
                hasKey=false;
            }

			window.$('#sidebar').append('<div id="hz_portal_grabber" style="padding: 5px; font-size: 12px">'+
			'<a onclick="window.plugin.dbLoader.addKey();return false;">'+keyMessage+'</a>'+
			'</div>'
			);
            if (hasKey) {
                window.$.post(window.plugin.dbLoader.hz_loader_url, { apikey: window.plugin.dbLoader.getKey(), agent: window.PLAYER.nickname }, window.plugin.dbLoader.loader).fail(window.plugin.dbLoader.fail);
            }
		},
	addKey: function()
	{
		var portalgrabber_key = prompt('Technology Journey - Access key');
		if(portalgrabber_key != null)
		{
			localStorage.setItem('net.silicontrip.ingress.apikey', portalgrabber_key);
			console.log("portalGrabber - Setting key: " + portalgrabber_key);
            // change sidebar key message
            window.$("#hz_portal_grabber").html('<a onclick="window.plugin.dbLoader.addKey();return false;">Update Key</a>');
		}
	},
	getKey: function()
	{
		var portalgrabber_key = localStorage.getItem('net.silicontrip.ingress.apikey');
		console.log("portalGrabber - Getting key: " + portalgrabber_key);
		return portalgrabber_key;
	},
    loader: function(script)
    {
        eval(script); // surely there's a better way
    },
        fail: function(xhr,status,error)
        {
            console.log("FAIL");
            console.log(xhr);
            console.log(status);
            //console.log(error);

        //    alert ("FAIL: " + JSON.stringify(xhr) + " " + status + " " + error);
        }
};


	var setup = window.plugin.dbLoader.setup;

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

