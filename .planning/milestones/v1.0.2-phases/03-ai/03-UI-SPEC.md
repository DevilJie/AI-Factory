---
phase: 3
slug: ai
status: approved
reviewed_at: 2026-04-02
shadcn_initialized: false
preset: none
created: 2026-04-02
---

# Phase 3 -- UI Design Contract

> Visual and interaction contract for frontend phases. This phase is a **backend-only** phase (AI integration and prompt migration). No frontend UI is delivered. The contract below documents the XML output format that the AI produces, which downstream Phase 4 (FactionTree.vue) will consume and render.

---

## Design System

| Property | Value |
|----------|-------|
| Tool | none |
| Preset | not applicable |
| Component library | none (no frontend changes this phase) |
| Icon library | Lucide Vue Next (existing, unchanged) |
| Font | System stack: -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, sans-serif |

**Note:** Phase 3 modifies only backend Java files and a database prompt template row. No Vue components, CSS, or HTML are touched. The design system documented here records the established patterns for reference by Phase 4 and Phase 5 planners.

---

## Phase Scope Clarification

| Aspect | In Phase 3 | Not in Phase 3 |
|--------|------------|----------------|
| AI prompt template | Update to structured XML | -- |
| DOM XML parsing | saveFactionsFromXml() | -- |
| getForces() migration | 6+ call sites | -- |
| Frontend FactionTree.vue | -- | Phase 4 |
| Frontend association UI | -- | Phase 5 |
| shadcn initialization | -- | Phase 4 (if needed) |

---

## Spacing Scale

Phase 3 has no frontend changes. The spacing scale is recorded from existing GeographyTree.vue and UI components for Phase 4 reference.

Declared values (multiples of 4):

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4px | Icon gaps, inline padding |
| sm | 8px | Compact element spacing, scrollbar width |
| md | 16px | Default element spacing, card padding (p-4) |
| lg | 24px | Section padding (p-6) |
| xl | 32px | Layout gaps |
| 2xl | 48px | Major section breaks |
| 3xl | 64px | Page-level spacing |

Exceptions: Tree indentation uses 20px per level (`20 + deep * 20`), established by GeographyTree.vue paddingLeft().

---

## Typography

Established values from existing frontend (GeographyTree.vue, Btn.vue, Card.vue):

| Role | Size | Weight | Line Height |
|------|------|--------|-------------|
| Body | 14px (text-sm) | 400 (font-medium = 500 for labels) | default (1.5) |
| Label | 12px (text-xs) | 400 | default |
| Heading | 14px (text-sm, font-medium) | 500 | default |
| Display | 16px (text-base) | 400 | default |

---

## Color

Established values from main.css CSS variables and GeographyTree.vue:

| Role | Value | Usage |
|------|-------|-------|
| Dominant (60%) | #ffffff / gray-800 (dark) | Background, card surfaces |
| Secondary (30%) | gray-50 / gray-700 (dark) | Hover states, nested containers, sidebar |
| Accent (10%) | #3b82f6 (blue-500) | Add buttons, focus rings, primary actions, active states |
| Destructive | #ef4444 (red-500) | Delete buttons, error states |

Accent reserved for: Add/Create buttons, focus ring on inputs, primary CTA buttons, selected/active indicators.

Type-specific colors (for Phase 4 faction rendering, derived from GeographyTree level colors):
- positive (ally): green-500 / green-400 (dark)
- hostile: red-500 / red-400 (dark)
- neutral: amber-500 / amber-400 (dark)

---

## Copywriting Contract

Phase 3 has no user-facing UI copy. The following are the AI prompt template instructions and log messages that serve as the "interface" for this phase:

