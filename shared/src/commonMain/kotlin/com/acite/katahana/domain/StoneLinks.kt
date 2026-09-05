package com.acite.katahana.domain

/**
 * Geometric relations used by the Connections overlay.
 *
 * 立/长: orthogonal adjacent (Δ 1,0). Teaching texts split 立 (toward the edge)
 * and 长 (along the side); both are this geometry.
 * 尖: diagonal adjacent (Δ 1,1).
 * 飞: knight / 日字 (Δ 1,2), i.e. 小飞.
 * 跳: one-point jump on a line (Δ 2,0) with the interval empty.
 */
enum class LinkKind {
    Nobi,
    Kosumi,
    Keima,
    Tobi,
}

data class StoneLink(
    val a: Point,
    val b: Point,
    val color: StoneColor,
    val kind: LinkKind,
    val via: List<Point> = emptyList(),
    /**
     * 跳 only. Sign of rotate90(b−a)=(dy,−dx) for the arch:
     * +1 along that normal, −1 opposite, 0 none (isolated pair).
     * Chosen so the arch leans away from the same-color component centroid.
     */
    val lean: Int = 0,
)

fun stoneLinks(snapshot: SessionSnapshot): List<StoneLink> {
    val byColor = Array(2) { ArrayList<Point>() }
    for (y in 0 until snapshot.size) {
        for (x in 0 until snapshot.size) {
            val color = snapshot.stoneAt(x, y) ?: continue
            byColor[if (color == StoneColor.Black) 0 else 1] += Point(x, y)
        }
    }
    val out = ArrayList<StoneLink>()
    for ((index, stones) in byColor.withIndex()) {
        if (stones.size < 2) continue
        val color = if (index == 0) StoneColor.Black else StoneColor.White
        val set = stones.toHashSet()
        out += linksForColor(snapshot, color, set)
    }
    return out
}

private val ORTHO = arrayOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)
private val DIAG = arrayOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)
private val KEIMA = arrayOf(
    1 to 2, 2 to 1, -1 to 2, -2 to 1,
    1 to -2, 2 to -1, -1 to -2, -2 to -1,
)

private fun linksForColor(
    snapshot: SessionSnapshot,
    color: StoneColor,
    stones: Set<Point>,
): List<StoneLink> {
    val seen = HashSet<Long>()
    val links = ArrayList<StoneLink>()

    fun add(a: Point, b: Point, kind: LinkKind, via: List<Point> = emptyList()) {
        val key = pairKey(a, b)
        if (!seen.add(key)) return
        links += StoneLink(a, b, color, kind, via)
    }

    for (p in stones) {
        for ((dx, dy) in ORTHO) {
            val hit = firstFriend(snapshot, color, p, dx, dy, maxSteps = 2) ?: continue
            val dist = kotlin.math.abs(hit.x - p.x) + kotlin.math.abs(hit.y - p.y)
            if (dist == 1) {
                add(p, hit, LinkKind.Nobi)
            } else {
                val mid = Point((p.x + hit.x) / 2, (p.y + hit.y) / 2)
                add(p, hit, LinkKind.Tobi, listOf(mid))
            }
        }
        for ((dx, dy) in DIAG) {
            val hit = firstFriend(snapshot, color, p, dx, dy, maxSteps = 1) ?: continue
            add(p, hit, LinkKind.Kosumi)
        }
        for ((dx, dy) in KEIMA) {
            val q = Point(p.x + dx, p.y + dy)
            if (q !in stones) continue
            if (keimaBlocked(p, q, stones)) continue
            val elbowA = Point(p.x, q.y)
            val elbowB = Point(q.x, p.y)
            val via = listOf(elbowA, elbowB).filter { snapshot.at(it) == null }
            add(p, q, LinkKind.Keima, via.ifEmpty { listOf(elbowA) })
        }
    }

    return orientLinks(keepTightest(links, stones))
}

/**
 * 跳 arches and 飞 elbows lean **away** from the component centroid so a
 * same-kind ring blooms outward instead of collapsing on two sides.
 */
private fun orientLinks(links: List<StoneLink>): List<StoneLink> {
    if (links.isEmpty()) return links
    val points = HashSet<Point>()
    for (link in links) {
        points += link.a
        points += link.b
    }
    val uf = PointUf(points)
    for (link in links) uf.union(link.a, link.b)

    val stonesByRoot = HashMap<Point, ArrayList<Point>>()
    val seen = HashSet<Point>()
    for (p in points) {
        if (!seen.add(p)) continue
        stonesByRoot.getOrPut(uf.find(p)) { ArrayList() }.add(p)
    }
    val centroid = HashMap<Point, Pair<Float, Float>>(stonesByRoot.size * 2)
    for ((root, pts) in stonesByRoot) {
        var sx = 0f
        var sy = 0f
        for (p in pts) {
            sx += p.x
            sy += p.y
        }
        val n = pts.size.toFloat()
        centroid[root] = sx / n to sy / n
    }

    val tobiMids = HashSet<Point>()
    for (link in links) {
        if (link.kind == LinkKind.Tobi) {
            link.via.firstOrNull()?.let { tobiMids.add(it) }
        }
    }

    return links.map { link ->
        val (cx, cy) = centroid[uf.find(link.a)] ?: return@map link
        when (link.kind) {
            LinkKind.Tobi -> {
                val mid = link.via.firstOrNull()
                    ?: Point((link.a.x + link.b.x) / 2, (link.a.y + link.b.y) / 2)
                link.copy(lean = tobiLean(link.a, link.b, mid, cx, cy))
            }
            LinkKind.Keima -> {
                if (link.via.size <= 1) link
                else link.copy(via = listOf(pickKeimaElbow(link.via, cx, cy, tobiMids)))
            }
            else -> link
        }
    }
}

