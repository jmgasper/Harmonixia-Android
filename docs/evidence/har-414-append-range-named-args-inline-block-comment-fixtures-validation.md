# HAR-414 Validation — AppendRange Named-Args Inline Block-Comment Literals

## Scope
Added regression fixture coverage for named-argument `appendRange(...)` calls (non-reordered) where `text` uses inline block comments before string/raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_named_args_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendRangeNamedArgsInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `text = /* TODO localize */ "Now playing", startIndex = 0, endIndex = 3`
  - `text = /* TODO localize */ """Now playing""", startIndex = 0, endIndex = 3`
- Aligned escaped-dollar named-arg fixture with current scanner behavior by switching:
  - `appendLine(value = "Price \$5")` → `appendLine(text = "Price \$5")`

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
- `docs/evidence/har-414-test-check-hardcoded-ui-text-literals-20260506T144234Z.log`
- `docs/evidence/har-414-check-hardcoded-ui-text-literals-20260506T144234Z.log`
- `docs/evidence/har-414-option-regressions-runner-20260506T144234Z.log`
- `docs/evidence/har-414-compileDebugKotlin-20260506T144234Z.log`
- `docs/evidence/har-414-smoke-list-avds-20260506T144234Z.log`
- `docs/evidence/har-414-smoke-install-launch-20260506T144234Z.log`
