package ru.terra.tproxy.proxy;


public class Updater {
	private static Updater instance = new Updater();
	private UpdateNotifier notifier;

	private Updater() {
	}

	public static Updater getUpdater() {
		return instance;
	}

	public void setNotifier(UpdateNotifier notifier) {
		this.notifier = notifier;
	}

	public synchronized void update(String url, Long size, String cache) {
		if (notifier != null)
			notifier.start(url, size, cache);
	}
}
