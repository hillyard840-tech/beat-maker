import urllib.request
import json
import os
import time

key = os.environ.get("GEMINI_API_KEY")
if not key:
    with open(".env.example", "r") as f:
        for line in f:
            if "GEMINI_API_KEY=" in line:
                key = line.split("=")[1].strip()
                break

if not key:
    print("NO KEY")
    exit(1)

url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key={key}"
data = {
  "contents": [{"parts":[{"text":"What is the latest score for the Golden State Warriors?"}],"role":"user"}],
  "tools": [{"googleSearch": {}}]
}
req = urllib.request.Request(url, data=json.dumps(data).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    with urllib.request.urlopen(req) as response:
        print(response.read().decode('utf-8'))
except urllib.error.HTTPError as e:
    print(f"ERROR {e.code}")
    print(e.read().decode('utf-8'))

