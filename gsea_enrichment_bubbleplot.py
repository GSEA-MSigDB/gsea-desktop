def plot_bubble_single_dataset(df, label, outpath, top_n=40):
    """
    Bubble plot for a single dataset: Y=gene set, X=-log10(NOM p-val), bubble size=|NES|, color=FDR q-val.
    Only top_n gene sets by |NES| are shown.
    """
    import matplotlib.pyplot as plt
    import numpy as np
    # Select top_n gene sets by |NES|
    df_sorted = df.copy().sort_values(by='NES', key=np.abs, ascending=False).head(top_n)
    names = df_sorted['NAME'].tolist()
    nes = df_sorted['NES'].values
    nom_pval = df_sorted['NOM p-val'].values
    fdr = df_sorted['FDR q-val'].values
    y = np.arange(len(names))
    # Bubble size scaling (cube root)
    min_nes = np.abs(nes).min()
    max_nes = np.abs(nes).max()
    min_diam = 5
    max_diam = 15
    def scale_size(n):
        val = np.abs(n)
        root_min = np.cbrt(min_nes)
        root_max = np.cbrt(max_nes)
        root_val = np.cbrt(val)
        if root_max - root_min < 1e-6:
            diam = (min_diam + max_diam) / 2
        else:
            diam = min_diam + (max_diam - min_diam) * (root_val - root_min) / (root_max - root_min)
        return diam ** 2
    sizes = [scale_size(n) for n in nes]
    # Diverging color scheme: NES sign × FDR bin
    import numpy as np
    fdr_bins = np.full_like(fdr, '', dtype=object)
    fdr_bins[fdr <= 0.05] = 'FDR ≤ 0.05'
    fdr_bins[(fdr > 0.05) & (fdr <= 0.25)] = '0.05 < FDR ≤ 0.25'
    fdr_bins[fdr > 0.25] = 'FDR > 0.25'
    # Color map: (NES sign, FDR bin) -> color
    color_map = {
        ('pos', 'FDR ≤ 0.05'): '#b2182b',      # strong red
        ('pos', '0.05 < FDR ≤ 0.25'): '#ef8a62', # medium red
        ('pos', 'FDR > 0.25'): '#cccccc',        # gray
        ('neg', 'FDR ≤ 0.05'): '#2166ac',      # strong blue
        ('neg', '0.05 < FDR ≤ 0.25'): '#67a9cf', # medium blue
        ('neg', 'FDR > 0.25'): '#cccccc',        # gray
    }
    nes_signs = np.where(nes >= 0, 'pos', 'neg')
    colors = [color_map[(sign, bin)] for sign, bin in zip(nes_signs, fdr_bins)]
    # Edge color: black for FDR ≤ 0.25, gray otherwise
    edge_colors = ['black' if b in ('FDR ≤ 0.05', '0.05 < FDR ≤ 0.25') else '#888888' for b in fdr_bins]
    # Plot
    # Dynamically adjust figure width for legends
    legend_width = 4.5  # width in inches for legends
    base_width = 7
    fig_width = base_width + legend_width
    fig, ax = plt.subplots(figsize=(fig_width, 0.4*top_n+2))
    sc = ax.scatter(-np.log10(nom_pval+1e-10), y, s=sizes, c=colors, edgecolor=edge_colors, linewidth=1.2, alpha=0.85)
    ax.set_yticks(y)
    ax.set_yticklabels(names, fontsize=8)
    ax.set_xlabel('-log10(NOM p-val)', labelpad=10)
    ax.set_ylabel('Gene set', labelpad=10)
    ax.set_title(f'{label}: Top {top_n} Gene Sets', pad=15)

    # Custom legend for NES sign × FDR bin (right, vertically centered)
    from matplotlib.lines import Line2D
    legend_elements = [
        Line2D([0], [0], marker='o', color='w', label='NES > 0, FDR ≤ 0.05',
            markerfacecolor=color_map[('pos', 'FDR ≤ 0.05')], markeredgecolor='black', markersize=10),
        Line2D([0], [0], marker='o', color='w', label='NES > 0, 0.05 < FDR ≤ 0.25',
            markerfacecolor=color_map[('pos', '0.05 < FDR ≤ 0.25')], markeredgecolor='black', markersize=10),
        Line2D([0], [0], marker='o', color='w', label='NES > 0, FDR > 0.25',
            markerfacecolor=color_map[('pos', 'FDR > 0.25')], markeredgecolor='#888888', markersize=10),
        Line2D([0], [0], marker='o', color='w', label='NES < 0, FDR ≤ 0.05',
            markerfacecolor=color_map[('neg', 'FDR ≤ 0.05')], markeredgecolor='black', markersize=10),
        Line2D([0], [0], marker='o', color='w', label='NES < 0, 0.05 < FDR ≤ 0.25',
            markerfacecolor=color_map[('neg', '0.05 < FDR ≤ 0.25')], markeredgecolor='black', markersize=10),
        Line2D([0], [0], marker='o', color='w', label='NES < 0, FDR > 0.25',
            markerfacecolor=color_map[('neg', 'FDR > 0.25')], markeredgecolor='#888888', markersize=10),
    ]
    legend1 = ax.legend(handles=legend_elements, title='NES sign & FDR bin', loc='center left', bbox_to_anchor=(1.01, 0.5), frameon=True, borderaxespad=0.)
    ax.add_artist(legend1)

    # Bubble size legend (below color legend, right side)
    min_size = scale_size(min_nes)
    mid_size = scale_size((min_nes + max_nes) / 2)
    max_size = scale_size(max_nes)
    size_legend = [
        Line2D([0], [0], marker='o', color='w', label=f'|NES|={min_nes:.2f}', markerfacecolor='gray', markeredgecolor='gray', markersize=np.sqrt(min_size)),
        Line2D([0], [0], marker='o', color='w', label=f'|NES|={((min_nes+max_nes)/2):.2f}', markerfacecolor='gray', markeredgecolor='gray', markersize=np.sqrt(mid_size)),
        Line2D([0], [0], marker='o', color='w', label=f'|NES|={max_nes:.2f}', markerfacecolor='gray', markeredgecolor='gray', markersize=np.sqrt(max_size)),
    ]
    legend2 = ax.legend(handles=size_legend, title='Bubble size: |NES|', loc='center left', bbox_to_anchor=(1.01, 0.05), frameon=True, borderaxespad=0.)
    ax.add_artist(legend2)

    # Vertical lines at significance thresholds
    y_min, y_max = ax.get_ylim()
    for thresh in [0.05, 0.25]:
        xval = -np.log10(thresh)
        ax.axvline(xval, color='black', linestyle='--', linewidth=1, alpha=0.7, zorder=1)
        # Place label above the axis, not below y-ticks
        ax.annotate(f'{thresh}', xy=(xval, y_max+0.2), xycoords='data', ha='center', va='bottom',
                    fontsize=8, backgroundcolor='white', rotation=0, clip_on=False)
    ax.set_ylim(y_min, y_max)

    # Ensure all legends are visible: tight layout, then reserve extra space for legends
    fig.tight_layout()
    fig.subplots_adjust(right=0.8)  # leave more space on right for legends
    import os
    os.makedirs(os.path.dirname(outpath), exist_ok=True)
    # Ensure both legends are included in the saved figure
    plt.savefig(outpath, bbox_inches='tight', bbox_extra_artists=[legend1, legend2])
    plt.close()

