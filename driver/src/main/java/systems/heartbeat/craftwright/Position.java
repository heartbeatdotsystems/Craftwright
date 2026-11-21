package systems.heartbeat.craftwright;

/** A point in the world. */
public record Position(double x, double y, double z) {
	/** Reads {@code x}, {@code y} and {@code z} of a wire object. */
	static Position of(com.google.gson.JsonObject object) {
		return new Position(object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble());
	}


	public double distanceTo(Position other) {
		double dx = x - other.x;
		double dy = y - other.y;
		double dz = z - other.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	/** Distance ignoring height. */
	public double horizontalDistanceTo(double otherX, double otherZ) {
		double dx = x - otherX;
		double dz = z - otherZ;
		return Math.sqrt(dx * dx + dz * dz);
	}

	public int blockX() {
		return (int) Math.floor(x);
	}

	public int blockY() {
		return (int) Math.floor(y);
	}

	public int blockZ() {
		return (int) Math.floor(z);
	}

	@Override
	public String toString() {
		return String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", x, y, z);
	}
}
