#!/usr/bin/env python3
"""Builds the reconcile mockups: writes one HTML page per screen, then renders each to a PNG with headless Chrome.

    python3 -I build.py            # from anywhere; writes ../NN-name.png
"""
import pathlib
import subprocess

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE.parent

S = 'stroke="#A9B8C2" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round"'
ICON = {
    "stick": f'<svg width="28" height="28" viewBox="0 0 28 28"><path {S} d="M8 25h12M10 25l1-5h6l1 5M14 20v-6"/><rect {S} x="10" y="3" width="8" height="11" rx="3"/><circle cx="16" cy="6" r="1.2" fill="#A9B8C2"/></svg>',
    "throttle": f'<svg width="28" height="28" viewBox="0 0 28 28"><path {S} d="M5 25h18M7 25l2-6h10l2 6M11 19l3-11M17 19l-1-11"/><rect {S} x="11" y="4" width="7" height="5" rx="1.5"/></svg>',
    "pedals": f'<svg width="28" height="28" viewBox="0 0 28 28"><rect {S} x="4" y="17" width="20" height="6" rx="1"/><path {S} d="M7 17l2-11h3l-1 11M21 17l-2-11h-3l1 11"/></svg>',
    "yoke": f'<svg width="28" height="28" viewBox="0 0 28 28"><path {S} d="M4 8v6c0 2 2 3 4 3h12c2 0 4-1 4-3V8M8 17v-3h12v3M14 17v8"/></svg>',
    "pad": f'<svg width="28" height="28" viewBox="0 0 28 28"><path {S} d="M8 9h12c3 0 5 3 5 8 0 3-1 4-3 4-1.5 0-2.5-2-4-3h-8c-1.5 1-2.5 3-4 3-2 0-3-1-3-4 0-5 2-8 5-8z"/><path {S} d="M9 13v4M7 15h4"/><circle cx="19" cy="14" r="1" fill="#A9B8C2"/><circle cx="21" cy="16" r="1" fill="#A9B8C2"/></svg>',
    "panel": f'<svg width="28" height="28" viewBox="0 0 28 28"><rect {S} x="3" y="7" width="22" height="14" rx="1.5"/><circle {S} cx="9" cy="14" r="2.5"/><path {S} d="M15 11h6M15 14h6M15 17h4"/></svg>',
}
SHIELD = '<svg width="22" height="22" viewBox="0 0 24 24"><path d="M12 2l8 3v6c0 5-3.5 9-8 11-4.5-2-8-6-8-11V5z" fill="none" stroke="#7CE8A8" stroke-width="1.8"/><path d="M8 12l3 3 5-6" fill="none" stroke="#7CE8A8" stroke-width="1.8"/></svg>'


def ctrl(icon, name, sub=""):
    sub_html = f'<div class="sub">{sub}</div>' if sub else ""
    return f'<div class="ctrl"><div class="icon">{ICON[icon]}</div><div><div class="name">{name}</div>{sub_html}</div></div>'


def steps(now):
    names = ["Plug in", "Choose", "Check", "Done"]
    out = []
    for i, n in enumerate(names, 1):
        cls = "done" if i < now else "now" if i == now else ""
        mark = "✓" if i < now else str(i)
        bar = '<span class="bar"></span>' if i < len(names) else ""
        out.append(f'<div class="step {cls}"><span class="n">{mark}</span>{n}{bar}</div>')
    return f'<div class="steps">{"".join(out)}</div>'


def page(title, inner, step=None, footer=""):
    stepper = steps(step) if step else ""
    return f"""<!doctype html><html><head><meta charset="utf-8"><link rel="stylesheet" href="style.css"></head>
<body><div class="dialog"><div class="titlebar"><h1>{title}</h1><span class="x">×</span></div>
{stepper}<div class="body">{inner}</div>{footer}</div></body></html>"""


def footer(left, right):
    return f'<div class="footer"><div class="left">{left}</div><div class="left">{right}</div></div>'


