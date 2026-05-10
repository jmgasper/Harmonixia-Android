# HAR-413 Validation — AppendRange Reordered Multiline Inline Block-Comment Literals

## Scope
Added regression fixture coverage for multiline reordered named-argument `appendRange(...)` calls where `text` uses inline block comments before string/raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_reordered_named_args_multiline_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendRangeReorderedNamedArgsMultilineInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `text = /* TODO localize */ "Now playing",`
  - `text = /* TODO localize */ """Now playing""",`

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
- `docs/evidence/har-413-test-check-hardcoded-ui-text-literals-20260506T133435Z.log`
- `docs/evidence/har-413-check-hardcoded-ui-text-literals-20260506T133435Z.log`
- `docs/evidence/har-413-option-regressions-runner-20260506T133435Z.log`
- `docs/evidence/har-413-compileDebugKotlin-20260506T133435Z.log`
- `docs/evidence/har-413-smoke-list-avds-20260506T133435Z.log`
- `docs/evidence/har-413-smoke-install-launch-20260506T133435Z.log`