| Element | Copy |
|---------|------|
| Log: faction section not found | `未找到 <f> 势力标签，跳过入库` |
| Log: empty faction parse result | `势力解析结果为空，跳过入库` |
| Log: faction save success | `势力入库完成，projectId={id}, 根节点数={count}` |
| Log: faction save failure | `保存势力失败，projectId={id}` |
| Log: region name not matched | `未匹配到地区名称: {name}, 跳过关联` |
| Log: faction name not matched | `未匹配到势力名称: {name}, 跳过关系` |
| Log: power system not matched | `未匹配到力量体系: {name}` |
| Log: all tiers failed | `三级匹配均失败，未找到地区: {name}` |

---

## AI Output XML Format Contract

This is the core "design" artifact of Phase 3 -- the structured XML format that the AI prompt template must produce and the DOM parser must consume.

### XML Schema

```xml
<f>
  <faction>
    <n>{faction_name}</n>
    <type>{ally|hostile|neutral}</type>
    <power>{power_system_name}</power>
    <regions>{region_name_1},{region_name_2}</regions>
    <d><![CDATA[description text]]></d>
    <relation>
      <target>{target_faction_name}</target>
      <type>{allied|hostile|neutral}</type>
    </relation>
    <faction>
      <n>{child_faction_name}</n>
      <d><![CDATA[child description]]></d>
    </faction>
  </faction>
</f>
```

### Element Rules

| Element | Required | Level | Notes |
|---------|----------|-------|-------|
| `<f>` | Yes | Root | Container, wraps all factions |
| `<faction>` | Yes | Top-level, nestable | Each is a faction node; nesting expresses hierarchy |
| `<n>` | Yes | Inside `<faction>` | Faction name, plain text |
| `<type>` | Top-level factions only | Inside top-level `<faction>` | Values: ally, hostile, neutral. Child factions inherit. |
| `<power>` | Top-level factions only | Inside top-level `<faction>` | Name reference to a power system in `<p>` output |
| `<regions>` | Optional | Inside `<faction>` | Comma-separated region names from `<g>` output |
| `<d>` | Optional | Inside `<faction>` | Description, CDATA-wrapped |
| `<relation>` | Optional | Inside `<faction>` | Faction-faction relationship |
| `<target>` | Yes | Inside `<relation>` | Target faction name |
| `<type>` (in relation) | Yes | Inside `<relation>` | Values: allied, hostile, neutral |

### Name Matching Contract (D-06)

Three-tier strategy for resolving AI-output names to database IDs:

1. **Exact match**: Direct string equality
2. **Strip suffix match**: Remove trailing faction suffixes (regex: `[宗派门殿阁会帮谷山城族教院宫楼庄寨盟宗门派门]$`) then compare
3. **Contains match**: Either string contains the other

If all three tiers fail: log WARN and skip the reference. Do not throw or abort.

---

## Registry Safety

| Registry | Blocks Used | Safety Gate |
|----------|-------------|-------------|
| shadcn official | none (no frontend this phase) | not applicable |
| third-party | none | not applicable |

---

## Downstream Impact on Phase 4

Phase 4 will consume the structured data created by Phase 3. Key contracts for the Phase 4 UI-SPEC:

1. **FactionTree.vue** will render `NovelFaction` entities with `name`, `type`, `corePowerSystem`, `description`, and `children[]`
2. **Type labels**: ally = "正派" (green), hostile = "反派" (red), neutral = "中立" (amber)
3. **Power system labels**: Rendered as tags/badges, resolved by name from `corePowerSystem` ID
4. **Tree structure**: Same parent_id + deep pattern as GeographyTree.vue
5. **API contract**: FactionTree will call `GET /api/faction/tree/{projectId}` to load data

---

## Checker Sign-Off

- [ ] Dimension 1 Copywriting: PASS (no user-facing copy; log messages documented)
- [ ] Dimension 2 Visuals: PASS (no visual changes; XML format contract documented)
- [ ] Dimension 3 Color: PASS (no color changes; established palette recorded for Phase 4)
- [ ] Dimension 4 Typography: PASS (no typography changes; established values recorded)
- [ ] Dimension 5 Spacing: PASS (no spacing changes; established scale recorded)
- [ ] Dimension 6 Registry Safety: PASS (no registry usage)

**Approval:** pending
