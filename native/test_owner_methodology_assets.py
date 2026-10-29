"""Regression gate: original owner-authored methodology files remain present.

These are data-preservation tests, not an assertion that generative outputs
implement every requirement. Never replace this source with model summaries.
"""
import unittest
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
METHODS=ROOT/"app/src/main/assets/methodology"

class OwnerMethodologyAssetsTests(unittest.TestCase):
    SOURCES={
        "Character Creation v3/characters_creation.md":500_000,
        "Character Creation v3/Design_Direction.md":100_000,
        "concepts_full_v3.md":1_000_000,
        "Feminisation/feminisation_reconstructed_current.md":15_000,
        "Outfit Creation/Outfit Creation.md":15_000,
        "Weapon Creation/Weapon Creation.md":20_000,
        "Transformation Creation/Transformation Creation.md":20_000,
        "Mutation/Mutation Creation.md":20_000,
        "Creature/Creature Creation.md":20_000,
        "Alternate Version/Alternate Universe Creation.md":20_000,
        "Art Style Correction/art style sanitisation.md":18_000,
        "Image Analysis/Illustration_Visual_Analysis_Text_Converter.md":20_000,
    }

    def test_originals_are_full_sources_not_500_character_summaries(self):
        for name,minimum in self.SOURCES.items():
            with self.subTest(original=name):
                path=METHODS/name
                self.assertTrue(path.is_file(),name)
                data=path.read_bytes()
                self.assertGreaterEqual(len(data),minimum,name)
                self.assertIn(b"## ",data,name)

    def test_no_token_sliced_native_generation_left(self):
        self.assertFalse((ROOT/
            "app/src/main/java/com/asterion/art/engine/ConditioningSchedule.kt").exists())
        worker=(ROOT/"app/src/main/java/com/asterion/art/work/SelectiveEditWorker.kt")
        self.assertNotIn("ConditioningSchedule",worker.read_text())
        executor=(ROOT/"app/src/main/java/com/asterion/art/work/GenerationExecutor.kt")
        self.assertNotIn("ConditioningSchedule",executor.read_text())

    def test_only_artwork_has_generic_five_pass_plan(self):
        planner=(ROOT/"app/src/main/java/com/asterion/art/engine/GenerationPlanner.kt")
        content=planner.read_text()
        self.assertIn("if (creation == null) (1..5).map",content)
        self.assertIn("else listOf(GenerationPass(1, core",content)

if __name__=="__main__":
    unittest.main()
