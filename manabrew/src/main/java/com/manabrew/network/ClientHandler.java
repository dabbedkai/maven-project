package com.manabrew.network;

import com.manabrew.inventory.*;
import com.manabrew.model.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler implements Runnable {

    // ── box geometry ─────────────────────────────────────────────────────────
    // total inner width between the ║ borders
    private static final int BOX_W = 60;
    // usable content per row (BOX_W minus the two padding spaces)
    private static final int CONTENT_W = BOX_W - 2;

    // ── player state ─────────────────────────────────────────────────────────
    private Socket    myConnection;
    private PrintWriter outBuffer;
    private String    playerName;
    private GameRoom  myRoom        = null;
    private String    currentRoomCode = "";
    private int       personalOrdersDone = 0;

    // rolling log shown below the UI box (last N lines of activity)
    private final LinkedList<String> recentLog = new LinkedList<>();
    private static final int LOG_MAX = 6;

    public ClientHandler(Socket s) {
        this.myConnection = s;
    }

    public String getUsername() { return playerName; }

    // ── message routing ───────────────────────────────────────────────────────
    // adds a line to the rolling log and redraws the screen
    // before the player is in a room it falls back to a plain println
    public synchronized void sendMessage(String m) {
        if (outBuffer == null) return;
        synchronized (recentLog) {
            if (recentLog.size() >= LOG_MAX) recentLog.removeFirst();
            recentLog.add(m);
        }
        if (myRoom != null) drawUI();
        else outBuffer.println(m);
    }

    // sends raw bytes without touching the log or triggering a redraw
    // only used during the pre-room tutorial/login phase
    private void raw(String m) {
        if (outBuffer != null) outBuffer.println(m);
    }

    // ── lifecycle ─────────────────────────────────────────────────────────────
    @Override
    public void run() {
        try {
            outBuffer  = new PrintWriter(myConnection.getOutputStream(), true);
            BufferedReader inReader = new BufferedReader(new InputStreamReader(myConnection.getInputStream()));

            // one-time tutorial shown before the player joins a room
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

        if      (lower.startsWith("join "))              handleJoin(cmd);
        else if (lower.equals("start") && myRoom != null) handleStart();
        else if (lower.startsWith("shop ") && myRoom != null) handleShop(cmd);
        else if (lower.startsWith("claim ") && myRoom != null) handleClaim(cmd);
        else if (lower.startsWith("brew ") && myRoom != null)  handleBrew(cmd);
        else if (myRoom != null)
            sendMessage(TerminalColors.DIM + "unknown command. check the footer for what's available." + TerminalColors.RESET);
        else
            raw("join a lobby first: join <code>");
    }

    // ── command handlers ──────────────────────────────────────────────────────

    // assigns the player to a room (creates it if it doesn't exist yet)
    private void handleJoin(String cmd) {
        String[] parts = cmd.split(" ", 2);
        if (parts.length < 2 || parts[1].isBlank()) { raw("usage: join <code>"); return; }
        currentRoomCode = parts[1].trim();
        myRoom = Server.getOrCreateRoom(currentRoomCode);
        myRoom.addPlayer(this, playerName);
        drawUI();
    }

    // only the host can kick off a round
    private void handleStart() {
        if (myRoom.hostPlayer.equals(playerName)) {
            myRoom.startRound();
        } else {
            sendMessage(TerminalColors.RED + "only the host (" + myRoom.hostPlayer + ") can start the round." + TerminalColors.RESET);
        }
    }

    // buys one unit of an ingredient from the shared vault
    private void handleShop(String cmd) {
        if (!myRoom.isShopPhase) {
            sendMessage(TerminalColors.RED + "shop is closed while a round is active." + TerminalColors.RESET);
            return;
        }
        String item  = cmd.substring(5).trim().toLowerCase();
        int    price = Pantry.getPrice(item);

        if (price < 0) {
            sendMessage(TerminalColors.RED + "'" + item + "' isn't sold here. check the recipe book for ingredient names." + TerminalColors.RESET);
            return;
        }
        if (myRoom.sharedVaultGold < price) {
            sendMessage(TerminalColors.RED + "not enough gold! need " + price + "g, vault has " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
            return;
        }

        myRoom.sharedVaultGold -= price;
        myRoom.roomPantry.addStock(item, 1);
        myRoom.broadcast(TerminalColors.GREEN + playerName + " bought 1x " + item + " for " + price + "g.  vault: " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
    }

    // locks an order so teammates don't step on each other
    private void handleClaim(String cmd) {
        if (myRoom.isShopPhase) {
            sendMessage(TerminalColors.RED + "no orders during shop phase. start the round first." + TerminalColors.RESET);
            return;
        }
        String wanted = cmd.substring(6).trim();
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

    // matches the typed ingredients to an order, deducts stock, waits brew time, pays out
    private void handleBrew(String cmd) throws InterruptedException {
        if (myRoom.isShopPhase) {
            sendMessage(TerminalColors.RED + "can't brew during shop phase." + TerminalColors.RESET);
            return;
        }

        // normalize and sort so "fairy dust,water" == "water,fairy dust"
        String[] typed = cmd.substring(5).trim().split(",");
        for (int i = 0; i < typed.length; i++) typed[i] = typed[i].trim().toLowerCase();
        Arrays.sort(typed);

        // scan open orders for one whose recipe matches exactly
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

        // warn on known volatile combos
        if (typed.length == 2 && Brewable.isVolatile(typed[0], typed[1])) {
            sendMessage(TerminalColors.RED + "⚠  volatile combo! handle with care." + TerminalColors.RESET);
        }

        if (!myRoom.roomPantry.takeIngredients(typed)) {
            sendMessage(TerminalColors.RED + "pantry is short on stock. wait for the next shop phase." + TerminalColors.RESET);
            return;
        }

        String potName   = target.getPotion().getName();
        int    brewSecs  = target.getPotion().getTier() * 2;

        myRoom.broadcast(TerminalColors.CYAN + "⚗  " + playerName + " is brewing " + potName + "...  (" + brewSecs + "s)" + TerminalColors.RESET);
        Thread.sleep(brewSecs * 1000L);

        // remove the order from the live list and pay out
        if (myRoom.orders.remove(target)) {
            int reward = target.getPotion().getPrice();
            myRoom.sharedVaultGold += reward;
            personalOrdersDone++;
            Server.potionsDelivered++;
            Server.totalGold += reward;
            myRoom.broadcast(TerminalColors.GREEN + "✓  " + playerName + " delivered " + potName + "!  +" + reward + "g   vault: " + myRoom.sharedVaultGold + "g." + TerminalColors.RESET);
        }
    }

    // ── screen drawing ────────────────────────────────────────────────────────
    // clears the terminal and redraws the full persistent UI
    synchronized void drawUI() {
        if (outBuffer == null || myRoom == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append(TerminalColors.CLEAR);

        // ── header ──
        sb.append(topBar("ManaBrew Tavern")).append("\n");

        // player stats row
        String phaseTag = myRoom.isShopPhase
            ? TerminalColors.YELLOW + "SHOP" + TerminalColors.RESET
            : TerminalColors.RED    + "BREWING" + TerminalColors.RESET;
        sb.append(boxRow(
            TerminalColors.BOLD + playerName + TerminalColors.RESET
            + "   vault: " + TerminalColors.YELLOW + myRoom.sharedVaultGold + "g" + TerminalColors.RESET
            + "   orders done: " + TerminalColors.GREEN + personalOrdersDone + TerminalColors.RESET
            + "   " + phaseTag
        )).append("\n");

        // room meta row
        sb.append(boxRow(
            "room: " + TerminalColors.CYAN + currentRoomCode + TerminalColors.RESET
            + "   round: " + myRoom.getRoundNum()
            + "   host: " + TerminalColors.YELLOW + myRoom.hostPlayer + TerminalColors.RESET
        )).append("\n");

        sb.append(divider()).append("\n");

        // main content switches based on phase
        if (myRoom.isShopPhase) buildShopView(sb);
        else                    buildRoundView(sb);

        sb.append(divider()).append("\n");

        // command hint so players always know what they can type
        String hint = myRoom.isShopPhase
            ? "shop <item>  |  start (host only)"
            : "claim <potion name>  |  brew <ing1,ing2>";
        sb.append(boxRow(TerminalColors.DIM + "> " + hint + TerminalColors.RESET)).append("\n");
        sb.append(bottomBar()).append("\n");

        // recent activity log pinned below the box
        sb.append(TerminalColors.DIM)
          .append("─── recent ").append("─".repeat(49))
          .append(TerminalColors.RESET).append("\n");
        synchronized (recentLog) {
            for (String line : recentLog) sb.append(line).append("\n");
        }
        sb.append(TerminalColors.GREEN + "\n> " + TerminalColors.RESET);

        outBuffer.print(sb.toString());
        outBuffer.flush();
    }

    // shop phase: ingredient price list + recipe book
    private void buildShopView(StringBuilder sb) {
        sb.append(boxRow(TerminalColors.YELLOW + TerminalColors.BOLD + " INGREDIENT SHOP" + TerminalColors.RESET)).append("\n");
        sb.append(emptyRow()).append("\n");
        sb.append(boxRow(TerminalColors.DIM + "  item              price   in stock" + TerminalColors.RESET)).append("\n");

        // each ingredient with its shop price and current pantry stock
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
        sb.append(boxRow(TerminalColors.DIM + "  name                 tier  sell    ingredients" + TerminalColors.RESET)).append("\n");

        // every potion the factory knows about
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
        sb.append(emptyRow()).append("\n");
    }

    // brewing phase: live order list with claim status
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
                sb.append(boxRow(
                    " " + status
                    + " " + TerminalColors.CYAN + padRight(tk.getPotion().getName(), 16) + TerminalColors.RESET
                    + " " + TerminalColors.DIM  + tk.getPotion().getRecipeString()       + TerminalColors.RESET
                )).append("\n");
            }
        }
        sb.append(emptyRow()).append("\n");
    }

    // ── tutorial ──────────────────────────────────────────────────────────────
    // shown once when the player first connects, before they join a room
    private void showTutorial() {
        StringBuilder t = new StringBuilder();
        t.append(TerminalColors.CLEAR);
        t.append(topBar("ManaBrew  -  Alchemist's Guide")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(boxRow("  you and your crew run a potion shop together.")).append("\n");
        t.append(boxRow("  orders roll in, you brew them, vault fills up.")).append("\n");
        t.append(boxRow("  run out of ingredients or time and you lose gold.")).append("\n");
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
        t.append(boxRow("  shop <item>         buy 1 unit  (gold from shared vault)")).append("\n");
        t.append(boxRow("  start               begin the round  (host only)")).append("\n");
        t.append(boxRow("  claim <potion name> lock an order before teammates do")).append("\n");
        t.append(boxRow("  brew <ing1,ing2>    mix ingredients to fill the order")).append("\n");
        t.append(boxRow("  quit                leave")).append("\n");
        t.append(emptyRow()).append("\n");
        t.append(bottomBar()).append("\n");
        outBuffer.print(t.toString());
        outBuffer.flush();
    }

    // ── box drawing helpers (all static so tutorial can call them too) ─────────

    // top border with a centered title built in
    private static String topBar(String title) {
        int sides = BOX_W - title.length() - 2;
        int left  = sides / 2;
        int right = sides - left;
        return TerminalColors.CYAN + TerminalColors.BOLD
            + "╔" + "═".repeat(left) + " " + title + " " + "═".repeat(right) + "╗"
            + TerminalColors.RESET;
    }

    // horizontal rule connecting the two vertical borders
    private static String divider() {
        return TerminalColors.CYAN + "╠" + "═".repeat(BOX_W) + "╣" + TerminalColors.RESET;
    }

    // bottom border
    private static String bottomBar() {
        return TerminalColors.CYAN + "╚" + "═".repeat(BOX_W) + "╝" + TerminalColors.RESET;
    }

    // wraps content in side borders and pads to the correct width
    private static String boxRow(String content) {
        int visLen = TerminalColors.stripAnsi(content).length();
        int pad    = Math.max(0, CONTENT_W - visLen);
        return TerminalColors.CYAN + "║ " + TerminalColors.RESET
            + content + " ".repeat(pad)
            + TerminalColors.CYAN + " ║" + TerminalColors.RESET;
    }

    // shorthand for a blank padded row
    private static String emptyRow() { return boxRow(""); }

    // right-pads a string to a fixed column width
    private static String padRight(String s, int len) {
        if (s.length() >= len) return s.substring(0, len);
        return s + " ".repeat(len - s.length());
    }

    // pulls ingredient names out of a recipe array into a plain String array
    private static String[] toNames(Ingredient[] ings) {
        String[] names = new String[ings.length];
        for (int i = 0; i < ings.length; i++) names[i] = ings[i].getName().toLowerCase();
        return names;
    }
}