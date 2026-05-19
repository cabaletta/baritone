package adris.altoclef.commands;

import adris.altoclef.AltoClef;
import adris.altoclef.commandsystem.*;
import adris.altoclef.tasks.misc.EquipArmorTask;
import adris.altoclef.util.ItemTarget;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

public class EquipCommand extends Command {
    public EquipCommand() throws CommandException {
        super("equip", "Equips armor", new Arg(ItemList.class, "[armors]"));
    }

    @Override
    protected void call(AltoClef mod, ArgParser parser) throws CommandException {
        ItemTarget[] items;
        if (parser.getArgUnits().length == 1) {
            switch (parser.getArgUnits()[0].toLowerCase()) { //Hot commands for the default full armor sets
                case "leather" -> items =
                        new ItemTarget[]{new ItemTarget(Items.LEATHER_HELMET),
                                new ItemTarget(Items.LEATHER_CHESTPLATE),
                                new ItemTarget(Items.LEATHER_LEGGINGS),
                                new ItemTarget(Items.LEATHER_BOOTS)};
                case "iron" -> items =
                        new ItemTarget[]{new ItemTarget(Items.IRON_HELMET),
                                new ItemTarget(Items.IRON_CHESTPLATE),
                                new ItemTarget(Items.IRON_LEGGINGS),
                                new ItemTarget(Items.IRON_BOOTS)};
                case "gold" -> items =
                        new ItemTarget[]{new ItemTarget(Items.GOLDEN_HELMET),
                                new ItemTarget(Items.GOLDEN_CHESTPLATE),
                                new ItemTarget(Items.GOLDEN_LEGGINGS),
                                new ItemTarget(Items.GOLDEN_BOOTS)};
                case "diamond" -> items =
                        new ItemTarget[]{new ItemTarget(Items.DIAMOND_HELMET)
                                , new ItemTarget(Items.DIAMOND_CHESTPLATE),
                                new ItemTarget(Items.DIAMOND_LEGGINGS),
                                new ItemTarget(Items.DIAMOND_BOOTS)};
                case "netherite" -> items =
                        new ItemTarget[]{new ItemTarget(Items.NETHERITE_HELMET), new ItemTarget(Items.NETHERITE_CHESTPLATE), new ItemTarget(Items.NETHERITE_LEGGINGS), new ItemTarget(Items.NETHERITE_BOOTS)};
                default -> {
                    items = parser.get(ItemList.class).items;          // if only one thing was provided, and it isn't an armor set, try to work it out.
                }
            }
        } else {
            items = parser.get(ItemList.class).items; // a list of items was provided
        }
        for (ItemTarget item : items) {
            for (Item i : item.getMatches()) {
                if (!(i instanceof ArmorItem)) {
                    items = null; // flag items as "bad" if any of the items are not ArmorItems
                    break;
                }
            }
            if (items == null) {
                break;
            }
        }


        if (items != null)
            mod.runUserTask(new EquipArmorTask(items), this::finish); // do not run the equip task with non armor items.
        else
            throw new CommandException("You must provide armor items."); //inform the user that they can only use armor items.
        //TODO Possibly add in a variable to tell the user what was wrong. However, this is less helpful if a list of items is wrong.
    }
}
