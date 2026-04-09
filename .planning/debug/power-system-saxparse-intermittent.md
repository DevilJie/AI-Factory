---
status: awaiting_human_verify
trigger: "power-system-saxparse-intermittent: WorldviewXmlParser main method throws SAXParseException about unclosed ss tag intermittently"
created: 2026-04-08T00:00:00Z
updated: 2026-04-08T00:15:00Z
---

## Current Focus

hypothesis: CONFIRMED AND FIXED - Sanitizer used lastIndexOf which removed the wrong closing tag, leaving valid elements unclosed. Fixed with stack-based approach.
test: All 34 WorldviewXmlParser tests pass including new test for stray closing tags
expecting: User verifies the fix works in their real environment
next_action: Wait for human verification

## Symptoms

expected: Power system XML should parse successfully every time
actual: SAXParseException: 元素类型 "ss" 必须由匹配的结束标记 "</ss>" 终止 (element type "ss" must be terminated by matching end tag)
errors: org.xml.sax.SAXParseException: 元素类型 "ss" 必须由匹配的结束标记 "</ss>" 终止
reproduction: Run the main method in WorldviewXmlParser.java around line 197. The XML content is hardcoded in the main method.
timeline: Intermittent - sometimes works, sometimes fails with same XML. User verified XML format looks correct.

## Eliminated

## Evidence

- timestamp: 2026-04-08T00:01:00Z
  checked: WorldviewXmlParser.java main() method XML (lines 197-259)
  found: |
    The XML contains 5 level blocks under <ss>. Examining the structure:
    1. Lines 204-214: <ll>...</ll> (觉醒境) - properly wrapped
    2. Lines 215-225: <ll>...</ll> (蜕变境) - properly wrapped
    3. Lines 226-236: <ll>...</ll> (真形境) - properly wrapped
    4. Lines 237-246: NO <ll> wrapper! Has <ln>法身境</ln> and child tags but missing <ll>...</ll>
    5. Lines 247-257: <ll>...</ll> (混沌境) - properly wrapped
  implication: Malformed XML in main() - missing <ll> wrapper on 4th level block.

- timestamp: 2026-04-08T00:05:00Z
  checked: sanitizeXmlForDomParsing tag balance fix (lines 765-776)
  found: |
    When closeCount > openCount (excess closes), the sanitizer used lastIndexOf to find
    and remove excess closing tags from the END of the string. This removed a VALID close tag
    (混沌境's </ll>) instead of the stray one (法身境's </ll>), leaving 混沌境's <ll> unclosed.
  implication: Need stack-based approach to correctly identify stray closing tags.

- timestamp: 2026-04-08T00:08:00Z
  checked: Whether escapeIllegalXmlChars affects this XML
  found: No raw & or < in text content for the main() XML. escapeIllegalXmlChars is a no-op here.
  implication: The bug is entirely in the tag balance sanitizer.

- timestamp: 2026-04-08T00:09:00Z
  checked: Whether the behavior is truly intermittent
  found: |
    With the exact hardcoded XML in main(), the failure is DETERMINISTIC - it always fails.
    The "intermittent" report likely refers to different LLM outputs in production.
  implication: The bug manifests whenever LLM output has an unmatched close tag for <ll> or <ss>.

- timestamp: 2026-04-08T00:14:00Z
  checked: Forward indexOf approach
  found: |
    Using indexOf (removing from the beginning) also removes the wrong tag. It removes the
    first valid </ll> (练气期's close) instead of the stray one. This causes nesting issues.
  implication: Neither forward nor backward linear search correctly identifies the stray. Need structural analysis.

- timestamp: 2026-04-08T00:15:00Z
  checked: Stack-based approach for finding stray closing tags
  found: |
    Implemented findStrayClosingPositions: walks through all open/close tag positions in order,
    tracking depth. Any close tag encountered when depth is 0 is a stray. This correctly
    identifies the stray </ll> (法身境's close) regardless of position.
  implication: Stack-based approach is the correct solution.

## Resolution

root_cause: |
  The sanitizeXmlForDomParsing method used lastIndexOf to remove excess closing tags,
  which removes from the END of the string. When LLM output has a stray closing tag
  (e.g., </ll> without a matching <ll>), the sanitizer incorrectly removed the LAST
  valid </ll> instead of the actual stray one. This left a different element unclosed,
  causing the SAXParseException about unclosed <ss> or <ll>.

  Additionally, the main() method test data had a missing <ll> opening tag for the
  法身境 level block (lines 237-246), serving as a convenient reproduction case.
fix: |
  1. Replaced the lastIndexOf-based removal with a stack-based approach (findStrayClosingPositions):
     walks through all open/close tag positions, tracks depth, identifies stray closes
     as those encountered when depth is already 0.
  2. Fixed main() test data to add missing <ll> wrapper around 法身境 block.
  3. Added regression test testParsePowerSystemXml_strayClosingTag_missingLlOpen.
verification: All 34 WorldviewXmlParser tests pass (including 1 new test). Pre-existing ChapterGeneration test failure is unrelated.
files_changed:
  - ai-factory-backend/src/main/java/com/aifactory/common/WorldviewXmlParser.java
  - ai-factory-backend/src/test/java/com/aifactory/common/WorldviewXmlParserTest.java
