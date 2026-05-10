# HAR-367 Validation - cover raw `appendRange` literals

Validation window (UTC): 20260505T001739Z - 20260505T001739Z

## Scope
- Extended `scripts/test-check-hardcoded-ui-text-literals.sh` with a dedicated failing fixture for raw-string `appendRange` hardcoded literals inside `buildAnnotatedString { ... }`:
  - `appendRange("""Now playing""", 0, 3)`
  - `appendRange(text = """Now playing""", startIndex = 0, endIndex = 3)`
- Added explicit assertions that scanner output contains both raw positional and raw named-arg `appendRange` literals.
- Kept scanner logic unchanged; this slice adds regression coverage for raw `appendRange(...)` literal detection.

## Validation Commands and Results
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-367-test-check-hardcoded-ui-text-literals-20260505T001739Z.log`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
   - Result: PASS
   - Log: `docs/evidence/har-367-check-hardcoded-ui-text-literals-20260505T001739Z.log`

## Outcome
HAR-367 adds explicit regression coverage for raw `appendRange(...)` literal detection in the UI literal scanner self-test suite.
