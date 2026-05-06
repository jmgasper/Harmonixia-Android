# HAR-412 Validation — AppendRange Reordered Inline Block-Comment Literals

## Scope
Added regression fixture coverage for reordered named-argument `appendRange(...)` calls where `text` uses inline block comments before string/raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_reordered_named_args_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendRangeReorderedNamedArgsInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `endIndex = 3, text = /* TODO localize */ "Now playing", startIndex = 0`
  - `endIndex = 3, text = /* TODO localize */ """Now playing""", startIndex = 0`

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
- `docs/evidence/har-412-test-check-hardcoded-ui-text-literals-20260506T122731Z.log`
- `docs/evidence/har-412-check-hardcoded-ui-text-literals-20260506T122731Z.log`
- `docs/evidence/har-412-option-regressions-runner-20260506T122731Z.log`
- `docs/evidence/har-412-compileDebugKotlin-20260506T122731Z.log`
- `docs/evidence/har-412-smoke-list-avds-20260506T122731Z.log`
- `docs/evidence/har-412-smoke-install-launch-20260506T122731Z.log`
