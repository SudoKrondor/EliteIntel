#!/usr/bin/env python3
"""Documentation checks for the BindForge / StarVizion docs.

Two checks, both over docs/ and CLAUDE.md:

  1. TERMINOLOGY - phrases that describe something that cannot exist, given how Elite
     Dangerous actually stores input configuration. See docs/00-overview/input-environment.md.
     These are ERRORS: they fail the run.

     Plus a softer pass that counts bare uses of "commander" where "user" is probably meant.
     These are WARNINGS while the cleanup is in progress, and are meant to be promoted to
     errors once it is finished.

  2. LINKS - relative links to files that do not exist, and #anchors that no heading produces.
     Also ERRORS.

Usage: python scripts/check_docs.py

Exit code 0 when clean, 1 when any error was found. Runs from anywhere; paths are resolved
from this file's location, not the working directory.

To silence a line deliberately - a definition page quoting a wrong form, for example - put
`<!-- terminology-ok -->` on that line.
"""

import io
import os
import re
import sys
import urllib.parse

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
# Match the opening of the marker, so a line can carry a reason:
#     <!-- terminology-ok: verbatim javadoc, quoted as written -->
SUPPRESS = '<!-- terminology-ok'

# Filled in as the checks run. A run that reports no errors should also say what it
# read - "0 errors" because nothing was scanned looks identical to a clean pass.
COVERAGE = {'files': 0, 'headings': 0, 'links': 0, 'lines': 0, 'skipped': 0}

# Files that are allowed to talk about commanders owning things, because they are not ours:
# Krondor's architecture proposal (his side genuinely is per-commander), and the archive,
# which is closed history and must never be edited.
SKIP_TERMINOLOGY = (
    'docs/99-archive',
    'docs/multi-install-proposal.md',
    'docs/Multiple-Installs.md',
)

# Files where a bare "commander" is the RIGHT word, so only the impossible constructions above
# are checked. Two reasons, both legitimate:
#
#   - it is Elite-Intel's own code vocabulary. CommanderThought, submitCommanderInput,
#     maxCommanderToolCalls and HudCommanderBlock are real identifiers; prose that drifts from
#     the class names is worse than prose that uses a word we avoid elsewhere. Renaming any of
#     it is Krondor's call, not a documentation sweep's.
#   - the file defines the words, so it has to be able to say them.
SKIP_BARE_COMMANDER = SKIP_TERMINOLOGY + (
    'docs/VEGA_ARCHITECTURE.md',            # CommanderThought, submitCommanderInput, COMMANDER turns
    'docs/ED_HUD_REFERENCE.md',             # the HudCommanderBlock widget
    'docs/00-overview/input-environment.md',  # defines the terms
    'CLAUDE.md',                            # defines the terms, and quotes the code vocabulary
)

# ---------------------------------------------------------------------------
# 1. Terminology
# ---------------------------------------------------------------------------

# Constructions that are wrong by construction. A commander owns no input file; .binds and
# StartPreset are one shared set per player, never per installation.
IMPOSSIBLE = [
    (r"per[-\s]commanders?\b",
     "no input file is scoped to a commander"),
    (r"commander'?s\s+(?:own\s+)?install(?:ation)?\b",
     "installations belong to the player, not to a commander"),
    (r"(?:each|every|the|a)\s+commander'?s\s+(?:\w+\s+){0,3}?"
     r"(?:\.?binds|bindings|DeviceMappings|buttonMaps?|button\s*maps?|StartPreset|presets?)\b",
     "no input file is scoped to a commander"),
    (r"per[-\s]install(?:ation)?\s+(?:\w+\s+){0,2}?(?:\.binds|StartPreset)\b",
     ".binds and StartPreset are one shared set per player"),
    (r"(?:each|every)\s+install(?:ation)?'?s\s+(?:\w+\s+){0,2}?(?:\.binds|StartPreset)\b",
     ".binds and StartPreset are one shared set per player"),
    (r"\bcommander[-\s]aware\b",
     "BindForge never branches on commander"),
    # Deliberately narrow. "Several commanders on one install" is TRUE and documented
    # (CMDR Reise Lang's three launchers), so phrasings about commanders coexisting are fine.
    # What is wrong is a commander *owning* a copy of something.
    (r"\bcommanders?\s+(?:each\s+)?(?:have|has|keeps?|owns?)\s+(?:their|its|his|her|a)\s+own\b",
     "commanders own no input file - the player owns one set, each installation owns another"),
]

