# HAR-416 Validation — AppendRange Positional Inline Block-Comment Literals

## Scope
Added regression fixture coverage for positional `appendRange(...)` calls where the first literal argument uses inline block comments before string/raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_positional_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendRangePositionalInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `appendRange(/* TODO localize */ "Now playing", 0, 3)`
  - `/* TODO localize */ """Now playing""", 0, 3`

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
- `docs/evidence/har-416-test-check-hardcoded-ui-text-literals-20260506T165833Z.log`
- `docs/evidence/har-416-check-hardcoded-ui-text-literals-20260506T165833Z.log`
- `docs/evidence/har-416-option-regressions-runner-20260506T165833Z.log`
- `docs/evidence/har-416-compileDebugKotlin-20260506T165833Z.log`
- `docs/evidence/har-416-smoke-list-avds-20260506T165833Z.log`
- `docs/evidence/har-416-smoke-install-launch-20260506T165833Z.log`
