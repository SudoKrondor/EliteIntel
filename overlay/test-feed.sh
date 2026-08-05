#!/usr/bin/env bash
# Drives the overlay with a scripted conversation, for manual visual checks.
#   ./overlay/test-feed.sh | ./distribution/overlays/elite-intel-overlay
set -u
T=$'\t'

emit() { printf '%s\n' "$*"; }

emit "V${T}1"
emit "CFG${T}alpha=0.25${T}scale=1.0${T}width=760"

emit "OBJ${T}MASSACRE CONTRACT${T}KREMAINN - BLOCH TERMINAL"
emit "BAR${T}PIRATES${T}12${T}20${T}normal"
emit "ROW${T}REWARD${T}4,120,000 cr${T}normal"
emit "ROW${T}EXPIRES${T}2d 04h${T}warn"
emit "END"
sleep 1

while true; do
  emit "SAY${T}CMDR${T}0${T}Nomad, what's left on the massacre contract?"
  sleep 4
  emit "SAY${T}Nomad${T}1${T}Twelve of twenty pirates down. Two contracts share the count."
  sleep 5
  emit "SAY${T}CMDR${T}0${T}Plot the next trade hop when we're done."
  sleep 4
  emit "SAY${T}Nomad${T}1${T}Gold to Titov Port. Twelve thousand four hundred eighty credits profit."
  sleep 5
  # Radio traffic (kind 2), so all three lane colours are on screen together -
  # which is the only way to judge whether they still read as one family.
  emit "SAY${T}Bloch Terminal traffic control${T}2${T}This is Bloch Terminal traffic control. Proceed to landing pad 7."
  sleep 5
done