# A bare "commander" is usually the wrong word for the human. It is legitimate when the
# sentence is actually about accounts, avatars, journals or Krondor's per-commander storage.
LEGITIMATE_NEARBY = re.compile(
    r"journal|FID|cmdr_|avatar|account|Frontier\s+account|LoadGame|multiple[-\s]commanders",
    re.I)
BARE_COMMANDER = re.compile(r"\bcommanders?\b", re.I)


def markdown_files():
    for base, dirs, files in os.walk(os.path.join(ROOT, 'docs')):
        dirs[:] = [d for d in dirs if d != 'node_modules']
        for name in sorted(files):
            if name.endswith('.md'):
                yield os.path.normpath(os.path.join(base, name))
    claude = os.path.normpath(os.path.join(ROOT, 'CLAUDE.md'))
    if os.path.exists(claude):
        yield claude


def rel(path):
    """Repo-relative, forward-slashed - for skip matching and for printing."""
    return os.path.relpath(path, ROOT).replace(os.sep, '/')


def skipped(path):
    return any(rel(path).startswith(s) for s in SKIP_TERMINOLOGY)


def check_terminology():
    errors = []
    for path in markdown_files():
        if skipped(path):
            COVERAGE['skipped'] += 1
            continue
        bare_ok = any(rel(path).startswith(s) for s in SKIP_BARE_COMMANDER)
        for n, line in enumerate(io.open(path, encoding='utf-8'), 1):
            COVERAGE['lines'] += 1
            if SUPPRESS in line:
                continue
            for pattern, why in IMPOSSIBLE:
                m = re.search(pattern, line, re.I)
                if m:
                    errors.append((path, n, m.group(0).strip(), why))
            if bare_ok:
                continue
            if BARE_COMMANDER.search(line) and not LEGITIMATE_NEARBY.search(line):
                errors.append((path, n, 'commander',
                               'say "user" for the person - a commander is a game account'))
    return errors


# ---------------------------------------------------------------------------
# 2. Links and anchors
# ---------------------------------------------------------------------------

def slug(heading):
    """GitHub's heading-to-anchor rule, near enough for our headings."""
    h = re.sub(r'\[([^\]]*)\]\([^)]*\)', r'\1', heading.strip())
    h = re.sub(r'`|\*\*|~~|\*', '', h).lower()
    h = re.sub(r'[^\w\s-]', '', h)
    return h.replace(' ', '-')


def check_links():
    anchors = {}
    for path in markdown_files():
        found = set()
        for line in io.open(path, encoding='utf-8'):
            m = re.match(r'#{1,6}\s+(.*)', line)
            if m:
                found.add(slug(m.group(1)))
        anchors[path] = found
    COVERAGE['files'] = len(anchors)
    COVERAGE['headings'] = sum(len(a) for a in anchors.values())

    errors = []
    for path in anchors:
        text = io.open(path, encoding='utf-8').read()
        for m in re.finditer(r'\]\(([^)]+)\)', text):
            link = m.group(1)
            if link.startswith(('http://', 'https://', 'mailto:')):
                continue
            COVERAGE['links'] += 1
            filepart, _, anchor = link.partition('#')
            n = text.count('\n', 0, m.start()) + 1
            if filepart:
                target = os.path.normpath(os.path.join(os.path.dirname(path), filepart))
                if not os.path.exists(target):
                    errors.append((path, n, link, 'no such file'))
                    continue
            else:
                target = path
            if anchor and anchors.get(target):
                if urllib.parse.unquote(anchor) not in anchors[target]:
                    errors.append((path, n, link, 'no heading makes that anchor'))
    return errors


# ---------------------------------------------------------------------------

def main():
    term_errors = check_terminology()
    link_errors = check_links()

    if term_errors:
        print('TERMINOLOGY:')
        print('  definitions: docs/00-overview/input-environment.md')
        print('  a line that genuinely needs the word can carry '
          '<!-- terminology-ok: reason -->')
        for path, n, text, why in term_errors:
            print('  %s:%d  "%s"' % (rel(path), n, text))
            print('      %s' % why)
        print()

    if link_errors:
        print('BROKEN LINKS:')
        for path, n, link, why in link_errors:
            print('  %s:%d  %s  - %s' % (rel(path), n, link, why))
        print()

    print('Checked %d markdown files: %d internal links and %d headings across all of them, '
          'and %d lines of prose for terminology (%d files are exempt from that - see '
          'SKIP_TERMINOLOGY).'
          % (COVERAGE['files'], COVERAGE['links'], COVERAGE['headings'],
             COVERAGE['lines'], COVERAGE['skipped']))
    total = len(term_errors) + len(link_errors)
    print('%d error(s).' % total)
    return 1 if total else 0


if __name__ == '__main__':
    sys.exit(main())