def plot_bubble_significance(all_dfs, run_labels, outpath, top_n=40, stat_col='FDR q-val'):
    """
    Bubble plot: Y=gene set, X=-log10(significance), one bubble per run, size=|NES|, color=NES, edge color=run.
    Only top_n gene sets by max |NES| across all runs are shown for clarity.
    stat_col: 'FDR q-val' or 'NOM p-val'
    """
    import matplotlib.pyplot as plt
    import numpy as np
    import matplotlib.colors as mcolors
    # Find union of all gene sets
    all_names = set()
    for df in all_dfs:
        all_names.update(df['NAME'])
    all_names = list(all_names)
    # Build NES and stat matrix (rows: gene sets, cols: runs)
    nes_mat = np.full((len(all_names), len(all_dfs)), np.nan)
    stat_mat = np.full((len(all_names), len(all_dfs)), np.nan)
    for j, df in enumerate(all_dfs):
        name2nes = dict(zip(df['NAME'], df['NES']))
        name2stat = dict(zip(df['NAME'], df[stat_col]))
        for i, name in enumerate(all_names):
            if name in name2nes:
                nes_mat[i, j] = name2nes[name]
                stat_mat[i, j] = name2stat[name]
    # Sort gene sets by descending sum of absolute NES across all runs (ignoring NaNs)
    abs_nes_sum = np.nansum(np.abs(nes_mat), axis=1)
    sort_idx = np.argsort(-abs_nes_sum)
    # Select top_n gene sets by this order
    top_idx = sort_idx[:top_n]
    nes_mat = nes_mat[top_idx, :]
    stat_mat = stat_mat[top_idx, :]
    names = [all_names[i] for i in top_idx]
    # Plot
    fig, ax = plt.subplots(figsize=(8, 0.4*top_n+2))
    color_list = list(mcolors.TABLEAU_COLORS.values()) + list(mcolors.CSS4_COLORS.values())
    run_colors = [color_list[i % len(color_list)] for i in range(len(run_labels))]
    # Perceptual scaling: map |NES| to bubble diameter, then square for area
    abs_nes = np.abs(nes_mat[~np.isnan(nes_mat)])
    min_nes = np.nanmin(abs_nes) if abs_nes.size > 0 else 0.1
    max_nes = np.nanmax(abs_nes) if abs_nes.size > 0 else 2.0
    min_diam = 10    # minimum bubble diameter in points
    max_diam = 25   # maximum bubble diameter in points
    def scale_size(nes):
        val = np.abs(nes)
        # Cube root scaling for diameter
        root_min = np.cbrt(min_nes)
        root_max = np.cbrt(max_nes)
        root_val = np.cbrt(val)
        if root_max - root_min < 1e-6:
            diam = (min_diam + max_diam) / 2
        else:
            diam = min_diam + (max_diam - min_diam) * (root_val - root_min) / (root_max - root_min)
        return diam ** 2  # scatter s is area in points^2

    # Use categorical colors for runs
    import matplotlib.colors as mcolors
    categorical_cmap = plt.get_cmap('tab10')
    run_colors = [categorical_cmap(i % 10) for i in range(len(run_labels))]
    for i in range(len(names)):
        for j in range(len(run_labels)):
            nes = nes_mat[i, j]
            stat = stat_mat[i, j]
            if np.isnan(nes) or np.isnan(stat):
                continue
            x = -np.log10(stat + 1e-10)
            size = scale_size(nes)
            bubble_color = run_colors[j]
            ax.scatter(x, i, s=size, color=bubble_color, edgecolor='black', linewidth=0.5, alpha=0.85)

    # Use symlog scale for X axis to spread out small values
    ax.set_xscale('symlog', linthresh=1)
    ax.set_yticks(range(len(names)))
    ax.set_yticklabels(names, fontsize=8)
    ax.set_xlabel(f'-log10({stat_col}) (symlog scale)')
    ax.set_ylabel('Gene set')
    ax.set_title(f'Bubble Plot: -log10({stat_col}) vs Gene Set (bubble size=|NES|, color=NES)')
    # No colorbar (colorbar removed for clarity)

    # Draw vertical lines at significance thresholds (0.05 and 0.25), transformed to X axis
    sig_thresholds = [0.05, 0.25]
    y_min, y_max = ax.get_ylim()
    for thresh in sig_thresholds:
        xval = -np.log10(thresh)
        ax.axvline(xval, color='black', linestyle='--', linewidth=1, alpha=0.7, zorder=1)
        # Place label just below the lowest y tick, rotated for clarity
        ax.text(xval, y_min - 0.5, f'{thresh}', color='black', ha='center', va='top',
                fontsize=8, backgroundcolor='white', rotation=90, clip_on=False)
    ax.set_ylim(y_min, y_max)

    # Bubble size legend (outside plot)
    from matplotlib.lines import Line2D
    # Bubble size legend (show a range of diameters)
    for legend_abs in [min_nes, (min_nes+max_nes)/2, max_nes]:
        root_min = np.cbrt(min_nes)
        root_max = np.cbrt(max_nes)
        root_val = np.cbrt(legend_abs)
        if root_max - root_min < 1e-6:
            diam = (min_diam + max_diam) / 2
        else:
            diam = min_diam + (max_diam - min_diam) * (root_val - root_min) / (root_max - root_min)
        ax.scatter([], [], s=diam**2, c='gray', alpha=0.7, edgecolor='gray', label=f'|NES|={legend_abs:.2f}')
    size_legend = ax.legend(
        loc='upper left', bbox_to_anchor=(1.01, 1),
        title='Bubble size: |NES| (diameter)', frameon=True
    )
    ax.add_artist(size_legend)

    # Run color legend (outside plot, below size legend)
    run_handles = [Line2D([0], [0], marker='o', color='w', markerfacecolor=run_colors[i], markeredgecolor=run_colors[i], markersize=10, label=run_labels[i], linewidth=2) for i in range(len(run_labels))]
    run_legend = ax.legend(
        handles=run_handles, loc='upper left', bbox_to_anchor=(1.01, 0.55),
        title='Run/Analysis', frameon=True
    )
    ax.add_artist(run_legend)

    fig.subplots_adjust(right=0.7)  # leave more space on right for legends
    plt.tight_layout()
    import os
    os.makedirs(os.path.dirname(outpath), exist_ok=True)
    plt.savefig(outpath, bbox_inches='tight', bbox_extra_artists=[size_legend, run_legend])
    plt.close()
