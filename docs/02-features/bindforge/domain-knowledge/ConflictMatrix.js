(function(){
  var STATE_UNTESTED = 0, STATE_NO = 1, STATE_YES = 2;
  var STATE_LABEL = {0:'UNTESTED',1:'NO CONFLICT',2:'CONFLICTS'};
  var STATE_CLASS = {0:'st-untested',1:'st-no',2:'st-yes'};

  function pairKey(a,b){
    return a < b ? a+'<::>'+b : b+'<::>'+a;
  }

  function load(storageKey, seed){
    var raw = localStorage.getItem(storageKey);
    if(raw){
      try { return JSON.parse(raw); } catch(e){}
    }
    var data = { subgroups: seed.subgroups.slice(), pairs: {} };
    (seed.pairs||[]).forEach(function(p){
      data.pairs[pairKey(p[0],p[1])] = p[2];
    });
    return data;
  }

  function save(storageKey, data){
    localStorage.setItem(storageKey, JSON.stringify(data));
  }

  function escapeHtml(s){
    var d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  function init(opts){
    var storageKey = opts.storageKey;
    var seed = { subgroups: opts.subgroups || [], pairs: opts.knownPairs || [] };
    var data = load(storageKey, seed);
    var root = document.getElementById(opts.mount || 'cm-root');

    function render(){
      root.innerHTML = '';
      root.appendChild(buildToolbar());
      root.appendChild(buildTable());
      root.appendChild(buildExport());
    }

    function buildToolbar(){
      var bar = document.createElement('div');
      bar.className = 'cm-toolbar';

      var legend = document.createElement('div');
      legend.className = 'cm-legend';
      legend.innerHTML =
        '<span class="cm-swatch st-untested"></span>Untested'+
        '<span class="cm-swatch st-no"></span>No Conflict'+
        '<span class="cm-swatch st-yes"></span>Conflicts';
      bar.appendChild(legend);

      var right = document.createElement('div');
      right.style.display = 'flex';
      right.style.gap = '8px';
      right.style.flexWrap = 'wrap';

      var addWrap = document.createElement('div');
      addWrap.className = 'cm-add';
      var input = document.createElement('input');
      input.type = 'text';
      input.placeholder = 'Add subgroup…';
      input.className = 'cm-input';
      var addBtn = document.createElement('button');
      addBtn.className = 'cm-btn';
      addBtn.textContent = '+ ADD';
      function addSubgroup(){
        var name = input.value.trim();
        if(!name) return;
        if(data.subgroups.indexOf(name) !== -1){ alert('That subgroup already exists.'); return; }
        data.subgroups.push(name);
        save(storageKey, data);
        input.value = '';
        render();
      }
      addBtn.onclick = addSubgroup;
      input.addEventListener('keydown', function(e){ if(e.key === 'Enter') addSubgroup(); });
      addWrap.appendChild(input);
      addWrap.appendChild(addBtn);
      right.appendChild(addWrap);

      var resetBtn = document.createElement('button');
      resetBtn.className = 'cm-btn cm-btn-danger';
      resetBtn.textContent = 'RESET';
      resetBtn.title = 'Reset this page back to its seed data';
      resetBtn.onclick = function(){
        if(confirm('Reset this page back to its seed data? This clears any testing you\'ve entered here.')){
          localStorage.removeItem(storageKey);
          data = load(storageKey, seed);
          render();
        }
      };
      right.appendChild(resetBtn);

      bar.appendChild(right);
      return bar;
    }

    function cycle(key){
      var cur = data.pairs[key] || STATE_UNTESTED;
      data.pairs[key] = (cur + 1) % 3;
      save(storageKey, data);
    }

    function removeSubgroup(name){
      data.subgroups = data.subgroups.filter(function(n){ return n !== name; });
      Object.keys(data.pairs).forEach(function(k){
        var parts = k.split('<::>');
        if(parts[0] === name || parts[1] === name) delete data.pairs[k];
      });
      save(storageKey, data);
      render();
    }

    function buildTable(){
      var wrap = document.createElement('div');
      wrap.className = 'cm-tablewrap';
      var subgroups = data.subgroups;

      if(subgroups.length === 0){
        var empty = document.createElement('div');
        empty.className = 'cm-empty';
        empty.textContent = 'No subgroups yet — add one above to start building the matrix.';
        wrap.appendChild(empty);
        return wrap;
      }

      var table = document.createElement('table');
      table.className = 'cm-table';

      var thead = document.createElement('thead');
      var headRow = document.createElement('tr');
      headRow.appendChild(document.createElement('th'));
      subgroups.forEach(function(name){
        var th = document.createElement('th');
        th.className = 'cm-colhead';
        var rm = document.createElement('span');
        rm.className = 'cm-remove';
        rm.textContent = '×';
        rm.title = 'Remove subgroup';
        rm.onclick = function(){
          if(confirm('Remove "'+name+'" and all its pair data?')) removeSubgroup(name);
        };
        th.appendChild(rm);
        var span = document.createElement('span');
        span.className = 'cm-headtext';
        span.textContent = name;
        th.appendChild(span);
        headRow.appendChild(th);
      });
      thead.appendChild(headRow);
      table.appendChild(thead);

      var tbody = document.createElement('tbody');
      subgroups.forEach(function(rowName, rowIdx){
        var tr = document.createElement('tr');
        var rowTh = document.createElement('th');
        rowTh.className = 'cm-rowhead';
        rowTh.textContent = rowName;
        tr.appendChild(rowTh);
        subgroups.forEach(function(colName, colIdx){
          var td = document.createElement('td');
          if(colIdx <= rowIdx){
            td.className = 'cm-blank';
          } else {
            var key = pairKey(rowName, colName);
            var st = data.pairs[key] || STATE_UNTESTED;
            td.className = 'cm-cell ' + STATE_CLASS[st];
            td.textContent = STATE_LABEL[st];
            td.title = rowName + ' × ' + colName + ' — click to change';
            td.onclick = function(){ cycle(key); render(); };
          }
          tr.appendChild(td);
        });
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      wrap.appendChild(table);
      return wrap;
    }

    function toMarkdown(){
      var subgroups = data.subgroups;
      if(subgroups.length === 0) return '(no subgroups yet)';
      var lines = [];
      lines.push('| | ' + subgroups.join(' | ') + ' |');
      lines.push('|---|' + subgroups.map(function(){ return '---'; }).join('|') + '|');
      subgroups.forEach(function(rowName){
        var cells = subgroups.map(function(colName){
          if(rowName === colName) return '—';
          var st = data.pairs[pairKey(rowName, colName)] || STATE_UNTESTED;
          return st === STATE_UNTESTED ? '?' : (st === STATE_YES ? '**YES**' : 'no');
        });
        lines.push('| **' + rowName + '** | ' + cells.join(' | ') + ' |');
      });
      return lines.join('\n');
    }

    function buildExport(){
      var wrap = document.createElement('div');
      wrap.className = 'cm-export';
      var btn = document.createElement('button');
      btn.className = 'cm-btn';
      btn.textContent = 'EXPORT AS MARKDOWN';
      var ta = document.createElement('textarea');
      ta.className = 'cm-exportarea';
      ta.style.display = 'none';
      ta.readOnly = true;
      btn.onclick = function(){
        ta.value = toMarkdown();
        ta.style.display = 'block';
        ta.focus();
        ta.select();
      };
      wrap.appendChild(btn);
      wrap.appendChild(ta);
      return wrap;
    }

    render();
  }

  window.ConflictMatrix = { init: init };
})();
