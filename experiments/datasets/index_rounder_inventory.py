"""Index metadata from the existing private Control/Rounder export."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import sqlite3


SCHEMA = """
CREATE TABLE IF NOT EXISTS rounder_snapshot (
    scope TEXT PRIMARY KEY, generation INTEGER NOT NULL,
    row_count INTEGER NOT NULL, digest TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS rounder_artwork (
    scope TEXT NOT NULL, path TEXT NOT NULL, parent TEXT NOT NULL,
    size_bytes INTEGER NOT NULL, modified INTEGER NOT NULL, reported_md5 TEXT,
    PRIMARY KEY (scope, path)
);
"""


def export_identity(checkpoint):
    state = json.loads(checkpoint.read_text(encoding="utf-8"))
    root = state["root"]
    account = state["account_fingerprint"]
    generation = state["generation"]
    report = state.get("summary", {})
    count = report.get("entries")
    if not (
        state.get("done") is True and report.get("private_local_export") is True
        and report.get("status") == "complete"
        and report.get("source") == "Rounder via Control"
        and re.fullmatch(r"[0-9a-f]{24}", account)
        and root.startswith("/") and (root == "/" or not root.endswith("/"))
        and type(generation) is int and generation > 0 and report.get("generation") == generation
        and report.get("account_fingerprint") == account and type(count) is int and count >= 0
    ):
        raise ValueError("Use a completed private Rounder inventory checkpoint")
    identity = hashlib.sha256(f"{account}|{root}|{generation}".encode()).hexdigest()[:20]
    stem = f"rounder-{identity}"
    if checkpoint.name != f"{stem}.checkpoint.json":
        raise ValueError("The checkpoint does not match its account, root and generation")
    scope = hashlib.sha256(f"{account}|{root}".encode()).hexdigest()
    return checkpoint.with_name(f"{stem}.jsonl.part"), scope, generation, root, count


def refresh(checkpoint, index, include_folders=False):
    entries, scope, generation, root, expected = export_identity(checkpoint)
    if any(index.samefile(source) if index.exists() else index.resolve() == source.resolve() for source in (entries, checkpoint)):
        raise ValueError("Write the index to a separate file")
    prefix = root.rstrip("/") + "/"
    digest = hashlib.sha256()
    previous = ""
    total = 0
    with sqlite3.connect(index) as db:
        db.executescript(SCHEMA)
        db.execute("BEGIN IMMEDIATE")
        current = db.execute("SELECT generation, digest FROM rounder_snapshot WHERE scope=?", (scope,)).fetchone()
        if current and generation < current[0]:
            raise ValueError("The selected export predates the indexed generation")
        db.execute("CREATE TEMP TABLE incoming AS SELECT * FROM rounder_artwork WHERE 0")
        batch = []
        with entries.open("rb") as source:
            for line in source:
                digest.update(line)
                entry = json.loads(line)
                path = entry["path"]
                if not (
                    path.startswith(prefix) and path > previous and "\\" not in path
                    and all(part not in ("", ".", "..") for part in path[1:].split("/"))
                    and entry["parent"] == (path.rpartition("/")[0] or "/")
                    and type(entry["folder"]) is bool
                ):
                    raise ValueError("The export contains invalid, duplicate or out-of-scope paths")
                total += 1
                previous = path
                if entry["folder"] or entry["type"] != "image":
                    continue
                md5 = entry["md5"]
                md5 = md5.lower() if isinstance(md5, str) and re.fullmatch(r"[0-9a-fA-F]{32}", md5) else None
                batch.append((scope, path, entry["parent"], entry["size"], entry["modified"], md5))
                if len(batch) == 2048:
                    db.executemany("INSERT INTO incoming VALUES (?, ?, ?, ?, ?, ?)", batch)
                    batch.clear()
        db.executemany("INSERT INTO incoming VALUES (?, ?, ?, ?, ?, ?)", batch)
        if total != expected:
            raise ValueError("The retained metadata does not match the completed export count")
        source_digest = digest.hexdigest()
        if current and generation == current[0] and source_digest != current[1]:
            raise ValueError("The indexed export changed within the same generation")
        if not current or generation != current[0]:
            db.execute("DELETE FROM rounder_artwork WHERE scope=?", (scope,))
            db.execute("INSERT INTO rounder_artwork SELECT * FROM incoming")
            db.execute("INSERT OR REPLACE INTO rounder_snapshot VALUES (?, ?, ?, ?)", (scope, generation, total, source_digest))
        images, folders, sizes, md5s, distinct = db.execute(
            "SELECT count(*), count(DISTINCT parent), count(CASE WHEN size_bytes > 0 THEN 1 END), "
            "count(reported_md5), count(DISTINCT reported_md5) FROM rounder_artwork WHERE scope=?", (scope,)
        ).fetchone()
        by_folder = {}
        if include_folders:
            for folder, count in db.execute(
                "SELECT parent, count(*) FROM rounder_artwork WHERE scope=? GROUP BY parent ORDER BY parent", (scope,)
            ):
                relative = folder[len(prefix):] if folder != root else ""
                top = relative.partition("/")[0]
                by_folder[top] = by_folder.get(top, 0) + count
    return {
        "authorized_root": root, "snapshot_sha256": source_digest, "generation": generation,
        "entries": total, "images": images, "folders_with_images": folders,
        "images_with_size": sizes, "images_with_reported_md5": md5s, "distinct_reported_md5": distinct,
        "additional_paths_with_repeated_reported_md5": md5s - distinct,
        "appreciation_available": False,
        **({"images_by_top_level_folder": by_folder} if include_folders else {}),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("checkpoint", type=Path)
    parser.add_argument("index", type=Path)
    parser.add_argument("--include-folders", action="store_true", help="include private folder names in the report")
    args = parser.parse_args()
    print(json.dumps(refresh(args.checkpoint, args.index, args.include_folders), sort_keys=True))


if __name__ == "__main__":
    main()
