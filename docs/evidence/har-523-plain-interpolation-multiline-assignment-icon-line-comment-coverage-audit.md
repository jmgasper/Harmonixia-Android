# HAR-523 Audit — Plain-Interpolation Multiline-Assignment Icon Line-Comment Coverage (Post HAR-522)

## Scope
Re-audited escaped and raw reordered named `Icon(...)` pass fixtures for multiline-assignment plain interpolation using `Play ${title} for \$5`, focused on line-comment coverage completeness after HAR-522.

## Observed Coverage
Escaped multiline-assignment (`"Play ${title} for \$5"`):
- Trailing line-comment: `scripts/test-check-hardcoded-ui-text-literals.sh:698`
- Leading line-comment: `scripts/test-check-hardcoded-ui-text-literals.sh:703`

Raw multiline-assignment (`"""Play ${title} for \$5"""`):
- Trailing line-comment: `scripts/test-check-hardcoded-ui-text-literals.sh:840`
- Leading line-comment: **missing**

## Gap Identified
The raw reordered named multiline-assignment leading line-comment variant is missing:
- target form:
  - `contentDescription =`
  - `    // localized`
  - `    """Play ${title} for \$5""",`
  - `imageVector = Icons.Outlined.PlayArrow`

## Validation
- Matrix evidence log: `docs/evidence/har-523-icon-plain-interpolation-multiline-assignment-line-comment-coverage-audit-20260510T104201Z.log`

## Next Action
Create the next child issue to add the missing raw multiline-assignment leading line-comment plain-interpolation pass fixture and verify scanner pass behavior.
