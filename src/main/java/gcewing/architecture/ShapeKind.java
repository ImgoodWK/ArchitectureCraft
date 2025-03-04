// ------------------------------------------------------------------------------
//
// ArchitectureCraft - Shape kinds
//
// ------------------------------------------------------------------------------

package gcewing.architecture;

import static gcewing.architecture.BaseBlockUtils.*;
import static gcewing.architecture.BaseDirections.*;
import static gcewing.architecture.BaseUtils.*;
import static java.lang.Math.*;

import java.util.*;

import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

import gcewing.architecture.BaseModClient.*;

// ------------------------------------------------------------------------------

public abstract class ShapeKind {

    public Object[] profiles; // indexed by local face

    public Object profileForLocalFace(Shape shape, EnumFacing face) {
        if (profiles != null) return profiles[face.ordinal()];
        else return null;
    }

    public double placementOffsetX() {
        return 0;
    }

    public abstract void renderShape(ShapeTE te, ITexture[] textures, IRenderTarget target, Trans3 t,
            boolean renderBase, boolean renderSecondary);

    public ItemStack newStack(Shape shape, IBlockState materialState, int stackSize) {
        return newStack(shape, materialState.getBlock(), getMetaFromBlockState(materialState), stackSize);
    }

    public ItemStack newStack(Shape shape, IBlockState materialState, int stackSize, boolean shaderEmissive) {
        return newStack(
                shape,
                materialState.getBlock(),
                getMetaFromBlockState(materialState),
                stackSize,
                shaderEmissive);
    }

    public ItemStack newStack(Shape shape, Block materialBlock, int materialMeta, int stackSize) {
        ShapeTE te = new ShapeTE(shape, materialBlock, materialMeta);
        int light = materialBlock.getLightValue();
        ItemStack result = BaseTileEntity.blockStackWithTileEntity(ArchitectureCraft.blockShape, stackSize, light, te);
        return result;
    }

    public ItemStack newStack(Shape shape, Block materialBlock, int materialMeta, int stackSize,
            boolean shaderEmissive) {
        ShapeTE te = new ShapeTE(shape, materialBlock, materialMeta);
        int light = materialBlock.getLightValue();
        if (shaderEmissive) {
            light = 15;
            ItemStack result = BaseTileEntity
                    .blockStackWithTileEntity(ArchitectureCraft.blockShapeSE, stackSize, light, te);
            return result;
        }
        ItemStack result = BaseTileEntity.blockStackWithTileEntity(ArchitectureCraft.blockShape, stackSize, light, te);
        return result;
    }

    public boolean orientOnPlacement(EntityPlayer player, ShapeTE te, BlockPos npos, IBlockState nstate, TileEntity nte,
            EnumFacing otherFace, Vector3 hit) {
        if (nte instanceof ShapeTE) return orientOnPlacement(player, te, (ShapeTE) nte, otherFace, hit);
        else return orientOnPlacement(player, te, null, otherFace, hit);
    }

