(function () {
    'use strict';

    var BATCH_SIZE  = 5;
    var COOKIE_NAME = 'ei_academy_test';
    var LOCKOUT_MS  = 24 * 60 * 60 * 1000;

    // Lower bound is inclusive: 45% is "clueless", not "fail".
    var GRADES = [
        { min: 95, label: 'Top of the class',  note: 'Flawless. VEGA salutes you, Commander. o7' },
        { min: 80, label: 'Pass',              note: 'Solid work. You know your way around the cockpit.' },
        { min: 70, label: 'Barely pass',       note: 'You made it. Just. Maybe rewatch an episode or two.' },
        { min: 60, label: 'Mostly clueless',   note: 'Some of it landed. Most of it made a crater.' },
        { min: 45, label: 'Clueless',          note: 'Back to the Academy playlist, Commander.' },
        { min: 0,  label: 'Fail',              note: 'Chuck yourself out of the airlock and start again at Episode 1.' }
    ];

    var app = document.getElementById('quiz');
    var questions, answers, batchIndex;

    /* ── cookie ─────────────────────────────────────────── */

    function readCompletedAt() {
        var m = document.cookie.match(new RegExp('(?:^|;\\s*)' + COOKIE_NAME + '=(\\d+)'));
        return m ? parseInt(m[1], 10) : 0;
    }

    function writeCompletedAt(ts) {
        document.cookie = COOKIE_NAME + '=' + ts + '; max-age=' + (LOCKOUT_MS / 1000) +
            '; path=/; SameSite=Lax' + (location.protocol === 'https:' ? '; Secure' : '');
    }

    /* ── helpers ────────────────────────────────────────── */

    function shuffle(arr) {
        for (var i = arr.length - 1; i > 0; i--) {
            var j = Math.floor(Math.random() * (i + 1));
            var t = arr[i]; arr[i] = arr[j]; arr[j] = t;
        }
        return arr;
    }

    function el(tag, cls, text) {
        var e = document.createElement(tag);
        if (cls) e.className = cls;
        if (text != null) e.textContent = text;
        return e;
    }

    function formatWait(ms) {
        var h = Math.floor(ms / 3600000), m = Math.ceil((ms % 3600000) / 60000);
        if (m === 60) { h++; m = 0; }
        return (h ? h + ' h ' : '') + m + ' min';
    }

    function gradeFor(pct) {
        for (var i = 0; i < GRADES.length; i++) if (pct >= GRADES[i].min) return GRADES[i];
        return GRADES[GRADES.length - 1];
    }

    function batchCount() { return Math.ceil(questions.length / BATCH_SIZE); }

    /* ── screens ────────────────────────────────────────── */

    function renderLocked(completedAt) {
        app.innerHTML = '';
        app.appendChild(el('h2', null, 'Test already taken'));
        app.appendChild(el('p', null,
            'You can take the Academy test once every 24 hours. Try again in ' +
            formatWait(completedAt + LOCKOUT_MS - Date.now()) + '.'));
        var last = el('p', 'quiz-dim', 'Last completed: ' + new Date(completedAt).toLocaleString());
        app.appendChild(last);
    }

    function renderIntro() {
        app.innerHTML = '';
        app.appendChild(el('h2', null, 'Ready, Commander?'));
        app.appendChild(el('p', null,
            questions.length + ' questions, ' + BATCH_SIZE + ' at a time, in random order. ' +
            'Answer every question in a batch to move on. There is no going back.'));
        app.appendChild(el('p', null,
            'You get one overall result at the end. You will not be told which answers were right or wrong. ' +
            'You can complete the test once every 24 hours.'));
        var start = el('button', 'quiz-btn', 'Start the test');
        start.type = 'button';
        start.addEventListener('click', function () { renderBatch(); });
        app.appendChild(start);
    }

    function renderBatch() {
        app.innerHTML = '';
        var from = batchIndex * BATCH_SIZE;
        var to   = Math.min(from + BATCH_SIZE, questions.length);

        var progress = el('div', 'quiz-progress');
        progress.appendChild(el('span', null, 'Batch ' + (batchIndex + 1) + ' of ' + batchCount()));
        progress.appendChild(el('span', null, 'Questions ' + (from + 1) + '–' + to + ' of ' + questions.length));
        app.appendChild(progress);

        var bar = el('div', 'quiz-bar');
        var fill = el('div', 'quiz-bar-fill');
        fill.style.width = (from / questions.length * 100) + '%';
        bar.appendChild(fill);
        app.appendChild(bar);

        var form = el('form', 'quiz-form');
        for (var i = from; i < to; i++) form.appendChild(renderQuestion(i));

        var next = el('button', 'quiz-btn', to === questions.length ? 'Finish' : 'Next batch');
        next.type = 'submit';
        next.disabled = true;
        form.appendChild(next);

        form.addEventListener('change', function () {
            for (var k = from; k < to; k++) if (answers[k] == null) return;
            next.disabled = false;
        });
        form.addEventListener('submit', function (ev) {
            ev.preventDefault();
            if (next.disabled) return;
            batchIndex++;
            if (batchIndex < batchCount()) { renderBatch(); window.scrollTo(0, 0); }
            else finish();
        });

        app.appendChild(form);
    }

    function renderQuestion(i) {
        var item = questions[i];
        var fs = el('fieldset', 'quiz-q');
        fs.appendChild(el('legend', null, (i + 1) + '. ' + item.q));
        item.order.forEach(function (optIdx) {
            var label = el('label', 'quiz-opt');
            var input = document.createElement('input');
            input.type = 'radio';
            input.name = 'q' + i;
            input.addEventListener('change', function () { answers[i] = optIdx; });
            label.appendChild(input);
            label.appendChild(el('span', null, item.o[optIdx]));
            fs.appendChild(label);
        });
        return fs;
    }

    function finish() {
        var correct = 0;
        for (var i = 0; i < questions.length; i++) if (answers[i] === 0) correct++;
        var pct = Math.round(correct / questions.length * 100);
        var grade = gradeFor(pct);
        writeCompletedAt(Date.now());

        app.innerHTML = '';
        app.appendChild(el('h2', null, 'Result'));
        var score = el('div', 'quiz-score');
        score.appendChild(el('div', 'quiz-score-pct', pct + '%'));
        score.appendChild(el('div', 'quiz-score-label', grade.label));
        app.appendChild(score);
        app.appendChild(el('p', null, grade.note));
        app.appendChild(el('p', 'quiz-dim', 'You can take the test again in 24 hours.'));
        window.scrollTo(0, 0);
    }

    /* ── boot ───────────────────────────────────────────── */

    var completedAt = readCompletedAt();
    if (completedAt && Date.now() - completedAt < LOCKOUT_MS) { renderLocked(completedAt); return; }

    questions = shuffle(window.ACADEMY_QUESTIONS.map(function (q) {
        return { q: q.q, o: q.o, order: shuffle(q.o.map(function (_, i) { return i; })) };
    }));
    answers = new Array(questions.length);
    batchIndex = 0;
    renderIntro();
})();
