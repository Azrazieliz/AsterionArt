"""Preview and verify the exact image corpora used by Anime fitting."""

import argparse
from hashlib import sha256
import json
from pathlib import Path
import re


SOURCE_REGISTER = Path(__file__).resolve().parents[1] / "datasets" / "anime_training_sources.json"


def review_training_sources(records):
    corpora, seen = {}, set()
    for record in records:
        dataset, content = record.get("dataset_id"), record.get("sha256")
        if not isinstance(dataset, str) or not dataset.strip():
            raise ValueError("Every image needs a recorded dataset")
        if not isinstance(content, str) or not re.fullmatch("[0-9a-f]{64}", content):
            raise ValueError(f"Every image in {dataset} needs its content SHA-256")
        if content in seen:
            raise ValueError("A selected image appears more than once")
        seen.add(content)
        corpora.setdefault(dataset, []).append(content)
    if not corpora:
        raise ValueError("The proposed training corpus is empty")
    review = {}
    for dataset, contents in sorted(corpora.items()):
        digest = sha256()
        for content in sorted(contents):
            digest.update(bytes.fromhex(content))
        review[dataset] = {"images": len(contents), "image_set_sha256": digest.hexdigest()}
    return review


def check_training_sources(records, competency, register=SOURCE_REGISTER, approved_population=None):
    decisions = json.loads(register.read_text(encoding="utf-8"))
    if decisions.get("version") != 1:
        raise ValueError("Unsupported Anime training source register")
    checked = {}
    for record in records:
        dataset = record.get("dataset_id")
        if dataset in checked:
            continue
        source = decisions["datasets"].get(dataset, {})
        if (
            source.get("status") != "approved"
            or not source.get("owner_decision", "").strip()
            or competency not in source.get("competencies", [])
        ):
            raise ValueError(f"Dataset {dataset or '(unrecorded)'} has no owner approval for {competency}")
        checked[dataset] = source.get("image_sets", {}).get(competency)
    selection = review_training_sources(records)
    population = selection
    if approved_population is not None:
        population = review_training_sources(approved_population)
    for dataset, signature in checked.items():
        if population.get(dataset, {}).get("image_set_sha256") != signature or signature is None:
            raise ValueError(f"The exact image set from {dataset} is awaiting confirmation for {competency}")
    if approved_population is not None:
        members = {(row["dataset_id"], row["sha256"]) for row in approved_population}
        if any((row["dataset_id"], row["sha256"]) not in members for row in records):
            raise ValueError("The selection includes an image outside the confirmed population")
    return selection


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    args = parser.parse_args()
    records = json.loads(args.manifest.read_text(encoding="utf-8"))
    print(json.dumps({"datasets": review_training_sources(records)}, indent=2))
