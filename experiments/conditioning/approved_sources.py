"""Require a recorded dataset adoption decision before fitting Anime weights."""

import json
from pathlib import Path


SOURCE_REGISTER = Path(__file__).resolve().parents[1] / "datasets" / "anime_training_sources.json"


def check_training_sources(records, competency, register=SOURCE_REGISTER):
    decisions = json.loads(register.read_text(encoding="utf-8"))
    if decisions.get("version") != 1:
        raise ValueError("Unsupported Anime training source register")
    approved_ids = set()
    for record in records:
        dataset = record.get("dataset_id")
        source = decisions["datasets"].get(dataset, {})
        if (
            source.get("status") != "approved"
            or not source.get("owner_decision", "").strip()
            or competency not in source.get("competencies", [])
        ):
            raise ValueError(f"Dataset {dataset or '(unrecorded)'} has no owner approval for {competency}")
        approved_ids.add(dataset)
    return sorted(approved_ids)
