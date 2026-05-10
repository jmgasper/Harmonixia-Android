# HAR-407 Validation — Append Positional Trailing Inline Block-Comment Literals

## Scope
Added regression fixture coverage for positional `append(...)` calls where literals are followed by same-line trailing inline block comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_trailing_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendTrailingInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `"Now playing" /* TODO localize */`
  - `"""Now playing""" /* TODO localize */`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `./gradlew --no-daemon :app:compileDebugKotlin`
5. `./scripts/smoke-debug-emulator.sh --list-avds`
6. `./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Result
All commands passed.

## Evidence Logs
- `docs/evidence/har-407-test-check-hardcoded-ui-text-literals-20260506T084904Z.log`
- `docs/evidence/har-407-check-hardcoded-ui-text-literals-20260506T084904Z.log`
- `docs/evidence/har-407-option-regressions-runner-20260506T084904Z.log`
- `docs/evidence/har-407-compileDebugKotlin-20260506T084904Z.log`
- `docs/evidence/har-407-smoke-list-avds-20260506T084904Z.log`
- `docs/evidence/har-407-smoke-install-launch-20260506T084904Z.log`
