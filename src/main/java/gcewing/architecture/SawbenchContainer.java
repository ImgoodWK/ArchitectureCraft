// ------------------------------------------------------------------------------
//
// ArchitectureCraft - SawbenchContainer
//
// ------------------------------------------------------------------------------

package gcewing.architecture;

import static gcewing.architecture.BaseBlockUtils.*;
import static gcewing.architecture.BaseUtils.*;

import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.network.play.server.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;

import gcewing.architecture.BaseDataChannel.*;

public class SawbenchContainer extends BaseContainer {

    public static int guWidth = 242;
    public static int guiHeight = 224;
    public static int inputSlotLeft = 12;
    public static int inputSlotTop = 19;
    public static int outputSlotLeft = 12;
    public static int outputSlotTop = 57;

    SawbenchTE te;
    SlotRange sawbenchSlotRange;
    Slot materialSlot, resultSlot;

    public static Container create(EntityPlayer player, World world, BlockPos pos) {
        TileEntity te = getWorldTileEntity(world, pos);
        if (te instanceof SawbenchTE) return new SawbenchContainer(player, (SawbenchTE) te);
        else return null;
    }

    public SawbenchContainer(EntityPlayer player, SawbenchTE te) {
        super(guWidth, guiHeight);
        this.te = te;
        sawbenchSlotRange = new SlotRange();
        materialSlot = addSlotToContainer(new Slot(te, 0, inputSlotLeft, inputSlotTop));
        sawbenchSlotRange.end();
        resultSlot = addSlotToContainer(new SlotSawbenchResult(te, 1, outputSlotLeft, outputSlotTop));
        addPlayerSlots(player, 8, guiHeight - 81);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.te.isUseableByPlayer(player);
    }

    @Override
    protected SlotRange transferSlotRange(int srcSlotIndex, ItemStack stack) {
        if (playerSlotRange.contains(srcSlotIndex)) return sawbenchSlotRange;
        else return playerSlotRange;
    }

    //
    // Server
    //

    @Override
    public void detectAndSendChanges() {
        for (int i = 0; i < this.inventorySlots.size(); ++i) {
            ItemStack newstack = ((Slot) this.inventorySlots.get(i)).getStack();
            ItemStack oldstack = (ItemStack) this.inventoryItemStacks.get(i);
            if (!ItemStack.areItemStacksEqual(oldstack, newstack)) {
                oldstack = newstack == null ? null : newstack.copy();
                this.inventoryItemStacks.set(i, oldstack);
                for (Object crafter : crafters) {
                    if (crafter instanceof EntityPlayerMP) {
                        // System.out.printf("SawbenchContainer.updateCraftingResults: sending %s in slot %d to
                        // player\n", newstack, i);
                        ((EntityPlayerMP) crafter).playerNetServerHandler
                                .sendPacket(new S2FPacketSetSlot(windowId, i, newstack));
                    } else((ICrafting) crafter).sendSlotContents(this, i, newstack);
                }
            }
        }
        te.updateResultSlot();// update the result slot to make shift clicking new material in work smoothly
    }

    @ServerMessageHandler("SelectShape")
    public void onSelectShape(EntityPlayer player, ChannelInput data) {
        int page = data.readInt();
        int slot = data.readInt();
        te.setSelectedShape(page, slot);
    }

    //
    // Client
    //

    @Override
    public void putStackInSlot(int i, ItemStack stack) {
        // Slot update packet has arrived from server. Do not trigger crafting behaviour.
        Slot slot = getSlot(i);
        if (slot instanceof SlotSawbench) {
            // System.out.printf("SawbenchContainer.putStackInSlot: %d %s on %s\n", i, stack, te.worldObj);
            ((SlotSawbench) slot).updateFromServer(stack);
        } else super.putStackInSlot(i, stack);
    }

    // Default transferStackInSlot does not invoke decrStackSize, so we need this
    // to get pending material used.
    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        Slot slot = getSlot(index);
        if (slot == resultSlot) return transferStackInResultSlot(player, index);
        else return super.transferStackInSlot(player, index);
    }

    protected ItemStack transferStackInResultSlot(EntityPlayer player, int index) {
        // if (!te.getWorld().isRemote)
        // System.out.printf("SawbenchContainer.transferStackInSlot: %s material %s result %s\n",
        // index, te.getStackInSlot(te.materialSlot), te.getStackInSlot(te.resultSlot));
        boolean materialWasPending = te.pendingMaterialUsage;
        ItemStack origMaterialStack = te.usePendingMaterial();
        ItemStack result = super.transferStackInSlot(player, index);
        // if (!te.getWorld().isRemote)
        // System.out.printf(
        // "SawbenchContainer.transferStackInSlot: returning %s material %s result %s\n",
        // result, te.getStackInSlot(te.materialSlot), te.getStackInSlot(te.resultSlot));
        if (materialWasPending) te.returnUnusedMaterial(origMaterialStack);
        return result;
    }

}

// ------------------------------------------------------------------------------

class SlotSawbench extends Slot {

    SawbenchTE te;
    int index;

    public SlotSawbench(SawbenchTE te, int index, int x, int y) {
        super(te, index, x, y);
        this.te = te;
        this.index = index;
    }

    void updateFromServer(ItemStack stack) {
        te.inventory.setInventorySlotContents(index, stack);
    }

}

// ------------------------------------------------------------------------------

class SlotSawbenchResult extends SlotSawbench {

    public SlotSawbenchResult(SawbenchTE te, int index, int x, int y) {
        super(te, index, x, y);
    }

    @Override
    public boolean isItemValid(ItemStack newstack) {
        return false;
    }

}
