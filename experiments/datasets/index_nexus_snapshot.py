"""Index an authorized Nexus SQLite snapshot without opening media bytes."""

import argparse
import json
from pathlib import Path
import re
import sqlite3


SCHEMA = """
CREATE TABLE IF NOT EXISTS source_image (
    scope TEXT NOT NULL, image_id INTEGER NOT NULL, uri TEXT NOT NULL,
    sha256 TEXT, phash TEXT, size_bytes INTEGER, width INTEGER, height INTEGER,
    mime TEXT NOT NULL, active INTEGER NOT NULL, appreciation INTEGER,
    favorite INTEGER NOT NULL, tags TEXT NOT NULL, taxonomy TEXT NOT NULL,
    artist_id TEXT, series_code TEXT, character_id TEXT,
    PRIMARY KEY (scope, image_id)
);
CREATE INDEX IF NOT EXISTS source_image_sha ON source_image(sha256);
CREATE INDEX IF NOT EXISTS source_image_preferences ON source_image(scope, appreciation) WHERE active = 1;
CREATE TABLE IF NOT EXISTS admission (
    scope TEXT NOT NULL, image_id INTEGER NOT NULL, sha256 TEXT NOT NULL,
    competency TEXT NOT NULL, permission TEXT NOT NULL, provenance TEXT NOT NULL,
    PRIMARY KEY (scope, image_id, sha256, competency)
);
"""


def columns(db, table):
    return {row[1] for row in db.execute(f"PRAGMA table_info({table})")}


def metadata_profile(data):
    try:
        value = json.loads(data or "{}")
        return value if isinstance(value, dict) else {}
    except json.JSONDecodeError:
        return {}


def source_cursor(snapshot):
    images = columns(snapshot, "images")
    profiles = columns(snapshot, "fusion_image_profiles")
    fingerprint = columns(snapshot, "fusion_image_fingerprints")
    required = {"image_id", "uri", "size_bytes", "width", "height", "mime_type", "active", "favorite", "tags_text"}
    if not required <= images:
        raise ValueError("The supplied snapshot does not contain the Nexus/Core image schema")
    profile_available = {"image_id", "rating", "metadata_json"} <= profiles
    fingerprint_available = {"image_id", "sha256", "perceptual_hash", "source_size_bytes"} <= fingerprint
    joins = []
    fields = [
        "i.image_id", "i.uri", "i.size_bytes", "i.width", "i.height", "i.mime_type", "i.active",
        "i.favorite", "i.tags_text", "i.taxonomy_text" if "taxonomy_text" in images else "''",
    ]
    if profile_available:
        joins.append("LEFT JOIN fusion_image_profiles p ON p.image_id = i.image_id")
        fields.extend(("p.rating", "p.metadata_json"))
    else:
        fields.extend(("NULL", "NULL"))
    if fingerprint_available:
        joins.append("LEFT JOIN fusion_image_fingerprints f ON f.image_id = i.image_id AND f.source_size_bytes = i.size_bytes")
        fields.extend(("f.sha256", "f.perceptual_hash"))
    else:
        fields.extend(("NULL", "NULL"))
    for table, key in (("artists", "artist_id"), ("series", "series_code"), ("characters", "character_id")):
        relation = f"fusion_image_{table}"
        if {"image_id", key} <= columns(snapshot, relation):
            fields.append(f"(SELECT MIN({key}) FROM {relation} WHERE image_id = i.image_id)")
        else:
            fields.append("NULL")
    return snapshot.execute(f"SELECT {', '.join(fields)} FROM images i {' '.join(joins)} ORDER BY i.image_id")


def catalogue_row(scope, row):
    image_id, uri, size, width, height, mime, active, favorite, tags, taxonomy, rating, profile, sha, phash, *groups = row
    manual = metadata_profile(profile).get("precision_overall_set") is True
    appreciation = rating if manual and isinstance(rating, int) and 0 <= rating <= 100 else None
    checked_sha = sha if isinstance(sha, str) and re.fullmatch("[0-9a-f]{64}", sha) and size and size > 0 else None
    return (scope, image_id, uri, checked_sha, phash or None, size, width, height, mime, active, appreciation,
            favorite, tags, taxonomy, *groups)


