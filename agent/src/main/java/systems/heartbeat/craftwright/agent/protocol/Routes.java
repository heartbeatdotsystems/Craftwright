package systems.heartbeat.craftwright.agent.protocol;

/** Where a feature announces the protocol methods it answers. */
public interface Routes {
	void on(String method, Handler handler);

	void onDeferred(String method, DeferredHandler handler);
}
