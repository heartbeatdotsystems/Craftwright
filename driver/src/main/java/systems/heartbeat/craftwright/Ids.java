package systems.heartbeat.craftwright;

/** Registry ids as tests write them: {@code diamond} means {@code minecraft:diamond}. */
final class Ids {
	private static final String DEFAULT_NAMESPACE = "minecraft:";

	private Ids() {
	}

	static String qualify(String id) {
		return id.contains(":") ? id : DEFAULT_NAMESPACE + id;
	}

	static boolean matches(String actual, String expected) {
		return actual.equals(qualify(expected));
	}
}
