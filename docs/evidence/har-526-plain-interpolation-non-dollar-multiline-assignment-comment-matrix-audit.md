# HAR-526 Audit — Plain-Interpolation Non-Dollar Multiline-Assignment Comment Matrix

## Scope
Audited reordered named `Icon(...)` pass fixtures for multiline-assignment plain interpolation using non-dollar content (`"Play ${title}"` / `"""Play ${title}"""`).

## Coverage Matrix
Escaped multiline-assignment (`"Play ${title}"`):
- No comment: `scripts/test-check-hardcoded-ui-text-literals.sh:640`
- Trailing line-comment: `...:645`
- Leading line-comment: `...:650`
- Trailing block-comment: **missing**
- Leading block-comment (single-line): **missing**
- Leading block-comment (multiline): **missing**

Raw multiline-assignment (`"""Play ${title}"""`):
- No comment: `...:807`
- Trailing line-comment: **missing**
- Leading line-comment: **missing**
- Trailing block-comment: **missing**
- Leading block-comment (single-line): **missing**
- Leading block-comment (multiline): **missing**

## Uncovered Fixtures Added
Added the escaped multiline-assignment leading line-comment variant:
- `contentDescription =`
- `    // localized`
- `    "Play ${title}",`
- `imageVector = Icons.Outlined.PlayArrow`

Added the raw multiline-assignment no-comment variant:
- `contentDescription =`
- `    """Play ${title}"""`
- `imageVector = Icons.Outlined.PlayArrow`

## Evidence
- Matrix log: `docs/evidence/har-526-icon-plain-interpolation-non-dollar-multiline-assignment-comment-matrix-audit-20260510T105443Z.log`

## Next Action
Add one missing raw multiline-assignment line-comment or block-comment variant, then re-audit remaining non-dollar cells.
