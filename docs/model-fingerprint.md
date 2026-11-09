# Calculate a model fingerprint

An extension's `domain.fingerprint` identifies the application's exact domain capability model (DCM). Hash the model's canonical JSON, rather than its original file bytes. Formatting and object-key order do not affect this digest; changing a model value does.

The following standalone Python recipe implements the [canonical byte rules](specs/compiler-artifact.md#canonical-bytes-and-digest-rules). Save it as `model-fingerprint.py` in your own workspace. It uses the standard library and accepts a UTF-8 JSON model. It is a recipe you can copy, not a compiler command or exported runtime API.

```python
import hashlib
import json
import re
import sys

def reject(value):
    raise ValueError("Unsupported canonical JSON value: " + value)

def integer(text):
    if not re.fullmatch(r"0|[1-9][0-9]*", text) or int(text) > 2**53 - 1:
        reject(text)
    return int(text)

def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate model key: " + key)
        result[key] = value
    return result

def quote(text):
    return '"' + ''.join('\\' + c if c in ('"', '\\') else
                         f'\\u{ord(c):04x}' if ord(c) < 32 else c for c in text) + '"'

def canonical(value):
    if isinstance(value, str):
        return quote(value)
    if isinstance(value, bool):
        return 'true' if value else 'false'
    if isinstance(value, int):
        return str(value)
    if isinstance(value, list):
        return '[' + ','.join(map(canonical, value)) + ']'
    if isinstance(value, dict):
        return '{' + ','.join(quote(k) + ':' + canonical(value[k]) for k in sorted(value)) + '}'
    raise ValueError("Unsupported canonical JSON value")

with open(sys.argv[1], 'rb') as model_file:
    raw = model_file.read(65_537)
if len(raw) > 65_536:
    raise ValueError("Model exceeds its byte limit")
model = json.loads(raw.decode('utf-8'), object_pairs_hook=unique_object,
                   parse_int=integer, parse_float=reject, parse_constant=reject)
encoded = canonical(model).encode('utf-8')
if len(encoded) > 65_536:
    raise ValueError("Canonical model exceeds its byte limit")
print('sha256:' + hashlib.sha256(encoded).hexdigest())
```

Run it against the model you will pass to the compiler:

```sh
python model-fingerprint.py dcm.json
```

Copy the printed `sha256:...` value into the source's `domain.fingerprint`, alongside the model's domain ID and version. Compile against that same model. The compiler validates its structure and semantics and rejects a mismatched fingerprint; calculating a digest alone does not validate a model.

After a model change, recalculate the digest, update affected source, then compile, test and review the new artifacts. Keep existing approved artifacts with the original model they require. See [model identity](specs/domain-model.md#model-fields-and-identity) and [embedding](embedding.md) for the host's selection and approval responsibilities.