TITLE = "MAKE YOUR GAME COPIES MATCH"
SAFE = f'<span class="safe">{SHIELD} Nothing changes until the last step. Everything is backed up first.</span>'

PAGES = {}

# --- 01 Welcome ----------------------------------------------------------------------------------------------
diagram = """
<div style="display:grid;grid-template-columns:1fr 1.15fr 1fr;gap:0;align-items:center;margin:8px 0 22px">
  <div style="border:1px solid var(--frame);background:var(--panel);padding:14px 16px">
    <span class="store">STEAM</span>
    <div style="margin-top:10px;font-size:14px">Your <b>Steam</b> copy</div>
    <div class="small">has its own list of your controllers</div>
    <div class="mono" style="margin-top:10px;color:var(--text2);line-height:1.7">RVWAP · Throttle · Yoke<br>Pedals · Spare · X52 …</div>
  </div>
  <div style="text-align:center;position:relative">
    <svg width="100%" height="120" viewBox="0 0 300 120" preserveAspectRatio="none">
      <path d="M0 60 H85" stroke="#3A4C58" stroke-width="2" stroke-dasharray="5 4"/>
      <path d="M215 60 H300" stroke="#3A4C58" stroke-width="2" stroke-dasharray="5 4"/>
    </svg>
    <div style="position:absolute;inset:14px 52px;border:2px solid var(--accent-dim);background:#12303A;display:grid;place-items:center;padding:8px">
      <div><div style="font-size:15px;font-weight:600">Your controls</div>
      <div class="small" style="color:var(--text2)">one set, shared by both copies</div></div>
    </div>
  </div>
  <div style="border:1px solid var(--frame);background:var(--panel);padding:14px 16px">
    <span class="store">EPIC</span>
    <div style="margin-top:10px;font-size:14px">Your <b>Epic</b> copy</div>
    <div class="small">has its own list of your controllers</div>
    <div class="mono" style="margin-top:10px;color:var(--text2);line-height:1.7">RightStick · LVWAP<br>Throttle · Yoke · X52 …</div>
  </div>
</div>"""
PAGES["01-welcome"] = (page(TITLE, f"""
<h2>Your two copies of Elite Dangerous don't agree about your controllers</h2>
<p class="lead">You have the game from <b>Steam</b> and from <b>Epic</b>. They share one set of controls, but each copy keeps
its own list of your controllers and button names. Right now the two lists are different, so your controls may work in
one copy and fall back to keyboard and mouse in the other.</p>
{diagram}
<div style="display:flex;gap:12px;margin-bottom:20px">
  <div class="pill"><b style="color:var(--accent)">3</b> questions for you</div>
  <div class="pill"><b style="color:var(--success)">4</b> things BindForge sorts out by itself</div>
  <div class="pill"><b style="color:var(--text2)">2</b> controllers already match</div>
</div>
<div class="note">This takes about a minute. When it's done, <b>both copies will have exactly the same controllers, names
and button names</b>, and your controls will work the same whichever one you launch.</div>
""", footer=footer(SAFE, '<span class="btn">Not now</span><span class="btn primary">Let\'s fix it</span>')), 1400)

# --- 02 Plug in ----------------------------------------------------------------------------------------------
rows = [
    ("stick", "VPC Stick WarBRD+ (right)", True, ""),
    ("stick", "VPC Stick WarBRD+ (left)", True, ""),
    ("throttle", "VPC Throttle CM3", True, ""),
    ("panel", "VPC Control Panel", True, ""),
    ("yoke", "Logitech Flight Yoke", True, ""),
    ("pedals", "T-Rudder pedals", False, ""),
    ("stick", "Logitech X56 Stick", False, ""),
    ("pad", "Spare gamepad", False, ""),
    ("stick", "Saitek X52", False, ""),
]
trs = "".join(
    f'<tr class="{"hit" if i == 0 else ""}"><td>{ctrl(ic, n)}</td>'
    f'<td>{"<span class=\'conn on\'>Plugged in</span>" if on else "<span class=\'conn off\'>Not plugged in</span>"}</td>'
    f'<td class="muted" style="font-size:13px">{"← you just pressed a button on this one" if i == 0 else ""}</td></tr>'
    for i, (ic, n, on, _) in enumerate(rows))
