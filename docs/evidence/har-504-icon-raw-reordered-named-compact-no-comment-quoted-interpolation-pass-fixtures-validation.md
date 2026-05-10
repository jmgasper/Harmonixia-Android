# HAR-504 Validation — Icon Raw Reordered Named Compact No-Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with compact single-line argument layout and quoted interpolation around title.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `Icon(contentDescription = """Play "$title"""", imageVector = Icons.Outlined.PlayArrow)`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw quoted interpolation without localization comments.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-504-test-check-hardcoded-ui-text-literals-20260510T014205Z.log`
- `docs/evidence/har-504-check-hardcoded-ui-text-literals-20260510T014205Z.log`
