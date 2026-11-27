"""Index recovered methodology sources for review. No methodology is executed or modified."""
import argparse
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path

ARCHIVE_SHA = '14888597e24d3e39cb9ec6b665ebed0f2a753321b8b12f7a4bb311f51134d9f2'
SOURCES = {
    'character': 'Methodology/Character Creation v3/characters_creation.md',
    'character_direction': 'Methodology/Character Creation v3/Design_Direction.md',
    'concepts': 'Methodology/Character Creation v3/Concepts.md',
    'creature': 'Methodology/Creature/Creature%20Creation.md',
    'outfit': 'Methodology/Outfit Creation/Outfit%20Creation.md',
    'weapon_visual': 'Methodology/Weapon Creation/Weapon%20Creation.md',
    'transformation': 'Methodology/Transformation Creation/Transformation%20Creation.md',
    'mutation': 'Methodology/Mutation/Mutation%20Creation.md',
    'alternate_universe': 'Methodology/Alternate Version/Alternate%20Universe%20Creation.md',
    'feminisation': 'Methodology/Feminisation/feminisation_reconstructed_current.md',
    'style_correction': 'Methodology/Art Style Correction/art%20style%20sanitisation.md',
    'character_sheet': 'Methodology/Character Sheet/character%20sheet.md',
    'theme': 'Methodology/Theme Sheet/Theme-Sheet-Workflow-v1.md',
    'element_sheet': 'Methodology/Element Sheet/Element_Sheets_Sole_Authoritative_Ruleset.md',
    'style_sheet': 'Methodology/Art Style Sheet/Art_Style_Master_Sheet_Absolute_Authoritative_Rules.md',
    'pose': 'Methodology/Pose/Pose_Workflow.md',
    'image_analysis': 'Methodology/Image Analysis/Illustration_Visual_Analysis_Text_Converter.md',
    'prompt_compilation': 'Methodology/Prompt Writing/prompt.md',
}

def sha(data):
    return hashlib.sha256(data).hexdigest()

def index(path):
    raw = Path(path).read_bytes()
    if sha(raw) != ARCHIVE_SHA:
        with zipfile.ZipFile(io.BytesIO(raw)) as outer:
            raw = outer.read('Methodology(2).zip')
    if sha(raw) != ARCHIVE_SHA:
        raise ValueError('Authoritative archive SHA-256 mismatch')
    records = []
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        for mode, name in SOURCES.items():
            data = archive.read(name)
            lines = data.decode('utf-8-sig').splitlines(keepends=True)
            starts = [i for i, line in enumerate(lines) if re.match(r'^#{1,6}\s+\S', line)]
            if mode == 'theme':
                starts = [i for i, line in enumerate(lines) if re.match(r'^\s*\d+\.\s+\S', line) and any(re.match(r'^={20,}', x) for x in lines[max(0,i-3):i])]
            # Include preambles and every line, without assuming a heading is a single requirement.
            starts = sorted(set([0] + starts + [len(lines)]))
            sections = []
            for i, j in zip(starts, starts[1:]):
                if j <= i:
                    continue
                block = ''.join(lines[i:j]).encode('utf-8')
                sections.append({'section_id':f'{mode}:{i+1}:{sha(block)[:12]}','start_line':i+1,'end_line':j,'normalized_text_sha256':sha(block),'mapping_status':'UNREVIEWED'})
            if not sections or sections[0]['start_line'] != 1 or sections[-1]['end_line'] != len(lines):
                raise ValueError(f'Incomplete line coverage: {mode}')
            records.append({'mode':mode,'archive_path':name,'source_sha256':sha(data),'line_count':len(lines),'sections':sections})
    return {'archive_sha256':ARCHIVE_SHA,'status':'INDEXED_NOT_SEMANTICALLY_MAPPED','sources':records}

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('archive')
    p.add_argument('output')
    a = p.parse_args()
    result = index(a.archive)
    Path(a.output).write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(f"Indexed {len(result['sources'])} sources, {sum(len(s['sections']) for s in result['sources'])} review sections. No requirements marked compliant.")

if __name__ == '__main__':
    main()
