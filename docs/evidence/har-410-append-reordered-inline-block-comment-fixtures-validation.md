# HAR-410 Validation — Append Reordered Inline Block-Comment Literals

## Scope
Added regression fixture coverage for reordered named-argument `append(...)` calls where `text` uses inline block comments before string/raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_named_arg_append_reordered_inline_block_comment_fail_dir`
  - `AnnotatedStringNamedArgAppendReorderedInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `end = 3, text = /* TODO localize */ "Now playing", start = 0`
  - `end = 3, text = /* TODO localize */ """Now playing""", start = 0`

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
- `docs/evidence/har-410-test-check-hardcoded-ui-text-literals-20260506T101248Z.log`
- `docs/evidence/har-410-check-hardcoded-ui-text-literals-20260506T101248Z.log`
- `docs/evidence/har-410-option-regressions-runner-20260506T101248Z.log`
- `docs/evidence/har-410-compileDebugKotlin-20260506T101248Z.log`
- `docs/evidence/har-410-smoke-list-avds-20260506T101248Z.log`
- `docs/evidence/har-410-smoke-install-launch-20260506T101248Z.log`
