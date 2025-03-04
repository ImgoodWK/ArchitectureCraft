// -----------------------------------------------------------------
//
// ArchitectureCraft - Base class for special shape renderers
//
// -----------------------------------------------------------------

package gcewing.architecture;

import static gcewing.architecture.BaseBlockUtils.*;
import static gcewing.architecture.BaseUtils.*;
import static gcewing.architecture.Shape.*;

import net.minecraft.block.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

import gcewing.architecture.BaseModClient.*;

public abstract class RenderShape {

    protected IBlockAccess blockWorld;
    protected BlockPos blockPos;
    protected ShapeTE te;
    protected ITexture[] textures;
    protected Trans3 t;
    protected IRenderTarget target;

    public RenderShape(ShapeTE te, ITexture[] textures, Trans3 t, IRenderTarget target) {
        this.te = te;
        this.blockWorld = getTileEntityWorld(te);
        this.blockPos = te.getPos();
        this.textures = textures;
        this.t = t;
        this.target = target;
    }

    protected abstract void render();

    protected IModel getModel(String name) {
        return ArchitectureCraft.mod.client.getModel(name);
    }

    // protected TileEntity getTileEntityInGlobalDir(EnumFacing gdir) {
    // if (blockWorld != null)
    // return blockWorld.getTileEntity(blockPos.offset(gdir));
    // else
    // return null;
    // }
    //
    // protected ShapeTE getShapeTEInGlobalDir(EnumFacing gdir) {
    // TileEntity te = getTileEntityInGlobalDir(gdir);
    // if (te instanceof ShapeTE)
    // return (ShapeTE)te;
    // else
    // return null;
    // }

}
