"""
Builds the graphs for the Assignment 4 report from TaskNN/data/*.csv.
Usage: python3 tools/make_graphs.py   (from the repository root, after run_all.sh)
"""
import csv
from pathlib import Path

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parent.parent
SLOTS = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100", "#e87ba4", "#008300", "#6250d6", "#e34948"]
MARKERS = ["o", "s", "^", "D", "v", "P", "X", "*"]
INK, MUTED, GRID = "#2b2b29", "#6b6a64", "#e4e3de"


def rows(path):
    with open(path) as f:
        return list(csv.DictReader(f))


def setup(ax, title, xlabel, ylabel, logx=False, logy=False):
    ax.set_title(title, fontsize=11, color=INK, loc="left", fontweight="bold")
    ax.set_xlabel(xlabel, color=MUTED, fontsize=9)
    ax.set_ylabel(ylabel, color=MUTED, fontsize=9)
    if logx: ax.set_xscale("log")
    if logy: ax.set_yscale("log")
    ax.grid(True, color=GRID, linewidth=0.8)
    for s in ["top", "right"]: ax.spines[s].set_visible(False)
    for s in ["left", "bottom"]: ax.spines[s].set_color(GRID)
    ax.tick_params(colors=MUTED, labelsize=8)
    ax.set_axisbelow(True)


def line(ax, x, y, i, label):
    ax.plot(x, y, color=SLOTS[i], marker=MARKERS[i], markersize=8, linewidth=2, label=label,
            markeredgecolor="white", markeredgewidth=1.5)
    ax.annotate(label, (x[-1], y[-1]), xytext=(6, 0), textcoords="offset points", fontsize=8, color=INK, va="center")


def save(fig, task, name):
    out = ROOT / task / "graphs"
    out.mkdir(exist_ok=True)
    fig.tight_layout()
    fig.savefig(out / name, dpi=160)
    plt.close(fig)
    print("wrote", out / name)


# Task 1 - dispatch time per event vs k buttons
d = rows(ROOT / "Task01/data/t1_dispatch.csv")
k = [int(r["k"]) for r in d]
fig, ax = plt.subplots(figsize=(7.2, 3.8))
setup(ax, "Task 1 - time to dispatch one event, k buttons", "number of buttons k", "ns per event")
line(ax, k, [float(r["chain_ns"]) for r in d], 0, "one listener, if-else chain")
line(ax, k, [float(r["direct_ns"]) for r in d], 1, "own listener per button")
ax.legend(fontsize=8, frameon=False, loc="upper left")
ax.set_xlim(0, 125)
save(fig, "Task01", "t1_dispatch.png")

# Task 2 - time per department change
d = rows(ROOT / "Task02/data/t2_lookup.csv")
m = [int(r["doctors"]) for r in d]
fig, ax = plt.subplots(figsize=(7.2, 3.8))
setup(ax, "Task 2 - time per department change", "doctors m (log scale)", "microseconds (log scale)", logx=True, logy=True)
line(ax, m, [float(r["map_us"]) for r in d], 0, "Map loaded once")
line(ax, m, [float(r["reread_us"]) for r in d], 1, "fetch on every change")
line(ax, m, [float(r["reread_plus_rtt_us"]) for r in d], 2, "fetch + 2 ms DB round trip")
ax.legend(fontsize=8, frameon=False, loc="center left")
ax.set_xlim(8, 40000)
save(fig, "Task02", "t2_lookup.png")

# Task 4 - repaint time vs n clicks
d = rows(ROOT / "Task04/data/t4_repaint.csv")
by_n = {}
for r in d:
    by_n.setdefault(int(r["n"]), []).append((float(r["points_ms"]), float(r["image_ms"])))
n = sorted(by_n)
med = lambda v: sorted(v)[len(v) // 2]
fig, ax = plt.subplots(figsize=(7.2, 3.8))
setup(ax, "Task 4 - time per repaint after n clicks", "clicks n (log scale)", "ms per repaint (log scale)", logx=True, logy=True)
line(ax, n, [med([a for a, _ in by_n[x]]) for x in n], 0, "redraw stored points")
line(ax, n, [med([b for _, b in by_n[x]]) for x in n], 1, "copy BufferedImage")
ax.legend(fontsize=8, frameon=False, loc="upper left")
ax.set_xlim(5, 400000)
save(fig, "Task04", "t4_repaint.png")

# Task 6 - all at once vs paged (two panels, one measure each, no dual axis)
d = rows(ROOT / "Task06/data/t6_paging_network.csv")
pick = {r["approach"]: r for r in d}
labels = ["all at once\non the EDT", "all at once\nin SwingWorker", "paged\nOFFSET 0", "paged\nOFFSET 99,980"]
keys = ["all at once on the EDT", "all at once in a SwingWorker", "paged LIMIT 20 OFFSET 0 (SwingWorker)", "paged LIMIT 20 OFFSET 99980"]
fig, axes = plt.subplots(1, 2, figsize=(9.2, 3.8))
load = [float(pick[k]["load_ms"]) for k in keys]
lag = [float(pick[k]["edt_max_delay_ms"]) for k in keys[:3]]
setup(axes[0], "Loading time (100,000 rows)", "", "ms (log scale)", logy=True)
bars = axes[0].bar(labels, load, color=[SLOTS[1], SLOTS[0], SLOTS[2], SLOTS[2]], width=0.6, edgecolor="white", linewidth=2)
for b, v in zip(bars, load): axes[0].annotate(f"{v:.1f}", (b.get_x() + b.get_width() / 2, v), xytext=(0, 3), textcoords="offset points", ha="center", fontsize=8, color=INK)
setup(axes[1], "Longest event-thread delay while loading", "", "ms (log scale)", logy=True)
bars = axes[1].bar(labels[:3], lag, color=[SLOTS[1], SLOTS[0], SLOTS[2]], width=0.6, edgecolor="white", linewidth=2)
for b, v in zip(bars, lag): axes[1].annotate(f"{v:.1f}", (b.get_x() + b.get_width() / 2, v), xytext=(0, 3), textcoords="offset points", ha="center", fontsize=8, color=INK)
for ax in axes: ax.tick_params(axis="x", labelsize=7.5)
save(fig, "Task06", "t6_paging.png")
