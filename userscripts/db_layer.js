        window.plugin.dbLayer = {
                hz_layer_url: window.silicontrip_ingress_url+"layer",
		layer: null,
		setup: function()
		{
			console.log("db_layer::setup");
			window.plugin.dbLayer.layer = new window.L.LayerGroup();
                        window.addLayerGroup('DB Layer', window.plugin.dbLayer.layer, true);

		//	window.$('#toolbox').append('<a onclick="window.plugin.dbLayer.drawLayer();return false;" accesskey="d" >DB Draw Layer</a>');
		//	console.log("<<< db_layer::setup");
		},
		drawLayer: function()
		{
			//console.log("db_layer::drawLayer");
                       // window.$.get(window.plugin.dbLayer.hz_layer_url+"?ll=The+Nexus+Web&rr=1.8" , window.plugin.dbLayer.render);
                    //    window.$.get(window.plugin.dbLayer.hz_layer_url+"?ll=The+Nexus+Web&l2=Car+Sticker" , window.plugin.dbLayer.render);
                     //   window.$.get(window.plugin.dbLayer.hz_layer_url+"?ll=The+Nexus+Web&l2=Army+road+reserve&l3=Batterham+Park" , window.plugin.dbLayer.render);
		},
		render: function(draw)
		{
			//console.log("db_layer::render");
			//window.plugin.dbLayer.layer = new window.L.LayerGroup();
			for (var i in draw)
			{
				var leafobj = draw[i];
				//console.log(leafobj.type);
				if (leafobj.type === "circle")
				{
					window.L.circle(leafobj.latLng,leafobj.options.radius,leafobj.options).addTo(window.plugin.dbLayer.layer);
				}
				if (leafobj.type === "rectangle")
				{
					window.L.rectangle(leafobj.latLng, leafobj.options).addTo(window.plugin.dbLayer.layer);
				}
				if (leafobj.type === "polyline")
				{
					window.L.polyline(leafobj.latLng, leafobj.options).addTo(window.plugin.dbLayer.layer);
				}
				if (leafobj.type === "polygon")
				{
					window.L.polygon(leafobj.latLng, leafobj.options).addTo(window.plugin.dbLayer.layer);
				}
			}
		}
	};

window.plugin.dbLayer.setup();
