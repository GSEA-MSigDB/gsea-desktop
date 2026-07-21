/* CoreMap Cytoscape.js bridge for JavaFX WebView */
(function () {
  if (typeof cytoscape !== "undefined" && typeof cytoscapeFcose !== "undefined") {
    cytoscape.use(cytoscapeFcose);
  }

  var cy = cytoscape({
    container: document.getElementById("cy"),
    elements: [],
    style: [
      {
        selector: "node",
        style: {
          label: "data(label)",
          "text-valign": "center",
          "text-halign": "center",
          "font-size": 9,
          "background-color": "data(cm_color)",
          "border-color": "data(cm_border)",
          "border-width": 2,
          shape: "data(cm_shape)",
          width: "data(cm_width)",
          height: "data(cm_height)",
          color: "#111827",
          "text-outline-width": 2,
          "text-outline-color": "#f8fafc",
          "text-wrap": "wrap",
          "text-max-width": 72
        }
      },
      {
        selector: "node[kind = 'set']",
        style: { "font-size": 8 }
      },
      {
        selector: "node[cm_split_img]",
        style: {
          "background-image": "data(cm_split_img)",
          "background-fit": "cover",
          "background-clip": "node",
          "background-image-opacity": 1,
          "background-color": "#ffffff"
        }
      },
      {
        selector: "edge",
        style: {
          // Function mappers — more reliable than data() for discrete props in JavaFX WebKit.
          width: function (ele) {
            var w = ele.data("cm_width");
            return w != null ? w : 1.5;
          },
          opacity: function (ele) {
            var o = ele.data("cm_opacity");
            return o != null ? o : 0.7;
          },
          "line-color": function (ele) {
            return ele.data("cm_color") || "#8A93A0";
          },
          "target-arrow-color": function (ele) {
            return ele.data("cm_color") || "#8A93A0";
          },
          "source-arrow-color": function (ele) {
            var shape = ele.data("cm_source_arrow") || "none";
            return shape === "none" ? "transparent" : (ele.data("cm_color") || "#8A93A0");
          },
          "curve-style": "bezier",
          "target-arrow-shape": function (ele) {
            return ele.data("cm_arrow") || "triangle";
          },
          "source-arrow-shape": function (ele) {
            return ele.data("cm_source_arrow") || "none";
          },
          "line-style": function (ele) {
            return ele.data("cm_line_style") || "solid";
          },
          "arrow-scale": function (ele) {
            // Match CoreMap edgeArrowScale: tee/square/circle slightly larger; triangle = 1.
            var shape = ele.data("cm_arrow") || "triangle";
            return shape === "tee" || shape === "square" || shape === "circle" ? 1.15 : 1;
          }
        }
      },
      {
        selector: "edge[edge_kind = 'set_link']",
        style: {
          "target-arrow-shape": "triangle",
          "source-arrow-shape": "none"
        }
      },
      {
        selector: "edge[edge_kind = 'membership']",
        style: {
          "target-arrow-shape": "none",
          "source-arrow-shape": "none",
          "line-style": "dashed"
        }
      },
      {
        selector: ".faded",
        style: { opacity: 0.15 }
      },
      {
        // Match CoreMap NetworkGraph HIGHLIGHT (#16A34A)
        selector: "node.highlighted",
        style: {
          opacity: 1,
          "border-width": 4,
          "border-color": "#16A34A",
          "z-index": 999
        }
      },
      {
        selector: "edge.highlighted",
        style: {
          opacity: 1,
          "line-color": "#16A34A",
          "source-arrow-color": "#16A34A",
          "target-arrow-color": "#16A34A",
          width: 4,
          "z-index": 999
        }
      }
    ],
    layout: { name: "preset" }
  });

  var pendingLayoutOpts = null;
  /** True after elements inject until a layout stop handler clears it. */
  var layoutDirty = false;
  /** Bumped when a new layout starts so superseded stop handlers do not notify. */
  var layoutToken = 0;

  function containerHasSize() {
    return cy.width() > 10 && cy.height() > 10;
  }

  function nodesLookStacked() {
    var nodes = cy.nodes();
    if (nodes.length < 3) return false;
    var counts = {};
    var peak = 0;
    nodes.forEach(function (node) {
      var pos = node.position();
      var key = Math.round(pos.x / 2) * 2 + "," + Math.round(pos.y / 2) * 2;
      var next = (counts[key] || 0) + 1;
      counts[key] = next;
      if (next > peak) peak = next;
    });
    return peak >= Math.max(3, Math.ceil(nodes.length * 0.35));
  }

  function notifyLayoutComplete() {
    try {
      if (window.javaCoreMap && typeof window.javaCoreMap.onLayoutComplete === "function") {
        window.javaCoreMap.onLayoutComplete();
      }
    } catch (e) { /* ignore */ }
  }

  function markLayoutCleanAndFit() {
    layoutDirty = false;
    pendingLayoutOpts = null;
    try {
      cy.fit(undefined, 40);
    } catch (e) { /* ignore */ }
    notifyLayoutComplete();
  }

  /**
   * Yield out of the LiveConnect stack before fcose/cose (WebKit kills long sync scripts).
   * Uses rAF when available; otherwise a 0-delay timer (event-loop turn, not a timed wait).
   */
  function deferOutOfLiveConnect(fn) {
    if (typeof requestAnimationFrame === "function") {
      requestAnimationFrame(fn);
    } else {
      setTimeout(fn, 0);
    }
  }

  /** Keep opts dirty until the container has size; ResizeObserver runs layout. */
  function schedulePendingLayout(opts) {
    pendingLayoutOpts = opts || pendingLayoutOpts || {};
    layoutDirty = true;
  }

  function runDeferredLayout() {
    if (!layoutDirty || cy.elements().empty()) {
      return;
    }
    var o = pendingLayoutOpts || {};
    pendingLayoutOpts = null;
    runLayout(o, false);
  }

  function runLayout(opts, force) {
    opts = opts || {};
    cy.resize();
    if (!force && !containerHasSize()) {
      schedulePendingLayout(opts);
      return;
    }
    if (cy.elements().empty()) {
      layoutDirty = false;
      notifyLayoutComplete();
      return;
    }
    var n = cy.nodes().length;
    var ideal = opts.idealEdgeLength != null ? opts.idealEdgeLength : (n > 80 ? 95 : 110);
    var repulsion = opts.nodeRepulsion != null ? opts.nodeRepulsion : (n > 80 ? 6500 : 7500);
    var previous = cy.scratch("_coremap_layout");
    var token = ++layoutToken;
    if (previous && typeof previous.stop === "function") {
      try { previous.stop(); } catch (e) { /* ignore */ }
    }
    function onStop() {
      if (token !== layoutToken) {
        return;
      }
      markLayoutCleanAndFit();
    }
    function onStopMaybeRetry() {
      if (token !== layoutToken) {
        return;
      }
      if (nodesLookStacked()) {
        try {
          var retryToken = ++layoutToken;
          cy.layout({
            name: "cose",
            animate: false,
            randomize: true,
            stop: function () {
              if (retryToken !== layoutToken) {
                return;
              }
              markLayoutCleanAndFit();
            }
          }).run();
          return;
        } catch (e2) { /* fall through */ }
      }
      markLayoutCleanAndFit();
    }
    try {
      // Large graphs: skip fcose in JavaFX WebView — it exceeds the script watchdog
      // ("JavaScript execution terminated") that CoreMap's Chromium never hits.
      if (n > 220) {
        var large = cy.layout({
          name: "cose",
          animate: false,
          randomize: opts.randomize !== false,
          nodeRepulsion: function () { return repulsion; },
          idealEdgeLength: ideal,
          nestingFactor: 0.1,
          stop: onStop
        });
        cy.scratch("_coremap_layout", large);
        large.run();
        return;
      }
      // animate:false — JavaFX WebView often stalls animated fcose before layoutstop/fit.
      var layout = cy.layout({
        name: "fcose",
        animate: false,
        animationDuration: 0,
        randomize: opts.randomize !== false,
        quality: n > 120 ? "draft" : "default",
        nodeDimensionsIncludeLabels: true,
        idealEdgeLength: ideal,
        nodeRepulsion: function () { return repulsion; },
        nestingFactor: 0.1,
        stop: onStopMaybeRetry
      });
      cy.scratch("_coremap_layout", layout);
      layout.run();
    } catch (e) {
      try {
        var fallback = cy.layout({
          name: "cose",
          animate: false,
          randomize: true,
          stop: onStop
        });
        cy.scratch("_coremap_layout", fallback);
        fallback.run();
      } catch (e2) {
        if (token === layoutToken) {
          markLayoutCleanAndFit();
        }
      }
    }
  }

  if (typeof ResizeObserver !== "undefined") {
    var ro = new ResizeObserver(function () {
      cy.resize();
      if ((layoutDirty || pendingLayoutOpts) && containerHasSize() && !cy.elements().empty()) {
        var o = pendingLayoutOpts || { randomize: true };
        pendingLayoutOpts = null;
        runLayout(o, true);
      }
    });
    var container = document.getElementById("cy");
    if (container) {
      ro.observe(container);
    }
  }

  window.coreMapBridge = {
    /** Normalize + collect Cytoscape elements from a nodes/edges payload. */
    _elsFromPayload: function (payload) {
      var els = [];
      (payload.nodes || []).forEach(function (n) {
        var d = n.data || n;
        if (!d.cm_color) d.cm_color = "#6B7280";
        if (!d.cm_border) d.cm_border = "#374151";
        if (!d.cm_shape) d.cm_shape = "ellipse";
        if (d.cm_width == null) d.cm_width = d.kind === "set" ? 42 : 28;
        if (d.cm_height == null) d.cm_height = d.kind === "set" ? 28 : 28;
        els.push({ group: "nodes", data: d, classes: n.classes || "" });
      });
      (payload.edges || []).forEach(function (e) {
        var d = e.data || e;
        if (d.cm_width == null) d.cm_width = 1.5;
        if (d.cm_opacity == null) d.cm_opacity = 0.7;
        if (!d.cm_color) d.cm_color = "#8A93A0";
        if (!d.cm_arrow) d.cm_arrow = d.directed === false ? "none" : "triangle";
        if (!d.cm_source_arrow) d.cm_source_arrow = "none";
        if (!d.cm_line_style) d.cm_line_style = "solid";
        els.push({ group: "edges", data: d });
      });
      return els;
    },
    _parseLayoutOpts: function (layoutOptsJson) {
      var layoutOpts = {};
      if (layoutOptsJson) {
        try {
          layoutOpts = typeof layoutOptsJson === "string" ? JSON.parse(layoutOptsJson) : layoutOptsJson;
        } catch (e) { /* ignore */ }
      }
      return layoutOpts;
    },
    /**
     * Full replace. Layout is deferred out of the LiveConnect stack so WebKit
     * does not terminate fcose mid-call; Java is notified via onLayoutComplete.
     */
    loadGraph: function (elementsJson, layoutOptsJson) {
      var payload = typeof elementsJson === "string" ? JSON.parse(elementsJson) : elementsJson;
      var layoutOpts = window.coreMapBridge._parseLayoutOpts(layoutOptsJson);
      var els = window.coreMapBridge._elsFromPayload(payload);
      cy.elements().remove();
      if (els.length) {
        cy.add(els);
      }
      cy.resize();
      layoutDirty = true;
      pendingLayoutOpts = layoutOpts;
      deferOutOfLiveConnect(runDeferredLayout);
    },
    beginGraphLoad: function () {
      layoutToken++;
      var previous = cy.scratch("_coremap_layout");
      if (previous && typeof previous.stop === "function") {
        try { previous.stop(); } catch (e) { /* ignore */ }
      }
      cy.elements().remove();
      pendingLayoutOpts = null;
      layoutDirty = false;
    },
    /** Add a small batch ({nodes:[], edges:[]}) — keep LiveConnect payloads tiny. */
    addGraphElements: function (elementsJson) {
      var payload = typeof elementsJson === "string" ? JSON.parse(elementsJson) : elementsJson;
      var els = window.coreMapBridge._elsFromPayload(payload);
      if (els.length) {
        cy.add(els);
      }
    },
    finishGraphLoad: function (layoutOptsJson) {
      var layoutOpts = window.coreMapBridge._parseLayoutOpts(layoutOptsJson);
      cy.resize();
      layoutDirty = true;
      pendingLayoutOpts = layoutOpts;
      deferOutOfLiveConnect(runDeferredLayout);
    },
    focusGenes: function (geneIdsJson) {
      return window.coreMapBridge.focusNeighborhood(geneIdsJson);
    },
    /**
     * Sidebar gene / hub / driver selection — CoreMap selectedIds path:
     * highlight each node plus its closed neighborhood. Camera stays put.
     */
    focusNeighborhood: function (geneIdsJson) {
      var ids = typeof geneIdsJson === "string" ? JSON.parse(geneIdsJson) : geneIdsJson;
      cy.elements().removeClass("highlighted faded");
      if (!ids || !ids.length) return false;
      var focusIdSet = {};
      ids.forEach(function (id) {
        if (id != null) focusIdSet[String(id).toUpperCase()] = true;
      });
      var focus = cy.collection();
      cy.nodes().forEach(function (n) {
        var nid = String(n.id() || "").toUpperCase();
        if (focusIdSet[nid]) {
          focus = focus.union(n.closedNeighborhood());
        }
      });
      if (focus.empty()) return false;
      cy.elements().difference(focus).addClass("faded");
      focus.addClass("highlighted");
      return true;
    },
    /** Path / bridge focus with optional exact cascade edge pairs (source\\ttarget). */
    focusPath: function (geneIdsJson, edgePairsJson) {
      var ids = typeof geneIdsJson === "string" ? JSON.parse(geneIdsJson) : geneIdsJson;
      var pairs = edgePairsJson
        ? (typeof edgePairsJson === "string" ? JSON.parse(edgePairsJson) : edgePairsJson)
        : null;
      var pairSet = {};
      (pairs || []).forEach(function (p) {
        if (p != null) pairSet[String(p).toUpperCase()] = true;
      });
      var useExact = pairs && pairs.length > 0;
      cy.elements().removeClass("highlighted faded");
      if (!ids || !ids.length) return false;
      var focusIdSet = {};
      ids.forEach(function (id) {
        if (id != null) focusIdSet[String(id).toUpperCase()] = true;
      });
      var focusNodes = cy.collection();
      cy.nodes().forEach(function (n) {
        var nid = String(n.id() || "").toUpperCase();
        if (focusIdSet[nid]) focusNodes = focusNodes.union(n);
      });
      if (focusNodes.empty()) return false;
      // Match CoreMap networkGraphFocus: exact cascade pairs for mechanism edges,
      // always keep membership/set_link among focused nodes; fall back to all
      // induced edges if no exact mechanism pairs hit.
      var exactEdges = cy.collection();
      var membershipEdges = cy.collection();
      cy.edges().forEach(function (e) {
        var s = String(e.source().id() || "").toUpperCase();
        var t = String(e.target().id() || "").toUpperCase();
        if (!focusIdSet[s] || !focusIdSet[t]) return;
        var kind = String(e.data("edge_kind") || "mechanism");
        if (kind === "membership" || kind === "set_link") {
          membershipEdges = membershipEdges.union(e);
          return;
        }
        if (!useExact || pairSet[s + "\t" + t]) {
          exactEdges = exactEdges.union(e);
        }
      });
      var focusEdges = membershipEdges.union(exactEdges);
      if (useExact && exactEdges.empty()) {
        focusEdges = membershipEdges.union(focusNodes.edgesWith(focusNodes));
      }
      var focus = focusNodes.union(focusEdges);
      cy.elements().difference(focus).addClass("faded");
      focusNodes.addClass("highlighted");
      focusEdges.addClass("highlighted");
      // Do not cy.fit here — CoreMap keeps camera stable on sidebar selection.
      return true;
    },
    /**
     * Ranked find (exact → prefix → substring on id/label), then neighborhood focus.
     * @returns {string|false} best-matching node id, or false
     */
    findAndFocus: function (query) {
      if (!query) return false;
      var q = String(query).toLowerCase().trim();
      if (!q) return false;
      var best = null;
      var bestRank = 99;
      cy.nodes().forEach(function (n) {
        var id = String(n.id() || "").toLowerCase();
        var label = String(n.data("label") || "").toLowerCase();
        var rank = 99;
        if (id === q || label === q) rank = 0;
        else if (id.indexOf(q) === 0 || label.indexOf(q) === 0) rank = 1;
        else if (id.indexOf(q) >= 0 || label.indexOf(q) >= 0) rank = 2;
        if (rank < bestRank) {
          bestRank = rank;
          best = n;
        }
      });
      if (!best) return false;
      var focus = best.closedNeighborhood();
      cy.elements().removeClass("highlighted faded");
      cy.elements().difference(focus).addClass("faded");
      focus.addClass("highlighted");
      return best.id();
    },
    focusEdge: function (edgeId) {
      if (!edgeId) return false;
      var e = cy.getElementById(edgeId);
      if (!e || e.empty() || !e.isEdge()) {
        e = cy.edges().filter(function (edge) {
          return edge.id() === edgeId || edge.data("id") === edgeId;
        });
      }
      if (!e || e.empty()) return false;
      var focus = e.union(e.connectedNodes());
      cy.elements().removeClass("highlighted faded");
      cy.elements().difference(focus).addClass("faded");
      focus.addClass("highlighted");
      return true;
    },
    fit: function () {
      cy.resize();
      cy.fit(undefined, 40);
    },
    /** Zoom about viewport center (CoreMap zoom chrome). Factor e.g. 1.25 or 0.8. */
    zoomBy: function (factor) {
      if (!factor || factor <= 0) return;
      cy.resize();
      var z = cy.zoom() * factor;
      z = Math.max(0.15, Math.min(2.5, z));
      var pan = cy.pan();
      var w = cy.width();
      var h = cy.height();
      cy.zoom({
        level: z,
        renderedPosition: { x: w / 2, y: h / 2 }
      });
      // Keep pan if zoom API didn't accept renderedPosition in this WebKit build.
      if (cy.zoom() !== z) {
        cy.zoom(z);
        cy.pan(pan);
      }
    },
    reflow: function () {
      cy.resize();
      layoutDirty = true;
      runLayout({ randomize: true }, true);
    },
    /**
     * Post-inject safety: lay out if still dirty/stacked. Returns true if a layout was started.
     */
    ensureLaidOut: function () {
      cy.resize();
      if (!containerHasSize()) {
        schedulePendingLayout(pendingLayoutOpts || { randomize: true });
        return false;
      }
      if (layoutDirty || pendingLayoutOpts || nodesLookStacked()) {
        var o = pendingLayoutOpts || { randomize: true };
        pendingLayoutOpts = null;
        runLayout(o, true);
        return true;
      }
      notifyLayoutComplete();
      return false;
    },
    clearFocus: function () {
      cy.elements().removeClass("highlighted faded");
    },
    exportPng: function (scope) {
      // scope: "view" (default) or "focus" — hide non-highlighted for focus (CoreMap export).
      var focus = scope === "focus" ? cy.elements(".highlighted") : cy.collection();
      var hide = scope === "focus" && focus.length > 0
        ? cy.elements().difference(focus)
        : cy.collection();
      hide.style("display", "none");
      try {
        return cy.png({
          output: "base64uri",
          bg: "#ffffff",
          full: true,
          scale: 2,
          maxWidth: 4096,
          maxHeight: 4096
        });
      } finally {
        hide.style("display", "element");
      }
    },
    exportCurrentJson: function () {
      return JSON.stringify(cy.json());
    },
    exportSelectedJson: function () {
      var nodes = [];
      var edges = [];
      cy.elements(".highlighted").forEach(function (ele) {
        var json = ele.json();
        if (ele.isNode()) {
          nodes.push({ data: json.data, classes: json.classes || "" });
        } else if (ele.isEdge()) {
          edges.push({ data: json.data });
        }
      });
      return JSON.stringify({ nodes: nodes, edges: edges });
    },
    hasSelection: function () {
      return cy.elements(".highlighted").length > 0;
    }
  };

  cy.on("tap", "node", function (evt) {
    var id = evt.target.id();
    var oe = evt.originalEvent;
    var additive = !!(oe && (oe.ctrlKey || oe.metaKey));
    if (window.javaCoreMap && window.javaCoreMap.onNodeSelected) {
      // Pass additive as string — JavaFX LiveConnect mishandles boolean overloads.
      window.javaCoreMap.onNodeSelected(id, additive ? "true" : "false");
    }
  });

  cy.on("tap", "edge", function (evt) {
    var data = evt.target.data();
    if (window.javaCoreMap && window.javaCoreMap.onEdgeSelected) {
      try {
        window.javaCoreMap.onEdgeSelected(JSON.stringify(data));
      } catch (err) {
        /* ignore */
      }
    }
  });

  cy.on("tap", function (evt) {
    if (evt.target === cy && window.javaCoreMap && window.javaCoreMap.onBackgroundTap) {
      window.javaCoreMap.onBackgroundTap();
    }
  });
})();
