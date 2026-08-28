package io.github.favasur.smoothterrain.client.render.struct;

import io.github.favasur.smoothterrain.util.PerformanceCriticalAllocation;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;

/**
 * 26.2 adaptation of the canonical texture-UV struct. The 26.2 {@link BakedQuad} is a record whose
 * per-vertex UVs are packed longs ({@code UVPair.pack}); the canonical 1.21.1 quad stored them as
 * floats inside an interleaved int array.
 */
@PerformanceCriticalAllocation
public final /* inline record */ class Texture {

	public static final Texture EVERYTHING = new Texture(0, 0, 1, 0, 1, 1, 0, 1);

	public /* final */ float u0;
	public /* final */ float v0;
	public /* final */ float u1;
	public /* final */ float v1;
	public /* final */ float u2;
	public /* final */ float v2;
	public /* final */ float u3;
	public /* final */ float v3;

	public Texture() {
		this(0, 0, 0, 0, 0, 0, 0, 0);
	}

	public Texture(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
		this.u0 = u0;
		this.v0 = v0;
		this.u1 = u1;
		this.v1 = v1;
		this.u2 = u2;
		this.v2 = v2;
		this.u3 = u3;
		this.v3 = v3;
	}

	public static Texture forQuadRearranged(Texture valhallaPls, BakedQuad quad, Direction faceDirection) {
		valhallaPls = forQuad(valhallaPls, quad);
		valhallaPls.rearrangeForDirection(faceDirection);
		return valhallaPls;
	}

	public static Texture forQuad(Texture valhallaPls, BakedQuad quad) {
		valhallaPls.unpackFromQuad(quad);
		return valhallaPls;
	}

	private void unpackFromQuad(BakedQuad quad) {
		u0 = UVPair.unpackU(quad.packedUV(0));
		v0 = UVPair.unpackV(quad.packedUV(0));
		u1 = UVPair.unpackU(quad.packedUV(1));
		v1 = UVPair.unpackV(quad.packedUV(1));
		u2 = UVPair.unpackU(quad.packedUV(2));
		v2 = UVPair.unpackV(quad.packedUV(2));
		u3 = UVPair.unpackU(quad.packedUV(3));
		v3 = UVPair.unpackV(quad.packedUV(3));
	}

	private void rearrangeForDirection(Direction direction) {
		switch (direction) {
			case NORTH:
			case EAST:
				break;
			case DOWN:
			case SOUTH:
			case WEST: {
				float u0 = this.u0;
				float v0 = this.v0;
				float u1 = this.u1;
				float v1 = this.v1;
				float u2 = this.u2;
				float v2 = this.v2;
				float u3 = this.u3;
				float v3 = this.v3;

				this.u0 = u3;
				this.v0 = v3;
				this.u1 = u0;
				this.v1 = v0;
				this.u2 = u1;
				this.v2 = v1;
				this.u3 = u2;
				this.v3 = v2;
				break;
			}
			case UP: {
				float u0 = this.u0;
				float v0 = this.v0;
				float u1 = this.u1;
				float v1 = this.v1;
				float u2 = this.u2;
				float v2 = this.v2;
				float u3 = this.u3;
				float v3 = this.v3;

				this.u0 = u2;
				this.v0 = v2;
				this.u1 = u3;
				this.v1 = v3;
				this.u2 = u0;
				this.v2 = v0;
				this.u3 = u1;
				this.v3 = v1;
				break;
			}
			default:
				throw new IllegalStateException("Unexpected value: " + direction);
		}
	}

}
