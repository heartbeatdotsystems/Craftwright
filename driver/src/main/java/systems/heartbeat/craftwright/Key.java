package systems.heartbeat.craftwright;

/** The movement keys a test can hold down, see {@link Player#hold(Key...)}. */
public enum Key {
	FORWARD("forward"), BACK("back"), LEFT("left"), RIGHT("right"), JUMP("jump"), SNEAK("sneak"), SPRINT("sprint");

	private final String wireName;

	Key(String wireName) {
		this.wireName = wireName;
	}

	String wireName() {
		return wireName;
	}
}