PAGES["02-plug-in"] = (page(TITLE, f"""
<h2>Plug in the controllers you fly with</h2>
<p class="lead">Elite only loads your controls when <b>every controller they use is plugged in</b>. If one is missing when
the game starts, it quietly switches to keyboard and mouse, and it can look as if your bindings were lost.
Plug yours in now. This list updates by itself.</p>
<table><tr><th style="width:52%">Controllers your Steam or Epic copy knows</th><th>Now</th><th></th></tr>{trs}</table>
<p class="small" style="margin-top:14px">Tip: press any button on a controller and its row lights up, so you know which one it is.
A controller you no longer own can stay unplugged; you can carry on without it.</p>
""", step=1, footer=footer(SAFE, '<span class="btn">Back</span><span class="btn primary">Next</span>')), 1400)

# --- 03 Question: two names for one controller -----------------------------------------------------------------
PAGES["03-question-name"] = (page(TITLE, f"""
<div class="qcount">Question 1 of 3</div>
<div style="display:flex;justify-content:space-between;align-items:flex-end">
<h2>Your right stick has a different name in each copy</h2><span class="conn on" style="margin-bottom:12px">Plugged in</span></div>
<div style="margin:6px 0 4px">{ctrl("stick", "VPC Stick WarBRD+ (right)", "Your controls find each controller by a short name. Pick the one to keep.")}</div>
<div class="choices">
  <div class="choice sel"><span class="radio"></span>
    <div class="from"><span class="store">STEAM</span> calls it</div>
    <div class="big">RVWAP</div>
    <ul><li class="ok">131 of your controls already use this name</li>
        <li class="info">Your 12 controls that use “RightStick” are moved to it</li></ul>
  </div>
  <div class="choice"><span class="radio"></span>
    <div class="from"><span class="store">EPIC</span> calls it</div>
    <div class="big">RightStick</div>
    <ul><li class="ok">12 of your controls already use this name</li>
        <li class="info">Your 131 controls that use “RVWAP” are moved to it</li></ul>
  </div>
</div>
<div class="note reassure"><b>Nothing stops working either way.</b> Your controls follow the name you pick. This only
decides what the controller is called.</div>
""", step=2, footer=footer('<span class="link">Make everything like my Steam copy…</span>',
                            '<span class="btn">Back</span><span class="btn primary">Next question</span>')), 1400)

# --- 04 Question: one name, two controllers --------------------------------------------------------------------
PAGES["04-question-controller"] = (page(TITLE, f"""
<div class="qcount">Question 2 of 3</div>
<h2>“Yoke” means a different controller in each copy</h2>
<p class="lead" style="margin-bottom:6px">48 of your controls are set up on <span class="mono" style="color:var(--text)">Yoke</span>.
Which controller should they work on?</p>
<div class="choices">
  <div class="choice"><span class="radio"></span>
    <div class="from"><span class="store">STEAM</span> uses</div>
    <div style="margin:10px 0 12px">{ctrl("yoke", "Logitech Flight Yoke", "<span class='conn on'>Plugged in</span>")}</div>
    <ul><li class="ok">Your 48 “Yoke” controls work on this yoke</li></ul>
  </div>
  <div class="choice"><span class="radio"></span>
    <div class="from"><span class="store">EPIC</span> uses</div>
    <div style="margin:10px 0 12px">{ctrl("stick", "Logitech X56 Stick", "<span class='conn off'>Not plugged in</span>")}</div>
    <ul><li class="ok">Your 48 “Yoke” controls work on this stick</li>
        <li class="warn">It isn't plugged in. Plug it in before you play, or the game falls back to keyboard and mouse.</li></ul>
  </div>
</div>
<div class="note">Not sure? It's usually the one you still own and have plugged in. The other controller keeps
working in the game, just without your “Yoke” controls; BindForge offers it a name of its own the next time it is plugged in.</div>
""", step=2, footer=footer('<span class="link">Make everything like my Steam copy…</span>',
                            '<span class="btn">Back</span><span class="btn disabled">Next question</span>')), 1400)