def plot_pval_grid(pair_data, outpath, pair_labels, method_order, short_names):
    """
    Grid of -log10(NOM p-val) for each run, bubble color = NES shift, size = |NES shift|, threshold at 0.05.
    """
    import matplotlib.pyplot as plt
    import numpy as np
    n = len(pair_data)
    ncols = min(3, n)
    nrows = (n + ncols - 1) // ncols
    fig, axes = plt.subplots(nrows, ncols, figsize=(6*ncols, 6*nrows), squeeze=False)
    # Find global axis limits
    all_x = []
    all_y = []
    for merged, _, _ in pair_data:
        x = -np.log10(merged['NOM p-val_1'] + 1e-10)
        y = -np.log10(merged['NOM p-val_2'] + 1e-10)
        all_x.append(x)
        all_y.append(y)
    all_x = np.concatenate(all_x)
    all_y = np.concatenate(all_y)
    minval = min(all_x.min(), all_y.min())
    maxval = max(all_x.max(), all_y.max())
    sig_thresh_005 = -np.log10(0.05)
    lower = min(minval, sig_thresh_005) - 0.5
    upper = max(maxval, sig_thresh_005) + 0.5
    size_legend = None
    color_legend = None
    for idx, ((merged, run1_label, run2_label), label) in enumerate(zip(pair_data, pair_labels)):
        row, col = divmod(idx, ncols)
        ax = axes[row][col]
        i1 = method_order[run1_label]
        i2 = method_order[run2_label]
        if i1 > i2:
            x = -np.log10(merged['NOM p-val_2'] + 1e-10)
            y = -np.log10(merged['NOM p-val_1'] + 1e-10)
            x_label = short_names[run2_label]
            y_label = short_names[run1_label]
            plot_title = f'{short_names[run2_label]} vs {short_names[run1_label]}'
            nes1 = merged['NES_2']
            nes2 = merged['NES_1']
        else:
            x = -np.log10(merged['NOM p-val_1'] + 1e-10)
            y = -np.log10(merged['NOM p-val_2'] + 1e-10)
            x_label = short_names[run1_label]
            y_label = short_names[run2_label]
            plot_title = f'{short_names[run1_label]} vs {short_names[run2_label]}'
            nes1 = merged['NES_1']
            nes2 = merged['NES_2']
        abs_shift = np.abs(nes2 - nes1)
        size = abs_shift * 200
        # Color logic: more extreme in direction of sign
        color = np.zeros_like(nes1)
        cmap_pos = plt.get_cmap('Blues')
        cmap_neg = plt.get_cmap('Reds')
        cmap_neutral = plt.get_cmap('Greys')
        # For each point, assign color value: positive = more extreme positive, negative = more extreme negative, neutral = sign flip
        color_vals = []
        color_maps = []
        for n1, n2 in zip(nes1, nes2):
            if n1 > 0 and n2 > 0:
                # More positive = more extreme
                color_vals.append(n2 - n1)
                color_maps.append('pos')
            elif n1 < 0 and n2 < 0:
                # More negative = more extreme
                color_vals.append(n1 - n2)  # n1-n2: more negative = more extreme
                color_maps.append('neg')
            else:
                # Sign flip
                color_vals.append(0)
                color_maps.append('neutral')
        # Normalize color values for each group
        import numpy as np
        color_vals = np.array(color_vals)
        color_array = np.zeros((len(color_vals), 4))  # RGBA
        # Positive group
        pos_idx = [i for i, m in enumerate(color_maps) if m == 'pos']
        if pos_idx:
            pos_vals = color_vals[pos_idx]
            vmin, vmax = min(0, pos_vals.min()), max(0.01, pos_vals.max())
            normed = (pos_vals - vmin) / (vmax - vmin + 1e-10)
            for i, ni in zip(pos_idx, normed):
                color_array[i] = cmap_pos(ni)
        # Negative group
        neg_idx = [i for i, m in enumerate(color_maps) if m == 'neg']
        if neg_idx:
            neg_vals = color_vals[neg_idx]
            vmin, vmax = min(0, neg_vals.min()), max(0.01, neg_vals.max())
            normed = (neg_vals - vmin) / (vmax - vmin + 1e-10)
            for i, ni in zip(neg_idx, normed):
                color_array[i] = cmap_neg(ni)
        # Neutral group
        neutral_idx = [i for i, m in enumerate(color_maps) if m == 'neutral']
        if neutral_idx:
            for i in neutral_idx:
                color_array[i] = cmap_neutral(0.5)
        sc = ax.scatter(x, y, c=color_array, s=size, alpha=0.7, edgecolor='k')
        ax.set_xlabel(f'-log10(NOM p-val) {x_label}')
        ax.set_ylabel(f'-log10(NOM p-val) {y_label}')
        ax.set_title(plot_title)
        ax.set_xlim(lower, upper)
        ax.set_ylim(lower, upper)
        # Diagonal
        ax.plot([lower, upper], [lower, upper], 'k--', lw=1, alpha=0.5)
        # Threshold line
        ax.axhline(sig_thresh_005, color='grey', linestyle=':', lw=1)
        ax.axvline(sig_thresh_005, color='grey', linestyle=':', lw=1)
        if idx == 0:
            from matplotlib.lines import Line2D
            from matplotlib.patches import Patch
            size_handles = [
                Line2D([0], [0], marker='o', color='w', label='|NES shift|=0.5',
                       markerfacecolor='gray', markeredgecolor='k', alpha=0.6, markersize=np.sqrt(0.5 * 200)),
                Line2D([0], [0], marker='o', color='w', label='|NES shift|=1.0',
                       markerfacecolor='gray', markeredgecolor='k', alpha=0.6, markersize=np.sqrt(1.0 * 200)),
                Line2D([0], [0], marker='o', color='w', label='|NES shift|=2.0',
                       markerfacecolor='gray', markeredgecolor='k', alpha=0.6, markersize=np.sqrt(2.0 * 200)),
            ]
            size_legend = fig.legend(
                handles=size_handles,
                loc='upper left',
                bbox_to_anchor=(0.83, 0.98),
                title='Bubble size: |NES shift|',
                frameon=True
            )
            color_handles = [
                Patch(facecolor=plt.get_cmap('Blues')(0.8), edgecolor='k', label='More extreme (+)'),
                Patch(facecolor=plt.get_cmap('Reds')(0.8), edgecolor='k', label='More extreme (-)'),
                Patch(facecolor=plt.get_cmap('Greys')(0.5), edgecolor='k', label='Sign flip'),
            ]
            color_legend = fig.legend(
                handles=color_handles,
                loc='upper left',
                bbox_to_anchor=(0.83, 0.67),
                title='Color: NES extremity',
                frameon=True
            )
    # Hide unused subplots
    for idx in range(len(pair_data), nrows*ncols):
        row, col = divmod(idx, ncols)
        axes[row][col].axis('off')
    fig.subplots_adjust(right=0.8, wspace=0.35, hspace=0.35)
    plt.tight_layout(rect=[0, 0, 0.8, 1])
    import os
    os.makedirs(os.path.dirname(outpath), exist_ok=True)
    extra_artists = [artist for artist in [size_legend, color_legend] if artist is not None]
    plt.savefig(outpath, bbox_inches='tight', bbox_extra_artists=extra_artists)
    plt.close()