def refresh(snapshot, index, scope):
    index.executescript(SCHEMA)
    index.execute("CREATE TEMP TABLE seen_image (image_id INTEGER PRIMARY KEY)")
    fields = [row[1] for row in index.execute("PRAGMA table_info(source_image)")]
    updated = fields[2:]
    assignments = ", ".join(f"{name}=excluded.{name}" for name in updated)
    differences = " OR ".join(f"source_image.{name} IS NOT excluded.{name}" for name in updated)
    query = (
        f"INSERT INTO source_image({', '.join(fields)}) VALUES ({','.join('?' for _ in fields)}) "
        f"ON CONFLICT(scope, image_id) DO UPDATE SET {assignments} WHERE {differences}"
    )
    changed = 0
    try:
        snapshot.execute("BEGIN")
        with index:
            cursor = source_cursor(snapshot)
            while batch := cursor.fetchmany(2048):
                records = [catalogue_row(scope, row) for row in batch]
                index.executemany("INSERT INTO seen_image VALUES (?)", ((row[1],) for row in records))
                changed += index.executemany(query, records).rowcount
            retired = index.execute(
                "UPDATE source_image SET active=0 WHERE scope=? AND active=1 "
                "AND image_id NOT IN (SELECT image_id FROM seen_image)", (scope,)
            ).rowcount
    finally:
        snapshot.rollback()
        index.execute("DROP TABLE seen_image")
    return changed + retired


def import_admissions(index, scope, lines):
    with index:
        for line in lines:
            if not line.strip():
                continue
            row = json.loads(line)
            if row["scope"] != scope or row["permission"] not in ("OWNER_AUTHORIZED", "CC0-1.0", "PUBLIC_DOMAIN"):
                raise ValueError("Training requires recorded authority for the addressed source")
            if not row["competency"].strip() or not row["provenance"].strip():
                raise ValueError("Record the competency and the source of its training permission")
            asset = index.execute(
                "SELECT sha256 FROM source_image WHERE scope=? AND image_id=? AND active=1",
                (scope, row["image_id"]),
            ).fetchone()
            if not asset or not asset[0] or asset[0] != row["sha256"]:
                raise ValueError("Training admission needs a current, fingerprinted image")
            index.execute(
                "INSERT INTO admission VALUES (?, ?, ?, ?, ?, ?) "
                "ON CONFLICT(scope, image_id, sha256, competency) DO UPDATE SET "
                "permission=excluded.permission, provenance=excluded.provenance",
                (scope, row["image_id"], row["sha256"], row["competency"], row["permission"], row["provenance"]),
            )


def summary(index, scope):
    record = index.execute(
        "SELECT count(*), count(sha256), count(DISTINCT sha256), count(appreciation), sum(appreciation=0), "
        "sum(width>0 AND height>0), sum(min(width,height)>=768), sum(length(tags)>0), "
        "count(DISTINCT artist_id), count(DISTINCT series_code), count(DISTINCT character_id) "
        "FROM source_image WHERE scope=? AND active=1", (scope,)
    ).fetchone()
    counts = (
        "active_images", "fingerprinted_images", "distinct_content_hashes", "manual_appreciation_scores",
        "explicit_zero_scores", "known_dimensions", "min_side_at_least_768", "with_tags",
        "indexed_artist_groups", "indexed_series_groups", "indexed_character_groups",
    )
    eligible = dict(index.execute(
        "SELECT competency, count(DISTINCT a.sha256) FROM admission a JOIN source_image i "
        "ON a.scope=i.scope AND a.image_id=i.image_id AND a.sha256=i.sha256 "
        "WHERE a.scope=? AND i.active=1 GROUP BY competency", (scope,)
    ))
    return {**dict(zip(counts, (value or 0 for value in record), strict=True)), "admitted_unique_by_competency": eligible}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshot", type=Path)
    parser.add_argument("index", type=Path)
    parser.add_argument("--scope", required=True)
    parser.add_argument("--admissions", type=Path)
    args = parser.parse_args()
    if args.index.resolve() == args.snapshot.resolve() or (
        args.index.exists() and args.snapshot.samefile(args.index)
    ):
        parser.error("The derived index must be separate from the source snapshot")
    with sqlite3.connect(args.snapshot.resolve().as_uri() + "?mode=ro", uri=True) as snapshot, \
            sqlite3.connect(args.index) as index:
        snapshot.execute("PRAGMA query_only=ON")
        changed = refresh(snapshot, index, args.scope)
        if args.admissions:
            with args.admissions.open() as entries:
                import_admissions(index, args.scope, entries)
        print(json.dumps({"changed_index_rows": changed, **summary(index, args.scope)}, sort_keys=True))


if __name__ == "__main__":
    main()
