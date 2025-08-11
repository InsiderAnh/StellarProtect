package io.github.insideranh.stellarprotect.utils;


import io.github.insideranh.stellarprotect.data.InspectSession;
import io.github.insideranh.stellarprotect.data.PlayerProtect;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class PaginationUtils {

    public static TextComponent buildPagination(String pageStart, String pageClick, int currentPage, int perPage, int maxPage, PlayerProtect playerProtect) {
        if (currentPage < 1 || maxPage < 1 || currentPage > maxPage) {
            return new TextComponent(pageStart);
        }

        TextComponent complete = new TextComponent(pageStart);
        complete.addExtra(currentPage + "/" + maxPage + "  §7(§f");

        int startPage;
        int endPage;
        boolean showStartDots = false;
        boolean showEndDots = false;

        if (maxPage <= 7) {
            startPage = 1;
            endPage = maxPage;
        } else {
            if (currentPage <= 4) {
                startPage = 1;
                endPage = 7;
                showEndDots = true;
            } else if (currentPage >= maxPage - 3) {
                startPage = maxPage - 6;
                endPage = maxPage;
                showStartDots = true;
            } else {
                startPage = currentPage - 3;
                endPage = currentPage + 3;
                showStartDots = true;
                showEndDots = true;
            }
        }

        boolean firstElement = true;

        if (showStartDots) {
            String pageArgument = getPage(playerProtect, 1, perPage);
            String click = pageClick.replace("<page>", "1");

            TextComponent start;
            if (currentPage == 1) {
                start = new TextComponent("§n1§r");
            } else {
                start = new TextComponent("1");
            }

            start.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stellarprotect " + pageArgument));
            start.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(click)));
            complete.addExtra(start);

            complete.addExtra(new TextComponent(" §7... §f"));
            firstElement = false;
        }

        for (int page = startPage; page <= endPage; page++) {
            String pageArgument = getPage(playerProtect, page, perPage);
            String click = pageClick.replace("<page>", String.valueOf(page));

            StringBuilder pageBuilder = new StringBuilder();

            if (!firstElement) {
                pageBuilder.append("§f");
            }
            firstElement = false;

            if (page == currentPage) {
                pageBuilder.append("§n").append(page).append("§r");
            } else {
                pageBuilder.append(page);
            }

            if (page < endPage || showEndDots) {
                pageBuilder.append(" §7| ");
            }

            TextComponent component = new TextComponent(pageBuilder.toString());
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stellarprotect " + pageArgument));
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(click)));
            complete.addExtra(component);
        }

        if (showEndDots) {
            String pageArgument = getPage(playerProtect, maxPage, perPage);
            String click = pageClick.replace("<page>", String.valueOf(maxPage));

            complete.addExtra(new TextComponent("§7... §f"));

            TextComponent pageComponent;
            if (currentPage == maxPage) {
                pageComponent = new TextComponent("§n" + maxPage + "§r");
            } else {
                pageComponent = new TextComponent(String.valueOf(maxPage));
            }

            pageComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/stellarprotect " + pageArgument));
            pageComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(click)));
            complete.addExtra(pageComponent);
        }

        complete.addExtra("§7)");
        return complete;

    }

    static String getPage(PlayerProtect playerProtect, int page, int perPage) {
        String pageArgument;
        if (playerProtect.getLookupSession() != null) {
            pageArgument = "nl p:" + page + "-" + perPage;
        } else if (playerProtect.getInspectSession() != null) {
            InspectSession inspectSession = playerProtect.getInspectSession();
            pageArgument = "ni l:" + inspectSession.getArgument() + " p:" + page + "-" + perPage;
        } else {
            return "";
        }
        return pageArgument;
    }

}