def plot_bubble_grid(pair_data, outpath, pair_labels, method_order, short_names):
    import matplotlib.pyplot as plt
    import numpy as np
    n = len(pair_data)
    ncols = min(3, n)
    nrows = (n + ncols - 1) // ncols
    fig, axes = plt.subplots(nrows, ncols, figsize=(6*ncols, 6*nrows), squeeze=False)
    # Find global axis limits
    all_x = []
    all_y = []
    for merged, _, _ in pair_data:
        x = -np.log10(merged['FDR q-val_1'] + 1e-10)
        y = -np.log10(merged['FDR q-val_2'] + 1e-10)
        all_x.append(x)
        all_y.append(y)
    all_x = np.concatenate(all_x)
    all_y = np.concatenate(all_y)
    minval = min(all_x.min(), all_y.min())
    maxval = max(all_x.max(), all_y.max())
    sig_thresh_005 = -np.log10(0.05)
    sig_thresh_025 = -np.log10(0.25)
    lower = min(minval, sig_thresh_025) - 0.5
    upper = max(maxval, sig_thresh_005) + 0.5
    size_legend = None
    color_legend = None
    for idx, ((merged, run1_label, run2_label), label) in enumerate(zip(pair_data, pair_labels)):
        row, col = divmod(idx, ncols)
        ax = axes[row][col]
        # Determine which is worse/better by method_order
        i1 = method_order[run1_label]
        i2 = method_order[run2_label]
        if i1 > i2:
            x = -np.log10(merged['FDR q-val_2'] + 1e-10)
            y = -np.log10(merged['FDR q-val_1'] + 1e-10)
            x_label = short_names[run2_label]
            y_label = short_names[run1_label]
            plot_title = f'{short_names[run2_label]} vs {short_names[run1_label]}'
            nes1 = merged['NES_2']
            nes2 = merged['NES_1']
        else:
            x = -np.log10(merged['FDR q-val_1'] + 1e-10)
            y = -np.log10(merged['FDR q-val_2'] + 1e-10)
            x_label = short_names[run1_label]
            y_label = short_names[run2_label]
            plot_title = f'{short_names[run1_label]} vs {short_names[run2_label]}'
            nes1 = merged['NES_1']
            nes2 = merged['NES_2']
        abs_shift = np.abs(nes2 - nes1)
        size = abs_shift * 200
        # Color logic: more extreme in direction of sign
        color = np.zeros_like(nes1)
        cmap_pos = plt.get_cmap('Blues')
        cmap_neg = plt.get_cmap('Reds')
        cmap_neutral = plt.get_cmap('Greys')
        color_vals = []
        color_maps = []
        for n1, n2 in zip(nes1, nes2):
            if n1 > 0 and n2 > 0:
                color_vals.append(n2 - n1)
                color_maps.append('pos')
            elif n1 < 0 and n2 < 0:
                color_vals.append(n1 - n2)
                color_maps.append('neg')
            else:
                color_vals.append(0)
                color_maps.append('neutral')
        import numpy as np
        color_vals = np.array(color_vals)
        color_array = np.zeros((len(color_vals), 4))
        pos_idx = [i for i, m in enumerate(color_maps) if m == 'pos']
        if pos_idx:
            pos_vals = color_vals[pos_idx]
            vmin, vmax = min(0, pos_vals.min()), max(0.01, pos_vals.max())
            normed = (pos_vals - vmin) / (vmax - vmin + 1e-10)
            for i, ni in zip(pos_idx, normed):
                color_array[i] = cmap_pos(ni)
        neg_idx = [i for i, m in enumerate(color_maps) if m == 'neg']
        if neg_idx:
            neg_vals = color_vals[neg_idx]
            vmin, vmax = min(0, neg_vals.min()), max(0.01, neg_vals.max())
            normed = (neg_vals - vmin) / (vmax - vmin + 1e-10)
            for i, ni in zip(neg_idx, normed):
                color_array[i] = cmap_neg(ni)
        neutral_idx = [i for i, m in enumerate(color_maps) if m == 'neutral']
        if neutral_idx:
            for i in neutral_idx:
                color_array[i] = cmap_neutral(0.5)
        sc = ax.scatter(x, y, c=color_array, s=size, alpha=0.7, edgecolor='k')
        ax.set_xlabel(f'-log10(FDR q-value) {x_label}')
        ax.set_ylabel(f'-log10(FDR q-value) {y_label}')
        ax.set_title(plot_title)
        ax.set_xlim(lower, upper)
        ax.set_ylim(lower, upper)
        # Diagonal
        ax.plot([lower, upper], [lower, upper], 'k--', lw=1, alpha=0.5)
        # Threshold lines
        ax.axhline(sig_thresh_005, color='grey', linestyle=':', lw=1)
        ax.axvline(sig_thresh_005, color='grey', linestyle=':', lw=1)
        ax.axhline(sig_thresh_025, color='orange', linestyle=':', lw=1)
        ax.axvline(sig_thresh_025, color='orange', linestyle=':', lw=1)
        if idx == 0:
            from matplotlib.lines import Line2D
            from matplotlib.patches import Patch
            size_handles = [
                Line2D([0], [0], marker='o', color='w', label='|NES shift|=0.5',
                       markerfacecolor='gray', markeredgecolor='k', alpha=0.6, markersize=np.sqrt(0.5 * 200)),
                Line2D([0], [0], marker='o', color='w', label='|NES shift|=1.0',
                       markerfacecolor='gray', markeredgecolor='k', alpha=0.6, markersize=np.sqrt(1.0 * 200)),
                Line2D([0], [0], marker='o', color='w', label='|NES shift|=2.0',
                       markerfacecolor='gray', markeredgecolor='k', alpha=0.6, markersize=np.sqrt(2.0 * 200)),
            ]
            size_legend = fig.legend(
                handles=size_handles,
                loc='upper left',
                bbox_to_anchor=(0.83, 0.98),
                title='Bubble size: |NES shift|',
                frameon=True
            )
            color_handles = [
                Patch(facecolor=plt.get_cmap('Blues')(0.8), edgecolor='k', label='More extreme (+)'),
                Patch(facecolor=plt.get_cmap('Reds')(0.8), edgecolor='k', label='More extreme (-)'),
                Patch(facecolor=plt.get_cmap('Greys')(0.5), edgecolor='k', label='Sign flip'),
            ]
            color_legend = fig.legend(
                handles=color_handles,
                loc='upper left',
                bbox_to_anchor=(0.83, 0.67),
                title='Color: NES extremity',
                frameon=True
            )
    # Hide unused subplots
    for idx in range(len(pair_data), nrows*ncols):
        row, col = divmod(idx, ncols)
        axes[row][col].axis('off')
    fig.subplots_adjust(right=0.8, wspace=0.35, hspace=0.35)
    plt.tight_layout(rect=[0, 0, 0.8, 1])
    import os
    os.makedirs(os.path.dirname(outpath), exist_ok=True)
    extra_artists = [artist for artist in [size_legend, color_legend] if artist is not None]
    plt.savefig(outpath, bbox_inches='tight', bbox_extra_artists=extra_artists)
    plt.close()
