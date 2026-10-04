
package mage.remote.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import mage.remote.MageRemoteException;
import mage.view.MatchView;
import mage.view.RoomUsersView;
import mage.view.TableView;
import mage.view.UserView;

/**
 * @author noxx
 */
public interface ServerState {

    UUID getMainRoomId();

    List<UserView> getUsers();

    Collection<RoomUsersView> getRoomUsers (UUID roomId) throws MageRemoteException;
    
    List<String> getServerMessages();

    Collection<TableView> getTables(UUID roomId) throws MageRemoteException;

    Collection<MatchView> getFinishedMatches(UUID roomId) throws MageRemoteException;

    String getVersionInfo();

    Boolean isServerReady();

    /**
     * Vuelve a pedir el estado al servidor y lo cachea solo si ya viene completo.
     * Hace falta porque el servidor acepta el login antes de terminar de cargar los tipos de
     * juego: si el cliente conecta en esa ventana, cachea un estado vacio y se queda sin
     * tipos de juego ni de jugador hasta que se vuelva a conectar.
     *
     * @return true si el estado se ha refrescado con datos completos
     */
    Boolean refreshServerState();

}
