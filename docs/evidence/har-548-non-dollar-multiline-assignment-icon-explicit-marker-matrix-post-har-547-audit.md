# HAR-548 Audit - Non-Dollar Multiline-Assignment Icon Explicit-Marker Matrix (Post HAR-547)

## Scope
Re-audited reordered named `Icon(...)` pass fixtures for non-dollar multiline-assignment plain interpolation after HAR-547, focused on explicit leading/trailing marker coverage for `"Play ${title}"` and `"""Play ${title}"""`.

## Coverage Matrix
Escaped multiline-assignment (`"Play ${title}"`):
- Trailing explicit line-comment marker: covered (`scripts/test-check-hardcoded-ui-text-literals.sh:651`)
- Trailing explicit block-comment marker (single-line): covered (`...:661`)
- Trailing explicit block-comment marker (multiline close): **missing**
- Leading explicit line-comment marker: covered (`...:672`)
- Leading explicit block-comment marker (single-line): covered (`...:683`)
- Leading explicit block-comment marker (multiline close): covered (`...:688`)

Raw multiline-assignment (`"""Play ${title}"""`):
- Trailing explicit line-comment marker: covered (`scripts/test-check-hardcoded-ui-text-literals.sh:867`)
- Trailing explicit block-comment marker (single-line): covered (`...:872`)
- Trailing explicit block-comment marker (multiline close): **missing**
- Leading explicit line-comment marker: covered (`...:893`)
- Leading explicit block-comment marker (single-line): covered (`...:904`)
- Leading explicit block-comment marker (multiline close): covered (`...:909`)

## Next Uncovered Fixture
Define the next uncovered fixture cell as escaped non-dollar multiline-assignment trailing explicit block-comment marker with multiline close:
- `contentDescription =`
- `    "Play ${title}" /* localized trailing block-comment`
- `        */,`
- `imageVector = Icons.Outlined.PlayArrow`

Raw counterpart with the same multiline explicit trailing marker remains uncovered after this cell:
- `    """Play ${title}""" /* localized trailing block-comment`
- `        */,`

## Validation
- `./scripts/test-check-hardcoded-ui-text-literals.sh`
- `./scripts/check-hardcoded-ui-text-literals.sh`

Both commands passed.

## Evidence
- Matrix log: `docs/evidence/har-548-icon-plain-interpolation-non-dollar-multiline-assignment-explicit-marker-matrix-post-har-547-audit-20260510T213004Z.log`
- Validation logs:
  - `docs/evidence/har-548-test-check-hardcoded-ui-text-literals-20260510T213019Z.log`
  - `docs/evidence/har-548-check-hardcoded-ui-text-literals-20260510T213019Z.log`
