package de.crazydev22.scoreprefix;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LuckPermsAPI {

    @NotNull
    public static LuckPerms getAPI() {
        return LuckPermsProvider.get();
    }

    public static Group getGroup(final Player player) {
        return getAPI().getGroupManager().getGroup(getUser(player).getPrimaryGroup());
    }

    public static User getUser(final Player player) {
        return getAPI().getPlayerAdapter(Player.class).getUser(player);
    }
}
