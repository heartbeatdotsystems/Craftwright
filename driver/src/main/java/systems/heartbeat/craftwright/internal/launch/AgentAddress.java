package systems.heartbeat.craftwright.internal.launch;

/**
 * How the agent inside the game finds its test.
 *
 * @param port  the driver's port on localhost
 * @param token shared secret, so nothing else on the machine can pose as a client
 */
public record AgentAddress(int port, String token) {
}
