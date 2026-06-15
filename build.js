#!/usr/bin/env node
'use strict';

/**
 * build.js — generates static HTML from wiki/*.md
 *
 * Run:  node build.js
 *
 * Outputs (all at project root, alongside index.html):
 *   index.html                        ← wiki/Home.md
 *   Installation-and-Configuration.html
 *   AllCommands.html  … etc.
 *   sitemap.xml
 *   robots.txt
 *
 * All asset paths are relative so the site works when opened locally
 * as files as well as when served from GitHub Pages.
 *
 * marked.js is downloaded from CDN on first run, cached in .build-cache/.
 */

const fs    = require('fs');
const path  = require('path');
const https = require('https');
const vm    = require('vm');

/* ── config ──────────────────────────────────────────────── */

const DOMAIN      = 'https://www.elite-intel.org';
const ROOT        = __dirname;
const WIKI_DIR    = path.join(ROOT, 'wiki');
const CACHE_DIR   = path.join(ROOT, '.build-cache');
const MARKED_URL  = 'https://cdn.jsdelivr.net/npm/marked@15/marked.min.js';
const MARKED_FILE = path.join(CACHE_DIR, 'marked.min.js');

const SKIP = new Set(['_Sidebar']);

/* ── download helper ─────────────────────────────────────── */

function download(url, dest) {
    return new Promise((resolve, reject) => {
        const file = fs.createWriteStream(dest);
        https.get(url, res => {
            if (res.statusCode === 301 || res.statusCode === 302) {
                file.close(() => fs.unlinkSync(dest));
                download(res.headers.location, dest).then(resolve).catch(reject);
                return;
            }
            if (res.statusCode !== 200) {
                reject(new Error(`HTTP ${res.statusCode} fetching ${url}`));
                return;
            }
            res.pipe(file);
            file.on('finish', () => file.close(resolve));
        }).on('error', err => { try { fs.unlinkSync(dest); } catch (_) {} reject(err); });
    });
}

/* ── load marked via Node vm (no npm required) ───────────── */

async function loadMarked() {
    if (!fs.existsSync(CACHE_DIR)) fs.mkdirSync(CACHE_DIR, { recursive: true });
    if (!fs.existsSync(MARKED_FILE)) {
        process.stdout.write('Downloading marked.js … ');
        await download(MARKED_URL, MARKED_FILE);
        console.log('done');
    }
    const code = fs.readFileSync(MARKED_FILE, 'utf8');
    const exportsObj = {};
    const ctx = { module: { exports: exportsObj }, exports: exportsObj, globalThis: {} };
    vm.runInNewContext(code, ctx);
    const mod = ctx.module.exports;
    return typeof mod.marked === 'function' ? mod.marked : mod;
}

/* ── markdown pre-processing ─────────────────────────────── */

