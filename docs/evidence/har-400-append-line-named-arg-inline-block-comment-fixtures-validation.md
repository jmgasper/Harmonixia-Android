# HAR-400 Validation — AppendLine Named-Arg Inline Block-Comment Literals

## Scope
Added regression fixture coverage for `appendLine` named arguments where literals are preceded by same-line inline block comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_named_arg_append_line_inline_block_comment_fail_dir`
  - `AnnotatedStringNamedArgAppendLineInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `text = /* TODO localize */ "Now playing"`
  - `value = /* TODO localize */ """Now playing"""`

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
- `docs/evidence/har-400-test-check-hardcoded-ui-text-literals-20260506T035433Z.log`
- `docs/evidence/har-400-check-hardcoded-ui-text-literals-20260506T035433Z.log`
- `docs/evidence/har-400-option-regressions-runner-20260506T035433Z.log`
- `docs/evidence/har-400-compileDebugKotlin-20260506T035433Z.log`
- `docs/evidence/har-400-smoke-list-avds-20260506T035433Z.log`
- `docs/evidence/har-400-smoke-install-launch-20260506T035433Z.log`
