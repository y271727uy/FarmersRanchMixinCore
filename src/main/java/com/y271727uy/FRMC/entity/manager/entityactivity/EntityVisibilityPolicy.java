package com.y271727uy.FRMC.entity.manager.entityactivity;

import com.y271727uy.FRMC.network.EntityActivityNetwork;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/** Server-side, fail-open interpretation of optional client visibility reports. */
public final class EntityVisibilityPolicy {
    private EntityVisibilityPolicy() {
    }

    /**
     * Returns true only when every supplied player supports Chloride cooperation and explicitly
     * reports the entity as not visible. A missing capability or report keeps the entity active.
     */
    public static boolean isHiddenFromAll(Collection<ServerPlayer> players, int entityId) {
        if (players.isEmpty()) {
            return false;
        }
        for (ServerPlayer player : players) {
            EntityActivityNetwork.ClientCapabilities capabilities = EntityActivityNetwork.capabilities(player);
            if (capabilities == null || !capabilities.chlorideVisibility()
                    || !Boolean.FALSE.equals(capabilities.visibility().get(entityId))) {
                return false;
            }
        }
        return true;
    }
}
