(function () {
  "use strict";

  var COLORS = {
    teal: "#0d9488",
    tealFill: "rgba(13, 148, 136, 0.18)",
    hit: "#94a3b8",
    le: "#0f766e",
    grid: "#e2e8f0",
    axis: "#64748b",
    metric: "#94a3b8",
    pos: "#dc2626",
    neg: "#2563eb",
    zero: "#94a3b8",
    peak: "#0f766e",
    ink: "#0f172a",
    annBg: "rgba(255,255,255,0.88)",
    hover: "rgba(100, 116, 139, 0.75)",
    hoverLe: "#0f766e",
    search: "rgba(15, 118, 110, 0.35)"
  };

  /** Dark theme for toolbar/header only; the canvas always uses light plot colors. */
  function applyChromeTheme() {
    var params = new URLSearchParams(window.location.search);
    var theme = params.get("theme");
    var dark = theme === "dark";
    if (theme !== "light" && !dark && window.matchMedia
        && window.matchMedia("(prefers-color-scheme: dark)").matches) {
      dark = true;
    }
    if (dark) {
      document.documentElement.setAttribute("data-theme", "dark");
    }
  }

  function $(sel, root) {
    return (root || document).querySelector(sel);
  }

  function fmt(n, d) {
    if (n == null || !isFinite(n)) return "—";
    return Number(n).toFixed(d);
  }

  var booted = false;

  function bootOnce(data) {
    if (booted || !data) return;
    booted = true;
    try {
      createViewer(data);
    } catch (e) {
      booted = false;
      showError("Could not render plot: " + (e && e.message ? e.message : e));
    }
  }

  /** Hosts (e.g. GSEA FX WebView) call this after loading the shared viewer with ?inject=1. */
  window.__enplotBoot = function (data) {
    bootOnce(data);
  };
  window.__enplotReady = window.__enplotBoot;

  function showError(msg) {
    var app = $("#app");
    app.innerHTML = '<div class="enplot-error">' + msg + "</div>";
  }

  /**
   * Stable FX host entry point (not replaced by inject-mode bootstrap).
   * @param {string|object} payload JSON string or parsed object from Java.
   */
  window.__enplotHostInject = function (payload) {
    try {
      var data = typeof payload === "string" ? JSON.parse(payload) : payload;
      window.ENPLOT_DATA = data;
      bootOnce(data);
    } catch (e) {
      showError("Invalid plot data: " + (e && e.message ? e.message : e));
    }
  };

  function loadEmbeddedOrFetch() {
    if (window.ENPLOT_DATA) {
      return Promise.resolve(window.ENPLOT_DATA);
    }
    var params = new URLSearchParams(window.location.search);
    if (params.get("inject") === "1") {
      // Data arrives from Java via __enplotHostInject / ENPLOT_DATA.
      return new Promise(function (resolve, reject) {
        if (window.ENPLOT_DATA) {
          resolve(window.ENPLOT_DATA);
          return;
        }
        var waited = 0;
        var t = setInterval(function () {
          if (window.ENPLOT_DATA) {
            clearInterval(t);
            resolve(window.ENPLOT_DATA);
            return;
          }
          if (booted) {
            clearInterval(t);
            resolve(window.ENPLOT_DATA);
            return;
          }
          waited += 50;
          if (waited >= 30000) {
            clearInterval(t);
            reject(new Error("Timed out waiting for plot data from host."));
          }
        }, 50);
      });
    }
    var url = window.ENPLOT_DATA_URL || params.get("data") || "";
    if (!url) {
      return Promise.reject(new Error("No enrichment plot data provided."));
    }
    // Script-tag load works under file:// when the payload is a .js boot file.
    if (/\.js$/i.test(url)) {
      return new Promise(function (resolve, reject) {
        window.__enplotReady = function (data) {
          resolve(data);
        };
        var s = document.createElement("script");
        s.src = url;
        s.onerror = function () {
          reject(new Error("Failed to load " + url));
        };
        document.head.appendChild(s);
      });
    }
    return fetch(url).then(function (r) {
      if (!r.ok) throw new Error("Failed to load " + url + " (" + r.status + ")");
      return r.json();
    });
  }

  function buildUi(app) {
    app.innerHTML =
      '<div class="enplot-header">' +
      '  <h1 id="enplot-title">Enrichment plot</h1>' +
      '  <div class="enplot-stats" id="enplot-stats"></div>' +
      "</div>" +
      '<div class="enplot-toolbar">' +
      '  <label><input type="checkbox" id="toggle-le" checked/> Highlight leading edge</label>' +
      '  <label><input type="checkbox" id="toggle-fill" checked/> Fill under ES</label>' +
      '  <label><input type="checkbox" id="toggle-ann" checked/> Annotations</label>' +
      '  <button type="button" id="btn-reset">Reset zoom</button>' +
      '  <button type="button" id="btn-save">Save PNG</button>' +
      '  <span class="enplot-search">' +
      '    <label for="gene-search">Gene</label>' +
      '    <input type="text" id="gene-search" placeholder="Symbol…" autocomplete="off" spellcheck="false"/>' +
      '    <button type="button" id="btn-find">Find</button>' +
      '    <button type="button" id="btn-clear-find">Clear</button>' +
      '    <span class="enplot-search-status" id="search-status"></span>' +
      "  </span>" +
      '  <span class="enplot-size">' +
      '    <label for="plot-width">W</label>' +
      '    <input type="number" id="plot-width" min="320" max="4000" step="10" title="Plot width (px)"/>' +
      '    <label for="plot-height">H</label>' +
      '    <input type="number" id="plot-height" min="280" max="2400" step="10" title="Plot height (px)"/>' +
      '    <button type="button" id="btn-size-apply">Apply size</button>' +
      '    <button type="button" id="btn-size-fit">Fit</button>' +
      "  </span>" +
      '  <span class="enplot-hint">Drag plot to zoom · scroll to pan · drag edges to resize</span>' +
      "</div>" +
      '<div class="enplot-stage-wrap" id="enplot-stage-wrap">' +
      '  <div class="enplot-stage-row">' +
      '    <div class="enplot-stage" id="enplot-stage">' +
      '      <canvas id="enplot-canvas"></canvas>' +
      '      <div class="enplot-tooltip" id="enplot-tooltip"></div>' +
      "    </div>" +
      '    <div class="enplot-resize-handle enplot-resize-width" id="enplot-resize-w" title="Drag to resize plot width"></div>' +
      "  </div>" +
      '  <div class="enplot-resize-handle enplot-resize-height" id="enplot-resize-h" title="Drag to resize plot height"></div>' +
      "</div>";
  }

  function extentY(points, padFrac) {
    var min = Infinity, max = -Infinity, i, y;
    for (i = 0; i < points.length; i++) {
      y = points[i][1];
      if (y < min) min = y;
      if (y > max) max = y;
    }
    if (!isFinite(min) || !isFinite(max)) {
      min = -1;
      max = 1;
    }
    if (min === max) {
      min -= 0.1;
      max += 0.1;
    }
    var pad = (max - min) * (padFrac || 0.08);
    return { min: min - pad, max: max + pad };
  }

  function createViewer(data) {
    applyChromeTheme();
    var app = $("#app");
    buildUi(app);

    var title = $("#enplot-title");
    var statsEl = $("#enplot-stats");
    var canvas = $("#enplot-canvas");
    var stage = $("#enplot-stage");
    var stageWrap = $("#enplot-stage-wrap");
    var tip = $("#enplot-tooltip");
    var resizeW = $("#enplot-resize-w");
    var resizeH = $("#enplot-resize-h");
    var searchInput = $("#gene-search");
    var searchStatus = $("#search-status");
    var widthInput = $("#plot-width");
    var heightInput = $("#plot-height");
    var ctx = canvas.getContext("2d");

    title.textContent = "EnPlot v2: " + (data.geneSet || "");
    var st = data.stats || {};
    var hitCount = (data.hits && data.hits.length) || 0;
    statsEl.innerHTML =
      "<span>ES " + fmt(st.es, 3) + "</span>" +
      "<span>NES " + fmt(st.nes, 2) + "</span>" +
      "<span>FDR " + fmt(st.fdr, 3) + "</span>" +
      "<span>NOM p " + fmt(st.np, 3) + "</span>" +
      "<span>FWER " + fmt(st.fwer, 3) + "</span>" +
      "<span>" + hitCount + " members</span>";

    var listSize = data.listSize || 1;
    var hitsBySymbol = {};
    (data.hits || []).forEach(function (h) {
      if (h && h.symbol) {
        hitsBySymbol[String(h.symbol).toUpperCase()] = h;
      }
    });

    var state = {
      x0: 0,
      x1: listSize - 1,
      showLe: true,
      showFill: true,
      showAnn: true,
      drag: null,
      resizing: null, // "w" | "h" | null
      resizeStart: null,
      sized: false,
      hoverPx: null,
      hoverHit: null,
      searchHit: null
    };

    var layout = {
      padL: 58,
      padR: 16,
      padT: 12,
      padB: 30,
      weights: [12, 3, 1, 7]
    };

    var SIZE_MIN_W = 320;
    var SIZE_MAX_W = 4000;
    var SIZE_MIN_H = 280;
    var SIZE_MAX_H = 2400;

    function clamp(n, lo, hi) {
      return Math.max(lo, Math.min(hi, Math.round(n)));
    }

    function syncSizeInputs() {
      widthInput.value = String(Math.round(stage.clientWidth));
      heightInput.value = String(Math.round(stage.clientHeight));
    }

    function setStageSize(widthPx, heightPx, markSized) {
      var w = clamp(widthPx, SIZE_MIN_W, SIZE_MAX_W);
      var h = clamp(heightPx, SIZE_MIN_H, SIZE_MAX_H);
      if (markSized !== false) {
        state.sized = true;
        stage.classList.add("is-sized");
        stageWrap.classList.add("is-sized");
        stage.style.flex = "0 0 auto";
      }
      stage.style.width = w + "px";
      stage.style.height = h + "px";
      widthInput.value = String(w);
      heightInput.value = String(h);
      draw();
    }

    function fitStageSize() {
      state.sized = false;
      stage.classList.remove("is-sized");
      stageWrap.classList.remove("is-sized");
      stage.style.flex = "";
      stage.style.width = "";
      stage.style.height = "";
      draw();
      // After layout settles, reflect natural size in the inputs.
      requestAnimationFrame(syncSizeInputs);
    }

    function applySizeInputs() {
      var w = parseInt(widthInput.value, 10);
      var h = parseInt(heightInput.value, 10);
      if (!isFinite(w)) w = stage.clientWidth;
      if (!isFinite(h)) h = stage.clientHeight;
      setStageSize(w, h, true);
    }

    function xScale(x, w) {
      var plotW = w - layout.padL - layout.padR;
      return layout.padL + ((x - state.x0) / Math.max(1e-9, state.x1 - state.x0)) * plotW;
    }

    function invX(px, w) {
      var plotW = w - layout.padL - layout.padR;
      return state.x0 + ((px - layout.padL) / Math.max(1e-9, plotW)) * (state.x1 - state.x0);
    }

    function yScale(y, y0, y1, top, h) {
      return top + h - ((y - y0) / Math.max(1e-9, y1 - y0)) * h;
    }

    function panelRects(h) {
      var avail = h - layout.padT - layout.padB;
      var sum = layout.weights.reduce(function (a, b) { return a + b; }, 0);
      var y = layout.padT;
      var gap = 4;
      var rects = [];
      for (var i = 0; i < layout.weights.length; i++) {
        var hh = Math.max(8, (layout.weights[i] / sum) * (avail - gap * 3));
        rects.push({ top: y, h: hh });
        y += hh + gap;
      }
      return rects;
    }

    function inDomain(x) {
      return x >= state.x0 && x <= state.x1;
    }

    function drawLabelBox(ctx, text, x, y, anchor) {
      ctx.font = "11px Segoe UI, sans-serif";
      var padX = 5, padY = 3;
      var lines = String(text).split("\n");
      var lineHeight = 14;
      var maxTw = 0;
      var i;
      for (i = 0; i < lines.length; i++) {
        maxTw = Math.max(maxTw, ctx.measureText(lines[i]).width);
      }
      var th = lineHeight * lines.length;
      var bx = x;
      var by = y;
      if (anchor === "bottom") {
        by = y - th - padY * 2 - 4;
      } else if (anchor === "top") {
        by = y + 4;
      }
      bx = Math.max(layout.padL, Math.min(bx - maxTw / 2 - padX, stage.clientWidth - layout.padR - maxTw - padX * 2));
      ctx.fillStyle = COLORS.annBg;
      ctx.strokeStyle = COLORS.grid;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.rect(bx, by, maxTw + padX * 2, th + padY * 2);
      ctx.fill();
      ctx.stroke();
      ctx.fillStyle = COLORS.ink;
      ctx.textAlign = "left";
      ctx.textBaseline = "top";
      for (i = 0; i < lines.length; i++) {
        ctx.fillText(lines[i], bx + padX, by + padY + i * lineHeight);
      }
    }

    function drawGuideLine(ctx, px, h, style) {
      if (px == null || px < layout.padL || px > stage.clientWidth - layout.padR) return;
      ctx.save();
      ctx.beginPath();
      ctx.setLineDash(style.dash);
      ctx.strokeStyle = style.color;
      ctx.lineWidth = style.width;
      ctx.globalAlpha = style.alpha == null ? 1 : style.alpha;
      ctx.moveTo(px, layout.padT);
      ctx.lineTo(px, h - layout.padB + 4);
      ctx.stroke();
      ctx.restore();
    }

    function showTipForHit(hit, px, py) {
      if (!hit) {
        tip.style.display = "none";
        return;
      }
      var rect = canvas.getBoundingClientRect();
      tip.style.display = "block";
      tip.style.left = Math.min(rect.width - 220, Math.max(8, (px != null ? px : 0) + 12)) + "px";
      tip.style.top = Math.max(8, (py != null ? py : 24) - 10) + "px";
      tip.innerHTML =
        "<strong>" + hit.symbol + "</strong><br/>" +
        "rank " + hit.rank +
        " · metric " + fmt(hit.metric, 3) +
        " · running ES " + fmt(hit.runningEs, 4) +
        (hit.leadingEdge ? "<br/>leading edge" : "");
    }

    function draw() {
      var dpr = window.devicePixelRatio || 1;
      var w = stage.clientWidth;
      var h = stage.clientHeight;
      canvas.width = Math.max(1, Math.floor(w * dpr));
      canvas.height = Math.max(1, Math.floor(h * dpr));
      canvas.style.width = w + "px";
      canvas.style.height = h + "px";
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.clearRect(0, 0, w, h);

      var rects = panelRects(h);
      var esY = extentY(data.esCurve, 0.12);
      var metY = extentY(data.metric, 0.1);

      drawEsPanel(ctx, w, rects[0], esY);
      drawHitsPanel(ctx, w, rects[1]);
      drawColorBar(ctx, w, rects[2]);
      drawMetricPanel(ctx, w, rects[3], metY);
      drawDomainAxis(ctx, w, h);

      // Search marker (faded dashed) under hover so hover stays primary.
      if (state.searchHit && inDomain(state.searchHit.rank)) {
        drawGuideLine(ctx, xScale(state.searchHit.rank, w), h, {
          dash: [5, 5],
          color: COLORS.search,
          width: 2,
          alpha: 1
        });
      }

      // Hover cursor line.
      if (state.hoverPx != null) {
        var leHover = state.hoverHit && state.hoverHit.leadingEdge;
        drawGuideLine(ctx, state.hoverPx, h, {
          dash: leHover ? [7, 4] : [4, 4],
          color: leHover ? COLORS.hoverLe : COLORS.hover,
          width: leHover ? 2.4 : 1.25,
          alpha: 1
        });
      }
    }

    function clipPlot(ctx, top, hh, w) {
      ctx.save();
      ctx.beginPath();
      ctx.rect(layout.padL, top, w - layout.padL - layout.padR, hh);
      ctx.clip();
    }

    function drawEsPanel(ctx, w, rect, yExt) {
      clipPlot(ctx, rect.top, rect.h, w);
      var zy = yScale(0, yExt.min, yExt.max, rect.top, rect.h);
      ctx.strokeStyle = COLORS.zero;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(layout.padL, zy);
      ctx.lineTo(w - layout.padR, zy);
      ctx.stroke();

      var curve = data.esCurve || [];
      if (state.showFill && curve.length > 1) {
        ctx.beginPath();
        var started = false;
        for (var i = 0; i < curve.length; i++) {
          var x = curve[i][0], y = curve[i][1];
          if (!inDomain(x)) continue;
          var px = xScale(x, w);
          var py = yScale(y, yExt.min, yExt.max, rect.top, rect.h);
          if (!started) {
            ctx.moveTo(px, zy);
            ctx.lineTo(px, py);
            started = true;
          } else {
            ctx.lineTo(px, py);
          }
        }
        if (started) {
          ctx.lineTo(xScale(Math.min(state.x1, curve[curve.length - 1][0]), w), zy);
          ctx.closePath();
          ctx.fillStyle = COLORS.tealFill;
          ctx.fill();
        }
      }

      ctx.beginPath();
      started = false;
      for (i = 0; i < curve.length; i++) {
        x = curve[i][0];
        y = curve[i][1];
        if (!inDomain(x)) continue;
        px = xScale(x, w);
        py = yScale(y, yExt.min, yExt.max, rect.top, rect.h);
        if (!started) {
          ctx.moveTo(px, py);
          started = true;
        } else {
          ctx.lineTo(px, py);
        }
      }
      ctx.strokeStyle = COLORS.teal;
      ctx.lineWidth = 2.25;
      ctx.lineJoin = "round";
      ctx.stroke();

      var peakRank = data.peakRank;
      var peakEs = data.peakEs;
      var peakX = null;
      if (peakRank != null && inDomain(peakRank)) {
        peakX = xScale(peakRank, w);
        ctx.setLineDash([4, 3]);
        ctx.strokeStyle = COLORS.peak;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(peakX, rect.top);
        ctx.lineTo(peakX, rect.top + rect.h);
        ctx.stroke();
        ctx.setLineDash([]);
      }
      ctx.restore();

      if (state.showAnn && peakX != null) {
        var ann = "ES=" + fmt(st.es, 3) + "  NES=" + fmt(st.nes, 2) + "  FDR=" + fmt(st.fdr, 3)
            + "\npeak at rank " + Math.round(peakRank);
        var zeroY = yScale(0, yExt.min, yExt.max, rect.top, rect.h);
        // Match static v2: stats on peak rank line, in whitespace opposite the ES peak.
        if (peakEs != null && peakEs >= 0) {
          drawLabelBox(ctx, ann, peakX, zeroY, "top");
        } else {
          drawLabelBox(ctx, ann, peakX, zeroY, "bottom");
        }
      }

      ctx.fillStyle = COLORS.axis;
      ctx.font = "11px Segoe UI, sans-serif";
      ctx.textAlign = "right";
      ctx.textBaseline = "middle";
      ctx.fillText(fmt(yExt.max, 2), layout.padL - 6, rect.top + 8);
      ctx.fillText(fmt(yExt.min, 2), layout.padL - 6, rect.top + rect.h - 8);
      ctx.save();
      ctx.translate(14, rect.top + rect.h / 2);
      ctx.rotate(-Math.PI / 2);
      ctx.textAlign = "center";
      ctx.fillText("Enrichment score (ES)", 0, 0);
      ctx.restore();
    }

    function drawHitsPanel(ctx, w, rect) {
      clipPlot(ctx, rect.top, rect.h, w);
      var hits = data.hits || [];
      for (var i = 0; i < hits.length; i++) {
        var h = hits[i];
        if (!inDomain(h.rank)) continue;
        var px = xScale(h.rank, w);
        var le = state.showLe && h.leadingEdge;
        ctx.strokeStyle = le ? COLORS.le : COLORS.hit;
        ctx.lineWidth = le ? 1.5 : 0.85;
        ctx.beginPath();
        ctx.moveTo(px, rect.top + 2);
        ctx.lineTo(px, rect.top + rect.h - 2);
        ctx.stroke();
      }
      ctx.restore();
      if (state.showAnn && state.showLe) {
        ctx.fillStyle = COLORS.le;
        ctx.font = "10px Segoe UI, sans-serif";
        ctx.save();
        ctx.translate(14, rect.top + rect.h / 2);
        ctx.rotate(-Math.PI / 2);
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        ctx.fillText("Leading edge", 0, 0);
        ctx.restore();
      }
    }

    function drawColorBar(ctx, w, rect) {
      clipPlot(ctx, rect.top, rect.h, w);
      var segs = data.colorBar || [];
      var plotL = layout.padL;
      var plotW = w - layout.padL - layout.padR;
      var y = rect.top + 1;
      var h = Math.max(1, rect.h - 2);
      if (segs.length === 0) {
        // Fallback when older payloads lack colorBar.
        var grad = ctx.createLinearGradient(plotL, 0, plotL + plotW, 0);
        grad.addColorStop(0, "rgb(210,40,40)");
        grad.addColorStop(0.5, "rgb(255,255,255)");
        grad.addColorStop(1, "rgb(40,80,210)");
        ctx.fillStyle = grad;
        ctx.fillRect(plotL, y, plotW, h);
      } else {
        for (var i = 0; i < segs.length; i++) {
          var s = segs[i];
          var x0 = s.start;
          var x1 = s.end;
          if (x1 < state.x0 || x0 > state.x1) continue;
          var px0 = xScale(Math.max(x0, state.x0), w);
          var px1 = xScale(Math.min(x1, state.x1), w);
          if (px1 <= px0) px1 = px0 + 1;
          ctx.fillStyle = s.color || "#ccc";
          ctx.fillRect(px0, y, px1 - px0, h);
        }
      }
      ctx.restore();
    }

    function metricExtents(metric) {
      var min = Infinity, max = -Infinity, i, y;
      for (i = 0; i < metric.length; i++) {
        y = metric[i][1];
        if (!isFinite(y)) continue;
        if (y < min) min = y;
        if (y > max) max = y;
      }
      if (!isFinite(min) || !isFinite(max)) {
        min = -1;
        max = 1;
      }
      return { min: min, max: max };
    }

    function drawMetricPanel(ctx, w, rect, yExt) {
      clipPlot(ctx, rect.top, rect.h, w);
      var metric = data.metric || [];

      // Horizontal grid (match static EnPlot v2).
      ctx.strokeStyle = COLORS.grid;
      ctx.lineWidth = 1;
      for (var g = 1; g <= 3; g++) {
        var gy = rect.top + (rect.h * g) / 4;
        ctx.beginPath();
        ctx.moveTo(layout.padL, gy);
        ctx.lineTo(w - layout.padR, gy);
        ctx.stroke();
      }

      // Zero line when metric spans zero.
      if (yExt.min < 0 && yExt.max > 0) {
        var zy = yScale(0, yExt.min, yExt.max, rect.top, rect.h);
        ctx.strokeStyle = COLORS.zero;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(layout.padL, zy);
        ctx.lineTo(w - layout.padR, zy);
        ctx.stroke();
      }

      // Ranked-metric curve.
      ctx.beginPath();
      var started = false;
      for (var i = 0; i < metric.length; i++) {
        var x = metric[i][0], y = metric[i][1];
        if (!inDomain(x)) continue;
        var px = xScale(x, w);
        var py = yScale(y, yExt.min, yExt.max, rect.top, rect.h);
        if (!started) {
          ctx.moveTo(px, py);
          started = true;
        } else {
          ctx.lineTo(px, py);
        }
      }
      ctx.strokeStyle = COLORS.metric;
      ctx.lineWidth = 1.25;
      ctx.lineJoin = "round";
      ctx.stroke();

      // Zero-cross rank (pos/neg split on ranked list).
      var zc = data.zeroCross;
      if (zc != null && zc >= state.x0 && zc <= state.x1) {
        var zx = xScale(zc, w);
        ctx.setLineDash([4, 3]);
        ctx.strokeStyle = COLORS.axis;
        ctx.lineWidth = 0.75;
        ctx.beginPath();
        ctx.moveTo(zx, rect.top);
        ctx.lineTo(zx, rect.top + rect.h);
        ctx.stroke();
        ctx.setLineDash([]);
        if (state.showAnn) {
          drawLabelBox(ctx, "Zero cross at " + Math.round(zc), zx, rect.top + rect.h * 0.55, "top");
        }
      }
      ctx.restore();

      // Y-axis ticks and title.
      ctx.fillStyle = COLORS.axis;
      ctx.font = "11px Segoe UI, sans-serif";
      ctx.textAlign = "right";
      ctx.textBaseline = "middle";
      ctx.fillText(fmt(yExt.max, 2), layout.padL - 6, rect.top + 8);
      ctx.fillText(fmt(yExt.min, 2), layout.padL - 6, rect.top + rect.h - 8);
      if (yExt.min < 0 && yExt.max > 0) {
        ctx.fillText("0", layout.padL - 6, yScale(0, yExt.min, yExt.max, rect.top, rect.h));
      }
      ctx.save();
      ctx.translate(14, rect.top + rect.h / 2);
      ctx.rotate(-Math.PI / 2);
      ctx.textAlign = "center";
      var metricTitle = "Ranked list metric";
      if (data.metricName) {
        metricTitle += " (" + data.metricName + ")";
      }
      ctx.fillText(metricTitle, 0, 0);
      ctx.restore();

      // Phenotype labels at max/min metric (EnPlot v2 parity).
      if (state.showAnn) {
        var ext = {
          min: data.metricMin != null ? data.metricMin : metricExtents(metric).min,
          max: data.metricMax != null ? data.metricMax : metricExtents(metric).max
        };
        ctx.font = "10px Segoe UI, sans-serif";
        ctx.textAlign = "left";
        if (data.classA) {
          var pyMax = yScale(ext.max, yExt.min, yExt.max, rect.top, rect.h);
          ctx.fillStyle = COLORS.pos;
          ctx.textBaseline = "bottom";
          ctx.fillText("'" + data.classA + "' (pos)", layout.padL + 4, pyMax - 2);
        }
        if (data.classB) {
          var pyMin = yScale(ext.min, yExt.min, yExt.max, rect.top, rect.h);
          ctx.fillStyle = COLORS.neg;
          ctx.textBaseline = "top";
          ctx.fillText("'" + data.classB + "' (neg)", layout.padL + 4, pyMin + 2);
        }
      }
    }

    function drawDomainAxis(ctx, w, h) {
      var axisY = h - layout.padB + 10;
      var plotW = w - layout.padL - layout.padR;
      ctx.strokeStyle = COLORS.axis;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(layout.padL, axisY);
      ctx.lineTo(w - layout.padR, axisY);
      ctx.stroke();

      var ticks = 5;
      ctx.fillStyle = COLORS.axis;
      ctx.font = "10px Segoe UI, sans-serif";
      ctx.textBaseline = "top";
      for (var t = 0; t <= ticks; t++) {
        var frac = t / ticks;
        var rank = state.x0 + frac * (state.x1 - state.x0);
        var px = layout.padL + frac * plotW;
        ctx.beginPath();
        ctx.moveTo(px, axisY);
        ctx.lineTo(px, axisY + (t === 0 || t === ticks ? 5 : 3));
        ctx.stroke();
        if (t === 0 || t === ticks || t === Math.floor(ticks / 2)) {
          ctx.textAlign = t === 0 ? "left" : (t === ticks ? "right" : "center");
          ctx.fillText(String(Math.round(rank)), px, axisY + 6);
        }
      }
      ctx.textAlign = "center";
      ctx.font = "11px Segoe UI, sans-serif";
      ctx.fillText("Rank in ordered dataset", (layout.padL + w - layout.padR) / 2, axisY + 20);
    }

    function nearestHit(rank) {
      var hits = data.hits || [];
      var best = null, bestD = Infinity;
      for (var i = 0; i < hits.length; i++) {
        var d = Math.abs(hits[i].rank - rank);
        if (d < bestD) {
          bestD = d;
          best = hits[i];
        }
      }
      var thresh = Math.max(2, (state.x1 - state.x0) * 0.012);
      return bestD <= thresh ? best : null;
    }

    function findGene(query) {
      var q = String(query || "").trim().toUpperCase();
      if (!q) return null;
      if (hitsBySymbol[q]) return hitsBySymbol[q];
      var hits = data.hits || [];
      var starts = null;
      var contains = null;
      for (var i = 0; i < hits.length; i++) {
        var sym = String(hits[i].symbol || "").toUpperCase();
        if (sym === q) return hits[i];
        if (!starts && sym.indexOf(q) === 0) starts = hits[i];
        if (!contains && sym.indexOf(q) >= 0) contains = hits[i];
      }
      return starts || contains;
    }

    function revealSearchHit(hit) {
      state.searchHit = hit;
      if (!hit) {
        searchStatus.textContent = "Not found";
        searchStatus.className = "enplot-search-status is-miss";
        tip.style.display = "none";
        draw();
        return;
      }
      searchStatus.textContent = "Found";
      searchStatus.className = "enplot-search-status is-hit";
      // Ensure the gene is in view (keep zoom if already visible).
      if (!inDomain(hit.rank)) {
        var span = Math.max(40, (state.x1 - state.x0) * 0.35);
        state.x0 = Math.max(0, hit.rank - span / 2);
        state.x1 = Math.min(listSize - 1, hit.rank + span / 2);
      }
      draw();
      var px = xScale(hit.rank, stage.clientWidth);
      showTipForHit(hit, px, 28);
    }

    function onMove(ev) {
      if (state.resizing) return;
      var rect = canvas.getBoundingClientRect();
      var px = ev.clientX - rect.left;
      var py = ev.clientY - rect.top;
      var w = stage.clientWidth;
      if (px < layout.padL || px > w - layout.padR) {
        state.hoverPx = null;
        state.hoverHit = null;
        // Keep search tooltip if present.
        if (state.searchHit && inDomain(state.searchHit.rank)) {
          showTipForHit(state.searchHit, xScale(state.searchHit.rank, w), 28);
        } else {
          tip.style.display = "none";
        }
        draw();
        return;
      }
      state.hoverPx = px;
      var rank = invX(px, w);
      state.hoverHit = nearestHit(rank);
      if (state.hoverHit) {
        showTipForHit(state.hoverHit, px, py);
      } else if (state.searchHit && inDomain(state.searchHit.rank)) {
        showTipForHit(state.searchHit, xScale(state.searchHit.rank, w), 28);
      } else {
        tip.style.display = "none";
      }
      draw();
    }

    canvas.addEventListener("mousemove", onMove);
    canvas.addEventListener("mouseleave", function () {
      state.hoverPx = null;
      state.hoverHit = null;
      if (state.searchHit && inDomain(state.searchHit.rank)) {
        showTipForHit(state.searchHit, xScale(state.searchHit.rank, stage.clientWidth), 28);
      } else {
        tip.style.display = "none";
      }
      draw();
    });

    canvas.addEventListener("mousedown", function (ev) {
      if (ev.button !== 0 || state.resizing) return;
      var rect = canvas.getBoundingClientRect();
      state.drag = { x: ev.clientX - rect.left };
    });

    window.addEventListener("mouseup", function (ev) {
      if (state.resizing) {
        resizeW.classList.remove("is-active");
        resizeH.classList.remove("is-active");
        state.resizing = null;
        state.resizeStart = null;
        document.body.style.cursor = "";
        syncSizeInputs();
        return;
      }
      if (!state.drag) return;
      var rect = canvas.getBoundingClientRect();
      var x1 = ev.clientX - rect.left;
      var a = invX(Math.min(state.drag.x, x1), stage.clientWidth);
      var b = invX(Math.max(state.drag.x, x1), stage.clientWidth);
      state.drag = null;
      if (b - a < Math.max(5, listSize * 0.002)) return;
      state.x0 = Math.max(0, a);
      state.x1 = Math.min(listSize - 1, b);
      draw();
    });

    window.addEventListener("mousemove", function (ev) {
      if (!state.resizing || !state.resizeStart) return;
      ev.preventDefault();
      var rs = state.resizeStart;
      if (state.resizing === "w") {
        setStageSize(rs.startW + (ev.clientX - rs.startX), rs.startH, true);
      } else if (state.resizing === "h") {
        setStageSize(rs.startW, rs.startH + (ev.clientY - rs.startY), true);
      }
    });

    canvas.addEventListener("wheel", function (ev) {
      ev.preventDefault();
      var span = state.x1 - state.x0;
      var shift = (ev.deltaY > 0 ? 1 : -1) * span * 0.08;
      var n0 = Math.max(0, state.x0 + shift);
      var n1 = Math.min(listSize - 1, state.x1 + shift);
      if (n1 - n0 < 5) return;
      state.x0 = n0;
      state.x1 = n1;
      draw();
    }, { passive: false });

    function startResize(axis, handle, ev) {
      ev.preventDefault();
      ev.stopPropagation();
      state.resizing = axis;
      state.resizeStart = {
        startX: ev.clientX,
        startY: ev.clientY,
        startW: stage.clientWidth,
        startH: stage.clientHeight
      };
      handle.classList.add("is-active");
      document.body.style.cursor = axis === "w" ? "ew-resize" : "ns-resize";
      tip.style.display = "none";
    }
    resizeW.addEventListener("mousedown", function (ev) {
      startResize("w", resizeW, ev);
    });
    resizeH.addEventListener("mousedown", function (ev) {
      startResize("h", resizeH, ev);
    });

    $("#toggle-le").addEventListener("change", function (e) {
      state.showLe = e.target.checked;
      draw();
    });
    $("#toggle-fill").addEventListener("change", function (e) {
      state.showFill = e.target.checked;
      draw();
    });
    $("#toggle-ann").addEventListener("change", function (e) {
      state.showAnn = e.target.checked;
      draw();
    });
    $("#btn-reset").addEventListener("click", function () {
      state.x0 = 0;
      state.x1 = listSize - 1;
      draw();
    });
    $("#btn-size-apply").addEventListener("click", applySizeInputs);
    $("#btn-size-fit").addEventListener("click", fitStageSize);
    widthInput.addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") {
        ev.preventDefault();
        applySizeInputs();
      }
    });
    heightInput.addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") {
        ev.preventDefault();
        applySizeInputs();
      }
    });
    $("#btn-save").addEventListener("click", function () {
      try {
        var name = (data.geneSet || "enplot").replace(/[^\w.-]+/g, "_");
        var a = document.createElement("a");
        a.download = name + "_enplot_v2.png";
        a.href = canvas.toDataURL("image/png");
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      } catch (err) {
        window.alert("Could not save snapshot: " + (err && err.message ? err.message : err));
      }
    });
    // FX host already provides Save snapshot… — hide the in-page duplicate.
    if (new URLSearchParams(window.location.search).get("inject") === "1") {
      var saveBtn = $("#btn-save");
      if (saveBtn) saveBtn.style.display = "none";
    }

    function runSearch() {
      revealSearchHit(findGene(searchInput.value));
    }
    $("#btn-find").addEventListener("click", runSearch);
    searchInput.addEventListener("keydown", function (ev) {
      if (ev.key === "Enter") {
        ev.preventDefault();
        runSearch();
      }
    });
    $("#btn-clear-find").addEventListener("click", function () {
      searchInput.value = "";
      state.searchHit = null;
      searchStatus.textContent = "";
      searchStatus.className = "enplot-search-status";
      tip.style.display = "none";
      draw();
    });

    if (typeof ResizeObserver !== "undefined") {
      new ResizeObserver(function () {
        draw();
        if (!state.resizing) {
          syncSizeInputs();
        }
      }).observe(stage);
      new ResizeObserver(function () {
        // When the host grows (e.g. FX tab), keep the stage filling the wrap unless user sized it.
        if (!state.resizing && !state.sized) {
          draw();
          syncSizeInputs();
        }
      }).observe(stageWrap);
    } else {
      window.addEventListener("resize", function () {
        draw();
        syncSizeInputs();
      });
    }

    draw();
    syncSizeInputs();
  }

  loadEmbeddedOrFetch().then(bootOnce).catch(function (err) {
    if (!booted) {
      showError(String(err && err.message ? err.message : err));
    }
  });
})();
