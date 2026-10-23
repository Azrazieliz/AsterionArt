import json
import struct
import tempfile
import unittest
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parent))

import numpy as np
from merge_anima import merge, header, interpolate


def small_checkpoint(path, values, variant="F16"):
    arrays = {}
    payload = b""
    for key, number in values.items():
        data = np.array(number, dtype="<f2").tobytes()
        arrays[key] = {"dtype": variant,"shape": [len(number)],
                       "data_offsets": [len(payload),len(payload)+len(data)]}
        payload += data
    raw = json.dumps(arrays,separators=(",", ":")).encode()
    raw += b" " * ((-len(raw)) % 8)
    path.write_bytes(struct.pack("<Q",len(raw))+raw+payload)


class MergeTests(unittest.TestCase):
    def test_equal_tensor_architecture_merges_correctly(self):
        with tempfile.TemporaryDirectory() as tmp:
            a,b,c = [Path(tmp)/name for name in ("rin.safetensors","miao.safetensors","fused.safetensors")]
            small_checkpoint(a,{"weights": [0.0,4.0],"bias":[2.0]})
            small_checkpoint(b,{"weights": [10.0,8.0],"bias":[6.0]})
            result=merge(a,b,c,0.25,1)
            base, spec=header(c)
            raw=c.read_bytes()[base+spec["weights"]["data_offsets"][0]:base+spec["weights"]["data_offsets"][1]]
            self.assertTrue(np.allclose(np.frombuffer(raw,dtype="<f2"),[2.5,5.0]))
            self.assertTrue(result["bytes"] > 0)
    def test_cross_family_or_different_architecture_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            a,b,c = [Path(tmp)/name for name in ("a.safetensors","b.safetensors","c.safetensors")]
            small_checkpoint(a,{"unet_layers": [0.,1.]})
            small_checkpoint(b,{"anima_layers": [0.,1.]})
            with self.assertRaisesRegex(ValueError,"tensor keys"):
                merge(a,b,c,0.5)
            self.assertFalse(c.exists())
    def test_bfloat16_is_numerically_merged(self):
        aa=np.array([0x3F80,0x4000],dtype="<u2").tobytes()
        bb=np.array([0x4000,0x4080],dtype="<u2").tobytes()
        out=interpolate(aa,bb,"BF16",0.5)
        self.assertEqual(len(out),4)


if __name__ == "__main__":
    unittest.main()
