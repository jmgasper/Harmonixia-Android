# HAR-494 Validation — Icon Raw Reordered Named Compact Single-Line No-Comment Plain Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with compact single-line argument layout and plain interpolation `"""Play ${title}"""` without a localization comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = """Play ${title}""", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw interpolation without currency escape and without comment tokens.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-494-test-check-hardcoded-ui-text-literals-20260509T210023Z.log`
- `docs/evidence/har-494-check-hardcoded-ui-text-literals-20260509T210023Z.log`
