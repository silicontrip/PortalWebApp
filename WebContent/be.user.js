/* global GM_info */

// ==UserScript==
// @id             iitc-plugin-bovine-ethology-bootstrap@jonatkins
// @name           IITC plugin: Bovine Ethology Bootstrap
// @category       Layer
// @version        2.1.0.20170313.231000
// @namespace      https://github.com/jonatkins/ingress-intel-total-conversion
// @updateURL      https://bovine-ethology-87719.appspot.com/bootstrap/bovine-ethology-bootstrap.user.js
// @downloadURL    https://bovine-ethology-87719.appspot.com/bootstrap/bovine-ethology-bootstrap.user.js
// @description    Bovine Ethology Server Edition Bootstrap
// @include        https://*.ingress.com/intel*
// @include        https://*.ingress.com/mission/*
// @match          https://*.ingress.com/intel*
// @match          https://*.ingress.com/mission/*
// @grant          none
// ==/UserScript==


function wrapper(plugin_info) {
// ensure plugin framework is there, even if iitc is not yet loaded
    if (typeof window.plugin !== 'function')
        window.plugin = function () {};

//PLUGIN AUTHORS: writing a plugin outside of the IITC build environment? if so, delete these lines!!
//(leaving them in place might break the 'About IITC' page or break update checks)

//END PLUGIN AUTHORS NOTE



// PLUGIN START ////////////////////////////////////////////////////////


// use own namespace for plugin
    window.plugin.bovineEthology = function () {};
    window.plugin.bovineEthology.serverBase = "https://bovine-ethology-87719.appspot.com/";
    window.plugin.bovineEthology.bootstrap = {};
    window.plugin.bovineEthology.serverOptions = {};

    /* 
     This BovineEthologyLoaded hook will be run when be has finished loading from the server.
     It can be used via iitc's addHook function.
     
     To ensure the hook exists at the time addHook is executed you should use add the hook from an 'iitcLoaded' hook function.
     EG: 
     window.addHook('iitcLoaded', function(){
     window.addHook('BovineEthologyLoaded', function(){ console.log('Moooooooo')});
     });
     */


    window.plugin.bovineEthology.onLoad = function ()
    {
        window.plugin.bovineEthology.server.getOptions(window.plugin.bovineEthology.afterLoad);
    };

    window.plugin.bovineEthology.afterLoad = function ()
    {
        //only run hook if load has been successful
        if (window.plugin.bovineEthology.serverOptions.user !== undefined)
        {
            console.log('BovineEthology: Firing BovineEthologyLoaded hook');
            window.runHooks('BovineEthologyLoaded');
        }
    };

    window.plugin.bovineEthology.intelSignOutUrl = null;
    window.plugin.bovineEthology.signOut = function ()
    {
        if (window.plugin.bovineEthology.serverOptions.logoutUrl !== undefined)
        {
            window.open(window.plugin.bovineEthology.serverOptions.logoutUrl, '_blank');
        }
        document.location.href = window.plugin.bovineEthology.intelSignOutUrl;
    };

    window.plugin.bovineEthology.bootstrapSetup = function () {
        window.pluginCreateHook('BovineEthologyLoaded');
        //add a hook to load the acutal plugin after iitc has loaded.
        addHook('iitcLoaded', window.plugin.bovineEthology.onLoad);
        if (window.iitcLoaded)
            window.plugin.bovineEthology.onLoad();
    };

// BOVINE ETHOLOGY SERVER CODE /////////////////////////////////////////     

    window.plugin.bovineEthology.server = {};

    window.plugin.bovineEthology.server.getOptions = function (callback)
    {
        console.log('BovineEthology: Getting options from sever');
        var successCallback = function (data, textStatus, jqXHR)
        {
            console.log('BovineEthology: Get options complete');
            if (data.user !== undefined)
            {
                window.plugin.bovineEthology.serverOptions = data;
                if (data.logoutUrl !== undefined)
                {
                    //make sure we get logged out of BE when logged out of intel.
                    window.plugin.bovineEthology.intelSignOutUrl = document.getElementById("signout").href;//$('#signout').attr('href');
                    $('#signout').attr('href', 'javascript:window.plugin.bovineEthology.signOut();');
                }
                window.plugin.bovineEthology.server.loadCode(callback);
            } else
            {
                if (data.loginUrl !== undefined)
                {
                    console.log('BovineEthology: User is not signed in');
                    var html = "<p>Bovine Ethology requires you to login to access server services</p><br/>";
                    html += "<a href=\"" + data.loginUrl + "\" target=\"_blank\">Sign in</a>";
                    window.dialog({title: 'Login Required', modal: false, width: 'auto', id: 'Login', html: html});
                }
            }
            //if(callback!==undefined) callback();

        };
        var data = JSON.stringify(window.PLAYER);
        window.plugin.bovineEthology.server.postAjax("GetOptions", data, successCallback);
    };

    //to be executed after we know the user has been authenticated
    window.plugin.bovineEthology.server.loadCode = function (callback)
    {
        console.log('BovineEthology: Loading code from server');
        var successCallback = function (data, textStatus, jqXHR)
        {
            if (data.code !== undefined)
            {
                //I relise this isn't normally the done thing, but the point 
                //here is to restrict code to only that which is required
                //for the rights the user has.
                eval(data.code);
                console.log('BovineEthology: Loading code complete');
                if (callback !== undefined)
                    callback();
            }

        };
        window.plugin.bovineEthology.server.postAjax("LoadCode", "{}", successCallback);
    };
    window.plugin.bovineEthology.server.outstandingRequests = [];
    window.plugin.bovineEthology.server.failedRequestCount = 0;
    window.plugin.bovineEthology.server.addRequest = function (request)
    {
        window.plugin.bovineEthology.server.outstandingRequests.push(request);
        if (window.plugin.bovineEthology.status !== undefined)
            window.plugin.bovineEthology.status.updateStatus();
    };
    window.plugin.bovineEthology.server.removeRequest = function (request)
    {
        window.plugin.bovineEthology.server.outstandingRequests.splice(window.plugin.bovineEthology.server.outstandingRequests.indexOf(request), 1);
        if (window.plugin.bovineEthology.status !== undefined)
            window.plugin.bovineEthology.status.updateStatus();
    };

    window.plugin.bovineEthology.server.postAjax = function (action, data, successCallback, errorCallback)
    {
        var xhr = new XMLHttpRequest();
        
        xhr.onreadystatechange = function () {
            if(xhr.readyState === XMLHttpRequest.DONE) {
                if(xhr.status === 200)
                {
                    window.plugin.bovineEthology.server.removeRequest(xhr);
                
                    if (successCallback)
                        successCallback(xhr.response, xhr.statusText, xhr);
                }
                else
                {
                    window.plugin.bovineEthology.server.removeRequest(xhr);

                    window.plugin.bovineEthology.server.failedRequestCount++;

                    if (errorCallback) {
                        errorCallback(xhr, xhr.statusText, xhr.responseText);
                    }
                }
            }
        };
        
        xhr.onerror = function (evt) {
            window.plugin.bovineEthology.server.removeRequest(xhr);

            window.plugin.bovineEthology.server.failedRequestCount++;

            if (errorCallback) {
                errorCallback(xhr, xhr.statusText, evt.type);
            }
        };
        
        xhr.open("POST", window.plugin.bovineEthology.serverBase + action, true);
        xhr.responseType = "json";
        xhr.timeout = 60000;
        xhr.withCredentials = true;
        xhr.send(data);
        
        xhr.action = action;
        window.plugin.bovineEthology.server.addRequest(xhr);
        
        return xhr;
    };


// BOVINE ETHOLOGY SERVER END //////////////////////////////////////////         
    var setup = window.plugin.bovineEthology.bootstrapSetup;


// PLUGIN END //////////////////////////////////////////////////////////


    setup.info = plugin_info; //add the script info data to the function as a property
    if (!window.bootPlugins)
        window.bootPlugins = [];
    window.bootPlugins.push(setup);
// if IITC has already booted, immediately run the 'setup' function
    if (window.iitcLoaded && typeof setup === 'function')
        setup();
} // wrapper end
// inject code into site context
var script = document.createElement('script');
var info = {};
if (typeof GM_info !== 'undefined' && GM_info && GM_info.script)
    info.script = {version: GM_info.script.version, name: GM_info.script.name, description: GM_info.script.description};
script.appendChild(document.createTextNode('(' + wrapper + ')(' + JSON.stringify(info) + ');'));
(document.body || document.head || document.documentElement).appendChild(script);

