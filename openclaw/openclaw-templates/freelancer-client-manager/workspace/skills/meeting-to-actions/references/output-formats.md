# Alternative output formats

## Executive brief
Five lines maximum, for someone senior who was not there.

```
<Meeting title>, <date>
Outcome: <one sentence>
Decided: <decision 1>; <decision 2>
Needs you: <anything requiring this reader's input or approval, or "Nothing">
Next: <the most important action item, owner, date>
```

## Standup
One block per person who spoke.

```
### <Name>
Done: <items>
Today: <items>
Blockers: <items or "None">
```
End with a "Team blockers" list of anything blocking more than one person, and any action item agreed during the standup.

## Notion / Markdown action table
Only the action items, for pasting into a task tracker.

```
| Status | Owner | Task | Due | Meeting | Source |
|---|---|---|---|---|---|
| To do | Priya | Send revised pricing deck to Acme | 2026-10-21 | Product sync 2026-10-18 | 12:40 |
```

## JSON
Use this schema exactly when the user asks for JSON or wants to feed the result into another tool. Output only the JSON in a fenced block. Use `null` for unknown values, never empty strings.

```json
{
  "meeting": {
    "title": "string",
    "date": "YYYY-MM-DD",
    "attendees": ["string"]
  },
  "summary": "string",
  "decisions": [
    { "text": "string", "changed_during_meeting": false }
  ],
  "action_items": [
    {
      "id": 1,
      "owner": "string or \"Unassigned\"",
      "task": "string",
      "due": "YYYY-MM-DD or null",
      "due_as_said": "string or null",
      "depends_on": [2],
      "source": "string (quote or timestamp)"
    }
  ],
  "open_questions": [
    { "text": "string", "raised_by": "string or null" }
  ],
  "risks": ["string"]
}
```

## Combined tracker for a series of meetings

```
| # | Owner | Task | Due | Status | First raised | Last update |
|---|---|---|---|---|---|---|
| 1 | Priya | Send revised pricing deck | 2026-10-21 | Done | 2026-10-11 | 2026-10-18: "deck went out Monday" |
```
Status values: `New`, `Open`, `Done`, `Overdue`, `Dropped` (explicitly cancelled in a meeting).