    public boolean orientOnPlacement(EntityPlayer player, ShapeTE te, ShapeTE nte, EnumFacing otherFace, Vector3 hit) {
        // boolean debug = !te.getWorld().isRemote;
        if (nte != null && !player.isSneaking()) {
            Object otherProfile = Profile.getProfileGlobal(nte.shape, nte.side, nte.turn, otherFace);
            if (otherProfile != null) {
                EnumFacing thisFace = oppositeFacing(otherFace);
                for (int i = 0; i < 4; i++) {
                    int turn = (nte.turn + i) & 3;
                    Object thisProfile = Profile.getProfileGlobal(te.shape, nte.side, turn, thisFace);
                    if (Profile.matches(thisProfile, otherProfile)) {
                        // if (debug)
                        // System.out.printf("ShapeKind.orientOnPlacement: side %s turn %s\n", nte.side, turn);
                        te.setSide(nte.side);
                        te.setTurn(turn);
                        te.setOffsetX(nte.getOffsetX());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean canPlaceUpsideDown() {
        return true;
    }

    public double sideZoneSize() {
        return 1 / 4d;
    }

    public boolean highlightZones() {
        return false;
    }

    public void onChiselUse(ShapeTE te, EntityPlayer player, EnumFacing face, Vector3 hit) {
        EnumFacing side = zoneHit(face, hit);
        // System.out.printf("ShapeKind.onChiselUse: face = %s, hit = %s, side = %s\n", face, hit, side);
        if (side != null) chiselUsedOnSide(te, player, side);
        else chiselUsedOnCentre(te, player);
    }

    public void chiselUsedOnSide(ShapeTE te, EntityPlayer player, EnumFacing side) {
        te.toggleConnectionGlobal(side);
    }

    public void chiselUsedOnCentre(ShapeTE te, EntityPlayer player) {
        if (te.secondaryBlockState != null) {
            ItemStack stack = newSecondaryMaterialStack(te.secondaryBlockState);
            if (stack != null) {
                if (!Utils.playerIsInCreativeMode(player)) {
                    BaseBlock block = (BaseBlock) te.getBlockType();
                    block.spawnAsEntity(getTileEntityWorld(te), getTileEntityPos(te), stack);
                }
                te.setSecondaryMaterial(null);
            }
        }
    }

    protected ItemStack newSecondaryMaterialStack(IBlockState state) {
        if (acceptsCladding()) return ArchitectureCraft.itemCladding.newStack(state, 1);
        else return null;
    }

    public void onHammerUse(ShapeTE te, EntityPlayer player, EnumFacing face, Vector3 hit) {
        // System.out.printf("ShapeKind.onHammerUse\n");
        if (player.isSneaking()) te.setSide((te.side + 1) % 6);
        else {
            double dx = te.getOffsetX();
            if (dx != 0) {
                dx = -dx;
                te.setOffsetX(dx);
            }
            if (dx >= 0) te.setTurn((te.turn + 1) % 4);
        }
        te.markChanged();
    }

    public EnumFacing zoneHit(EnumFacing face, Vector3 hit) {
        double r = 0.5 - sideZoneSize();
        int dir = face.ordinal();
        // System.out.printf("ShapeKind.zoneHit: hit = (%.3f,%.3f,%.3f) r = %.3f\n",
        // hit.x, hit.y, hit.z, r);
        if (hit.x <= -r && dir != WEST) return F_WEST;
        if (hit.x >= r && dir != EAST) return F_EAST;
        if (hit.y <= -r && dir != DOWN) return F_DOWN;
        if (hit.y >= r && dir != UP) return F_UP;
        if (hit.z <= -r && dir != NORTH) return F_NORTH;
        if (hit.z >= r && dir != SOUTH) return F_SOUTH;
        return null;
    }

    public boolean acceptsCladding() {
        return false;
    }

    public boolean isValidSecondaryMaterial(IBlockState state) {
        return false;
    }

    public boolean secondaryDefaultsToBase() {
        return false;
    }

    public AxisAlignedBB getBounds(ShapeTE te, IBlockAccess world, BlockPos pos, IBlockState state, Entity entity,
            Trans3 t) {
        List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
        addCollisionBoxesToList(te, world, pos, state, entity, t, list);
        return Utils.unionOfBoxes(list);
    }

    public void addCollisionBoxesToList(ShapeTE te, IBlockAccess world, BlockPos pos, IBlockState state, Entity entity,
            Trans3 t, List list) {
        int mask = te.shape.occlusionMask;
        int param = mask & 0xff;
        double r, h;
        switch (mask & 0xff00) {
            case 0x000: // 2x2x2 cubelet bitmap
                for (int i = 0; i < 8; i++) if ((mask & (1 << i)) != 0) {
                    Vector3 p = new Vector3(
                            (i & 1) != 0 ? 0.5 : -0.5,
                            (i & 4) != 0 ? 0.5 : -0.5,
                            (i & 2) != 0 ? 0.5 : -0.5);
                    addBox(Vector3.zero, p, t, list);
                }
                break;
            case 0x100: // Square, full size in Y
                r = param / 16.0;
                addBox(new Vector3(-r, -0.5, -r), new Vector3(r, 0.5, r), t, list);
                break;
            case 0x200: // Slab, full size in X and Y
                r = param / 32.0;
                addBox(new Vector3(-0.5, -0.5, -r), new Vector3(0.5, 0.5, r), t, list);
                break;
            case 0x300: // Slab in back corner
                r = ((param & 0xf) + 1) / 16.0; // width and length of slab
                h = ((param >> 4) + 1) / 16.0; // height of slab from bottom
                addBox(new Vector3(-0.5, -0.5, 0.5 - r), new Vector3(-0.5 + r, -0.5 + h, 0.5), t, list);
                break;
            case 0x400: // Slab at back
            case 0x500: // Slabs at back and right
                r = ((param & 0xf) + 1) / 16.0; // thickness of slab
                h = ((param >> 4) + 1) / 16.0; // height of slab from bottom
                addBox(new Vector3(-0.5, -0.5, 0.5 - r), new Vector3(0.5, -0.5 + h, 0.5), t, list);
                if ((mask & 0x100) != 0)
                    addBox(new Vector3(-0.5, -0.5, -0.5), new Vector3(-0.5 + r, -0.5 + h, 0.5), t, list);
                break;
            default: // Full cube
                addBox(new Vector3(-0.5, -0.5, -0.5), new Vector3(0.5, 0.5, 0.5), t, list);
        }
    }

    protected void addBox(Vector3 p0, Vector3 p1, Trans3 t, List list) {
        // addBox(t.p(p0), t.p(p1), list);
        t.addBox(p0, p1, list);
    }

    // ------------------------------------------------------------------------------

    public static Roof Roof = new Roof();

    public static class Roof extends ShapeKind {

        // static boolean debugPlacement = true;

        @Override
        public boolean acceptsCladding() {
            return true;
        }

        @Override
        public boolean secondaryDefaultsToBase() {
            return true;
        }

        public void renderShape(ShapeTE te, ITexture[] textures, IRenderTarget target, Trans3 t, boolean renderBase,
                boolean renderSecondary) {
            new RenderRoof(te, textures, t, target, renderBase, renderSecondary).render();
        }

        // @Override
        // public boolean orientOnPlacement(EntityPlayer player, ShapeTE te, ShapeTE nte, EnumFacing face,
        // Vector3 hit)
        // {
        //// if (!te.getWorld().isRemote)
        //// System.out.printf("Roof.orientOnPlacement\n");
        // if (!player.isSneaking() && nte != null && nte.shape.kind instanceof Roof) {
        // EnumFacing nlf = nte.localFace(face);
        // Profile np = profileForLocalFace(nte.shape, nlf);
        // Profile p = opposite(np);
        // EnumFacing lf = localFaceForProfile(te.shape, p);
        // if (lf != null) {
        // int turn = BaseUtils.turnToFace(lf, face.getOpposite());
        // if (debugPlacement && !te.getWorld().isRemote) {
        // System.out.printf(
        // "Roof.orientOnPlacement: Aligning profile %s on local side %s of neighbour " +
        // "with profile %s on local side %s\n", np, nlf, p, lf);
        // System.out.printf("Roof.orientOnPlacement: Turning local side %s to face global direction %s\n",
        // lf, face.getOpposite());
        // System.out.printf("Roof.orientOnPlacement: side %s turn %s\n", nte.side, turn);
        // }
        // te.setSide(nte.side);
        // te.setTurn(turn);
        // return true;
        // }
        // }
        // return false;
        // }

        protected enum RoofProfile {
            None,
            Left,
            Right,
            Ridge,
            Valley
        };

        static {
            Profile.declareOpposite(RoofProfile.Left, RoofProfile.Right);
        }

        // protected RoofProfile opposite(RoofProfile p) {
        // switch (p) {
        // case Left: return RoofProfile.Right;
        // case Right: return RoofProfile.Left;
        // }
        // return p;
        // }

        @Override
        public Object profileForLocalFace(Shape shape, EnumFacing face) {
            int dir = face.ordinal();
            switch (shape) {
                case RoofTile:
                case RoofOverhang:
                    switch (dir) {
                        case EAST:
                            return RoofProfile.Left;
                        case WEST:
                            return RoofProfile.Right;
                    }
                    break;
                case RoofOuterCorner:
                case RoofOverhangOuterCorner:
                    switch (dir) {
                        case SOUTH:
                            return RoofProfile.Left;
                        case WEST:
                            return RoofProfile.Right;
                    }
                    break;
                case RoofInnerCorner:
                case RoofOverhangInnerCorner:
                    switch (dir) {
                        case EAST:
                            return RoofProfile.Left;
                        case NORTH:
                            return RoofProfile.Right;
                    }
                    break;
                case RoofRidge:
                case RoofSmartRidge:
                case RoofOverhangRidge:
                    return RoofProfile.Ridge;
                case RoofValley:
                case RoofSmartValley:
                case RoofOverhangValley:
                    return RoofProfile.Valley;
            }
            return RoofProfile.None;
        }

    }

    // ------------------------------------------------------------------------------

    public static Model Model(String name) {
        return new Model(name, null);
    }

    public static Model Model(String name, Object[] profiles) {
        return new Model(name, profiles);
    }

    public static class Model extends ShapeKind {

        protected String modelName;
        private IModel model;

        public Model(String name, Object[] profiles) {
            this.modelName = "shape/" + name + ".smeg";
            this.profiles = profiles;
        }

        @Override
        public boolean secondaryDefaultsToBase() {
            return true;
        }

        @Override
        public AxisAlignedBB getBounds(ShapeTE te, IBlockAccess world, BlockPos pos, IBlockState state, Entity entity,
                Trans3 t) {
            return t.t(getModel().getBounds());
        }

        public void renderShape(ShapeTE te, ITexture[] textures, IRenderTarget target, Trans3 t, boolean renderBase,
                boolean renderSecondary) {
            IModel model = getModel();
            model.render(t, target, textures);
        }

        protected IModel getModel() {
            if (model == null) model = ArchitectureCraft.mod.getModel(modelName);
            return model;
        }

        @Override
        public boolean acceptsCladding() {
            BaseModel model = (BaseModel) getModel();
            for (BaseModel.Face face : model.faces) if (face.texture >= 2) return true;
            return false;
        }

        @Override
        public void addCollisionBoxesToList(ShapeTE te, IBlockAccess world, BlockPos pos, IBlockState state,
                Entity entity, Trans3 t, List list) {
            if (te.shape.occlusionMask == 0) getModel().addBoxesToList(t, list);
            else super.addCollisionBoxesToList(te, world, pos, state, entity, t, list);
        }

        @Override
        public double placementOffsetX() {
            List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
            getModel().addBoxesToList(Trans3.ident, list);
            AxisAlignedBB bounds = Utils.unionOfBoxes(list);
            if (Shape.debugPlacement) {
                for (AxisAlignedBB box : list) System.out.printf("ShapeKind.Model.placementOffsetX: %s\n", box);
                System.out.printf("ShapeKind.Model.placementOffsetX: bounds = %s\n", bounds);
            }
            return 0.5 * (1 - (bounds.maxX - bounds.minX));
        }

    }

    // ------------------------------------------------------------------------------

    public static abstract class Window extends ShapeKind {

        public enum FrameKind {
            None,
            Plain,
            Corner
        };

        public EnumFacing[] frameSides;
        public boolean[] frameAlways;
        public FrameKind[] frameKinds;
        public EnumFacing[] frameOrientations;
        public Trans3[] frameTrans;

        @Override
        public boolean orientOnPlacement(EntityPlayer player, ShapeTE te, ShapeTE nte, EnumFacing otherFace,
                Vector3 hit) {
            int turn = -1;
            // If click is on side of a non-window block, orient perpendicular to it
            if (!player.isSneaking() && (nte == null || !(nte.shape.kind instanceof ShapeKind.Window))) {
                switch (otherFace.ordinal()) {
                    case EAST:
                    case WEST:
                        turn = 0;
                        break;
                    case NORTH:
                    case SOUTH:
                        turn = 1;
                        break;
                }
            }
            if (turn >= 0) {
                te.setSide(0);
                te.setTurn(turn);
                return true;
            } else return false;
        }

        public FrameKind frameKindForLocalSide(EnumFacing side) {
            return frameKinds[side.ordinal()];
        }

        public EnumFacing frameOrientationForLocalSide(EnumFacing side) {
            return frameOrientations[side.ordinal()];
        }

        @Override
        public boolean canPlaceUpsideDown() {
            return false;
        }

        @Override
        public double sideZoneSize() {
            return 1 / 8d; // 3/32d;
        }

        @Override
        public boolean highlightZones() {
            return true;
        }

        public void renderShape(ShapeTE te, ITexture[] textures, IRenderTarget target, Trans3 t, boolean renderBase,
                boolean renderSecondary) {
            new RenderWindow(te, textures, t, target, renderBase, renderSecondary).render();
        }

        // @Override
        // public void chiselUsedOnCentre(ShapeTE te, EntityPlayer player) {
        // if (te.secondaryBlockState != null) {
        // ItemStack stack = BaseUtils.blockStackWithState(te.secondaryBlockState, 1);
        // dropSecondaryMaterial(te, player, stack);
        // }
        // }

        @Override
        protected ItemStack newSecondaryMaterialStack(IBlockState state) {
            return BaseBlockUtils.blockStackWithState(state, 1);
        }

        @Override
        public boolean isValidSecondaryMaterial(IBlockState state) {
            Block block = state.getBlock();
            return block == Blocks.glass_pane || block == Blocks.stained_glass_pane;
        }

        @Override
        public void addCollisionBoxesToList(ShapeTE te, IBlockAccess world, BlockPos pos, IBlockState state,
                Entity entity, Trans3 t, List list) {
            final double r = 1 / 8d, s = 3 / 32d;
            double[] e = new double[4];
            addCentreBoxesToList(r, s, t, list);
            for (int i = 0; i <= 3; i++) {
                boolean frame = frameAlways[i] || !isConnectedGlobal(te, t.t(frameSides[i]));
                if (entity == null || frame) {
                    Trans3 ts = t.t(frameTrans[i]);
                    addFrameBoxesToList(i, r, s, ts, list);
                }
                e[i] = frame ? 0.5 - r : 0.5;
            }
            if (te.secondaryBlockState != null) addGlassBoxesToList(r, s, 1 / 32d, e, t, list);
        }

        protected void addCentreBoxesToList(double r, double s, Trans3 t, List list) {}

        protected void addFrameBoxesToList(int i, double r, double s, Trans3 ts, List list) {
            ts.addBox(-0.5, -0.5, -s, 0.5, -0.5 + r, s, list);
        }

        protected void addGlassBoxesToList(double r, double s, double w, double e[], Trans3 t, List list) {
            t.addBox(-e[3], -e[0], -w, e[1], e[2], w, list);
        }

        protected boolean isConnectedGlobal(ShapeTE te, EnumFacing globalDir) {
            return getConnectedWindowGlobal(te, globalDir) != null;
        }

        public ShapeTE getConnectedWindowGlobal(ShapeTE te, EnumFacing globalDir) {
            EnumFacing thisLocalDir = te.localFace(globalDir);
            FrameKind thisFrameKind = frameKindForLocalSide(thisLocalDir);
            if (thisFrameKind != FrameKind.None) {
                EnumFacing thisOrient = frameOrientationForLocalSide(thisLocalDir);
                ShapeTE nte = te.getConnectedNeighbourGlobal(globalDir);
                if (nte != null && nte.shape.kind instanceof Window) {
                    Window otherKind = (Window) nte.shape.kind;
                    EnumFacing otherLocalDir = nte.localFace(oppositeFacing(globalDir));
                    FrameKind otherFrameKind = otherKind.frameKindForLocalSide(otherLocalDir);
                    if (otherFrameKind != FrameKind.None) {
                        EnumFacing otherOrient = otherKind.frameOrientationForLocalSide(otherLocalDir);
                        if (framesMatch(
                                thisFrameKind,
                                otherFrameKind,
                                te.globalFace(thisOrient),
                                nte.globalFace(otherOrient)))
                            return nte;
                    }
                }
            }
            return null;
        }

        protected boolean framesMatch(FrameKind kind1, FrameKind kind2, EnumFacing orient1, EnumFacing orient2) {
            if (kind1 == kind2) {
                switch (kind1) {
                    case Plain:
                        return facingAxesEqual(orient1, orient2);
                    default:
                        return orient1 == orient2;
                }
            }
            return false;
        }

        // protected EnumFacing getFrameOrientationGlobal(ShapeTE te, EnumFacing globalDir) {
        // Trans3 t = te.localToGlobalRotation();
        // EnumFacing localDir = t.it(globalDir);
        // return frameOrientations[localDir.ordinal()];
        // }

    }

    // ------------------------------------------------------------------------------

    public static Cladding Cladding = new Cladding();

    public static class Cladding extends ShapeKind {

        public void renderShape(ShapeTE te, ITexture[] textures, IRenderTarget target, Trans3 t, boolean renderBase,
                boolean renderSecondary) {}

        public ItemStack newStack(Shape shape, Block materialBlock, int materialMeta, int stackSize) {
            return ArchitectureCraft.itemCladding.newStack(materialBlock, materialMeta, stackSize);
        }

    }

    // ------------------------------------------------------------------------------

    public static Model Banister(String name) {
        return new Banister(name);
    }

    public static class Banister extends Model {

        public Banister(String modelName) {
            super(modelName, Profile.Generic.tbOffset);
        }

        public boolean orientOnPlacement(EntityPlayer player, ShapeTE te, BlockPos npos, IBlockState nstate,
                TileEntity nte, EnumFacing otherFace, Vector3 hit) {
            // System.out.printf("Banister.orientOnPlacement: nstate = %s\n", nstate);
            if (!player.isSneaking()) {
                Block nblock = nstate.getBlock();
                boolean placedOnStair = false;
                int nside = -1; // Side that the neighbouring block is placed on
                int nturn = -1; // Turn of the neighbouring block
                if (nblock instanceof BlockStairs && (otherFace == F_UP || otherFace == F_DOWN)) {
                    placedOnStair = true;
                    nside = stairsSide(nstate);
                    nturn = BaseUtils.turnToFace(F_SOUTH, stairsFacing(nstate));
                    if (nside == 1 && (nturn & 1) == 0) nturn ^= 2;
                } else if (nblock instanceof ShapeBlock) {
                    if (nte instanceof ShapeTE) {
                        placedOnStair = true;
                        nside = ((ShapeTE) nte).side;
                        nturn = ((ShapeTE) nte).turn;
                    }
                }
                if (placedOnStair) {
                    int side = oppositeFacing(otherFace).ordinal();
                    if (side == nside) {
                        Vector3 h = Trans3.sideTurn(side, 0).ip(hit);
                        double offx = te.shape.offsetXForPlacementHit(side, nturn, hit);
                        te.setSide(side);
                        te.setTurn(nturn & 3);
                        te.setOffsetX(offx);
                        return true;
                    }
                }
            }
            return super.orientOnPlacement(player, te, npos, nstate, nte, otherFace, hit);
        }

        private static EnumFacing stairsFacingMap[] = { F_WEST, F_EAST, F_SOUTH, F_NORTH };

        private static EnumFacing stairsFacing(IBlockState state) {
            int meta = getMetaFromBlockState(state);
            return stairsFacingMap[meta & 3];
        }

        private static int stairsSide(IBlockState state) {
            int meta = getMetaFromBlockState(state);
            return (meta >> 2) & 1;
        }

        @Override
        public double placementOffsetX() {
            return 6 / 16d;
        }

    }

    // ------------------------------------------------------------------------------

}
