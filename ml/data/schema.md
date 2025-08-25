# Dataset Schema (TES Minimal)

Episode fields:
- number: integer
- title: string
- importance: 1..5
  5: critical (must-watch), 4: major, 3: important, 2: minor, 1: flavor
- arcs: string[] (labels from controlled vocabulary)
- summary_short: <= 120 chars, author's brief notes (no copyrighted text)

Show fields:
- core_arcs: subset of arcs to guarantee coverage

Arcs vocabulary (got):
- "Stark Family"
- "War of the Five Kings"
- "White Walkers"
- "Daenerys Rise"
- "Lannister Power"
- "Night's Watch"
- "Politics in King's Landing"
- "North vs South"
- "Beyond the Wall"
- "Essos Politics"