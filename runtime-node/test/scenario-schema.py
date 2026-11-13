import json
import sys
from pathlib import Path
import runpy


revision = sys.argv[1] if len(sys.argv) == 2 else "2"
if len(sys.argv) > 2 or revision not in ("2", "3", "4"):
    raise ValueError("Scenario revision must be 2, 3 or 4")
checker = Path(__file__).resolve().parents[2] / "scripts/check-keir-contracts.py"
validator = runpy.run_path(str(checker))["validators"](int(revision))["scenario"]
json.dump([validator.is_valid(document) for document in json.load(sys.stdin.buffer)], sys.stdout)
