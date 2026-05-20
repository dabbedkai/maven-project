package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    // ── box geometry ─────────────────────────────────────────────────────────
    private static final int BOX_W = 72;
    private static final int CONTENT_W = BOX_W - 2;

    // ── player state ─────────────────────────────────────────────────────────
    private Socket    myConnection;
    private PrintWriter outBuffer;
    private String    playerName;
    private GameRoom  myRoom        = null;
    private String    currentRoomCode = "";
    private int       personalOrdersDone = 0;
    
    // volatile penalty timers
    private long stunnedUntil = 0;
    private long recipeBookHiddenUntil = 0;

    // rolling log shown below the UI box
    private final LinkedList<String> recentLog = new LinkedList<>();
    private static final int LOG_MAX = 6;

    public ClientHandler(Socket s) {
        this.myConnection = s;
    }

    public String getUsername() { return playerName; }

    // ── message routing ───────────────────────────────────────────────────────
    public synchronized void sendMessage(String m) {
        if (outBuffer == null) return;
        synchronized (recentLog) {
            if (recentLog.size() >= LOG_MAX) recentLog.removeFirst();
            recentLog.add(m);
        }
        if (myRoom != null) drawUI();
        else outBuffer.println(m);
    }

    private void raw(String m) {
        if (outBuffer != null) outBuffer.println(m);
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void run() {
        try {
            outBuffer  = new PrintWriter(myConnection.getOutputStream(), true);
            BufferedReader inReader = new BufferedReader(new InputStreamReader(myConnection.getInputStream()));

            showTutorial();

            raw(TerminalColors.CYAN + "\nenter your alchemist name: " + TerminalColors.RESET);
            playerName = inReader.readLine();
            if (playerName == null || playerName.isBlank()) playerName = "Wanderer";

            raw(TerminalColors.YELLOW + "welcome, " + playerName + "." + TerminalColors.RESET);
            raw("type  'join <code>'  to enter or create a lobby (e.g. join 1234)\n");

            String cmdLine;
            while ((cmdLine = inReader.readLine()) != null) {
                cmdLine = cmdLine.trim();
                if (cmdLine.equalsIgnoreCase("quit")) break;
                handleCommand(cmdLine);
            }

        } catch (Exception ex) {
            if (myRoom != null) myRoom.handleDisconnect(this);
            System.out.println(TerminalColors.DIM + "log: " + playerName + " disconnected." + TerminalColors.RESET);
        }
    }

    // ── command dispatcher ────────────────────────────────────────────────────
    private void handleCommand(String cmd) throws InterruptedException {
        String lower = cmd.toLowerCase();

        // enforce stun penalty so they can't spam commands while blasted
        if (myRoom != null && System.currentTimeMillis() < stunnedUntil) {
            long secs = (stunnedUntil - System.currentTimeMillis()) / 1000 + 1;
            sendMessage(TerminalColors.RED + "You are stunned and cannot act for " + secs + "s!" + TerminalColors.RESET);
            return;
        }

        if      (lower.startsWith("join"))               handleJoin(cmd);
        else if (lower.equals("start") && myRoom != null) handleStart();
        else if (lower.startsWith("shop") && myRoom != null) handleShop(cmd);
        else if (lower.startsWith("claim") && myRoom != null) handleClaim(cmd);
        else if (lower.startsWith("brew") && myRoom != null)  handleBrew(cmd);
        else if (myRoom != null)
            sendMessage(TerminalColors.DIM + "unknown command. check the footer for what's available." + TerminalColors.RESET);
        else
            raw("join a lobby first: join <code>");
    }

    // ── command handlers ──────────────────────────────────────────────────────

    // assigns the player to a room and makes sure their name is unique
    private void handleJoin(String cmd) {
        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) { raw("usage: join <code>"); return; }
        currentRoomCode = parts[1].trim();
        myRoom = Server.getOrCreateRoom(currentRoomCode);
        
        // fix duplicate names so the claim mechanics don't break
        boolean nameTaken = true;
        while (nameTaken) {
            nameTaken = false;
            for (ClientHandler p : myRoom.players) {
                if (p.getUsername().equalsIgnoreCase(playerName)) {
                    nameTaken = true;
                    // tack a random number onto the end if it's taken
                    playerName = playerName + new Random().nextInt(100); 
                    break;
                }
            }
        }
        
        myRoom.addPlayer(this, playerName);
        drawUI();
    }

    private void handleStart() {
        if (myRoom.hostPlayer.equals(playerName)) {
            myRoom.startRound();
        } else {
            sendMessage(TerminalColors.RED + "only the host (" + myRoom.hostPlayer + ") can start the round." + TerminalColors.RESET);
        }
    }

    private void handleShop(String cmd) {
        if (!myRoom.isShopPhase) {
            sendMessage(TerminalColors.RED + "shop is closed while a round is active." + TerminalColors.RESET);
            return;
        }
        
        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(TerminalColors.RED + "usage: shop <item> [qty]" + TerminalColors.RESET);
            return;
        }
        
        String rawItem = parts[1].trim().toLowerCase();
        String item = rawItem;
        int qty = 1; 
        
        // sniff out if they threw a number at the end for bulk buying
        int lastSpace = rawItem.lastIndexOf(' ');
        if (lastSpace != -1) {
            String lastWord = rawItem.substring(lastSpace + 1);
            try {
                qty = Integer.parseInt(lastWord);
                item = rawItem.substring(0, lastSpace).trim(); 
            } catch (NumberFormatException e) {
                // wasn't a number, leave qty at 1
            }
        }
        
        if (qty <= 0) {
            sendMessage(TerminalColors.RED + "quantity must be at least 1." + TerminalColors.RESET);
            return;
        }
        
        int unitPrice = Pantry.getPrice(item);
        if (unitPrice < 0) {
            sendMessage(TerminalColors.RED + "'" + item + "' isn't sold here. check the recipe book." + TerminalColors.RESET);
            return;
        }
        
        int totalCost = unitPrice * qty;
        if (myRoom.sharedVaultGold < totalCost) {
            sendMessage(TerminalColors.RED + "not enough gold! need " + totalCost + "g, vault has " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
            return;
        }

        myRoom.sharedVaultGold -= totalCost;
        myRoom.roomPantry.addStock(item, qty);
        myRoom.broadcast(TerminalColors.GREEN + playerName + " bought " + qty + "x " + item + " for " + totalCost + "g.  vault: " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
    }

    private void handleClaim(String cmd) {
        if (myRoom.isShopPhase) {
            sendMessage(TerminalColors.RED + "no orders during shop phase. start the round first." + TerminalColors.RESET);
            return;
        }
        
        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(TerminalColors.RED + "usage: claim <potion name>" + TerminalColors.RESET);
            return;
        }
        
        String wanted = parts[1].trim();
        boolean found = false;

        for (OrderTicket ticket : myRoom.orders.getSnapshot()) {
            if (ticket.getPotion().getName().equalsIgnoreCase(wanted)) {
                found = true;
                if (ticket.claim(playerName)) {
                    myRoom.broadcast(TerminalColors.BLUE + playerName + " claimed: " + wanted + "!" + TerminalColors.RESET);
                } else {
                    sendMessage(TerminalColors.RED + ticket.getClaimedBy() + " already has that one." + TerminalColors.RESET);
                }
                break;
            }
        }
        if (!found) sendMessage(TerminalColors.RED + "no order called '" + wanted + "'. check your spelling." + TerminalColors.RESET);
    }

    private void handleBrew(String cmd) throws InterruptedException {
        if (myRoom.isShopPhase) {
            sendMessage(TerminalColors.RED + "can't brew during shop phase." + TerminalColors.RESET);
            return;
        }

        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].trim().isEmpty()) {
            sendMessage(TerminalColors.RED + "usage: brew <ing1,ing2>" + TerminalColors.RESET);
            return;
        }

        String[] typed = parts[1].trim().split(",");
        boolean hasTypo = false;

        for (int i = 0; i < typed.length; i++) {
            typed[i] = typed[i].trim().toLowerCase();
            if (typed[i].isEmpty()) {
                sendMessage(TerminalColors.RED + "invalid ingredient format. usage: brew <ing1,ing2>" + TerminalColors.RESET);
                return;
            }
            if (Pantry.getPrice(typed[i]) == -1) hasTypo = true;
        }

        if (hasTypo) {
            triggerExplosion("Typo in ingredients caused a volatile reaction!");
            return;
        }

        Arrays.sort(typed);

        OrderTicket target = null;
        for (OrderTicket tk : myRoom.orders.getSnapshot()) {
            String[] req = toNames(tk.getPotion().getRecipe());
            Arrays.sort(req);
            if (Arrays.equals(typed, req)) { target = tk; break; }
        }

        if (target == null) {
            sendMessage(TerminalColors.RED + "no active order matches those ingredients." + TerminalColors.RESET);
            return;
        }
        if (!playerName.equals(target.getClaimedBy())) {
            sendMessage(TerminalColors.RED + "claim it first: claim " + target.getPotion().getName() + TerminalColors.RESET);
            return;
        }

        if (!myRoom.roomPantry.takeIngredients(typed)) {
            sendMessage(TerminalColors.RED + "pantry is short on stock. wait for the next shop phase." + TerminalColors.RESET);
            return;
        }

        if (typed.length == 2 && Brewable.isVolatile(typed[0], typed[1])) {
            if (Math.random() < 0.5) {
                triggerExplosion("Volatile combo became unstable!");
            } else {
                sendMessage(TerminalColors.YELLOW + "⚠ volatile combo handled safely..." + TerminalColors.RESET);
            }
        }

        String potName   = target.getPotion().getName();
        int    brewSecs  = target.getPotion().getTier() * 2;

        myRoom.broadcast(TerminalColors.CYAN + "⚗  " + playerName + " is brewing " + potName + "...  (" + brewSecs + "s)" + TerminalColors.RESET);
        Thread.sleep(brewSecs * 1000L);

        // payout phase - feed the gold into the quota tracker
        if (myRoom.orders.remove(target)) {
            int reward = target.getPotion().getPrice();
            myRoom.sharedVaultGold += reward;
            myRoom.goldEarnedThisRound += reward;
            personalOrdersDone++;
            Server.potionsDelivered++;
            Server.totalGold += reward;
            myRoom.broadcast(TerminalColors.GREEN + "✓  " + playerName + " delivered " + potName + "!  +" + reward + "g   vault: " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
        }
    }

    private void triggerExplosion(String reason) {
        sendMessage(TerminalColors.RED + "💥 BOOM! " + reason + TerminalColors.RESET);
        Random rng = new Random();
        int penaltyType = rng.nextInt(3);

        switch (penaltyType) {
            case 0:
                int loss = 15 + rng.nextInt(16); 
                myRoom.sharedVaultGold -= loss;
                if (myRoom.sharedVaultGold < 0) myRoom.sharedVaultGold = 0;
                myRoom.broadcast(TerminalColors.RED + playerName + "'s cauldron exploded! Team lost " + loss + "g." + TerminalColors.RESET);
                break;
            case 1:
                recipeBookHiddenUntil = System.currentTimeMillis() + 45000; 
                sendMessage(TerminalColors.MAGENTA + "Soot covers your eyes! Recipes and orders are unreadable for 45s." + TerminalColors.RESET);
                break;
            case 2:
                stunnedUntil = System.currentTimeMillis() + 15000; 
                sendMessage(TerminalColors.YELLOW + "You are stunned by the blast and can't act for 15s!" + TerminalColors.RESET);
                break;
        }
        drawUI();
    }

    // ── screen drawing ────────────────────────────────────────────────────────
    synchronized void drawUI() {
        if (outBuffer == null || myRoom == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(TerminalColors.CLEAR);

        sb.append(topBar("ManaBrew Tavern")).append("\n");

        String phaseTag = myRoom.isShopPhase
            ? TerminalColors.YELLOW + "SHOP" + TerminalColors.RESET
            : TerminalColors.RED    + "BREWING" + TerminalColors.RESET;
            
        sb.append(boxRow(
            TerminalColors.BOLD + playerName + TerminalColors.RESET
            + "   vault: " + TerminalColors.YELLOW + myRoom.sharedVaultGold + "g" + TerminalColors.RESET
            + "   orders done: " + TerminalColors.GREEN + personalOrdersDone + TerminalColors.RESET
            + "   " + phaseTag
        )).append("\n");

        // visually swap the quota tag based on if they are prepping or actively playing
        String quotaTag = myRoom.isShopPhase 
            ? "   next quota: " + TerminalColors.YELLOW + myRoom.calculateQuota() + "g" + TerminalColors.RESET
            : "   quota: " + TerminalColors.YELLOW + myRoom.goldEarnedThisRound + "/" + myRoom.currentQuota + "g" + TerminalColors.RESET;

        sb.append(boxRow(
            "room: " + TerminalColors.CYAN + currentRoomCode + TerminalColors.RESET
            + "   round: " + myRoom.getRoundNum()
            + "   host: " + TerminalColors.YELLOW + myRoom.hostPlayer + TerminalColors.RESET
            + quotaTag
        )).append("\n");

        sb.append(divider()).append("\n");

        if (myRoom.isShopPhase) buildShopView(sb);
        else                    buildRoundView(sb);

        sb.append(divider()).append("\n");

        String hint = myRoom.isShopPhase
            ? "shop <item> [qty] | start (host only)"
            : "claim <potion name> | brew <ing1,ing2>";
        sb.append(boxRow(TerminalColors.DIM + "> " + hint + TerminalColors.RESET)).append("\n");
        sb.append(bottomBar()).append("\n");

        sb.append(TerminalColors.DIM)
          .append("─── recent ")
          .append("─".repeat(Math.max(0, BOX_W - 9)))
          .append(TerminalColors.RESET).append("\n");
          
        synchronized (recentLog) {
            for (String line : recentLog) sb.append(line).append("\n");
        }
        sb.append(TerminalColors.GREEN + "\n> " + TerminalColors.RESET);

        outBuffer.print(sb.toString());
        outBuffer.flush();
    }

    private void buildShopView(StringBuilder sb) {
        sb.append(boxRow(TerminalColors.YELLOW + TerminalColors.BOLD + " INGREDIENT SHOP" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");
        sb.append(boxRow(TerminalColors.DIM + "  item              price   in stock" + TerminalColors.RESET)).append("\n");

        String[][] catalog = {
            {"water",        " 5g"},
            {"dragon scale", "15g"},
            {"fairy dust",   "12g"},
            {"fire pepper",  "10g"},
            {"lunar shard",  "20g"},
            {"void extract", "25g"},
        };
        
        for (String[] row : catalog) {
            int qty = myRoom.roomPantry.getStock(row[0]);
            String stockStr = qty == 0
                ? TerminalColors.RED   + "out of stock" + TerminalColors.RESET
                : TerminalColors.GREEN + qty + " units"  + TerminalColors.RESET;
            sb.append(boxRow(
                "  " + TerminalColors.CYAN + padRight(row[0], 16) + TerminalColors.RESET
                + TerminalColors.YELLOW + row[1] + TerminalColors.RESET
                + "     " + stockStr
            )).append("\n");
        }

        sb.append(divider()).append("\n");
        sb.append(boxRow(TerminalColors.MAGENTA + TerminalColors.BOLD + " RECIPE BOOK" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");

        if (System.currentTimeMillis() < recipeBookHiddenUntil) {
            sb.append(boxRow(TerminalColors.RED + "  [ You can't read through the soot! ]" + TerminalColors.RESET)).append("\n");
            sb.append(emptyRow()).append("\n");
        } else {
            sb.append(boxRow(TerminalColors.DIM + "  name                 tier  sell    ingredients" + TerminalColors.RESET)).append("\n");
            for (String type : PotionFactory.ALL_TYPES) {
                Potion p = PotionFactory.create(type);
                if (p == null) continue;
                sb.append(boxRow(
                    "  " + padRight(p.getName(), 21)
                    + " T" + p.getTier()
                    + "   " + padRight(p.getPrice() + "g", 5)
                    + "   " + TerminalColors.DIM + p.getRecipeString() + TerminalColors.RESET
                )).append("\n");
            }
        }
        sb.append(emptyRow()).append("\n");
    }

    private void buildRoundView(StringBuilder sb) {
        List<OrderTicket> snap = myRoom.orders.getSnapshot();
        sb.append(boxRow(TerminalColors.RED + TerminalColors.BOLD
            + " ACTIVE ORDERS (" + snap.size() + ")" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");

        if (snap.isEmpty()) {
            sb.append(boxRow("  no orders yet... they're coming.")).append("\n");
        } else {
            for (OrderTicket tk : snap) {
                String claimedBy = tk.getClaimedBy();
                String status    = claimedBy == null
                    ? TerminalColors.GREEN  + "[open]           " + TerminalColors.RESET
                    : TerminalColors.YELLOW + "[" + padRight(claimedBy, 15) + "]" + TerminalColors.RESET;
                
                String recipeStr = tk.getPotion().getRecipeString();
                if (System.currentTimeMillis() < recipeBookHiddenUntil) {
                    recipeStr = "[ soot-covered ]";
                }

                sb.append(boxRow(
                    " " + status
                    + " " + TerminalColors.CYAN + padRight(tk.getPotion().getName(), 16) + TerminalColors.RESET
                    + " " + TerminalColors.DIM  + recipeStr + TerminalColors.RESET
                )).append("\n");
            }
        }
        sb.append(emptyRow()).append("\n");
    }

    private void showTutorial() {
        StringBuilder t = new StringBuilder();
        t.append(TerminalColors.CLEAR);
        t.append(topBar("ManaBrew  -  Alchemist's Guide")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  you and your crew run a potion shop together.")).append("\n");
        t.append(boxRow("  hit the required gold quota before the timer ends")).append("\n");
        t.append(boxRow("  or the tavern goes bankrupt and you start over.")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(divider()).append("\n");
        t.append(boxRow(TerminalColors.YELLOW + "  THE TWO PHASES" + TerminalColors.RESET)).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  SHOP PHASE  ──  buy ingredients from the shared vault,")).append("\n");
        t.append(boxRow("                  check the recipe book, plan what you need.")).append("\n");
        t.append(boxRow("                  host types  start  when the team is ready.")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  BREW PHASE  ──  orders appear automatically over time.")).append("\n");
        t.append(boxRow("                  claim one before a teammate does, then brew")).append("\n");
        t.append(boxRow("                  it with the right ingredients to earn gold.")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(divider()).append("\n");
        t.append(boxRow(TerminalColors.YELLOW + "  COMMANDS" + TerminalColors.RESET)).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  join <code>         enter or create a lobby")).append("\n");
        t.append(boxRow("  shop <item> [qty]   buy ingredients (e.g. shop water 5)")).append("\n");
        t.append(boxRow("  start               begin the round  (host only)")).append("\n");
        t.append(boxRow("  claim <potion name> lock an order before teammates do")).append("\n");
        t.append(boxRow("  brew <ing1,ing2>    mix ingredients to fill the order")).append("\n");
        t.append(boxRow("  quit                leave")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(bottomBar()).append("\n");
        outBuffer.print(t.toString());
        outBuffer.flush();
    }

    private static String topBar(String title) {
        int sides = BOX_W - title.length() - 2;
        int left  = sides / 2;
        int right = sides - left;
        return TerminalColors.CYAN + TerminalColors.BOLD
            + "╔" + "═".repeat(left) + " " + title + " " + "═".repeat(right) + "╗"
            + TerminalColors.RESET;
    }

    private static String divider() {
        return TerminalColors.CYAN + "╠" + "═".repeat(BOX_W) + "╣" + TerminalColors.RESET;
    }

    private static String bottomBar() {
        return TerminalColors.CYAN + "╚" + "═".repeat(BOX_W) + "╝" + TerminalColors.RESET;
    }

    private static String boxRow(String content) {
        int visLen = TerminalColors.stripAnsi(content).length();
        int pad    = Math.max(0, CONTENT_W - visLen);
        return TerminalColors.CYAN + "║ " + TerminalColors.RESET
            + content + " ".repeat(pad)
            + TerminalColors.CYAN + " ║" + TerminalColors.RESET;
    }

    private static String emptyRow() { return boxRow(""); }

    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    private static String[] toNames(Ingredient[] ings) {
        String[] names = new String[ings.length];
        for (int i = 0; i < ings.length; i++) names[i] = ings[i].getName().toLowerCase();
        return names;
    }
}