def plot_bubble_all_pairs(pair_data, outpath, pair_labels):
    import matplotlib.pyplot as plt
    import numpy as np
    import matplotlib.lines as mlines
    import matplotlib.patches as mpatches
    marker_styles = ['o', 's', '^', 'D', 'P', 'X', '*', 'v']  # up to 8 pairs
    fig, ax = plt.subplots(figsize=(9, 11))
    all_x = []
    all_y = []
    scatter_handles = []
    legend_labels = []
    for i, ((merged, run1_label, run2_label), marker) in enumerate(zip(pair_data, marker_styles)):
        x = -np.log10(merged['FDR q-val_1'] + 1e-10)
        y = -np.log10(merged['FDR q-val_2'] + 1e-10)
        color = merged['NES_2'] - merged['NES_1']
        sc = ax.scatter(x, y, c=color, cmap='coolwarm', alpha=0.7, edgecolor='k', marker=marker)
        all_x.append(x)
        all_y.append(y)
        # For legend: create a dummy handle for each pair's marker
        scatter_handles.append(plt.Line2D([0], [0], marker=marker, color='w', markerfacecolor='gray', markeredgecolor='k', markersize=10, linestyle='None'))
        legend_labels.append(f'{run1_label} vs {run2_label}')
    ax.set_xlabel('-log10(FDR q-value) Run A')
    ax.set_ylabel('-log10(FDR q-value) Run B')
    ax.set_title('GSEA Significance All-Pairs Comparison (colored by NES shift)')
    fig.colorbar(sc, ax=ax, label='NES Shift (Run2 - Run1)')
    # Diagonal and threshold lines
    all_x = np.concatenate(all_x)
    all_y = np.concatenate(all_y)
    minval = min(all_x.min(), all_y.min())
    maxval = max(all_x.max(), all_y.max())
    sig_thresh_005 = -np.log10(0.05)
    sig_thresh_025 = -np.log10(0.25)
    lower = min(minval, sig_thresh_025) - 0.5
    upper = max(maxval, sig_thresh_005) + 0.5
    ax.set_xlim(lower, upper)
    ax.set_ylim(lower, upper)
    # Diagonal
    ax.plot([lower, upper], [lower, upper], 'k--', lw=1, alpha=0.5)
    # Threshold lines
    ax.axhline(sig_thresh_005, color='grey', linestyle=':', lw=1)
    ax.axvline(sig_thresh_005, color='grey', linestyle=':', lw=1)
    ax.axhline(sig_thresh_025, color='orange', linestyle=':', lw=1)
    ax.axvline(sig_thresh_025, color='orange', linestyle=':', lw=1)
    # Custom legend handles for lines
    diag_legend = mlines.Line2D([], [], color='k', linestyle='--', label='Diagonal (x=y)')
    fdr005_legend = mlines.Line2D([], [], color='grey', linestyle=':', label='FDR=0.05')
    fdr025_legend = mlines.Line2D([], [], color='orange', linestyle=':', label='FDR=0.25')
    # Compose legend: marker shapes for pairs, then lines
    handles = scatter_handles + [diag_legend, fdr005_legend, fdr025_legend]
    labels = legend_labels + ['Diagonal (x=y)', 'FDR=0.05', 'FDR=0.25']
    legend = fig.legend(
        handles,
        labels,
        loc='lower center',
        bbox_to_anchor=(0.5, 0.02),
        ncol=3,
        frameon=True
    )
    # Reserve dedicated space beneath the plot for the legend.
    fig.tight_layout(rect=[0, 0.18, 1, 1])
    fig.savefig(outpath, bbox_inches='tight', bbox_extra_artists=[legend])
    plt.close(fig)
