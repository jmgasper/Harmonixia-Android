# HAR-419 Validation — AppendRange Named-Arg Multiline Trailing Inline Block-Comment Literals

## Scope
Added regression fixture coverage for multiline named-arg `appendRange(...)` calls where the `text` literal argument uses trailing inline block comments after string/raw literals.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_named_args_multiline_trailing_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendRangeNamedArgsMultilineTrailingInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `text = "Now playing" /* TODO localize */,`
  - `text = """Now playing""" /* TODO localize */,`

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
- `docs/evidence/har-419-test-check-hardcoded-ui-text-literals-20260506T191850Z.log`
- `docs/evidence/har-419-check-hardcoded-ui-text-literals-20260506T191850Z.log`
- `docs/evidence/har-419-option-regressions-runner-20260506T191850Z.log`
- `docs/evidence/har-419-compileDebugKotlin-20260506T191850Z.log`
- `docs/evidence/har-419-smoke-list-avds-20260506T191850Z.log`
- `docs/evidence/har-419-smoke-install-launch-20260506T191850Z.log`
