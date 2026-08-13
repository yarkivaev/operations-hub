# operations-hub

Industry-neutral operation-graph scheduling (`yarkivaev:operations-hub`).

Examples and fixtures use a school timetable (class, lesson, room, teacher).
Do not add manufacturing catalogs or locale-specific production names.

## Modules

| Module | Artifact | Application logic |
|--------|----------|-------------------|
| root | `operations-hub` | No |
| `plan-export/` | `operations-hub-plan-export` | No — CSV of `Plan` |
| `plan-http/` | `operations-hub-plan-http` | No — read HTTP over graph + `Plan` |

## Core API (0.5)

- Entry: `Schedule.plan(operations, constraints, resources, now, prior = Plan.empty)`
- Warm-start: feasible atomic rows from `prior` are kept; remainder is placed greedily
- Graph: `Operation(id, body, successor, actual)` with `Body.Atom|Composite` and `Successor.Done|Then|All|OneOf`
- Compatibility: `Operation(id, kind, successor)` builds `Body.Atom(kind)`
- Resource: catalog identity (`id`, `tags`)
- Constraints: `Capacity`, `Forbidden`, `SameInterval` — world of one planning run (atoms only for `SameInterval`)
- Interval: `[start, end)` plus optional `ResourceId` (composites use envelope, no resource)
- Read: `Projections.timeline(operations, plan)`
- Packages: `hub.operation`, `hub.resource`, `hub.schedule`, `hub.projection`

Prefer Scaladoc on public types for field-level meaning.
