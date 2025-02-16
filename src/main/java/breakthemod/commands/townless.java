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

import breakthemod.utils.Prefix;
import breakthemod.utils.fetch;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class townless extends Command{
    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");
    public  void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> command = LiteralArgumentBuilder
                .<FabricClientCommandSource>literal("townless")
                .executes(context -> {
                    
                    MinecraftClient client = MinecraftClient.getInstance();

                    if (client.player == null) {
                        LOGGER.error("Player instance is null, cannot send feedback.");
                        return 0;
                    }
                    CompletableFuture.runAsync(() -> {
                        try {
                            List<String> players = new ArrayList<>();
                            List<String> townless = new ArrayList<>();
                            JsonObject payload = new JsonObject();
                            List<String> batch = new ArrayList<>();
                            int counter = 0;
                        
                            if (client == null || client.world == null) {
                                throw new IllegalStateException("Minecraft client or world is null.");
                            }
                        
                            for (PlayerListEntry entry : client.getNetworkHandler().getPlayerList()) {
                                String playerName = entry.getProfile().getName();
                                players.add(playerName); 
                            }
            
                            for (String player : players) {
                                batch.add(player);
                                counter++;
                                if (counter == 100) {
                                    payload.add("query", buildJsonArray(batch));
                                    batch.clear(); // Clear the batch for the next group
                                    List<String> Processedbatch = processBatch(payload);
                                    for (String User: Processedbatch){
                                        townless.add(User);
                                    }
                                    counter = 0;
                                }
                            }
                        
                            // Process any remaining players
                            if (!batch.isEmpty()) {
                                payload.add("query", buildJsonArray(batch));
                                List<String> Processedbatch = processBatch(payload);
                                for (String User: Processedbatch){
                                    townless.add(User);
                                }
                            }
                        
                          
                            
                                                
                            client.execute(() -> {
                                try {
                                    if (townless.isEmpty()) {
                                        sendMessage(client, Text.literal("No townless users found.").setStyle(Style.EMPTY.withColor(Formatting.RED)));
                                        return;
                                    }
                                    JsonObject Payload = new JsonObject();
                                    JsonArray query = new JsonArray();
                                    query.add(client.player.getUuid().toString());
                                    Payload.add("query", query);
                                    String jsonResponse = new fetch().Fetch("https://api.earthmc.net/v3/aurora/players", Payload.toString());
                                    JsonArray parsed = JsonParser.parseString(jsonResponse).getAsJsonArray();
                                    MutableText message = Text.literal("Townless Users:\n").setStyle(Style.EMPTY.withColor(Formatting.AQUA));
                                
                                    for (String user : townless) {
                                        String inviteMessage = "/msg " + user + " Hi! I see you're new here, wanna join my Town? I can help you out! Get Free enchanted Armor, Pickaxe, Diamonds, Iron, wood, food, stone, house, and ability to teleport! Type /t join " + parsed.get(0).getAsJsonObject().get("town").getAsJsonObject().get("name").getAsString();
                                        
                                        Text userText = Text.literal(user)
                                            .setStyle(Style.EMPTY
                                                .withColor(Formatting.GREEN)
                                                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, inviteMessage))
                                                .withHoverEvent(new net.minecraft.text.HoverEvent(
                                                    net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                                    Text.literal("Click to copy invite message for " + user)
                                                ))
                                            );
                                
                                        message.append(userText).append(Text.literal("\n"));
                                    }
                                
                                    sendMessage(client, message);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    client.execute(() -> sendMessage(client, Text.literal("Command has exited with an exception").setStyle(Style.EMPTY.withColor(Formatting.RED))));
                                    LOGGER.error("Command has exited with an exception: " + e.getMessage());
                                }
                            });

                                
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            client.execute(() -> sendMessage(client, Text.literal("Command has exited with an exception").setStyle(Style.EMPTY.withColor(Formatting.RED))));
                            LOGGER.error("Command has exited with an exception: " + e.getMessage());
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
    
    /**
     * Builds a JsonArray from a List of strings.
     */
    private static JsonArray buildJsonArray(List<String> list) {
        JsonArray jsonArray = new JsonArray();
        for (String item : list) {
            jsonArray.add(item);
        }
        return jsonArray;
    }
    
    
    private static List<String> processBatch(JsonObject payload) throws Exception {
        try {
            List<String> townless = new ArrayList<>();
            JsonObject template = new JsonObject();
            template.addProperty("name", true);
            template.addProperty("uuid", true);
            template.addProperty("status", true);
            payload.add("template", template);

            String response = new fetch().Fetch("https://api.earthmc.net/v3/aurora/players", payload.toString());
            JsonArray jsonArray = JsonParser.parseString(response).getAsJsonArray();

            for (JsonElement user : jsonArray) {
                JsonObject userObject = user.getAsJsonObject();
                boolean hasTown = userObject.get("status").getAsJsonObject().get("hasTown").getAsBoolean();
    
                if (!hasTown) {
                    townless.add(userObject.get("name").getAsString());
                }
            }
            return townless;

        } catch (Exception e) {
            // ego pote
            LOGGER.error("Error while processing batch: {}, from processBatch function in townless command", e.getMessage(), e);
            throw e;
        }

    }
      

}