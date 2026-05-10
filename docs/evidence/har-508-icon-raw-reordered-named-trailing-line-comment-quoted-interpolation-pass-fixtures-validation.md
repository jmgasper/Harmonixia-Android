# HAR-508 Validation — Icon Raw Reordered Named Trailing Line Comment Quoted Interpolation Pass Fixture

## Scope
Extended pass-fixture coverage for raw reordered named `Icon(...)` with quoted interpolation `"""Play "$title""""` and a trailing line comment on `contentDescription`.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `contentDescription = """Play "$title"""", // localized`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: ensure scanner pass behavior remains correct for reordered named raw quoted interpolation with trailing line-comment annotation.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-508-test-check-hardcoded-ui-text-literals-20260510T035724Z.log`
- `docs/evidence/har-508-check-hardcoded-ui-text-literals-20260510T035724Z.log`
