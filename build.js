#!/usr/bin/env node
'use strict';

/**
 * build.js  generates static HTML from wiki/*.md and wiki/<locale>/*.md
 *
 * Run:  node build.js
 *
 * Outputs:
 *   ROOT/*.html          ← English (default)
 *   ROOT/fr/*.html       ← French
 *   ROOT/es/*.html       ← Spanish
 *   ROOT/de/*.html       ← German
 *   ROOT/uk/*.html       ← Ukrainian
 *   ROOT/ru/*.html       ← Russian
 *   sitemap.xml          ← all locales, with hreflang
 *   robots.txt
 *
 * Asset paths are relative so the site works when opened locally as files
 * as well as when served from GitHub Pages.
 *
 * marked.js is downloaded from CDN on first run, cached in .build-cache/.
 */

const fs    = require('fs');
const path  = require('path');
const https = require('https');
const vm    = require('vm');

/* ── config ──────────────────────────────────────────────── */

const DOMAIN     = 'https://www.elite-intel.org';
const ROOT       = __dirname;
const WIKI_DIR   = path.join(ROOT, 'wiki');
const CACHE_DIR  = path.join(ROOT, '.build-cache');
const MARKED_URL  = 'https://cdn.jsdelivr.net/npm/marked@15/marked.min.js';
const MARKED_FILE = path.join(CACHE_DIR, 'marked.min.js');

const SKIP = new Set(['_Sidebar', '_Nav']);

// Locale definitions  code matches wiki subdir and output subdir
// en has no subdir (lives at root)
const LOCALES = [
    { code: 'en', label: 'EN', wikiDir: WIKI_DIR,                    outDir: ROOT,                    urlBase: `${DOMAIN}/`,     assetBase: '',   hreflang: 'en' },
    { code: 'fr', label: 'FR', wikiDir: path.join(WIKI_DIR, 'fr'),   outDir: path.join(ROOT, 'fr'),   urlBase: `${DOMAIN}/fr/`,  assetBase: '../', hreflang: 'fr' },
    { code: 'es', label: 'ES', wikiDir: path.join(WIKI_DIR, 'es'),   outDir: path.join(ROOT, 'es'),   urlBase: `${DOMAIN}/es/`,  assetBase: '../', hreflang: 'es' },
    { code: 'de', label: 'DE', wikiDir: path.join(WIKI_DIR, 'de'),   outDir: path.join(ROOT, 'de'),   urlBase: `${DOMAIN}/de/`,  assetBase: '../', hreflang: 'de' },
    { code: 'uk', label: 'UK', wikiDir: path.join(WIKI_DIR, 'uk'),   outDir: path.join(ROOT, 'uk'),   urlBase: `${DOMAIN}/uk/`,  assetBase: '../', hreflang: 'uk' },
    { code: 'ru', label: 'RU', wikiDir: path.join(WIKI_DIR, 'ru'),   outDir: path.join(ROOT, 'ru'),   urlBase: `${DOMAIN}/ru/`,  assetBase: '../', hreflang: 'ru' },
];

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
    md = md.replace(GITHUB_WIKI_RE, (_, page) => `${page}.html`);
    md = md.replace(YT_TOKEN_RE, (_, id) =>
        `<div class="yt-embed"><iframe src="https://www.youtube.com/embed/${id}" ` +
        `title="YouTube video" frameborder="0" allowfullscreen ` +
        `allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"></iframe></div>`
    );
    return md;
}

/* ── post-process rendered HTML ──────────────────────────── */

const BARE_HREF_RE = /href="([A-Za-z][A-Za-z0-9_-]*)"/g;

