package systems.heartbeat.craftwright.agent.control;

/** Which movement keys are held. Immutable, so a walk can add to what the test holds without changing it. */
public record Keys(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
	public static final Keys NONE = new Keys(false, false, false, false, false, false, false);

	public boolean any() {
		return forward || back || left || right || jump || sneak || sprint;
	}

	public Keys withForward() {
		return new Keys(true, back, left, right, jump, sneak, sprint);
	}

	public Keys withJump() {
		return new Keys(forward, back, left, right, true, sneak, sprint);
	}
}
