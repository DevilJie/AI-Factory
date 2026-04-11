# Phase 17: AI Generation Constraints - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-11
**Phase:** 17-ai-generation-constraints
**Areas discussed:** Status auto-update, Constraint detail level, Constraint language style

---

## Status Auto-Update After Generation

| Option | Description | Selected |
|--------|-------------|----------|
| Auto-update | 章节生成后自动更新伏笔状态（pending→in_progress, in_progress→completed） | ✓ |
| Manual confirm | 保持手动管理，用户在前端确认后才更新 | |
| Semi-auto | 自动更新埋设，回收需手动确认 | |

**User's choice:** Auto-update
**Notes:** Phase 16 D-05 explicitly deferred status updates to Phase 17. Auto-update is the simplest and most practical approach.

### Follow-up: Re-generation handling

| Option | Description | Selected |
|--------|-------------|----------|
| No rollback | 重新生成时保持已更新状态 | ✓ |
| Rollback status | 重新生成时回滚到之前状态 | |

**User's choice:** No rollback
**Notes:** Re-generation is typically for content changes, not foreshadowing changes. Keeping status is simpler and more practical.

---

## Constraint Detail Level

| Option | Description | Selected |
|--------|-------------|----------|
| Title only | 最简洁但 LLM 可能不知道如何埋设/回收 | |
| Title + Description | LLM 知道伏笔内容和预期写法，token 占用适中 | ✓ |
| Full info | 标题+描述+类型+布局线，信息最全但 token 占用最高 | |

**User's choice:** Title + Description
**Notes:** Generation phase doesn't need planning phase's full info (type, layout line). Title + description gives LLM enough context without excessive token usage.

---

## Constraint Language Style

| Option | Description | Selected |
|--------|-------------|----------|
| Directive paragraph | 指令式段落（"必须埋设/回收"），LLM 遵从度高 | ✓ |
| Concise list | 简洁信息导向，缺少指令感 | |
| Narrative guidance | 柔和叙述引导，占用 token 多 | |

**User's choice:** Directive paragraph
**Notes:** Consistent with Phase 13 character constraint style ("必须严格遵循"). Strong directive language for high LLM compliance while maintaining natural writing flow.

---

## Claude's Discretion

- Constraint text exact wording and formatting
- Whether to show empty prompt when no foreshadowing or omit entirely
- Status update timing (immediately after generation vs during generation)
- Batch status update optimization

## Deferred Ideas

None — discussion stayed within phase scope