function postprocessHtml(html, assetBase) {
    html = html.replace(BARE_HREF_RE, (_, slug) => `href="${slug}.html"`);
    if (assetBase) {
        html = html.replace(/src="images\//g, `src="${assetBase}images/`);
    }
    return html;
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
    return 'EliteIntel  AI voice assistant for Elite Dangerous. Natural language commands, offline-capable, open-source.';
}

/* ── HTML escaping ───────────────────────────────────────── */

function esc(s) {
    return String(s).replace(/[&<>"']/g, c => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));
}

/* ── locale switcher ─────────────────────────────────────── */

function buildLocaleSwitcher(currentCode, page, assetBase) {
    const pageFile = page === 'Home' ? 'index.html' : `${page}.html`;
    return LOCALES.map(loc => {
        if (loc.code === currentCode) {
            return `<span class="locale-current">${loc.label}</span>`;
        }
        // For English (root), link is just pageFile; for others it's ../fr/pageFile etc.
        // But we're generating relative links from the current page's directory.
        // English pages are at ROOT, locale pages are at ROOT/<code>/
        // So from ROOT: fr/index.html; from ROOT/fr/: ../index.html or ../es/index.html
        let href;
        if (currentCode === 'en') {
            // Currently at root, linking to subdir
            href = loc.code === 'en' ? pageFile : `${loc.code}/${pageFile}`;
        } else if (loc.code === 'en') {
            // Currently in a subdir, linking to root
            href = `../${pageFile}`;
        } else {
            // Currently in one subdir, linking to another subdir
            href = `../${loc.code}/${pageFile}`;
        }
        return `<a href="${href}" class="locale-link">${loc.label}</a>`;
    }).join('');
}

/* ── hreflang link tags ──────────────────────────────────── */

function buildHreflangTags(page) {
    const pageFile = page === 'Home' ? 'index.html' : `${page}.html`;
    return LOCALES.map(loc => {
        const url = loc.code === 'en'
            ? (page === 'Home' ? `${DOMAIN}/` : `${DOMAIN}/${pageFile}`)
            : (page === 'Home' ? `${loc.urlBase}` : `${loc.urlBase}${pageFile}`);
        return `    <link rel="alternate" hreflang="${loc.hreflang}" href="${url}">`;
    }).join('\n');
}

/* ── nav labels ──────────────────────────────────────────── */

// English defaults  also the fallback when a locale has no _Nav.md
const DEFAULT_NAV = {
    section_app:                       'App',
    section_wiki:                      'Wiki',
    section_howdoi:                    'How Do I…?',
    section_community:                 'Community',
    link_matrix:                       'Matrix Chat ↗',
    link_privacy:                      '🔒 Privacy Policy',
    link_contact:                      '📧 Contact Developer',
    Home:                              'Home',
    Installation:                      'Installation',
    Configuration:                     'Configuration',
    Options:                           'Options',
    'installing-local-llms':           'Choose your LLM',
    'Install-Ollama-Local-LLM-Linux':  'Ollama  Linux',
    'Install-Ollama-Local-LLM-Windows':'Ollama  Windows',
    'Install-LM-Studio-Linux':         'LM Studio  Linux',
    'Install-LM-Studio-Windows':       'LM Studio  Windows',
    'AMD-RX-7800XT-LLM-Setup':         'AMD RX Series',
    'General-Operation':               'General Operation',
    TradeRoutePlotting:                'Trade & Profit',
    'Search-galaxy-with-EliteIntel':   'Search the Galaxy',
    'Discovery-Assistance':            'Discovery & ExoBiology',
    'Navigation-Assistance':           'Navigation',
    'Pirate-Massacre-Mission-Tracking':'Pirate Missions',
    AllCommands:                       'All Commands & Queries',
    'Obscure-System-Commands':         'System Commands',
};

// Read _Nav.md from a wiki dir: lines of "key: value", # lines ignored
function loadNavLabels(wikiDir) {
    const labels = { ...DEFAULT_NAV };
    const navFile = path.join(wikiDir, '_Nav.md');
    if (!fs.existsSync(navFile)) return labels;
    for (const line of fs.readFileSync(navFile, 'utf8').split('\n')) {
        const t = line.trim();
        if (!t || t.startsWith('#')) continue;
        const colon = t.indexOf(':');
        if (colon < 1) continue;
        const key = t.slice(0, colon).trim();
        const val = t.slice(colon + 1).trim();
        if (key && val) labels[key] = val;
    }
    return labels;
}

// Build page-slug → title map from all .md files in a wiki dir
function buildTitleMap(wikiDir) {
    const map = {};
    if (!fs.existsSync(wikiDir)) return map;
    for (const f of fs.readdirSync(wikiDir)) {
        if (!f.endsWith('.md')) continue;
        const page = path.basename(f, '.md');
        const md   = fs.readFileSync(path.join(wikiDir, f), 'utf8');
        const t    = extractTitle(md);
        if (t) map[page] = t;
    }
    return map;
}

/* ── sidebar ─────────────────────────────────────────────── */

function buildSidebar(activePage, assetBase, nav, titleMap) {
    const lbl = page => nav[page] || titleMap[page] || page.replace(/-/g, ' ');
    const img = (src, alt) => `<img src="${assetBase}images/${src}" class="inline" height="20" alt="${alt}">`;
    const lnk = (page, icon, extra = '') => {
        const href   = page === 'Home' ? 'index.html' : `${page}.html`;
        const active = page === activePage ? ' active' : '';
        const text   = (icon ? img(icon, '') + ' ' : '') + esc(lbl(page));
        return `<a href="${href}" class="nav-link${extra}${active}">${text}</a>`;
    };
    const sec = key => `<div class="nav-section">${esc(nav[key])}</div>`;

    return `
            ${lnk('Home', 'elite-logo.png')}

            ${sec('section_app')}
            ${lnk('Installation',  'release.png')}
            ${lnk('Configuration', 'settings.png')}
            ${lnk('Options',       'microchip-ai.png')}

            ${sec('section_wiki')}
            ${lnk('installing-local-llms',            'microchip-ai.png')}
            ${lnk('Install-Ollama-Local-LLM-Linux',   'ai.png', ' nav-sub')}
            ${lnk('Install-Ollama-Local-LLM-Windows',  'ai.png', ' nav-sub')}
            ${lnk('Install-LM-Studio-Linux',           'ai.png', ' nav-sub')}
            ${lnk('Install-LM-Studio-Windows',         'ai.png', ' nav-sub')}
            ${lnk('AMD-RX-7800XT-LLM-Setup',           'ai.png', ' nav-sub')}

            ${sec('section_howdoi')}
            ${lnk('General-Operation',                'manual.png')}
            ${lnk('TradeRoutePlotting',               'manual.png')}
            ${lnk('Search-galaxy-with-EliteIntel',    'manual.png')}
            ${lnk('Discovery-Assistance',             'manual.png')}
            ${lnk('Navigation-Assistance',            'manual.png')}
            ${lnk('Pirate-Massacre-Mission-Tracking', 'manual.png')}
            ${lnk('AllCommands',                      'manual.png')}
            ${lnk('Obscure-System-Commands',          'manual.png')}

            ${sec('section_community')}
            <a href="https://matrix.to/#/#krondor:matrix.org" class="nav-link nav-external" target="_blank" rel="noopener">${img('communications.png', '')} ${esc(nav['link_matrix'])}</a>
            <a href="https://www.youtube.com/@SudoKrondor" class="nav-link nav-external" target="_blank" rel="noopener">&#9654; YouTube &#8599;</a>
            <a href="https://github.com/SudoKrondor/EliteIntel" class="nav-link nav-external" target="_blank" rel="noopener">&#8903; GitHub &#8599;</a>

            <a href="Privacy.html" class="nav-link${activePage === 'Privacy' ? ' active' : ''}">${esc(nav['link_privacy'])}</a>
            <a href="Contact.html" class="nav-link${activePage === 'Contact' ? ' active' : ''}">${esc(nav['link_contact'])}</a>`;
}

/* ── HTML template ───────────────────────────────────────── */

function renderPage({ title, description, canonical, body, activePage, locale, page, nav, titleMap }) {
    const { code, assetBase } = locale;
    const localeSwitcher = buildLocaleSwitcher(code, page, assetBase);
    const hreflangTags   = buildHreflangTags(page);
    return `<!DOCTYPE html>
<html lang="${locale.hreflang}">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>${esc(title)}  EliteIntel</title>
    <meta name="description" content="${esc(description)}">
    <link rel="canonical" href="${canonical}">
${hreflangTags}
    <meta property="og:title" content="${esc(title)}  EliteIntel">
    <meta property="og:description" content="${esc(description)}">
    <meta property="og:url" content="${canonical}">
    <meta property="og:type" content="website">
    <meta property="og:image" content="${DOMAIN}/images/site-preview.png">
    <meta property="og:image:type" content="image/png">
    <meta name="twitter:card" content="summary_large_image">
    <meta name="twitter:image" content="${DOMAIN}/images/site-preview.png">
    <link rel="stylesheet" href="${assetBase}css/main.css">
</head>
<body>

<header class="site-header">
    <button class="hamburger" id="hamburger" aria-label="Toggle navigation" aria-expanded="false">
        <span></span><span></span><span></span>
    </button>
    <a class="site-logo" href="${assetBase}index.html">ELITE<span>INTEL</span></a>
    <nav class="header-actions">
        <img src="${assetBase}images/windows.png" alt="Windows" class="os-icon" title="Windows">
        <img src="${assetBase}images/linux.png" alt="Linux" class="os-icon" title="Linux">
        <a class="btn-primary" href="https://github.com/SudoKrondor/EliteIntel/releases" target="_blank" rel="noopener">Download 1.0</a>
        <a class="btn-secondary" href="https://matrix.to/#/#krondor:matrix.org" target="_blank" rel="noopener">Become a tester 1.1</a>
        <a class="btn-ghost" href="https://matrix.to/#/#krondor:matrix.org" target="_blank" rel="noopener">Support</a>
        <a class="btn-ghost" href="https://github.com/SudoKrondor/EliteIntel" target="_blank" rel="noopener">GitHub</a>
        <div class="locale-switcher">${localeSwitcher}</div>
    </nav>
</header>

<div class="sidebar-overlay" id="overlay"></div>

<div class="layout">
    <aside class="sidebar" id="sidebar" aria-label="Wiki navigation">
        <nav class="wiki-nav">
${buildSidebar(activePage, assetBase, nav, titleMap)}
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

function buildSitemap(allPages) {
    // allPages: array of { page, locales: [{code, url}] }
    // Use hreflang xhtml entries for SEO
    const entries = allPages.map(({ page, localeUrls }) => {
        const alts = localeUrls.map(({ hreflang, url }) =>
            `    <xhtml:link rel="alternate" hreflang="${hreflang}" href="${url}"/>`
        ).join('\n');
        // Primary URL is English
        const primaryUrl = localeUrls.find(l => l.hreflang === 'en').url;
        return `  <url>\n    <loc>${primaryUrl}</loc>\n${alts}\n  </url>`;
    }).join('\n');

    return `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9"
        xmlns:xhtml="http://www.w3.org/1999/xhtml">
${entries}
</urlset>
`;
}

/* ── clean old generated files ───────────────────────────── */

function cleanOutputs() {
    // Remove all .html files at ROOT
    for (const f of fs.readdirSync(ROOT)) {
        if (f.endsWith('.html')) fs.unlinkSync(path.join(ROOT, f));
    }
    // Remove and recreate locale subdirs
    for (const loc of LOCALES) {
        if (loc.code === 'en') continue;
        if (fs.existsSync(loc.outDir)) fs.rmSync(loc.outDir, { recursive: true, force: true });
        fs.mkdirSync(loc.outDir, { recursive: true });
    }
}

/* ── build one locale ────────────────────────────────────── */

async function buildLocale(locale, marked, sitemapIndex) {
    const { code, wikiDir, outDir, urlBase, assetBase } = locale;

    if (!fs.existsSync(wikiDir)) {
        console.log(`  [${code}] wiki dir not found, skipping`);
        return;
    }

    const nav      = loadNavLabels(wikiDir);
    const titleMap = buildTitleMap(wikiDir);

    const mdFiles = fs.readdirSync(wikiDir)
        .filter(f => f.endsWith('.md') && !SKIP.has(path.basename(f, '.md')));

    for (const file of mdFiles) {
        const page   = path.basename(file, '.md');
        const isHome = page === 'Home';

        let md = fs.readFileSync(path.join(wikiDir, file), 'utf8');
        md = preprocessMd(md);

        const title       = extractTitle(md) || page.replace(/-/g, ' ');
        const description = extractDescription(md);
        const outFile     = isHome ? 'index.html' : `${page}.html`;
        const canonical   = isHome ? urlBase : `${urlBase}${page}.html`;

        let body = marked.parse(md);
        body = postprocessHtml(body, assetBase);

        const html = renderPage({ title, description, canonical, body, activePage: page, locale, page, nav, titleMap });
        fs.writeFileSync(path.join(outDir, outFile), html, 'utf8');

        // Track for sitemap
        if (!sitemapIndex.has(page)) sitemapIndex.set(page, []);
        sitemapIndex.get(page).push({ hreflang: locale.hreflang, url: canonical });

        console.log(`  [${code}] ${outFile}`);
    }
}

/* ── main ────────────────────────────────────────────────── */

async function main() {
    const marked = await loadMarked();
    marked.use({ gfm: true, breaks: false });

    console.log('Cleaning old output…');
    cleanOutputs();

    // page → [{hreflang, url}]
    const sitemapIndex = new Map();

    for (const locale of LOCALES) {
        await buildLocale(locale, marked, sitemapIndex);
    }

    // Build sitemap
    const allPages = Array.from(sitemapIndex.entries()).map(([page, localeUrls]) => ({ page, localeUrls }));
    fs.writeFileSync(path.join(ROOT, 'sitemap.xml'), buildSitemap(allPages), 'utf8');
    console.log('  sitemap.xml');

    fs.writeFileSync(
        path.join(ROOT, 'robots.txt'),
        `User-agent: *\nAllow: /\nSitemap: ${DOMAIN}/sitemap.xml\n`,
        'utf8'
    );
    console.log('  robots.txt');

    const totalPages = Array.from(sitemapIndex.values()).reduce((n, arr) => n + arr.length, 0);
    console.log(`\nDone  ${totalPages} pages across ${LOCALES.length} locales`);
}

main().catch(err => { console.error(err); process.exit(1); });
