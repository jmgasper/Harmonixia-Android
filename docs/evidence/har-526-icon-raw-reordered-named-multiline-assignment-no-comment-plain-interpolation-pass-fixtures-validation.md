# HAR-526 Validation — Icon Raw Reordered Named Multiline Assignment No-Comment Plain Interpolation Pass Fixture

## Scope
Added one missing raw non-dollar plain-interpolation fixture for reordered named `Icon(...)` using multiline assignment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with:
  - `contentDescription =`
  - `    """Play ${title}"""`
  - `imageVector = Icons.Outlined.PlayArrow`
- Purpose: cover the missing raw multiline-assignment no-comment form for non-dollar plain interpolation.

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-526-test-check-hardcoded-ui-text-literals-20260510T115702Z.log`
- `docs/evidence/har-526-check-hardcoded-ui-text-literals-20260510T115702Z.log`
