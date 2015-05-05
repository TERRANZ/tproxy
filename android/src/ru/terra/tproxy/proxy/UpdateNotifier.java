package ru.terra.tproxy.proxy;

public interface UpdateNotifier {
	public void start(String url, Long size, String cache);
}
