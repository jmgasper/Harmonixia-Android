# HAR-500 Validation — Icon Raw Reordered Named Leading Multiline Block Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with plain interpolation `"""Play ${title}"""` and a leading multiline block comment on `contentDescription`.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription = /* localized`
  - `    */ """Play ${title}""",`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw plain interpolation with leading multiline block-comment annotation.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-500-test-check-hardcoded-ui-text-literals-20260509T221941Z.log`
- `docs/evidence/har-500-check-hardcoded-ui-text-literals-20260509T221941Z.log`
