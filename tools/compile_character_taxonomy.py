"""Compile the approved selector into data. The app never packages the source prose."""

import argparse
import hashlib
import json
import re
from pathlib import Path


def compile_taxonomy(source: bytes) -> dict:
    text = source.decode("utf-8")
    hierarchy = re.search(
        r"(?ms)^## 4\. Canonical recursive selector hierarchy\s.*?^\x60{3}json\s+(.*?)^\x60{3}",
        text,
    )
    if not hierarchy:
        raise ValueError("The canonical selector JSON in Section 4 is missing")
    source_graph = json.loads(hierarchy[1])
    roster = text.split("## 5. SELECTABLE CHARACTER CONCEPT ENTRIES", 1)[1]
    roster = roster.split("## 6. VARIANT / SUBCONCEPT REGISTRY", 1)[0]
    pairs = re.findall(r"(?m)^### (SC\d+) — (.+)$", roster)
    leaves = {code: name.strip() for code, name in pairs}
    if not leaves or len(leaves) != len(pairs):
        raise ValueError("Missing or duplicate SC leaf headings")

    nodes = source_graph["nodes"]
    paths = {}
    visited = set()

    def visit(code, ancestors):
        if code in visited:
            raise ValueError(f"Repeated branch or cycle at {code}")
        visited.add(code)
        node = nodes[code]
        if node.get("id", code) != code or not re.fullmatch(r"(BC|SF)\d+", code):
            raise ValueError(f"Invalid branch identity: {code}")
        path = [*ancestors, code]
        for leaf in node["leaves"]:
            if leaf in paths:
                raise ValueError(f"Multiple canonical paths for {leaf}")
            paths[leaf] = path
        for child in node["children"]:
            visit(child, path)

    for root in source_graph["roots"]:
        visit(root, [])
    if set(leaves) != set(paths) or visited != set(nodes):
        raise ValueError("The hierarchy and canonical roster differ")
    domains = source_graph["domains"]
    aliases = source_graph["aliases"]
    facets = source_graph["query_facets"]
    retired = source_graph["retired_sf"]
    targets = set(leaves) | set(nodes)
    for record in [*aliases, *facets, *domains.values()]:
        if not record["targets"] or set(record["targets"]) - targets:
            raise ValueError(f"Invalid selector targets: {record['targets']}")
    if set(retired) & set(nodes):
        raise ValueError("A retired branch is active")
    for record in retired.values():
        if set(record.get("former_leaves", [])) - set(leaves):
            raise ValueError("Retired branch has missing former leaves")

    return {
        "version": source_graph["version"],
        "sourceSha256": hashlib.sha256(source).hexdigest(),
        "roots": source_graph["roots"],
        "nodes": {
            code: {key: node[key] for key in ("name", "description", "leaves", "children")}
            for code, node in nodes.items()
        },
        "leaves": {code: {"name": name, "path": paths[code]} for code, name in leaves.items()},
        "aliases": [
            {key: alias[key] for key in ("terms", "targets", "kind")} for alias in aliases
        ],
        "domains": {
            code: {key: domain[key] for key in ("name", "targets")}
            for code, domain in domains.items()
        },
        "retired": {
            code: {"name": item["name"], "targets": item.get("former_leaves", [])}
            for code, item in retired.items()
        },
        "ambiguities": [
            {"terms": item["terms"], "candidates": item["candidates"]}
            for item in source_graph["ambiguity_examples"]
        ],
        "facets": [{key: item[key] for key in ("terms", "targets")} for item in facets],
        "joinWords": source_graph["query_composition"]["join_words"],
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    compiled = compile_taxonomy(args.source.read_bytes())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(compiled, ensure_ascii=False, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
