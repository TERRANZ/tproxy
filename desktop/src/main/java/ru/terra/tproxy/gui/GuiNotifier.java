package ru.terra.tproxy.gui;

import javafx.application.Platform;

/**
 * Date: 09.04.15
 * Time: 12:40
 */
public class GuiNotifier {
    public static MainController mainController;

    public static void msg(String msg) {
        Platform.runLater(() -> mainController.lvLog.getItems().add(msg));
    }
}