import os
import glob
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

# Utility to find all gsea_report_*.tsv files in a directory
def find_gsea_report_files(analysis_dir):
    return glob.glob(os.path.join(analysis_dir, "gsea_report_*.tsv"))

def clean_run_label(path_or_label):
    """
    Normalize run labels for plot titles/legends by trimming at '.Gsea' when present.
    """
    label = os.path.basename(os.path.normpath(path_or_label))
    idx = label.lower().find(".gsea")
    if idx != -1:
        label = label[:idx]
    return label

def load_gsea_results(report_files):
    import re
    dfs = []
    required_cols = ['NAME', 'NES', 'FDR q-val']
    for f in report_files:
        df = pd.read_csv(f, sep='\t')
        # Clean up column names (strip, remove HTML tags)
        df.columns = [re.sub(r'<.*?>', '', c).strip() for c in df.columns]
        # Check for required columns
        missing = [col for col in required_cols if col not in df.columns]
        if missing:
            print(f"Warning: {f} is missing columns: {missing}. Skipping this file.")
            continue
        # Add phenotype from filename
        pheno = os.path.basename(f).split("gsea_report_")[1].split(".")[0]
        df['PHENOTYPE'] = pheno
        # Normalize PHENOTYPE by stripping trailing _digits
        df['PHENOTYPE_NORM'] = df['PHENOTYPE'].apply(lambda x: re.sub(r'_\d+$', '', x))
        dfs.append(df)
    if dfs:
        return pd.concat(dfs, ignore_index=True)
    else:
        return pd.DataFrame()

