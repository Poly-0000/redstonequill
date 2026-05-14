/*
 * @file Inventories.java
 * @author Stefan Wilhelm (wile)
 * @copyright (C) 2020 Stefan Wilhelm
 * @license MIT (see https://opensource.org/licenses/MIT)
 *
 * General inventory item handling functionality.
 */
package wfphantom.redstonequill.libmc;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiPredicate;


public class Inventories {
    public static boolean areItemStacksIdentical(ItemStack a, ItemStack b) {
        return (a.getItem() == b.getItem()) && ItemStack.isSameItemSameComponents(a, b);
    }

    public static boolean areItemStacksIdenticalIgnoreDamage(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        if (!a.isDamageableItem()) return ItemStack.isSameItemSameComponents(a, b);
        final DataComponentMap bc = b.getComponents();
        return a.getComponents().stream().allMatch(a_tdc -> {
            if (!bc.has(a_tdc.type())) return false;
            if (a_tdc.value().equals(bc.get(a_tdc.type()))) return true;
            return a_tdc.type().equals(DataComponents.DAMAGE);
        });
    }

    public static ItemStack extract(Player player, @Nullable ItemStack match, int amount, boolean simulate) {
        if (amount <= 0) return ItemStack.EMPTY;
        final InventoryRange ir = InventoryRange.fromPlayerInventory(player);
        if (match == null) {
            return ir.extract(amount, false, simulate);
        } else {
            ItemStack mstack = match.copy();
            mstack.setCount(amount);
            return ir.extract(mstack, simulate);
        }
    }

    private static ItemStack checked(ItemStack stack) {
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    public static class InventoryRange implements Container, Iterable<ItemStack> {
        protected final Container inventory_;
        protected final int offset_, size_, num_rows;
        protected int max_stack_size_ = 64;
        protected BiPredicate<Integer, ItemStack> validator_ = (index, stack) -> true;

        public static InventoryRange fromPlayerInventory(Player player) {
            return new InventoryRange(player.getInventory(), 0, 36, 4);
        }

        public InventoryRange(Container inventory, int offset, int size, int num_rows) {
            this.inventory_ = inventory;
            this.offset_ = Mth.clamp(offset, 0, inventory.getContainerSize() - 1);
            this.size_ = Mth.clamp(size, 0, inventory.getContainerSize() - this.offset_);
            this.num_rows = num_rows;
        }


        @Override
        public void clearContent() {
            for (int i = 0; i < size_; ++i) setItem(i, ItemStack.EMPTY);
        }

        @Override
        public int getContainerSize() {
            return size_;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < size_; ++i)
                if (!inventory_.getItem(offset_ + i).isEmpty()) {
                    return false;
                }
            return true;
        }

        @Override
        public ItemStack getItem(int index) {
            return inventory_.getItem(offset_ + index);
        }

        @Override
        public ItemStack removeItem(int index, int count) {
            return inventory_.removeItem(offset_ + index, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int index) {
            return inventory_.removeItemNoUpdate(offset_ + index);
        }

        @Override
        public void setItem(int index, ItemStack stack) {
            inventory_.setItem(offset_ + index, stack);
        }

        @Override
        public int getMaxStackSize() {
            return Math.min(max_stack_size_, inventory_.getMaxStackSize());
        }

        @Override
        public void setChanged() {
            inventory_.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return inventory_.stillValid(player);
        }

        @Override
        public void startOpen(Player player) {
            inventory_.startOpen(player);
        }

        @Override
        public void stopOpen(Player player) {
            inventory_.stopOpen(player);
        }

        @Override
        public boolean canPlaceItem(int index, ItemStack stack) {
            return validator_.test(offset_ + index, stack) && inventory_.canPlaceItem(offset_ + index, stack);
        }

        public Iterator<ItemStack> iterator() {
            return new InventoryRangeIterator(this);
        }

        public static class InventoryRangeIterator implements Iterator<ItemStack> {
            private final InventoryRange parent_;
            private int index = 0;

            public InventoryRangeIterator(InventoryRange range) {
                parent_ = range;
            }

            public boolean hasNext() {
                return index < parent_.size_;
            }

            public ItemStack next() {
                if (index >= parent_.size_) throw new NoSuchElementException();
                return parent_.getItem(index++);
            }
        }

        public ItemStack extract(int amount, boolean random, boolean simulate) {
            ItemStack out_stack = ItemStack.EMPTY;
            int offset = random ? (int) (Math.random() * size_) : 0;
            for (int k = 0; k < size_; ++k) {
                int i = (offset + k) % size_;
                final ItemStack stack = getItem(i);
                if (stack.isEmpty()) continue;
                if (out_stack.isEmpty()) {
                    if (stack.getCount() < amount) {
                        out_stack = stack;
                        if (!simulate) setItem(i, ItemStack.EMPTY);
                        if (!out_stack.isStackable()) break;
                        amount -= out_stack.getCount();
                    } else {
                        if (!simulate) {
                            out_stack = stack.split(amount);
                        } else {
                            out_stack = stack.copy();
                            out_stack.setCount(amount);
                        }
                        break;
                    }
                } else if (areItemStacksIdentical(stack, out_stack)) {
                    if (stack.getCount() <= amount) {
                        out_stack.grow(stack.getCount());
                        amount -= stack.getCount();
                        if (!simulate) setItem(i, ItemStack.EMPTY);
                    } else {
                        out_stack.grow(amount);
                        if (!simulate) {
                            stack.shrink(amount);
                            if (stack.isEmpty()) setItem(i, ItemStack.EMPTY);
                        }
                        break;
                    }
                }
            }
            if ((!out_stack.isEmpty()) && (!simulate)) setChanged();
            return out_stack;
        }

        public ItemStack extract(final ItemStack request_stack, boolean simulate) {
            if (request_stack.isEmpty()) return ItemStack.EMPTY;
            List<ItemStack> matches = new ArrayList<>();
            for (int i = 0; i < size_; ++i) {
                final ItemStack stack = getItem(i);
                if ((!stack.isEmpty()) && (areItemStacksIdenticalIgnoreDamage(stack, request_stack))) {
                    matches.add(stack);
                }
            }
            matches.sort(Comparator.comparingInt(ItemStack::getCount));
            if (matches.isEmpty()) return ItemStack.EMPTY;
            if (!simulate) {
                int n_left = request_stack.getCount();
                ItemStack fetched_stack = matches.getFirst().split(n_left);
                n_left -= fetched_stack.getCount();
                for (int i = 1; (i < matches.size()) && (n_left > 0); ++i) {
                    ItemStack stack = matches.get(i).split(n_left);
                    n_left -= stack.getCount();
                    fetched_stack.grow(stack.getCount());
                }
                return checked(fetched_stack);
            } else {
                int amount = 0;
                for (ItemStack match : matches) amount += match.getCount();
                if (amount == 0) return ItemStack.EMPTY;
                final ItemStack stack = request_stack.copy();
                if (amount < stack.getCount()) stack.setCount(amount);
                return stack;
            }
        }
    }

    public static void give(Player entity, ItemStack stack) {
        entity.getInventory().placeItemBackInInventory(stack);
    }
}