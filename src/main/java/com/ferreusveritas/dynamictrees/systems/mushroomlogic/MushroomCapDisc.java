package com.ferreusveritas.dynamictrees.systems.mushroomlogic;

import com.ferreusveritas.dynamictrees.util.SimpleBitmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MushroomCapDisc {

	public static final int MIN_RADIUS = 1;
	public static final int MAX_RADIUS = 8;

	private static final SimpleBitmap[] CIRCLES = new SimpleBitmap[MAX_RADIUS + 1];
	private static final SimpleBitmap[] INTERIORS = new SimpleBitmap[MAX_RADIUS + 1];
	private static final List<List<Vec2i>> PRECOMPUTED_RINGS = new ArrayList<>();

	static {
		int[] circleData = {0x0, 0x48, 0x488, 0x3690, 0x248D1, 0x16D919, 0xDB5B19, 0x7FF6B19};
		for (int radius = MIN_RADIUS; radius <= MAX_RADIUS; radius++) {
			SimpleBitmap whole = circleBitmapGen(radius, circleData[radius - MIN_RADIUS]);
			SimpleBitmap inside = new SimpleBitmap(whole.getW(), whole.getH());

			for (int z = 0; z < inside.getH(); z++) {
				for (int x = 0; x < inside.getW(); x++) {
					boolean in = radius == 1 ? x == 1 && z == 1 : CIRCLES[radius - 1].isPixelOn(x - 1, z - 1);
					inside.setPixel(x, z, in ? 1 : 0);
				}
			}

			CIRCLES[radius] = whole;
			INTERIORS[radius] = inside;
		}
		CIRCLES[0] = CIRCLES[MIN_RADIUS];
		INTERIORS[0] = INTERIORS[MIN_RADIUS];

		PRECOMPUTED_RINGS.add(Collections.emptyList());

		for (int radius = MIN_RADIUS; radius <= MAX_RADIUS; radius++) {
			List<Vec2i> ring = new ArrayList<>();
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (isEdge(x, z, radius)) {
						ring.add(new Vec2i(x, z));
					}
				}
			}
			PRECOMPUTED_RINGS.add(Collections.unmodifiableList(ring));
		}
	}

	private static SimpleBitmap circleBitmapGen(int radius, int points) {
		int dim = radius * 2 + 1;
		int top = 0;
		int bot = dim - 1;
		int[] lines = new int[dim];

		while (top <= bot) {
			int slice = ((points >> (top * 3)) & 0x7) + 1;
			lines[top++] = lines[bot--] = bitRun(radius - slice, radius + 1 + slice);
		}

		return new SimpleBitmap(dim, dim, lines);
	}

	private static int bitRun(int start, int stop) {
		if (start < stop) {
			return (int) ((0xFFFFFFFFL >>> (32 - stop)) & (0xFFFFFFFFL << start));
		}
		return (int) ((0xFFFFFFFFL >>> (32 - stop)) | (0xFFFFFFFFL << start));
	}

	private static boolean isEdge(int x, int z, int radius) {
		int px = x + radius;
		int pz = z + radius;
		return CIRCLES[radius].isPixelOn(px, pz) && !INTERIORS[radius].isPixelOn(px, pz);
	}

	public static List<Vec2i> getPrecomputedRing(int radius) {
		return PRECOMPUTED_RINGS.get(Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, radius)));
	}

}
