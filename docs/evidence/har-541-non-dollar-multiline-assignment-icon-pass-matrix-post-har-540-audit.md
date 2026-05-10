# HAR-541 Audit — Non-Dollar Multiline-Assignment Icon Pass Matrix (Post HAR-540)

## Scope
Re-audited reordered named `Icon(...)` pass fixtures for multiline-assignment plain interpolation using non-dollar content (`"Play ${title}"` / `"""Play ${title}"""`) after HAR-540.

## Coverage Matrix
Escaped multiline-assignment (`"Play ${title}"`):
- No comment: covered (`scripts/test-check-hardcoded-ui-text-literals.sh:641`)
- Trailing line-comment: covered (`...:646`)
- Trailing explicit line-comment marker: covered (`...:651`)
- Trailing block-comment: covered (`...:656`)
- Trailing explicit block-comment marker: covered (`...:661`)
- Leading line-comment: covered (`...:666`)
- Leading block-comment (single-line): covered (`...:672`)
- Leading block-comment (multiline): covered (`...:677`)
- Leading block-comment (widened close-block): covered (`...:683`)

Raw multiline-assignment (`"""Play ${title}"""`):
- No comment: covered (`scripts/test-check-hardcoded-ui-text-literals.sh:840`)
- Trailing line-comment: covered (`...:845`)
- Trailing explicit line-comment marker: covered (`...:850`)
- Trailing block-comment: covered (`...:860`)
- Trailing explicit block-comment marker: covered (`...:855`)
- Leading line-comment: covered (`...:870`)
- Leading block-comment (single-line): covered (`...:876`)
- Leading block-comment (multiline): covered (`...:881`)
- Leading block-comment (widened close-block): covered (`...:887`)

## Next Uncovered Fixture
The post-HAR-540 matrix above is complete. The next uncovered adjacent fixture is the explicit-marker leading line-comment variant (currently no `// localized leading line-comment` match in this multiline-assignment non-dollar slice):
- `contentDescription =`
- `    // localized leading line-comment`
- `    "Play ${title}",`
- `imageVector = Icons.Outlined.PlayArrow`

Follow-up raw counterpart (same explicit leading marker text with `"""Play ${title}"""`) remains uncovered as well.

## Validation
- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

## Evidence
- Matrix log: `docs/evidence/har-541-icon-plain-interpolation-non-dollar-multiline-assignment-comment-matrix-audit-20260510T190438Z.log`
- Validation logs:
  - `docs/evidence/har-541-test-check-hardcoded-ui-text-literals-20260510T190542Z.log`
  - `docs/evidence/har-541-check-hardcoded-ui-text-literals-20260510T190542Z.log`