def merge_results(df1, df2):
    # Merge on gene set name and normalized phenotype
    return pd.merge(
        df1, df2,
        left_on=['NAME', 'PHENOTYPE_NORM'],
        right_on=['NAME', 'PHENOTYPE_NORM'],
        suffixes=('_1', '_2')
    )

def compute_global_fdr_axis_limits(pair_data, margin=0.5):
    """
    Compute shared axis limits across all pairwise significance plots.
    """
    all_x = []
    all_y = []
    for merged, _, _ in pair_data:
        all_x.append(-np.log10(merged['FDR q-val_1'] + 1e-10).values)
        all_y.append(-np.log10(merged['FDR q-val_2'] + 1e-10).values)
    if not all_x or not all_y:
        return None
    all_x = np.concatenate(all_x)
    all_y = np.concatenate(all_y)
    minval = min(all_x.min(), all_y.min())
    maxval = max(all_x.max(), all_y.max())
    sig_thresh_005 = -np.log10(0.05)
    sig_thresh_025 = -np.log10(0.25)
    lower = min(minval, sig_thresh_025) - margin
    upper = max(maxval, sig_thresh_005) + margin
    return (lower, upper)

def plot_bubble(merged, outpath, run1_label='Run 1', run2_label='Run 2', axis_limits=None):
    # Scatter plot: -log10(FDR q-value) for each run, colored by NES shift
    import numpy as np
    x = -np.log10(merged['FDR q-val_1'] + 1e-10)
    y = -np.log10(merged['FDR q-val_2'] + 1e-10)
    color = merged['NES_2'] - merged['NES_1']
    plt.figure(figsize=(8,8))
    sc = plt.scatter(x, y, c=color, cmap='coolwarm', alpha=0.7, edgecolor='k')
    plt.xlabel(f'-log10(FDR q-value) {run1_label}')
    plt.ylabel(f'-log10(FDR q-value) {run2_label}')
    plt.title('GSEA Significance Comparison (colored by NES shift)')
    plt.colorbar(sc, label='NES Shift (Run2 - Run1)')
    # Diagonal reference line
    sig_thresh_005 = -np.log10(0.05)
    sig_thresh_025 = -np.log10(0.25)
    if axis_limits is None:
        minval = min(x.min(), y.min())
        maxval = max(x.max(), y.max())
        # Ensure axis limits include all data and both thresholds, with margin
        lower = min(minval, sig_thresh_025) - 0.5
        upper = max(maxval, sig_thresh_005) + 0.5
    else:
        lower, upper = axis_limits
    plt.xlim(lower, upper)
    plt.ylim(lower, upper)
    plt.plot([lower, upper], [lower, upper], 'k--', lw=1, alpha=0.5)
    # Add lines for significance thresholds
    plt.axhline(sig_thresh_005, color='grey', linestyle=':', lw=1, label='FDR=0.05')
    plt.axvline(sig_thresh_005, color='grey', linestyle=':', lw=1)
    plt.axhline(sig_thresh_025, color='orange', linestyle=':', lw=1, label='FDR=0.25')
    plt.axvline(sig_thresh_025, color='orange', linestyle=':', lw=1)
    plt.legend(['Diagonal (x=y)', 'FDR=0.05', 'FDR=0.25'])
    plt.tight_layout()
    plt.savefig(outpath)
    plt.close()

