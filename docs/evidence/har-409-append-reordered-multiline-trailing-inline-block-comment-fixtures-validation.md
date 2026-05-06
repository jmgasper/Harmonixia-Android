# HAR-409 Validation — Append Reordered Multiline Trailing Inline Block-Comment Literals

## Scope
Added regression fixture coverage for multiline reordered named-argument `append(...)` calls where `text` literals are followed by same-line trailing inline block comments.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_named_arg_append_reordered_multiline_trailing_inline_block_comment_fail_dir`
  - `AnnotatedStringNamedArgAppendReorderedMultilineTrailingInlineBlockCommentLiteral.kt`
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
- `docs/evidence/har-409-test-check-hardcoded-ui-text-literals-20260506T090337Z.log`
- `docs/evidence/har-409-check-hardcoded-ui-text-literals-20260506T090337Z.log`
- `docs/evidence/har-409-option-regressions-runner-20260506T090337Z.log`
- `docs/evidence/har-409-compileDebugKotlin-20260506T090337Z.log`
- `docs/evidence/har-409-smoke-list-avds-20260506T090337Z.log`
- `docs/evidence/har-409-smoke-install-launch-20260506T090337Z.log`
