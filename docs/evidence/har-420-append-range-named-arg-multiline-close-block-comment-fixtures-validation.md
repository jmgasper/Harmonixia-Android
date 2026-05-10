# HAR-420 Validation — AppendRange Named-Arg Multiline Close-Block-Comment Literals

## Scope
Added regression fixture coverage for multiline named-arg `appendRange(...)` calls where the `text` literal argument is preceded by a multiline-close inline block comment.

## Code Change
- Updated `scripts/test-check-hardcoded-ui-text-literals.sh` with fixture:
  - `annotated_string_append_range_named_args_multiline_close_block_comment_inline_literal_fail_dir`
  - `AnnotatedStringAppendRangeNamedArgsMultilineCloseBlockCommentInlineLiteral.kt`
- Added assertions for emitted snippets:
  - `*/ "Now playing",`
  - `*/ """Now playing""",`

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
- `docs/evidence/har-420-test-check-hardcoded-ui-text-literals-20260506T202320Z.log`
- `docs/evidence/har-420-check-hardcoded-ui-text-literals-20260506T202320Z.log`
- `docs/evidence/har-420-option-regressions-runner-20260506T202320Z.log`
- `docs/evidence/har-420-compileDebugKotlin-20260506T202320Z.log`
- `docs/evidence/har-420-smoke-list-avds-20260506T202320Z.log`
- `docs/evidence/har-420-smoke-install-launch-20260506T202320Z.log`
