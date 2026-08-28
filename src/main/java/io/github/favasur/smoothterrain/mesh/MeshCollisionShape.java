package io.github.favasur.smoothterrain.mesh;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import io.github.favasur.smoothterrain.util.Face;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link VoxelShape} backed by an actual triangle mesh instead of axis-aligned boxes.
 *
 * <p>Minecraft's entity movement collides against {@code VoxelShape}s by calling
 * {@link #collide(Direction.Axis, AABB, double)} once per axis with the entity's current bounding
 * box and the remaining motion along that axis. The base implementation sweeps the box through the
 * shape's discrete box cells; this implementation sweeps the box against the mesh's triangles
 * instead, so the player stops exactly at the visible (possibly sloped, possibly rotated) surface -
 * the collision is the mesh, not a voxelized or boxed approximation of it.
 *
 * <p>The sweep is exact per axis: projected onto the two axes perpendicular to the motion, the
 * box is a static rectangle R and each triangle projects to a triangle T. The first contact happens
 * when the box front face reaches the extreme of the triangle's coordinate along the motion axis
 * over the (convex) overlap polygon R&cap;T. Because that coordinate is affine over the triangle,
 * its min/max over the polygon is attained at a polygon vertex, which we enumerate exactly
 * (rectangle corners inside the triangle, triangle vertices inside the rectangle, and
 * edge/edge crossings). No sampling, no stepping.
 *
 * <p>The shape is only ever consumed by the movement funnel (it is injected into
 * {@code CollisionGetter#getBlockCollisions}), so the discrete-shape machinery behind it is an
 * empty 1x1x1 grid and every other {@code VoxelShape} method is answered from the triangle list.
 */
public final class MeshCollisionShape extends VoxelShape {

	private static final Logger LOG = LogManager.getLogger("SmoothTerrain");
	/** Throttles the per-tick debug logs so a slow slope doesn't flood the log every frame. */
	private static long lastStepDebugLog = 0L;

	/** A world-space triangle. */
	public static final class Tri {
		public final double ax, ay, az;
		public final double bx, by, bz;
		public final double cx, cy, cz;

		public Tri(double ax, double ay, double az, double bx, double by, double bz, double cx, double cy, double cz) {
			this.ax = ax;
			this.ay = ay;
			this.az = az;
			this.bx = bx;
			this.by = by;
			this.bz = bz;
			this.cx = cx;
			this.cy = cy;
			this.cz = cz;
		}

		double min(int axis) {
			return Math.min(axis == 0 ? ax : axis == 1 ? ay : az,
				Math.min(axis == 0 ? bx : axis == 1 ? by : bz, axis == 0 ? cx : axis == 1 ? cy : cz));
		}

		double max(int axis) {
			return Math.max(axis == 0 ? ax : axis == 1 ? ay : az,
				Math.max(axis == 0 ? bx : axis == 1 ? by : bz, axis == 0 ? cx : axis == 1 ? cy : cz));
		}

		double coord(int axis, int vertex) {
			switch (vertex) {
				case 0: return axis == 0 ? ax : axis == 1 ? ay : az;
				case 1: return axis == 0 ? bx : axis == 1 ? by : bz;
				default: return axis == 0 ? cx : axis == 1 ? cy : cz;
			}
		}

		public Tri translate(double x, double y, double z) {
			return new Tri(ax + x, ay + y, az + z, bx + x, by + y, bz + z, cx + x, cy + y, cz + z);
		}

		/**
		 * The fraction of {@code motion} (0..1) the box may move along {@code axis} before first
		 * touching this triangle, or 1.0 when it does not hit within this tick. The box is static
		 * on the other two axes {@code u}/{@code v} spanning {@code [rMin,rMax]x[sMin,sMax]}.
		 */
		double sweptFraction(int axis, int u, int v,
							 double bMin, double bMax, double rMin, double rMax, double sMin, double sMax,
							 double motion, boolean pos) {
			double tMin = min(axis);
			double tMax = max(axis);
			final double eps = 1.0E-9;
			// Entirely behind, moving away: no hit.
			if (pos ? tMax < bMin - eps : tMin > bMax + eps) {
				return 1.0;
			}
			double p0u = coord(u, 0), p0v = coord(v, 0);
			double p1u = coord(u, 1), p1v = coord(v, 1);
			double p2u = coord(u, 2), p2v = coord(v, 2);

			// Enumerate the vertices of P = rectangle ∩ triangle (2D on u,v).
			double[][] pts = new double[19][2];
			int n = 0;
			// Rectangle corners inside the triangle.
			if (inTriangle(rMin, sMin, p0u, p0v, p1u, p1v, p2u, p2v)) { pts[n][0] = rMin; pts[n][1] = sMin; n++; }
			if (inTriangle(rMax, sMin, p0u, p0v, p1u, p1v, p2u, p2v)) { pts[n][0] = rMax; pts[n][1] = sMin; n++; }
			if (inTriangle(rMax, sMax, p0u, p0v, p1u, p1v, p2u, p2v)) { pts[n][0] = rMax; pts[n][1] = sMax; n++; }
			if (inTriangle(rMin, sMax, p0u, p0v, p1u, p1v, p2u, p2v)) { pts[n][0] = rMin; pts[n][1] = sMax; n++; }
			// Triangle vertices inside the rectangle.
			if (inRect(p0u, p0v, rMin, rMax, sMin, sMax)) { pts[n][0] = p0u; pts[n][1] = p0v; n++; }
			if (inRect(p1u, p1v, rMin, rMax, sMin, sMax)) { pts[n][0] = p1u; pts[n][1] = p1v; n++; }
			if (inRect(p2u, p2v, rMin, rMax, sMin, sMax)) { pts[n][0] = p2u; pts[n][1] = p2v; n++; }
			// Rectangle edge x triangle edge crossings (the rectangle edges are axis-aligned).
			for (int e = 0; e < 3; e++) {
				double x1 = e == 0 ? p0u : e == 1 ? p1u : p2u;
				double y1 = e == 0 ? p0v : e == 1 ? p1v : p2v;
				double x2 = e == 0 ? p1u : e == 1 ? p2u : p0u;
				double y2 = e == 0 ? p1v : e == 1 ? p2v : p0v;
				if (segSeg(rMin, sMin, rMax, sMin, x1, y1, x2, y2, pts[n])) n++;
				if (segSeg(rMax, sMin, rMax, sMax, x1, y1, x2, y2, pts[n])) n++;
				if (segSeg(rMax, sMax, rMin, sMax, x1, y1, x2, y2, pts[n])) n++;
				if (segSeg(rMin, sMax, rMin, sMin, x1, y1, x2, y2, pts[n])) n++;
			}
			if (n == 0 || !hasArea(pts, n)) {
				// The rectangle and the triangle's silhouette do not overlap with positive area:
				// touching along an edge or at a point (e.g. a box resting on a surface sweeping
				// sideways) is not a collision on this axis.
				return 1.0;
			}

			// The triangle's coordinate along the motion axis is affine over the triangle:
			// a(u,v) = p0a - (n_u*(u-p0u) + n_v*(v-p0v)) / n_a  from the plane equation.
			double p0a = coord(axis, 0);
			double e1x = coord(0, 1) - coord(0, 0), e1y = coord(1, 1) - coord(1, 0), e1z = coord(2, 1) - coord(2, 0);
			double e2x = coord(0, 2) - coord(0, 0), e2y = coord(1, 2) - coord(1, 0), e2z = coord(2, 2) - coord(2, 0);
			double nx = e1y * e2z - e1z * e2y;
			double ny = e1z * e2x - e1x * e2z;
			double nz = e1x * e2y - e1y * e2x;
			if (nx * nx + ny * ny + nz * nz < 1.0E-16) {
				return 1.0; // degenerate triangle
			}
			double na = axis == 0 ? nx : axis == 1 ? ny : nz;
			double nu = axis == 0 ? ny : axis == 1 ? nz : nx;
			double nv = axis == 0 ? nz : axis == 1 ? nx : ny;
			double aMin = Double.POSITIVE_INFINITY;
			double aMax = Double.NEGATIVE_INFINITY;
			if (Math.abs(na) < 1.0E-12) {
				return 1.0;
			}
			for (int i = 0; i < n; i++) {
				double aVal = p0a - (nu * (pts[i][0] - p0u) + nv * (pts[i][1] - p0v)) / na;
				if (aVal < aMin) aMin = aVal;
				if (aVal > aMax) aMax = aVal;
			}
			double contact = pos ? (aMin - bMax) / motion : (aMax - bMin) / motion;
			if (contact >= 1.0) {
				return 1.0; // first contact is beyond this tick's motion
			}
			if (contact <= 0.0) {
				// The box already reaches the triangle's extreme along this axis (touching it or
				// embedded in it - both normal when resting on a surface, which the box straddles
				// on a slope). Decide by whether a tiny step in the motion direction GROWS the 3D
				// overlap with the triangle: entering or deepening is blocked, leaving (jumping off
				// a surface, escaping an embedded position, sliding along it) is allowed.
				boolean before = strictOverlap(bMin, bMax, aMin, aMax);
				double step = Math.copySign(Math.max(Math.abs(motion) * 1.0E-6, 1.0E-9), motion);
				boolean after = strictOverlap(bMin + step, bMax + step, aMin, aMax);
				if (before == after) {
					double len0 = overlapLength(bMin, bMax, aMin, aMax);
					double len1 = overlapLength(bMin + step, bMax + step, aMin, aMax);
					return len1 > len0 + 1.0E-12 ? 0.0 : 1.0;
				}
				return after ? 0.0 : 1.0;
			}
			return contact;
		}
	}

	/** Cell size of the collision broadphase grid, in blocks (power of two). */
	private static final int CELL_SHIFT = 2;
	private static final int CELL_SIZE = 1 << CELL_SHIFT;

	private final Tri[] triangles;
	private final AABB bounds;
	/**
	 * Distinct Y coordinates of the mesh's surface vertices, sorted ascending. Minecraft's
	 * automatic step-up (Entity.collide -> collectCandidateStepUpHeights) derives its candidate
	 * step heights from every collider's {@link #getCoords(Direction.Axis#Y)}: returning the
	 * surface heights lets the player walk up smooth slopes the way it walks up vanilla steps.
	 * The whole section bounds (the old behaviour) hid the surface heights inside a single
	 * min/max pair, so the step-up never engaged and slopes were unwalkable.
	 */
	private final DoubleList yCoords;
	/** The 4-block cell of the shape's bounds min corner; cell keys are stored relative to it so
	 *  they stay small (a shape spans at most a few cells) and never overflow the packed fields. */
	private final int baseCellX, baseCellY, baseCellZ;
	/** Cell index: packed cell coordinates to the triangles that overlap them. Colliding entities
	 *  only test the cells their swept box touches, instead of every triangle of every section
	 *  shape in the funnel. */
	private final Long2ObjectOpenHashMap<int[]> cells;

	public MeshCollisionShape(List<Tri> tris) {
		super(new BitSetDiscreteVoxelShape(1, 1, 1));
		this.triangles = tris.toArray(new Tri[0]);
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
		for (Tri t : triangles) {
			minX = Math.min(minX, t.min(0)); maxX = Math.max(maxX, t.max(0));
			minY = Math.min(minY, t.min(1)); maxY = Math.max(maxY, t.max(1));
			minZ = Math.min(minZ, t.min(2)); maxZ = Math.max(maxZ, t.max(2));
		}
		if (triangles.length == 0) {
			minX = minY = minZ = maxX = maxY = maxZ = 0;
		}
		// Build the auto-step candidate heights from the distinct surface Y values, coalesced to a
		// 1/16-block grid. Surface nets emit a vertex roughly every 1/60 of a block along a slope,
		// so uncoalesced the candidate set balloons (the measured run served 98 coords) and the
		// vanilla auto-step churns over near-identical micro-heights every horizontal collision,
		// which is what reads as stutter / slow ascent. Snapping to 1/16 collapses those
		// near-duplicates into a single representative height per surface level without removing
		// any real step the player can stand on.
		it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet ySet = new it.unimi.dsi.fastutil.doubles.DoubleOpenHashSet();
		for (Tri t : triangles) {
			// Round to the nearest 1/16 (0.0625) so same-level vertices share one candidate.
			ySet.add(java.lang.Math.round(t.ay * 16.0) / 16.0);
			ySet.add(java.lang.Math.round(t.by * 16.0) / 16.0);
			ySet.add(java.lang.Math.round(t.cy * 16.0) / 16.0);
		}
		double[] yArr = ySet.toDoubleArray();
		java.util.Arrays.sort(yArr);
		this.yCoords = DoubleArrayList.wrap(yArr);
		this.bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		if (LOG.isDebugEnabled()) {
			// Surface-step monitoring for the "walks slowly / stutters on slopes" bug: log how many
			// distinct surface Y coordinates are being handed to the vanilla auto-step, plus the
			// reachable band (tris count, height span). A very large / widely spread candidate set is
			// the prime suspect for the residual stutter.
			LOG.debug("MeshCollisionShape section: tris={} cellBounds={}..{} yCoords={} span={}..{}",
					triangles.length, minX, maxX, yArr.length,
					yArr.length == 0 ? Double.NaN : yArr[0],
					yArr.length == 0 ? Double.NaN : yArr[yArr.length - 1]);
		}

		this.baseCellX = (int) Math.floor(minX / CELL_SIZE);
		this.baseCellY = (int) Math.floor(minY / CELL_SIZE);
		this.baseCellZ = (int) Math.floor(minZ / CELL_SIZE);
		Long2ObjectOpenHashMap<IntArrayList> cellLists = new Long2ObjectOpenHashMap<>();
		for (int i = 0; i < triangles.length; i++) {
			Tri t = triangles[i];
			// Add the triangle to every 4-block cell its AABB overlaps (cell coords relative to
			// the shape's base cell, so the packed keys stay in a small signed range).
			int cx0 = (int) Math.floor(t.min(0) / CELL_SIZE) - baseCellX, cx1 = (int) Math.floor(t.max(0) / CELL_SIZE) - baseCellX;
			int cy0 = (int) Math.floor(t.min(1) / CELL_SIZE) - baseCellY, cy1 = (int) Math.floor(t.max(1) / CELL_SIZE) - baseCellY;
			int cz0 = (int) Math.floor(t.min(2) / CELL_SIZE) - baseCellZ, cz1 = (int) Math.floor(t.max(2) / CELL_SIZE) - baseCellZ;
			for (int cx = cx0; cx <= cx1; cx++) {
				for (int cy = cy0; cy <= cy1; cy++) {
					for (int cz = cz0; cz <= cz1; cz++) {
						cellLists.computeIfAbsent(packRel(cx, cy, cz), k -> new IntArrayList()).add(i);
					}
				}
			}
		}
		this.cells = new Long2ObjectOpenHashMap<>(cellLists.size());
		for (it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry<IntArrayList> entry : cellLists.long2ObjectEntrySet()) {
			cells.put(entry.getLongKey(), entry.getValue().toIntArray());
		}
	}

	/** World-space triangle mesh from a section's cached faces (relative to its area start). */
	public static MeshCollisionShape fromFaces(List<Face> faces, BlockPos areaStart) {
		List<Tri> tris = new ArrayList<>(faces.size() * 2);
		double ox = areaStart.getX(), oy = areaStart.getY(), oz = areaStart.getZ();
		for (Face f : faces) {
			tris.add(new Tri(
					f.v0.x + ox, f.v0.y + oy, f.v0.z + oz,
					f.v1.x + ox, f.v1.y + oy, f.v1.z + oz,
					f.v2.x + ox, f.v2.y + oy, f.v2.z + oz
			));
			tris.add(new Tri(
					f.v0.x + ox, f.v0.y + oy, f.v0.z + oz,
					f.v2.x + ox, f.v2.y + oy, f.v2.z + oz,
					f.v3.x + ox, f.v3.y + oy, f.v3.z + oz
			));
		}
		return new MeshCollisionShape(tris);
	}

	/**
	 * Builds a world-space triangle surface from a block-local voxel shape rotated around
	 * {@code (cx, cy, cz)}. This is the server-safe fallback for rotated blocks when a client baked
	 * model is unavailable. Each source box contributes its six outward faces; the normal movement
	 * path is therefore still a real triangle sweep rather than the old XYZ-grid voxel envelope.
	 */
	public static MeshCollisionShape fromVoxelShape(VoxelShape shape, double cx, double cy, double cz,
												float yaw, float pitch) {
		if (shape.isEmpty()) {
			return new MeshCollisionShape(List.of());
		}
		org.joml.Quaternionf rotation = io.github.favasur.smoothterrain.util.OffGridTransform.rotation(yaw, pitch);
		List<Tri> tris = new ArrayList<>();
		shape.forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
			double[][] p = new double[8][3];
			p[0] = point(rotation, cx, cy, cz, x0, y0, z0);
			p[1] = point(rotation, cx, cy, cz, x1, y0, z0);
			p[2] = point(rotation, cx, cy, cz, x1, y1, z0);
			p[3] = point(rotation, cx, cy, cz, x0, y1, z0);
			p[4] = point(rotation, cx, cy, cz, x0, y0, z1);
			p[5] = point(rotation, cx, cy, cz, x1, y0, z1);
			p[6] = point(rotation, cx, cy, cz, x1, y1, z1);
			p[7] = point(rotation, cx, cy, cz, x0, y1, z1);
			// The winding points out of the source box for west/east, down/up, north/south.
			addQuad(tris, p[0], p[4], p[7], p[3]); // west
			addQuad(tris, p[1], p[2], p[6], p[5]); // east
			addQuad(tris, p[0], p[1], p[5], p[4]); // down
			addQuad(tris, p[3], p[7], p[6], p[2]); // up
			addQuad(tris, p[0], p[3], p[2], p[1]); // north
			addQuad(tris, p[4], p[5], p[6], p[7]); // south
		});
		if (LOG.isDebugEnabled()) {
			// Rotated-block collision geometry monitor ("no physics" / "render offset N blocks"
			// bug): report the source shape bounds, the rotation center the mesh was generated
			// around, and the resulting world AABB so any centre-offset is visible in the log.
			double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
			double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
			for (Tri t : tris) {
				minX = Math.min(minX, t.min(0)); maxX = Math.max(maxX, t.max(0));
				minY = Math.min(minY, t.min(1)); maxY = Math.max(maxY, t.max(1));
				minZ = Math.min(minZ, t.min(2)); maxZ = Math.max(maxZ, t.max(2));
			}
			LOG.debug("Rotated block collision: srcShapeBounds={} center=({},{},{}) yaw={} pitch={} -> worldAABB=({},{},{})-({},{},{}) tris={}",
					shape.bounds(), cx, cy, cz, yaw, pitch,
					minX, minY, minZ, maxX, maxY, maxZ, tris.size());
		}
		return new MeshCollisionShape(tris);
	}

	private static double[] point(org.joml.Quaternionf rotation, double cx, double cy, double cz,
									  double x, double y, double z) {
		org.joml.Vector3f p = rotation.transform(
			new org.joml.Vector3f((float) (x - 0.5), (float) (y - 0.5), (float) (z - 0.5)),
			new org.joml.Vector3f());
		return new double[]{cx + p.x, cy + p.y, cz + p.z};
	}

	private static void addQuad(List<Tri> out, double[] p0, double[] p1, double[] p2, double[] p3) {
		out.add(new Tri(p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]));
		out.add(new Tri(p0[0], p0[1], p0[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2]));
	}

	// ------------------------------------------------------------------
	// VoxelShape overrides
	// ------------------------------------------------------------------

	/**
	 * The exact per-axis sweep against the mesh. Called by Minecraft's entity movement
	 * ({@code Entity.collideWithShapes}) once per axis with the entity's current bounding box.
	 */
	@Override
	public double collide(Direction.Axis axis, AABB box, double motion) {
		if (triangles.length == 0) {
			return motion;
		}
		if (Math.abs(motion) < 1.0E-7D) {
			return 0.0D;
		}
		int a = axis.ordinal();
		int u = (a + 1) % 3;
		int v = (a + 2) % 3;
		double bMin = coord(box, a, false), bMax = coord(box, a, true);
		double rMin = coord(box, u, false), rMax = coord(box, u, true);
		double sMin = coord(box, v, false), sMax = coord(box, v, true);
		boolean pos = motion > 0;
		double best = 1.0;
		// Fall-through probe: while falling down (Y, negative motion) note whether any terrain
		// triangle overlaps the swept vertical window but fails to stop the fall. If that happens
		// the collision floor is not watertight under the player (a hole / displaced-vertex gap).
		boolean ySinkCandidates = axis == Direction.Axis.Y && !pos;
		boolean yCandidateSeen = false;
		// Broadphase: only test triangles in the 4-block cells the swept box (box inflated by the
		// motion along this axis) touches. The cell coordinates must stay in WORLD X/Y/Z order;
		// the old implementation used the perpendicular axes as X/Z unconditionally, which made
		// horizontal (X/Z) movement miss most triangles.
		double sweptMin = pos ? bMin : bMin + motion;
		double sweptMax = pos ? bMax + motion : bMax;
		double xMin = coord(box, 0, false), xMax = coord(box, 0, true);
		double yMin = coord(box, 1, false), yMax = coord(box, 1, true);
		double zMin = coord(box, 2, false), zMax = coord(box, 2, true);
		if (a == 0) {
			xMin = sweptMin;
			xMax = sweptMax;
		} else if (a == 1) {
			yMin = sweptMin;
			yMax = sweptMax;
		} else {
			zMin = sweptMin;
			zMax = sweptMax;
		}
		int cx0 = (int) Math.floor(xMin / CELL_SIZE) - baseCellX, cx1 = (int) Math.floor(xMax / CELL_SIZE) - baseCellX;
		int cy0 = (int) Math.floor(yMin / CELL_SIZE) - baseCellY, cy1 = (int) Math.floor(yMax / CELL_SIZE) - baseCellY;
		int cz0 = (int) Math.floor(zMin / CELL_SIZE) - baseCellZ, cz1 = (int) Math.floor(zMax / CELL_SIZE) - baseCellZ;
		for (int cx = cx0; cx <= cx1; cx++) {
			for (int cy = cy0; cy <= cy1; cy++) {
				for (int cz = cz0; cz <= cz1; cz++) {
					int[] bucket = cells.get(packRel(cx, cy, cz));
					if (bucket == null) {
						continue;
					}
					for (int idx : bucket) {
						Tri t = triangles[idx];
						if (t.max(a) < sweptMin || t.min(a) > sweptMax) {
							continue;
						}
						// A triangle overlaps the fall window at all - if the player still passes through,
						// the surface has a gap. Mark that a floor triangle was in range this downward sweep.
						if (ySinkCandidates) {
							yCandidateSeen = true;
						}
						double f = t.sweptFraction(a, u, v, bMin, bMax, rMin, rMax, sMin, sMax, motion, pos);
						if (f < best) {
							best = f;
							if (best <= 0.0) {
								return 0.0;
							}
						}
					}
				}
			}
		}
		if (axis != Direction.Axis.Y && best < 1.0D) {
			// A horizontal collision consumed part of the motion - the very event that triggers the
			// vanilla auto-step. Log (throttled) the axis, how much motion survived, and the size of
			// the candidate step-height set, to correlate the stutter with step-up behaviour.
			long now = System.currentTimeMillis();
			if (now - lastStepDebugLog > 500L) {
				lastStepDebugLog = now;
				LOG.debug("Slope hit axis={} survived={} of {} (blocked ~{}) yCoords={}",
						axis, best * motion, motion, (1.0D - best) * motion, yCoords.size());
			}
		}
		if (yCandidateSeen && best >= 1.0D) {
			// Falling down through a window with terrain triangles in it but NO collision contact: the
			// collision floor under the player is incomplete (a gap / displaced-vertex hole).
			long now = System.currentTimeMillis();
			if (now - lastStepDebugLog > 500L) {
				lastStepDebugLog = now;
				LOG.debug("POSSIBLE FALL-THROUGH: falling Y with {} triangles in range but no contact (box={}, motion={})",
						triangles.length, box, motion);
			}
		}
		return best * motion;
	}

	/** Applies {@code consumer} to every world-space triangle in this shape. */
	public void forEachTriangle(java.util.function.Consumer<Tri> consumer) {
		for (Tri triangle : triangles) {
			consumer.accept(triangle);
		}
	}

	@Override
	public boolean isEmpty() {
		return triangles.length == 0;
	}

	@Override
	public AABB bounds() {
		return bounds;
	}

	@Override
	public DoubleList getCoords(Direction.Axis axis) {
		if (axis == Direction.Axis.Y) {
			// The surface heights (sorted) - consumed by the vanilla auto-step to pick how far to
			// lift the entity when walking into a slope.
			long now = System.currentTimeMillis();
			if (now - lastStepDebugLog > 500L) {
				lastStepDebugLog = now;
				if (LOG.isDebugEnabled()) {
					LOG.debug("Auto-step serving {} candidate Y coords (first={} last={}): {}",
							yCoords.size(),
							yCoords.isEmpty() ? "n/a" : Double.toString(yCoords.getDouble(0)),
							yCoords.isEmpty() ? "n/a" : Double.toString(yCoords.getDouble(yCoords.size() - 1)),
							yCoords);
				}
			}
			return yCoords;
		}
		double lo = axis == Direction.Axis.X ? bounds.minX : bounds.minZ;
		double hi = axis == Direction.Axis.X ? bounds.maxX : bounds.maxZ;
		return DoubleArrayList.wrap(new double[]{lo, hi});
	}

	@Override
	public VoxelShape move(double x, double y, double z) {
		if (x == 0.0 && y == 0.0 && z == 0.0) {
			return this;
		}
		List<Tri> moved = new ArrayList<>(triangles.length);
		for (Tri t : triangles) {
			moved.add(t.translate(x, y, z));
		}
		return new MeshCollisionShape(moved);
	}

	/**
	 * Emits every triangle edge so debug overlays (and any other {@code forAllEdges} consumer) can
	 * draw the exact collision surface instead of the empty discrete grid behind this shape.
	 */
	@Override
	public void forAllEdges(Shapes.DoubleLineConsumer consumer) {
		for (Tri t : triangles) {
			consumer.consume(t.ax, t.ay, t.az, t.bx, t.by, t.bz);
			consumer.consume(t.bx, t.by, t.bz, t.cx, t.cy, t.cz);
			consumer.consume(t.cx, t.cy, t.cz, t.ax, t.ay, t.az);
		}
	}

	/** Packs three cell coordinates (relative to the shape's base cell, each within +-2^20) into one
	 *  long key: 21 bits per axis, masked so negative values stay in their own field. */
	private static long packRel(int x, int y, int z) {
		return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFFL);
	}

	// ------------------------------------------------------------------
	// 2D geometry helpers
	// ------------------------------------------------------------------

	private static double coord(AABB box, int axis, boolean max) {
		switch (axis) {
			case 0: return max ? box.maxX : box.minX;
			case 1: return max ? box.maxY : box.minY;
			default: return max ? box.maxZ : box.minZ;
		}
	}

	/** True when the overlap polygon has positive area (its points are not all collinear). */
	private static boolean hasArea(double[][] pts, int n) {
		if (n < 3) {
			return false;
		}
		int i0 = 0, i1 = 1;
		double best = -1.0;
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				double dx = pts[i][0] - pts[j][0];
				double dy = pts[i][1] - pts[j][1];
				double d = dx * dx + dy * dy;
				if (d > best) {
					best = d;
					i0 = i;
					i1 = j;
				}
			}
		}
		if (best < 1.0E-18) {
			return false;
		}
		double ax = pts[i0][0], ay = pts[i0][1];
		double bx = pts[i1][0], by = pts[i1][1];
		for (int i = 0; i < n; i++) {
			if (i == i0 || i == i1) {
				continue;
			}
			double cross = (bx - ax) * (pts[i][1] - ay) - (by - ay) * (pts[i][0] - ax);
			if (Math.abs(cross) > 1.0E-9 * Math.sqrt(best)) {
				return true;
			}
		}
		return false;
	}

	private static boolean strictOverlap(double bMin, double bMax, double aMin, double aMax) {
		final double eps = 1.0E-9;
		return aMin < bMax - eps && aMax > bMin + eps;
	}

	private static double overlapLength(double bMin, double bMax, double aMin, double aMax) {
		return Math.max(0.0, Math.min(bMax, aMax) - Math.max(bMin, aMin));
	}

	private static boolean inRect(double x, double y, double rMin, double rMax, double sMin, double sMax) {
		final double eps = 1.0E-9;
		return x >= rMin - eps && x <= rMax + eps && y >= sMin - eps && y <= sMax + eps;
	}

	/** Inclusive point-in-triangle (boundary counts) via same-sign cross products. */
	private static boolean inTriangle(double px, double py,
									  double ax, double ay, double bx, double by, double cx, double cy) {
		double d1 = cross(px, py, ax, ay, bx, by);
		double d2 = cross(px, py, bx, by, cx, cy);
		double d3 = cross(px, py, cx, cy, ax, ay);
		boolean hasNeg = d1 < 0 || d2 < 0 || d3 < 0;
		boolean hasPos = d1 > 0 || d2 > 0 || d3 > 0;
		return !(hasNeg && hasPos);
	}

	private static double cross(double px, double py, double ax, double ay, double bx, double by) {
		return (bx - ax) * (py - ay) - (by - ay) * (px - ax);
	}

	/**
	 * Proper segment-segment intersection; writes the point to {@code out} and returns true.
	 * Collinear overlaps are deliberately not reported here - their endpoints are already captured
	 * by the inclusive corner/vertex tests in the caller (a collinear overlap's extremes are
	 * always either a rectangle corner on the triangle edge or a triangle vertex in the rectangle).
	 */
	private static boolean segSeg(double a1x, double a1y, double a2x, double a2y,
								  double b1x, double b1y, double b2x, double b2y,
								  double[] out) {
		double d = (a2x - a1x) * (b2y - b1y) - (a2y - a1y) * (b2x - b1x);
		if (Math.abs(d) < 1.0E-12) {
			return false; // parallel or collinear
		}
		double t = ((b1x - a1x) * (b2y - b1y) - (b1y - a1y) * (b2x - b1x)) / d;
		double u = ((b1x - a1x) * (a2y - a1y) - (b1y - a1y) * (a2x - a1x)) / d;
		if (t < -1.0E-9 || t > 1.0 + 1.0E-9 || u < -1.0E-9 || u > 1.0 + 1.0E-9) {
			return false;
		}
		out[0] = a1x + t * (a2x - a1x);
		out[1] = a1y + t * (a2y - a1y);
		return true;
	}
}
