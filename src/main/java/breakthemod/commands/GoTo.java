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
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import breakthemod.utils.Prefix;
import breakthemod.utils.fetch;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.StringArgumentType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GoTo extends Command {
    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("goto")
                    .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            return handleGoToCommand(name, MinecraftClient.getInstance());
                        })
                    )
            );
        });
    }

    private static int handleGoToCommand(String townName, MinecraftClient client) {
        if (client.player == null) {
            LOGGER.error("Player instance is null, cannot send feedback.");
            return 0;
        }

        CompletableFuture.runAsync(() -> {
            try {
                fetch fetchInstance = new fetch();
                int radius = 500;
                int maxAttempts = 3;

                while (maxAttempts > 0) {
                    // Fetch nearby towns
                    JsonObject nearbyPayload = new JsonObject();
                    JsonArray queryArray = new JsonArray();
                    JsonObject query = new JsonObject();
                    query.addProperty("target_type", "TOWN");
                    query.addProperty("target", townName);
                    query.addProperty("search_type", "TOWN");
                    query.addProperty("radius", radius);
                    queryArray.add(query);
                    nearbyPayload.add("query", queryArray);

                    String nearbyResponse = fetchInstance.Fetch("https://api.earthmc.net/v3/aurora/nearby", nearbyPayload.toString());
                    JsonArray nearbyArray = JsonParser.parseString(nearbyResponse).getAsJsonArray().get(0).getAsJsonArray();
                    List<String> towns = new ArrayList<>();

                    for (JsonElement element : nearbyArray) {
                        if (element.getAsJsonObject().has("name")) {
                            towns.add(element.getAsJsonObject().get("name").getAsString());
                        }
                    }

                    if (towns.isEmpty()) {
                        LOGGER.info("No towns found within radius {}. Increasing radius.", radius);
                        radius += 500;
                        maxAttempts--;
                        continue;
                    }

                    // Fetch town details
                    JsonObject townDetailsPayload = new JsonObject();
                    JsonArray townQuery = new JsonArray();
                    towns.forEach(townQuery::add);
                    townDetailsPayload.add("query", townQuery);

                    String townDetailsResponse = fetchInstance.Fetch("https://api.earthmc.net/v3/aurora/towns/", townDetailsPayload.toString());
                    JsonArray townDetailsArray = JsonParser.parseString(townDetailsResponse).getAsJsonArray();
                    List<String> validTowns = new ArrayList<>();

                    for (JsonElement element : townDetailsArray) {
                        JsonObject Element = element.getAsJsonObject();
                        if (element.isJsonObject()) {
                            JsonObject town = Element.getAsJsonObject();
                            JsonObject status = town.get("status").getAsJsonObject();

                            if (status.get("isPublic").getAsBoolean() && status.get("canOutsidersSpawn").getAsBoolean()) {
                                validTowns.add(town.get("name").getAsString());
                            } else if (status.get("isCapital").getAsBoolean()) {
                                String nationUuid = town.get("nation").getAsJsonObject().get("uuid").getAsString();

                                JsonObject nationPayload = new JsonObject();
                                JsonArray nationQuery = new JsonArray();
                                nationQuery.add(nationUuid);
                                nationPayload.add("query", nationQuery);

                                String nationResponse = fetchInstance.Fetch("https://api.earthmc.net/v3/aurora/nations/", nationPayload.toString());
                                JsonObject nationDetails = JsonParser.parseString(nationResponse).getAsJsonArray().get(0).getAsJsonObject();

                                if (nationDetails.has("status") && nationDetails.get("status").getAsJsonObject().get("isPublic").getAsBoolean()) {
                                    validTowns.add(town.get("name").getAsString());
                                }
                            }
                        }
                    }

                    if (!validTowns.isEmpty()) {
                        sendMessage(client, Text.literal("Found suitable spawn in: " + validTowns)
                            .setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
                        return;
                    }

                    radius += 500;
                    maxAttempts--;
                }

                sendMessage(client, Text.literal("No suitable spawns found.")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED)));
            } catch (Exception e) {
                LOGGER.error("Command exited with an exception: ", e);
                sendMessage(client, Text.literal("Command exited with an exception.")
                    .setStyle(Style.EMPTY.withColor(Formatting.RED)));
            }
        });

        return 1;
    }

    private static void sendMessage(@NotNull MinecraftClient client, Text message) {
        client.execute(() -> {
            if (client.player != null) {
                Text prefix = Prefix.getPrefix();
                Text chatMessage = Text.literal("").append(prefix).append(message);
                client.player.sendMessage(chatMessage, false);
            }
        });
    }
}