# --- 05 Question: button names differ ----------------------------------------------------------------------
PAGES["05-question-buttons"] = (page(TITLE, f"""
<div class="qcount">Question 3 of 3</div>
<div style="display:flex;justify-content:space-between;align-items:flex-end">
<h2>Some buttons on your throttle are named differently</h2><span class="conn on" style="margin-bottom:12px">Plugged in</span></div>
<div style="margin:6px 0 4px">{ctrl("throttle", "VPC Throttle CM3", "Button names are what the game's Controls screen shows, e.g. “Boost” instead of “Joy 1”.")}</div>
<div class="choices three">
  <div class="choice"><span class="radio"></span>
    <div class="from"><span class="store">STEAM</span></div>
    <div class="what">Use Steam's names</div>
    <ul><li class="info">e.g. Boost, Landing gear, Cargo scoop</li></ul>
  </div>
  <div class="choice"><span class="radio"></span>
    <div class="from"><span class="store">EPIC</span></div>
    <div class="what">Use Epic's names</div>
    <ul><li class="info">e.g. Afterburner, Gear, Scoop</li></ul>
  </div>
  <div class="choice sel"><span class="radio"></span>
    <div class="from"><span class="store">STEAM</span> <span class="store">EPIC</span></div>
    <div class="what">Choose button by button</div>
    <ul><li class="ok" style="color:var(--text)">7 buttons · 3 chosen</li></ul>
    <div style="margin-top:10px"><span class="btn" style="padding:7px 14px;font-size:12px">Choose names…</span></div>
  </div>
</div>
<div class="note reassure"><b>Button names never change what a button does.</b> Names only one copy has are kept
either way: 11 more buttons are named in only one copy, so nothing is lost.</div>
""", step=2, footer=footer('<span class="link">Make everything like my Steam copy…</span>',
                            '<span class="btn">Back</span><span class="btn disabled">Check the result</span>')), 1400)

# --- 06 Button by button ------------------------------------------------------------------------------------
btn_rows = [
    ("Button 1", "Joy_1", "Boost", "Afterburner", 0, True),
    ("Button 4", "Joy_4", "Landing gear", "Gear", 0, False),
    ("Button 7", "Joy_7", "Cargo scoop", "Scoop", 1, False),
    ("Button 12", "Joy_12", "Flight assist", "FA off", None, False),
    ("Button 15", "Joy_15", "Night vision", "NV", None, False),
    ("Hat 2, up", "Joy_POV2Up", "Target ahead", "Select target", None, False),
    ("Rotary axis", "Joy_RZAxis", "Throttle", "Main throttle", None, False),
]


def chip(text, sel):
    return f'<span class="chip {"sel" if sel else ""}"><span class="dot"></span>{text}</span>'


btrs = "".join(
    f'<tr class="{"hit" if hit else ""}"><td><div style="font-weight:600">{human}</div>'
    f'<div class="small mono" style="font-size:12px">{code}</div></td>'
    f'<td>{chip(a, pick == 0)}</td><td>{chip(b, pick == 1)}</td>'
    f'<td class="muted" style="font-size:13px">{"← you pressed this" if hit else ""}</td></tr>'
    for human, code, a, b, pick, hit in btn_rows)
PAGES["06-button-by-button"] = (page("BUTTON NAMES: VPC THROTTLE CM3", f"""
<p class="lead" style="margin-bottom:14px">These 7 buttons have a different name in each copy. Click the name to keep.
<b>Press a button on the throttle</b> to jump to it.</p>
<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px">
  <span class="small">Buttons named the same in both copies, or in only one, are kept and not shown.</span>
  <span style="color:var(--accent);font-size:14px">3 of 7 chosen</span></div>
<table><tr><th style="width:22%">Button</th><th style="width:28%"><span class="store">STEAM</span> &nbsp;<span class="link" style="font-size:12px;text-transform:none;letter-spacing:0">use all</span></th>
<th style="width:28%"><span class="store">EPIC</span> &nbsp;<span class="link" style="font-size:12px;text-transform:none;letter-spacing:0">use all</span></th><th></th></tr>{btrs}</table>
""", footer=footer("", '<span class="btn">Cancel</span><span class="btn primary">Done</span>')), 1400)