def main(run1_dir, run2_dir, outpath):
    files1 = find_gsea_report_files(run1_dir)
    files2 = find_gsea_report_files(run2_dir)
    df1 = load_gsea_results(files1)
    df2 = load_gsea_results(files2)
    if df1.empty or df2.empty:
        print("No GSEA report files found in one or both directories.")
        return
    print("Run 1 unique gene sets:", df1['NAME'].unique())
    print("Run 1 unique phenotypes:", df1['PHENOTYPE_NORM'].unique())
    print("Run 2 unique gene sets:", df2['NAME'].unique())
    print("Run 2 unique phenotypes:", df2['PHENOTYPE_NORM'].unique())
    merged = merge_results(df1, df2)
    if merged.empty:
        print("No overlapping gene sets found between runs.")
        # Print out a few sample NAME/PHENOTYPE pairs for further debugging
        print("Sample Run 1 NAME/PHENOTYPE:")
        print(df1[['NAME', 'PHENOTYPE']].head())
        print("Sample Run 2 NAME/PHENOTYPE:")
        print(df2[['NAME', 'PHENOTYPE']].head())
        return
    # Use the last part of the directory as the label for clarity
    run1_label = clean_run_label(run1_dir)
    run2_label = clean_run_label(run2_dir)
    plot_bubble(merged, outpath, run1_label, run2_label)
    print(f"Bubble plot saved to {outpath}")

if __name__ == "__main__":
    import argparse
    from itertools import combinations
    parser = argparse.ArgumentParser(description="GSEA Enrichment Bubble Plot Diagnostic (all pairs)")
    parser.add_argument('runs', nargs='+', help="Paths to 2-4 GSEA analysis directories")
    parser.add_argument('--outdir', default=".", help="Output directory for plots")
    args = parser.parse_args()
    if not (2 <= len(args.runs) <= 4):
        print("Please provide 2 to 4 analysis directories.")
        exit(1)
    # Build per-run data, then render summary and pairwise plots
    all_dfs = []
    run_labels = []
    for run_dir in args.runs:
        files = find_gsea_report_files(run_dir)
        df = load_gsea_results(files)
        if not df.empty:
            all_dfs.append(df)
            run_labels.append(clean_run_label(run_dir))
    if all_dfs:
        outpath_bubble = os.path.join(args.outdir, "gsea_enrichment_bubble_significance.png")
        plot_bubble_significance(all_dfs, run_labels, outpath_bubble, top_n=40, stat_col='FDR q-val')
        print(f"Bubble plot (significance) saved to {outpath_bubble}")
        # Per-dataset bubble plots
        for df, label in zip(all_dfs, run_labels):
            outpath_single = os.path.join(args.outdir, f"gsea_bubble_{label}.png")
            plot_bubble_single_dataset(df, label, outpath_single, top_n=40)
            print(f"Single-dataset bubble plot saved to {outpath_single}")
        # Pairwise plots across all runs
        pair_data = []
        pair_labels = []
        method_order = {label: i for i, label in enumerate(run_labels)}
        short_names = {label: label for label in run_labels}
        for i, j in combinations(range(len(all_dfs)), 2):
            run1_label = run_labels[i]
            run2_label = run_labels[j]
            merged = merge_results(all_dfs[i], all_dfs[j])
            if merged.empty:
                print(f"Skipping pair {run1_label} vs {run2_label}: no overlapping gene sets.")
                continue
            pair_data.append((merged, run1_label, run2_label))
            pair_labels.append(f"{run1_label} vs {run2_label}")
        shared_pairwise_limits = compute_global_fdr_axis_limits(pair_data) if pair_data else None
        for merged, run1_label, run2_label in pair_data:
            safe_run1 = "".join(ch if ch.isalnum() or ch in ("-", "_") else "_" for ch in run1_label)
            safe_run2 = "".join(ch if ch.isalnum() or ch in ("-", "_") else "_" for ch in run2_label)
            outpath_pairwise_sig = os.path.join(
                args.outdir,
                f"gsea_bubble_significance_{safe_run1}_vs_{safe_run2}.png"
            )
            plot_bubble(
                merged,
                outpath_pairwise_sig,
                run1_label,
                run2_label,
                axis_limits=shared_pairwise_limits
            )
            print(f"Pairwise significance bubble plot saved to {outpath_pairwise_sig}")
        if pair_data:
            outpath_pairwise_fdr = os.path.join(args.outdir, "gsea_bubble_pairwise_fdr_grid.png")
            plot_bubble_grid(pair_data, outpath_pairwise_fdr, pair_labels, method_order, short_names)
            print(f"Pairwise FDR grid saved to {outpath_pairwise_fdr}")
            outpath_pairwise_nom = os.path.join(args.outdir, "gsea_bubble_pairwise_nom_grid.png")
            plot_pval_grid(pair_data, outpath_pairwise_nom, pair_labels, method_order, short_names)
            print(f"Pairwise NOM p-val grid saved to {outpath_pairwise_nom}")
            outpath_pairwise_overlay = os.path.join(args.outdir, "gsea_bubble_pairwise_overlay.png")
            plot_bubble_all_pairs(pair_data, outpath_pairwise_overlay, pair_labels)
            print(f"Pairwise overlay bubble plot saved to {outpath_pairwise_overlay}")
        else:
            print("No pairwise plots generated: no overlapping gene sets across run pairs.")
