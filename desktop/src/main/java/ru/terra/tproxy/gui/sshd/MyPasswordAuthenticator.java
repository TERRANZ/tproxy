package ru.terra.tproxy.gui.sshd;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.LoggerFactory;

/**
 * Date: 09.04.15
 * Time: 14:08
 */
public class MyPasswordAuthenticator implements PasswordAuthenticator {
    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        LoggerFactory.getLogger(this.getClass()).info("Requesting auth with " + username + " : " + password);
        return true;
    }
}
