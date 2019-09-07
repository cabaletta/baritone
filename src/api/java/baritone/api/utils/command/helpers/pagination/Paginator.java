/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.api.utils.command.helpers.pagination;

import baritone.api.utils.Helper;
import baritone.api.utils.command.exception.CommandInvalidTypeException;
import baritone.api.utils.command.helpers.arguments.ArgConsumer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.List;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;

public class Paginator<E> implements Helper {
    public final List<E> entries;
    public int pageSize = 8;
    public int page = 1;

    public Paginator(List<E> entries) {
        this.entries = entries;
    }

    public Paginator(E... entries) {
        this.entries = asList(entries);
    }

    public Paginator<E> setPageSize(int pageSize) {
        this.pageSize = pageSize;

        return this;
    }

    public int getMaxPage() {
        return (entries.size() - 1) / pageSize + 1;
    }

    public boolean validPage(int page) {
        return page > 0 && page <= getMaxPage();
    }

    public Paginator<E> skipPages(int pages) {
        page += pages;

        return this;
    }

    public void display(Function<E, ITextComponent> transform, String commandPrefix) {
        int offset = (page - 1) * pageSize;

        for (int i = offset; i < offset + pageSize; i++) {
            if (i < entries.size()) {
                logDirect(transform.apply(entries.get(i)));
            } else {
                logDirect("--", TextFormatting.DARK_GRAY);
            }
        }

        boolean hasPrevPage = nonNull(commandPrefix) && validPage(page - 1);
        boolean hasNextPage = nonNull(commandPrefix) && validPage(page + 1);

        ITextComponent prevPageComponent = new TextComponentString("<<");

        if (hasPrevPage) {
            prevPageComponent.getStyle()
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format("%s %d", commandPrefix, page - 1)
                    ))
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString("Click to view previous page")
                    ));
        } else {
            prevPageComponent.getStyle().setColor(TextFormatting.DARK_GRAY);
        }

        ITextComponent nextPageComponent = new TextComponentString(">>");

        if (hasNextPage) {
            nextPageComponent.getStyle()
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format("%s %d", commandPrefix, page + 1)
                    ))
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString("Click to view next page")
                    ));
        } else {
            nextPageComponent.getStyle().setColor(TextFormatting.DARK_GRAY);
        }

        ITextComponent pagerComponent = new TextComponentString("");
        pagerComponent.getStyle().setColor(TextFormatting.GRAY);
        pagerComponent.appendSibling(prevPageComponent);
        pagerComponent.appendText(" | ");
        pagerComponent.appendSibling(nextPageComponent);
        pagerComponent.appendText(String.format(" %d/%d", page, getMaxPage()));
        logDirect(pagerComponent);
    }

    public void display(Function<E, ITextComponent> transform) {
        display(transform, null);
    }

    public static <T> void paginate(ArgConsumer consumer, Paginator<T> pagi, Runnable pre, Function<T, ITextComponent> transform, String commandPrefix) {
        int page = 1;

        consumer.requireMax(1);

        if (consumer.has()) {
            page = consumer.getAs(Integer.class);

            if (!pagi.validPage(page)) {
                throw new CommandInvalidTypeException(
                        consumer.consumed(),
                        String.format(
                                "a valid page (1-%d)",
                                pagi.getMaxPage()
                        ),
                        consumer.consumed().value
                );
            }
        }

        pagi.skipPages(page - pagi.page);

        if (nonNull(pre)) {
            pre.run();
        }

        pagi.display(transform, commandPrefix);
    }

    public static <T> void paginate(ArgConsumer consumer, List<T> elems, Runnable pre, Function<T, ITextComponent> transform, String commandPrefix) {
        paginate(consumer, new Paginator<>(elems), pre, transform, commandPrefix);
    }

    public static <T> void paginate(ArgConsumer consumer, T[] elems, Runnable pre, Function<T, ITextComponent> transform, String commandPrefix) {
        paginate(consumer, asList(elems), pre, transform, commandPrefix);
    }

    public static <T> void paginate(ArgConsumer consumer, Paginator<T> pagi, Function<T, ITextComponent> transform, String commandPrefix) {
        paginate(consumer, pagi, null, transform, commandPrefix);
    }

    public static <T> void paginate(ArgConsumer consumer, List<T> elems, Function<T, ITextComponent> transform, String commandPrefix) {
        paginate(consumer, new Paginator<>(elems), null, transform, commandPrefix);
    }

    public static <T> void paginate(ArgConsumer consumer, T[] elems, Function<T, ITextComponent> transform, String commandPrefix) {
        paginate(consumer, asList(elems), null, transform, commandPrefix);
    }

    public static <T> void paginate(ArgConsumer consumer, Paginator<T> pagi, Runnable pre, Function<T, ITextComponent> transform) {
        paginate(consumer, pagi, pre, transform, null);
    }

    public static <T> void paginate(ArgConsumer consumer, List<T> elems, Runnable pre, Function<T, ITextComponent> transform) {
        paginate(consumer, new Paginator<>(elems), pre, transform, null);
    }

    public static <T> void paginate(ArgConsumer consumer, T[] elems, Runnable pre, Function<T, ITextComponent> transform) {
        paginate(consumer, asList(elems), pre, transform, null);
    }

    public static <T> void paginate(ArgConsumer consumer, Paginator<T> pagi, Function<T, ITextComponent> transform) {
        paginate(consumer, pagi, null, transform, null);
    }

    public static <T> void paginate(ArgConsumer consumer, List<T> elems, Function<T, ITextComponent> transform) {
        paginate(consumer, new Paginator<>(elems), null, transform, null);
    }

    public static <T> void paginate(ArgConsumer consumer, T[] elems, Function<T, ITextComponent> transform) {
        paginate(consumer, asList(elems), null, transform, null);
    }
}