# --- 07 Check -----------------------------------------------------------------------------------------------
final = [
    ("stick", "VPC Stick WarBRD+ (right)", "RVWAP", "—", "Your choice. 12 controls moved from “RightStick”", True),
    ("stick", "VPC Stick WarBRD+ (left)", "LVWAP", "—", "Was only in Epic, added to Steam", True),
    ("throttle", "VPC Throttle CM3", "Throttle", "21 named", "Your choice, button by button", True),
    ("yoke", "Logitech Flight Yoke", "Yoke", "—", "Your choice", True),
    ("pedals", "T-Rudder pedals", "Pedals", "—", "Was only in Steam, added to Epic", False),
    ("pad", "Spare gamepad", "Spare", "—", "Was only in Steam, added to Epic", False),
    ("stick", "Saitek X52", "X52", "12 named", "Already the same", False),
    ("panel", "VPC Control Panel", "VPCPanel", "9 named", "Already the same", True),
]
ftrs = "".join(
    f'<tr><td>{ctrl(ic, n)}</td><td class="mono">{nm}</td><td class="muted">{bn}</td><td style="font-size:13px;color:var(--text2)">{why}</td>'
    f'<td>{"<span class=\'conn on\'>Plugged in</span>" if on else "<span class=\'conn off\'>Not plugged in</span>"}</td></tr>'
    for ic, n, nm, bn, why, on in final)
PAGES["07-check"] = (page(TITLE, f"""
<h2>This is what both copies will have</h2>
<p class="lead" style="margin-bottom:14px">Steam and Epic will get exactly this list. Your controls are updated to match it.</p>
<table><tr><th>Controller</th><th>Name</th><th>Button names</th><th>Why</th><th>Now</th></tr>{ftrs}</table>
<div class="section-h">Also tidied up</div>
<div class="small" style="color:var(--text2)">• An old second name for the Saitek X52, <span class="mono">OldX52</span>, that the game never used is removed.
<br>• The Logitech X56 Stick loses the name “Yoke” (your choice). It keeps working, without your “Yoke” controls.</div>
<div class="note" style="margin-top:16px"><b>3 controllers aren't plugged in.</b> Your controls use the <b>T-Rudder pedals</b>, so plug them in before you
play. No control uses the spare gamepad or the Saitek X52, so they can stay unplugged.</div>
""", step=3, footer=footer(SAFE, '<span class="btn">Back</span><span class="btn primary">Make both copies match</span>')), 1400)

# --- 08 Done ------------------------------------------------------------------------------------------------
PAGES["08-done"] = (page(TITLE, f"""
<div style="display:flex;gap:16px;align-items:center;margin-bottom:10px">
<svg width="44" height="44" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10.5" fill="none" stroke="#7CE8A8" stroke-width="1.6"/><path d="M7 12.5l3.2 3.2L17 9" fill="none" stroke="#7CE8A8" stroke-width="1.8"/></svg>
<h2 style="margin:0">Both copies now match</h2></div>
<table style="margin:14px 0 20px"><tr><td style="width:38%"><span class="store">STEAM</span> &nbsp;Steam copy</td><td class="ok">✓ Updated</td></tr>
<tr><td><span class="store">EPIC</span> &nbsp;Epic copy</td><td class="ok">✓ Updated</td></tr>
<tr><td>Your controls</td><td class="ok">✓ Updated: 12 controls moved to RVWAP</td></tr></table>
<div class="note"><b>Before you play:</b> plug in the <b>T-Rudder pedals</b>. If a controller your controls use is
missing when the game starts, the game switches to keyboard and mouse.</div>
<p class="small" style="margin-top:16px">Changed your mind? Your files from before are kept forever: File Manager → Backups → <i>Before BindForge</i> → Restore.</p>
""", step=4, footer=footer('<label style="font-size:13px;color:var(--text2)"><input type="checkbox"> Don\'t show this again when there is nothing to warn about</label>',
                            '<span class="btn primary">Close</span>')), 1400)

