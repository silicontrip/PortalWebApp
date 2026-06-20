#!/usr/bin/env python3

import sys
import json
import requests

far=[]
with open(sys.argv[1]) as ff:
	for line in ff:
		fo = json.loads(line)
		far.append(fo)

fars = json.dumps(far)
payload={"agent": "silicontrip","apikey":"test123","fields":fars}

url="http://localhost:8080/portalApi/submitEntity"
res = requests.post(url,data=payload)

