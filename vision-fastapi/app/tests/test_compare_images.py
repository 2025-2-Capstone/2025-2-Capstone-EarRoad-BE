from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence

import requests

from app.config import settings

URL = "http://127.0.0.1:8000/analyze/photo"
HEADERS = {"X-Auth-Token": "dev-shared-token"}
POI_KEY = "POI_DEMO"

SAMPLES_DIR = Path(__file__).parent / "samples"

CHANGE_COLOR_WEIGHT = 0.25
CHANGE_WARM_WEIGHT = 0.15
CHANGE_NEW_OBJECT_WEIGHT = 0.1
CHANGE_CONF_WEIGHT = 0.05


@dataclass
class AnalysisSnapshot:
    """Thin wrapper around the response payload we care about."""

    path: Path
    payload: Dict[str, object]

    @property
    def score(self) -> float:
        return float(self.payload.get("score", 0.0))

    @property
    def objects(self) -> Sequence[Dict[str, object]]:
        raw = self.payload.get("objects") or []
        return raw if isinstance(raw, Sequence) else []


def _numeric_session_photos(prefix: str) -> List[Path]:
    """Return sample paths like 01_1.jpg, 02_2.jpg while skipping blur/finger."""

    def is_numeric_variant(path: Path) -> bool:
        parts = path.stem.split("_")
        return len(parts) >= 2 and parts[1].isdigit()

    return sorted(
        (p for p in SAMPLES_DIR.glob(f"{prefix}_*.jpg") if is_numeric_variant(p)),
        key=lambda p: int(p.stem.split("_")[1]),
    )


def _post_image(path: Path, session_id: str) -> AnalysisSnapshot:
    with open(path, "rb") as f:
        response = requests.post(
            URL,
            headers=HEADERS,
            files={"image": f},
            data={"poiKey": POI_KEY, "sessionId": session_id},
            timeout=100,
        )
    response.raise_for_status()
    return AnalysisSnapshot(path=path, payload=response.json())

def _clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def _object_labels(snapshot: AnalysisSnapshot) -> Dict[str, float]:
    labels: Dict[str, float] = {}
    for obj in snapshot.objects:
        label = obj.get("label")
        confidence = obj.get("confidence", 0.0)
        if isinstance(label, str):
            labels[label] = float(confidence)
    return labels


def compute_change_bonus(
        *,
        previous: AnalysisSnapshot,
        current: AnalysisSnapshot,
) -> float:
    """Return an additive weight favouring noticeable scenic changes."""

    color_prev = float(previous.payload.get("colorfulness", 0.0))
    color_curr = float(current.payload.get("colorfulness", 0.0))
    warm_prev = float(previous.payload.get("warmRatio", 0.0))
    warm_curr = float(current.payload.get("warmRatio", 0.0))

    color_norm = 0.0
    if settings.SCORE_COLORFULNESS_MAX > 0:
        color_norm = _clamp01(abs(color_curr - color_prev) / settings.SCORE_COLORFULNESS_MAX)

    warm_norm = 0.0
    if settings.SCORE_WARM_TOLERANCE > 0:
        warm_norm = _clamp01(abs(warm_curr - warm_prev) / settings.SCORE_WARM_TOLERANCE)

    prev_objects = _object_labels(previous)
    curr_objects = _object_labels(current)
    new_labels = set(curr_objects) - set(prev_objects)

    shared_conf_gain = sum(
        max(0.0, curr_objects[label] - prev_objects.get(label, 0.0))
        for label in curr_objects.keys() & prev_objects.keys()
    )

    bonus = (
            CHANGE_COLOR_WEIGHT * color_norm
            + CHANGE_WARM_WEIGHT * warm_norm
            + CHANGE_NEW_OBJECT_WEIGHT * len(new_labels)
            + CHANGE_CONF_WEIGHT * shared_conf_gain
    )
    return round(bonus, 6)


def run_session(
        *,
        prefix: str,
        session_id: str,
        reference: Optional[AnalysisSnapshot] = None,
) -> List[AnalysisSnapshot]:
    snapshots: List[AnalysisSnapshot] = []
    for image_path in _numeric_session_photos(prefix):
        snapshot = _post_image(image_path, session_id)
        if reference is not None:
            bonus = compute_change_bonus(previous=reference, current=snapshot)
            snapshot.payload["changeBonus"] = bonus
            snapshot.payload["adjustedScore"] = snapshot.score + bonus
        else:
            snapshot.payload["changeBonus"] = 0.0
            snapshot.payload["adjustedScore"] = snapshot.score
        snapshots.append(snapshot)
        print(
            json.dumps(
                {
                    "session": session_id,
                    "file": image_path.name,
                    "score": snapshot.score,
                    "changeBonus": snapshot.payload["changeBonus"],
                    "adjustedScore": snapshot.payload["adjustedScore"],
                }
            )
        )
    return snapshots

def _select_best(snapshots: Iterable[AnalysisSnapshot], *, use_adjusted: bool = False) -> Optional[AnalysisSnapshot]:
    key = (lambda snap: snap.payload.get("adjustedScore", snap.score)) if use_adjusted else (lambda snap: snap.score)
    try:
        return max(snapshots, key=key)
    except ValueError:
        return None


def main() -> None:
    first_session = run_session(prefix="01", session_id="SESSION_INITIAL")
    baseline = _select_best(first_session)
    if baseline is None:
        print("No baseline photo found for prefix 01")
        return

    print("Baseline selected:", baseline.path.name, "score=", baseline.score)

    second_session = run_session(
        prefix="02",
        session_id="SESSION_FOLLOWUP",
        reference=baseline,
    )
    best_followup = _select_best(second_session, use_adjusted=True)
    if best_followup is not None:
        print(
            "Follow-up selection:",
            best_followup.path.name,
            "base=",
            best_followup.score,
            "bonus=",
            best_followup.payload.get("changeBonus"),
            "adjusted=",
            best_followup.payload.get("adjustedScore"),
        )


if __name__ == "__main__":
    main()