const GITHUB_WIKI_RE = /https?:\/\/github\.com\/(?:SudoKrondor|stone-alex)\/EliteIntel\/wiki\/([^\s"')]+)/g;
const YT_TOKEN_RE    = /\[\[youtube:([A-Za-z0-9_-]+)\]\]/g;

function preprocessMd(md) {
    // GitHub wiki absolute links → relative .html files (all at root)
    md = md.replace(GITHUB_WIKI_RE, (_, page) => `${page}.html`);

    // YouTube embed tokens
    md = md.replace(YT_TOKEN_RE, (_, id) =>
        `<div class="yt-embed"><iframe src="https://www.youtube.com/embed/${id}" ` +
        `title="YouTube video" frameborder="0" allowfullscreen ` +
        `allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe></div>`
    );

    return md;
}

/* ── post-process rendered HTML ──────────────────────────── */

// Bare slug hrefs from markdown like [text](PageName) → PageName.html
const BARE_HREF_RE = /href="([A-Za-z][A-Za-z0-9_-]*)"/g;

function postprocessHtml(html) {
    return html.replace(BARE_HREF_RE, (_, slug) => `href="${slug}.html"`);
}

/* ── metadata extraction ─────────────────────────────────── */

function extractTitle(md) {
    const m = md.match(/^# +(.+)$/m);
    if (!m) return '';
    return m[1]
        .replace(/!\[.*?\]\(.*?\)/g, '')
        .replace(/<[^>]+>/g, '')
        .replace(/\*\*/g, '')
        .trim();
}

function extractDescription(md) {
    for (const line of md.split('\n')) {
        const t = line.trim();
        if (!t || t.startsWith('#') || t.startsWith('!') || t.startsWith('---') ||
            t.startsWith('```') || t.startsWith('<') || t.startsWith('|')) continue;
        const clean = t
            .replace(/\*\*(.+?)\*\*/g, '$1')
            .replace(/\*(.+?)\*/g, '$1')
            .replace(/\[(.+?)\]\(.+?\)/g, '$1')
            .replace(/<[^>]+>/g, '')
            .trim();
        if (clean.length > 30) return clean.slice(0, 160);
    }
    return 'EliteIntel — AI voice assistant for Elite Dangerous. Natural language commands, offline-capable, open-source.';
}

/* ── HTML escaping ───────────────────────────────────────── */

function esc(s) {
    return String(s).replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}

/* ── sidebar ─────────────────────────────────────────────── */

let _activePage = '';

function navLink(page, label, extra = '') {
    const href   = page === 'Home' ? 'index.html' : `${page}.html`;
    const active = page === _activePage ? ' active' : '';
    return `<a href="${href}" class="nav-link${extra}${active}">${label}</a>`;
}

function buildSidebar(activePage) {
    _activePage = activePage;
    const img = (src, alt) => `<img src="images/${src}" class="inline" height="20" alt="${alt}">`;

    return `
            ${navLink('Home',                            img('elite-logo.png','')    + ' Home')}

            <div class="nav-section">App</div>
            ${navLink('Installation',  img('release.png','')       + ' Installation')}
            ${navLink('Configuration',    img('settings.png','')      + ' Configuration')}
            ${navLink('Options',    img('microchip-ai.png','')      + ' Options')}

            <div class="nav-section">Wiki</div>
            ${navLink('installing-local-llms',           img('microchip-ai.png','')  + ' Choose your LLM')}
            ${navLink('Install-Ollama-Local-LLM-Linux',  img('ai.png','')            + ' Ollama &mdash; Linux',    ' nav-sub')}
            ${navLink('Install-Ollama-Local-LLM-Windows',img('ai.png','')            + ' Ollama &mdash; Windows',  ' nav-sub')}
            ${navLink('Install-LM-Studio-Linux',         img('ai.png','')            + ' LM Studio &mdash; Linux', ' nav-sub')}
            ${navLink('Install-LM-Studio-Windows',       img('ai.png','')            + ' LM Studio &mdash; Windows',' nav-sub')}
            ${navLink('AMD-RX-7800XT-LLM-Setup',        img('ai.png','')            + ' AMD RX Series',           ' nav-sub')}

            <div class="nav-section">How Do I…?</div>
            ${navLink('General-Operation',               img('manual.png','')        + ' General Operation')}
            ${navLink('TradeRoutePlotting',              img('manual.png','')        + ' Trade &amp; Profit')}
            ${navLink('Commodity-Searching',             img('manual.png','')        + ' Commodity Search')}
            ${navLink('Discovery-Assistance',            img('manual.png','')        + ' Discovery')}
            ${navLink('Navigation-Assistance',           img('manual.png','')        + ' Navigation')}
            ${navLink('Pirate-Massacre-Mission-Tracking',img('manual.png','')        + ' Pirate Missions')}
            ${navLink('AllCommands',                     img('manual.png','')        + ' All Commands')}
            ${navLink('Obscure-System-Commands',         img('manual.png','')        + ' System Commands')}

            <div class="nav-section">Community</div>
            <a href="https://matrix.to/#/#krondor:matrix.org" class="nav-link nav-external" target="_blank" rel="noopener">${img('communications.png','')} Matrix Chat ↗</a>
            <a href="https://www.youtube.com/@SudoKrondor" class="nav-link nav-external" target="_blank" rel="noopener">▶ YouTube ↗</a>
            <a href="https://github.com/SudoKrondor/EliteIntel" class="nav-link nav-external" target="_blank" rel="noopener">⎇ GitHub ↗</a>

            ${navLink('Privacy', '🔒 Privacy Policy')}
            ${navLink('Contact', '📧 Contact Developer')}`;
}

/* ── HTML template ───────────────────────────────────────── */

function renderPage({ title, description, canonical, body, activePage }) {
    return `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${esc(title)} — EliteIntel</title>
    <meta name="description" content="${esc(description)}">
    <link rel="canonical" href="${canonical}">
    <meta property="og:title" content="${esc(title)} — EliteIntel">
    <meta property="og:description" content="${esc(description)}">
    <meta property="og:url" content="${canonical}">
    <meta property="og:type" content="website">
    <link rel="stylesheet" href="css/main.css">
</head>
<body>

<header class="site-header">
    <button class="hamburger" id="hamburger" aria-label="Toggle navigation" aria-expanded="false">
        <span></span><span></span><span></span>
    </button>
    <a class="site-logo" href="index.html">ELITE<span>INTEL</span></a>
    <nav class="header-actions">
        <img src="images/windows.png" alt="Windows" class="os-icon" title="Windows">
        <img src="images/linux.png" alt="Linux" class="os-icon" title="Linux">
        <a class="btn-primary" href="https://github.com/SudoKrondor/EliteIntel/releases" target="_blank" rel="noopener">Download 1.0</a>
        <a class="btn-ghost" href="https://matrix.to/#/#krondor:matrix.org" target="_blank" rel="noopener">Support</a>
        <a class="btn-ghost" href="https://matrix.to/#/#krondor:matrix.org" target="_blank" rel="noopener">Become a tester 1.1</a>
        <a class="btn-ghost" href="https://github.com/SudoKrondor/EliteIntel" target="_blank" rel="noopener">GitHub</a>
    </nav>
</header>

<div class="sidebar-overlay" id="overlay"></div>

<div class="layout">
    <aside class="sidebar" id="sidebar" aria-label="Wiki navigation">
        <nav class="wiki-nav">
${buildSidebar(activePage)}
        </nav>
    </aside>
    <main class="content" id="content" role="main">
        <div class="content-inner markdown-body" id="content-inner">
${body}
        </div>
    </main>
</div>

<script>
(function(){
    var h=document.getElementById('hamburger'),s=document.getElementById('sidebar'),o=document.getElementById('overlay');
    function open(){s.classList.add('open');o.classList.add('active');h.setAttribute('aria-expanded','true');h.classList.add('is-open');}
    function close(){s.classList.remove('open');o.classList.remove('active');h.setAttribute('aria-expanded','false');h.classList.remove('is-open');}
    h.addEventListener('click',function(){s.classList.contains('open')?close():open();});
    o.addEventListener('click',close);
    document.querySelectorAll('.nav-link').forEach(function(a){a.addEventListener('click',function(){if(window.innerWidth<900)close();});});
})();
</script>
</body>
</html>`;
}

/* ── sitemap ─────────────────────────────────────────────── */

function buildSitemap(pages) {
    const locs = [`${DOMAIN}/`, ...pages.map(p => `${DOMAIN}/${p}.html`)];
    const entries = locs.map(l => `  <url><loc>${l}</loc></url>`).join('\n');
    return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${entries}
</urlset>
`;
}

/* ── main ────────────────────────────────────────────────── */

async function main() {
    const marked = await loadMarked();
    marked.use({ gfm: true, breaks: false });

    const mdFiles = fs.readdirSync(WIKI_DIR)
        .filter(f => f.endsWith('.md') && !SKIP.has(path.basename(f, '.md')));

    const builtPages = [];

    for (const file of mdFiles) {
        const page   = path.basename(file, '.md');
        const isHome = page === 'Home';

        let md = fs.readFileSync(path.join(WIKI_DIR, file), 'utf8');
        md = preprocessMd(md);

        const title       = extractTitle(md) || page.replace(/-/g, ' ');
        const description = extractDescription(md);
        const outFile     = isHome ? 'index.html' : `${page}.html`;
        const canonical   = isHome ? `${DOMAIN}/` : `${DOMAIN}/${page}.html`;

        let body = marked.parse(md);
        body = postprocessHtml(body);

        const html = renderPage({ title, description, canonical, body, activePage: page });

        fs.writeFileSync(path.join(ROOT, outFile), html, 'utf8');

        if (isHome) {
            console.log(`  index.html  (Home)`);
        } else {
            builtPages.push(page);
            console.log(`  ${outFile}`);
        }
    }

    fs.writeFileSync(path.join(ROOT, 'sitemap.xml'), buildSitemap(builtPages), 'utf8');
    console.log('  sitemap.xml');

    fs.writeFileSync(
        path.join(ROOT, 'robots.txt'),
        `User-agent: *\nAllow: /\nSitemap: ${DOMAIN}/sitemap.xml\n`,
        'utf8'
    );
    console.log('  robots.txt');

    console.log(`\nDone — ${builtPages.length + 1} pages, sitemap, robots.txt`);
}

main().catch(err => { console.error(err); process.exit(1); });
