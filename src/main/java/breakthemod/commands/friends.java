/*
 * This file is part of BreakTheMod.
 *
 * BreakTheMod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreakTheMod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreakTheMod. If not, see <https://www.gnu.org/licenses/>.
 */
package breakthemod.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import breakthemod.utils.*;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import java.util.*;

public class friends extends Command {

    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> command = LiteralArgumentBuilder
                .<FabricClientCommandSource>literal("onlinefriends")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();

                    if (client.player == null) {
                        LOGGER.error("Player instance is null, cannot send feedback.");
                        return 0;
                    }

                    CompletableFuture.runAsync(() -> {
                        try {
                            fetch Fetch = new fetch();

                            // Fetch and parse the JSON
                            JsonObject payload = new JsonObject();
                            JsonArray query = new JsonArray();
                            query.add(client.player.getUuid().toString());
                            payload.add("query", query);
                            JsonObject template = new JsonObject();
                            template.addProperty("friends",true);
                            payload.addProperty("template", template.toString());
                            String jsonResponse = Fetch.Fetch("https://api.earthmc.net/v3/aurora/players", payload.toString());

                            JsonArray user = JsonParser.parseString(jsonResponse).getAsJsonArray();
                            JsonObject Friends = user.get(0).getAsJsonObject();
                            // Ensure the user array is not empty
                            if (user.size() == 0) {
                                LOGGER.warn("User array is empty, no data available.");
                                client.execute(() -> sendMessage(client, Text.literal("No friends data available").setStyle(Style.EMPTY.withColor(Formatting.DARK_RED))));
                                return;
                            }
                    
                            List<String> friends = new ArrayList<>();
                    
                            for (JsonElement friend : Friends.get("friends").getAsJsonArray()) {
                                JsonObject Friend = friend.getAsJsonObject();
                                Collection<PlayerListEntry> players = client.getNetworkHandler().getPlayerList();
                                for (PlayerListEntry player : players) {
                                    if (player.getProfile().getName().equals(Friend.get("name").getAsString())) {
                                        friends.add(player.getProfile().getName());
                                    }
                                }
                            }
                    
                            client.execute(() -> {
                                if (friends.isEmpty()) {
                                    sendMessage(client, Text.literal("No friends online").setStyle(Style.EMPTY.withColor(Formatting.DARK_RED)));
                                } else {
                                    Text styledPart = Text.literal("Online Friends: ").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
                                    Text onlineStaffText = Text.literal(String.join(", ", friends))
                                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN));
                                    Text message = Text.literal("")
                                        .append(styledPart)
                                        .append(onlineStaffText)
                                        .append(" [" + friends.size() + "]");
                                    sendMessage(client, message);
                                }
                            });
                        } catch (Exception e) {
                            LOGGER.error("An error occurred while fetching friends: ", e);
                            client.execute(() -> sendMessage(client, Text.literal("An error occurred while fetching friends").setStyle(Style.EMPTY.withColor(Formatting.RED))));
                        }
                    });
            return 1;

            });
            dispatcher.register(command);  

        });            
                
    }

    private static void sendMessage(MinecraftClient client, Text message) {
        client.execute(() -> {
            if (client.player != null) {
                Text prefix = Prefix.getPrefix();
                Text chatMessage = Text.literal("").append(prefix).append(message);
                client.player.sendMessage(chatMessage, false);
            }
        });
    }
}
