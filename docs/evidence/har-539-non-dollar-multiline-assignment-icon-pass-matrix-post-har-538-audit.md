# HAR-539 Audit — Non-Dollar Multiline-Assignment Icon Pass Matrix (Post HAR-538)

## Scope
Re-audited reordered named `Icon(...)` pass fixtures for multiline-assignment plain interpolation using non-dollar content after HAR-538 (`"""Play ${title}""", // localized trailing line-comment`).

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
- No comment: covered (`...:840`)
- Trailing line-comment: covered (`...:845`)
- Trailing explicit line-comment marker: covered (`...:850`)
- Trailing block-comment: covered (`...:855`)
- Trailing explicit block-comment marker: **missing**
- Leading line-comment: covered (`...:860`)
- Leading block-comment (single-line): covered (`...:866`)
- Leading block-comment (multiline): covered (`...:872`)
- Leading block-comment (widened close-block): covered (`...:878`)

## Next Uncovered Fixture
Define the next fixture as raw non-dollar multiline-assignment trailing explicit block-comment marker:
- `contentDescription =`
- `    """Play ${title}""" /* localized trailing block-comment */,`
- `imageVector = Icons.Outlined.PlayArrow`

## Evidence
- Matrix log: `docs/evidence/har-539-icon-plain-interpolation-non-dollar-multiline-assignment-comment-matrix-audit-20260510T175440Z.log`
