# test_batch.py
import glob, requests, json

URL = "http://127.0.0.1:8000/analyze/photo"
H = {"X-Auth-Token": "dev-shared-token"}
imgs = sorted(glob.glob("samples/*.jpg"))[:10]

for i, path in enumerate(imgs, 1):
    with open(path, "rb") as f:
        r = requests.post(URL, headers=H,
                          files={"image": f},
                          data={"poiKey": "POI_123", "sessionId": "SESSION_abc"},
                          timeout=20)
    print(i, path, r.status_code, json.dumps(r.json()))
