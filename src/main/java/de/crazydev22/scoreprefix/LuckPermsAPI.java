package de.crazydev22.scoreprefix;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

	public static int getWeight(@NotNull Player player) {
		return Integer.MAX_VALUE - LuckPermsAPI.getGroup(player).getWeight().orElse(0);
	}

	@NotNull
	public static String getPrefix(@NotNull Player player) {
		String prefix = LuckPermsAPI.getUser(player).getCachedData().getMetaData().getPrefix();
		var group = LuckPermsAPI.getGroup(player);
		if (prefix == null && group != null)
			prefix = group.getCachedData().getMetaData().getPrefix();
		prefix = ScorePrefix.process(player, prefix);
		return prefix != null ? prefix : "";
	}

	@NotNull
	public static String getSuffix(@NotNull Player player) {
		String suffix = LuckPermsAPI.getUser(player).getCachedData().getMetaData().getSuffix();
		var group = LuckPermsAPI.getGroup(player);
		if (suffix == null && group != null)
			suffix = group.getCachedData().getMetaData().getSuffix();
		suffix = ScorePrefix.process(player, suffix);
		return suffix != null ? suffix : "";
	}
}
