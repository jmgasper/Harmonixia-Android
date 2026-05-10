# HAR-405 Validation — AppendRange Named-Arg Trailing Inline Block-Comment Literals

## Scope
Added regression fixture coverage for canonical named-arg `appendRange(...)` calls where `text` literals are followed by same-line trailing inline block comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_named_args_trailing_inline_block_comment_fail_dir`
  - `AnnotatedStringAppendRangeNamedArgsTrailingInlineBlockCommentLiteral.kt`
- Added assertions for emitted snippets:
  - `text = "Now playing" /* TODO localize */, startIndex = 0, endIndex = 3`
  - `text = """Now playing""" /* TODO localize */, startIndex = 0, endIndex = 3`

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
- `docs/evidence/har-405-test-check-hardcoded-ui-text-literals-20260506T063145Z.log`
- `docs/evidence/har-405-check-hardcoded-ui-text-literals-20260506T063145Z.log`
- `docs/evidence/har-405-option-regressions-runner-20260506T063145Z.log`
- `docs/evidence/har-405-compileDebugKotlin-20260506T063145Z.log`
- `docs/evidence/har-405-smoke-list-avds-20260506T063145Z.log`
- `docs/evidence/har-405-smoke-install-launch-20260506T063145Z.log`
