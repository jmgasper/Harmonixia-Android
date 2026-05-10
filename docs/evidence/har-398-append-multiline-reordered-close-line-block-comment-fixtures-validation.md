# HAR-398 Validation — Multiline Reordered Append/AppendRange Close-Line Block-Comment Literals

## Scope
Added regression fixture coverage in `scripts/test-check-hardcoded-ui-text-literals.sh` for multiline reordered named-arg `appendRange`/`append` calls where hardcoded literals appear immediately after a block-comment close line.

## Code Change
- Added fixture directory and Kotlin sample:
  - `annotated_string_append_multiline_reordered_named_args_close_block_comment_inline_literal_fail_dir`
  - `AnnotatedStringAppendMultilineReorderedNamedArgsCloseBlockCommentInlineLiteral.kt`
- Added assertions confirming scanner output includes close-line block-comment literal snippets:
  - `*/ "Now playing",`
  - `*/ """Now playing""",`

## Validation Commands
1. `./scripts/test-check-hardcoded-ui-text-literals.sh`
2. `./scripts/check-hardcoded-ui-text-literals.sh`
3. `./scripts/test-local-validation-option-regressions-runner.sh`
4. `./gradlew --no-daemon :app:compileDebugKotlin`
5. `./scripts/smoke-debug-emulator.sh --list-avds`
6. `./scripts/smoke-debug-emulator.sh --avd Medium_Phone --connect-timeout 90 --boot-timeout 240 --launch-wait 2 --task :app:installDebug`

## Results
- 1–3 passed on first execution.
- Initial compile attempt failed due missing Java runtime (`JAVA_HOME` unset).
- First remediation attempt with JDK 21 failed because project enforces JDK 17.
- Installed local Temurin JDK 17 and re-ran compile + smoke checks successfully.
- Final status: all six validation steps passed.

## Evidence Logs
- `docs/evidence/har-398-test-check-hardcoded-ui-text-literals-20260506T023641Z.log`
- `docs/evidence/har-398-check-hardcoded-ui-text-literals-20260506T023641Z.log`
- `docs/evidence/har-398-option-regressions-runner-20260506T023641Z.log`
- `docs/evidence/har-398-compileDebugKotlin-20260506T023641Z.log` (expected env failure: no JAVA_HOME)
- `docs/evidence/har-398-compileDebugKotlin-20260506T024009Z.log` (expected policy failure: JDK 21 not allowed)
- `docs/evidence/har-398-compileDebugKotlin-20260506T024027Z.log` (pass on JDK 17)
- `docs/evidence/har-398-smoke-list-avds-20260506T024027Z.log`
- `docs/evidence/har-398-smoke-install-launch-20260506T024027Z.log`
