package io.github.favasur.smoothterrain.util;

import org.joml.Quaternionf;

/** Loader-neutral copy of the Builder Tools rotation helper used by the mesh collision shape. */
public final class OffGridTransform {
    private OffGridTransform() {
    }

    public static Quaternionf rotation(float yawDeg, float pitchDeg) {
        return new Quaternionf()
                .rotateY((float) Math.toRadians(-yawDeg))
                .mul(new Quaternionf().rotateX((float) Math.toRadians(pitchDeg)));
    }
}