/** +1 = along (dy, −dx), −1 = opposite, 0 = collinear with the centroid. */
internal fun tobiLean(a: Point, b: Point, mid: Point, cx: Float, cy: Float): Int {
    val dx = (b.x - a.x).toFloat()
    val dy = (b.y - a.y).toFloat()
    val dot = dy * (cx - mid.x) - dx * (cy - mid.y)
    return when {
        dot > 1e-3f -> -1
        dot < -1e-3f -> 1
        else -> 0
    }
}

internal fun pickKeimaElbow(
    elbows: List<Point>,
    cx: Float,
    cy: Float,
    avoid: Set<Point>,
): Point {
    var best = elbows.first()
    var bestDist = dist2(best, cx, cy)
    var bestFree = if (best in avoid) 0 else 1
    for (i in 1 until elbows.size) {
        val e = elbows[i]
        val d = dist2(e, cx, cy)
        val free = if (e in avoid) 0 else 1
        if (d > bestDist + 1e-4f || (kotlin.math.abs(d - bestDist) <= 1e-4f && free > bestFree)) {
            best = e
            bestDist = d
            bestFree = free
        }
    }
    return best
}

private fun dist2(p: Point, cx: Float, cy: Float): Float {
    val dx = p.x - cx
    val dy = p.y - cy
    return dx * dx + dy * dy
}

/**
 * Layer by tightness: 立, then 尖, then 跳, then 飞.
 * Drop a link only if the two stones are already joined by **strictly tighter**
 * kinds. Same-kind cycles stay (A1 A3 C1 C3 keeps four 跳; A1 A2 B1 B2 keeps
 * four 立 but drops the two 尖 chords).
 */
private fun keepTightest(links: List<StoneLink>, stones: Set<Point>): List<StoneLink> {
    val uf = PointUf(stones)
    val kept = ArrayList<StoneLink>(links.size)
    val order = arrayOf(LinkKind.Nobi, LinkKind.Kosumi, LinkKind.Tobi, LinkKind.Keima)
    for (kind in order) {
        val layer = ArrayList<StoneLink>()
        for (link in links) {
            if (link.kind != kind) continue
            if (uf.connected(link.a, link.b)) continue
            layer += link
        }
        kept += layer
        for (link in layer) uf.union(link.a, link.b)
    }
    return kept
}

private class PointUf(points: Collection<Point>) {
    private val parent = HashMap<Point, Point>(points.size * 2)

    init {
        for (p in points) parent[p] = p
    }

    fun find(p: Point): Point {
        val par = parent[p] ?: p
        if (par == p) return p
        val root = find(par)
        parent[p] = root
        return root
    }

    fun union(a: Point, b: Point) {
        val ra = find(a)
        val rb = find(b)
        if (ra != rb) parent[ra] = rb
    }

    fun connected(a: Point, b: Point): Boolean = find(a) == find(b)
}

private fun firstFriend(
    snapshot: SessionSnapshot,
    color: StoneColor,
    from: Point,
    dx: Int,
    dy: Int,
    maxSteps: Int,
): Point? {
    for (step in 1..maxSteps) {
        val q = Point(from.x + dx * step, from.y + dy * step)
        if (!q.inBounds(snapshot.size)) return null
        val occupant = snapshot.at(q)
        if (occupant == color) return q
        if (occupant != null) return null
    }
    return null
}

/** A 飞 is dropped if a friend already sits in the 2×3 rectangle (tighter 立/尖). */
private fun keimaBlocked(a: Point, b: Point, stones: Set<Point>): Boolean {
    val x0 = minOf(a.x, b.x)
    val x1 = maxOf(a.x, b.x)
    val y0 = minOf(a.y, b.y)
    val y1 = maxOf(a.y, b.y)
    for (y in y0..y1) {
        for (x in x0..x1) {
            val p = Point(x, y)
            if (p == a || p == b) continue
            if (p in stones) return true
        }
    }
    return false
}

private fun SessionSnapshot.at(p: Point): StoneColor? =
    if (p.inBounds(size)) stoneAt(p.x, p.y) else null

private fun pairKey(a: Point, b: Point): Long {
    val (p, q) = if (a.x < b.x || (a.x == b.x && a.y <= b.y)) a to b else b to a
    return (p.x.toLong() shl 48) or (p.y.toLong() shl 32) or (q.x.toLong() shl 16) or q.y.toLong()
}
