import requests, json
from pathlib import Path

n = 18
URL = "http://127.0.0.1:8000/analyze/photo"
H = {"X-Auth-Token": "dev-shared-token"}

SAMPLES_DIR = Path(__file__).parent / "samples"
imgs = sorted(SAMPLES_DIR.glob("*.jpg"))[:n]  # <-- 항상 app/tests/samples/*.jpg

for i, path in enumerate(imgs, 1):
    with open(path, "rb") as f:
        r = requests.post(URL, headers=H,
                          files={"image": f},
                          data={"poiKey": "POI_123", "sessionId": "SESSION_abc"},
                          timeout=100)
    print(i, path, r.status_code, json.dumps(r.json()))
