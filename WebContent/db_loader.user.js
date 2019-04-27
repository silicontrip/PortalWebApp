// ==UserScript==
// @id             iitc-plugin-db-loader@silicontrip
// @name           IITC Plugin: DB technology journey
// @category       layer
// @version        0.2.9
// @namespace      https://github.com/jonatkins/ingress-intel-total-conversion
// @description
// @updateURL      https://quadrant.silicontrip.net:8181/portalApi/db_loader.user.js
// @downloadURL    https://quadrant.silicontrip.net:8181/portalApi/db_loader.user.js
// @include        https://*.ingress.com/intel*
// @include        http://*.ingress.com/intel*
// @match          https://*.ingress.com/intel*
// @match          http://*.ingress.com/intel*
// @grant          none
// ==/UserScript==

function wrapper(plugin_info) {
	// ensure plugin framework is there, even if iitc is not yet loaded
	if(typeof window.plugin !== 'function') window.plugin = function() {};

	// use own namespace for plugin
	window.plugin.dbLoader = {
		hz_loader_url: "https://quadrant.silicontrip.net:8181/portalApi/loader",
		setup: function() {
			console.log('dbLoader::setup');

			window.PLAYER.apikey = window.plugin.dbLoader.getKey();
			var keyMessage="Update Key";
			if (window.PLAYER.apikey == null) {
				keyMessage="Enter Your API Key";
			}

			window.$('#sidebar').append('<div id="hz_portal_grabber" style="padding: 5px; font-size: 12px">'+
			'<a onclick="window.plugin.dbLoader.addKey();return false;">'+keyMessage+'</a></div>');
            console.log("Link Possible API Key :"+ window.PLAYER.apikey);
			if (window.PLAYER.apikey !== null) {
                console.log("Establishing Portal link.");
				window.$.post(window.plugin.dbLoader.hz_loader_url,
					{ apikey: window.PLAYER.apikey, agent: window.PLAYER.nickname }, window.plugin.dbLoader.loader).fail(window.plugin.dbLoader.fail);
			}
		},
	addKey: function()
	{
		var portalgrabber_key = prompt('Technology Journey - Access key');
		if(portalgrabber_key != null)
		{
			localStorage.setItem('net.silicontrip.ingress.apikey', portalgrabber_key);
			console.log("portalGrabber - Setting key: " + portalgrabber_key);
			window.$("#hz_portal_grabber").html('<a onclick="window.plugin.dbLoader.addKey();return false;">Update Key</a>');
		}
	},
	getKey: function()
	{
		var portalgrabber_key = localStorage.getItem('net.silicontrip.ingress.apikey');
		console.log("Hacking Portal - Portal Key Acquired: " + portalgrabber_key);
		return portalgrabber_key;
	},
	loader: function(script_text)
	{
        console.log("Portal Link Established.");
		//var script = document.createElement('script');
		//script.appendChild(document.createTextNode(script_text));
        //console.log(document.body);
        //console.log(document.head);
        //console.log(document.documentElement);
		//(document.body || document.head || document.documentElement).appendChild(script);
	},
		fail: function(xhr,status,error)
		{
            console.log("Portal Link Failed.");
            alert ("Unable to establish Portal Link.");
			console.log("FAIL");
			console.log(xhr);
			console.log(status);
			console.log(error);
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

script.appendChild(document.createTextNode('('+ wrapper +')();'));
(document.body || document.head || document.documentElement).appendChild(script);



