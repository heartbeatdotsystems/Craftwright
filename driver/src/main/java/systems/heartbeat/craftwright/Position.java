package systems.heartbeat.craftwright;

/** A point in the world. */
public record Position(double x, double y, double z) {

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
}