# --- 09 Shortcut: make everything like one copy ------------------------------------------------------------
PAGES["09-shortcut-one-copy"] = (page("MAKE EVERYTHING LIKE ONE COPY", f"""
<h2>Use one copy's controllers for both?</h2>
<p class="lead">Skip the questions: everything is set up the way one copy has it. Pick the copy you play most.</p>
<div class="choices">
  <div class="choice sel"><span class="radio"></span><div class="from"><span class="store">STEAM</span></div>
    <div class="what">Like my Steam copy</div>
    <ul><li class="ok">Right stick is called RVWAP, “Yoke” is the Logitech Flight Yoke</li>
        <li class="ok">Steam's button names on the throttle</li>
        <li class="info">The left stick (LVWAP), only in Epic, is kept too</li></ul></div>
  <div class="choice"><span class="radio"></span><div class="from"><span class="store">EPIC</span></div>
    <div class="what">Like my Epic copy</div>
    <ul><li class="ok">Right stick is called RightStick, “Yoke” is the Logitech X56 Stick</li>
        <li class="ok">Epic's button names on the throttle</li>
        <li class="info">The pedals and gamepad, only in Steam, are kept too</li></ul></div>
</div>
<div class="note reassure"><b>Nothing is thrown away.</b> Controllers only the other copy knows are kept, and your controls move to the
names you end up with. You'll see the result before anything changes.</div>
""", footer=footer("", '<span class="btn">Back to the questions</span><span class="btn primary">Check the result</span>')), 1400)

# --- 10 Later: a copy changed outside BindForge ------------------------------------------------------------
PAGES["10-later-a-copy-changed"] = (page("YOUR EPIC COPY CHANGED", f"""
<h2>Your Epic copy's controllers changed since BindForge last saved them</h2>
<p class="lead">They were edited outside BindForge, by hand or by another tool.
Which do you want both copies to have?</p>
<div class="choices">
  <div class="choice sel"><span class="radio"></span><div class="from"><span class="store saved">SAVED IN BINDFORGE</span></div>
    <div class="what">Put back what I had</div>
    <ul><li class="ok">Throttle button names as you last saved them</li><li class="info">Epic's 3 new button names are dropped (they're in the backup)</li></ul></div>
  <div class="choice"><span class="radio"></span><div class="from"><span class="store">EPIC</span></div>
    <div class="what">Keep Epic's changes</div>
    <ul><li class="ok">Epic's 3 new throttle button names go to Steam too</li></ul></div>
</div>
<p style="margin:0"><span class="link">Choose button by button…</span></p>
""", footer=footer(SAFE, '<span class="btn">Not now</span><span class="btn primary">Check the result</span>')), 1400)


def main():
    chrome = "google-chrome"
    for name, (html, height) in PAGES.items():
        src = HERE / f"{name}.html"
        src.write_text(html, encoding="utf-8")
        png = OUT / f"{name}.png"
        subprocess.run([chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars", "--force-device-scale-factor=1",
                        f"--window-size=1040,{height}", f"--screenshot={png}", src.as_uri()],
                       check=True, capture_output=True)
        trim(png)
        print(png)


def trim(png):
    """Crops the window to the dialog plus a 20px margin, so each PNG is as tall as its screen."""
    from PIL import Image
    image = Image.open(png).convert("RGB")
    width, height = image.size
    background = image.getpixel((5, height - 5))
    bottom = height - 1
    while bottom > 0 and all(sum(abs(a - b) for a, b in zip(image.getpixel((x, bottom)), background)) < 40
                             for x in range(0, width, 7)):
        bottom -= 1
    image.crop((0, 0, width, min(height, bottom + 21))).save(png)


if __name__ == "__main__":
    main()
