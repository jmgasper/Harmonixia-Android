# HAR-480 Validation — AnnotatedString Compact Escaped Constructor Pass Matrix Audit

## Scope
Audited compact escaped `AnnotatedString(...)` constructor pass coverage in `scripts/test-check-hardcoded-ui-text-literals.sh` and added the next uncovered fixture.

## Matrix Audit
Reviewed compact escaped constructor pass cases across arg form and trailing-comment style:

- Named `AnnotatedString(text = "Now \\${title} costs \\$5"...)`: covered for none, trailing `/* ... */`, trailing multiline `/* ...`, and trailing `//`.
- Positional `AnnotatedString("Now \\${title} costs \\$5"...)`: covered for trailing `/* ... */`, trailing multiline `/* ...`, and trailing `//`.
- Uncovered before this change: positional compact escaped constructor with no trailing comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` pass fixtures with:
  - `BasicText(text = AnnotatedString("Now ${title} costs \$5"))`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`

## Result
Both commands passed.

## Evidence Logs
- `docs/evidence/har-480-test-check-hardcoded-ui-text-literals-20260509T135033Z.log`
- `docs/evidence/har-480-check-hardcoded-ui-text-literals-20260509T135033Z.log`
