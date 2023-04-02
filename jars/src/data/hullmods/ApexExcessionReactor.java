package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.ApexUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;
import plugins.ApexExcessionRenderPlugin;
import plugins.ApexModPlugin;

import java.awt.*;
import java.util.*;

import static data.ApexUtils.text;
import static data.shipsystems.ApexExcessionSystem.CHARGE_MULT;
import static plugins.ApexModPlugin.POTATO_MODE;

public class ApexExcessionReactor extends BaseHullMod
{
    public static final float BASE_CHARGE_RATE = 250f;
    public static final float PHASE_CHARGE_MULT = 1f; // not used atm, only charges while phased
    public static final float DAMAGE_CHARGE_MULT = 0.2f;

    public static final float MAX_STORED_CHARGE = 3000f; // can "store" up to this much
    public static final float ARC_RANGE = 400f;
    public static final float ARC_FIGHTER_DAMAGE = 250f;
    public static final float FIGHTER_WEIGHT = 150f; // fighter value for weighting
    private static final float DAMAGE_CUTOFF = 50f; // intercepts if projectile deals this much HE damage (reduced for other types)

    public static final float ARC_SIPHON_AMOUNT = 25f; // armor stored per hit
    public static final float MAX_STORED_ARMOR = 1000f;
    public static final float REPAIR_RATE = 15f; // flat armor repaired per second

    public static final Color CHARGE_COLOR = new Color(89, 170, 255);

    public static final Color REMOVE_COLOR = new Color(0,157,255,155);

    public static final HashMap<ShipAPI, Float> chargeMap = new HashMap<>(); // tracks stored damage, in case there's more than one of these things
    public static final HashMap<ShipAPI, Float> repairMap = new HashMap<>(); // tracks stored repair
    public static final HashMap<ShipAPI, Float> dpTimeMap = new HashMap<>();
    public static final HashMap<ShipAPI, ApexExcessionRenderPlugin> pluginMap = new HashMap<>();

    public static final WeightedRandomPicker<Vector2f> arcOrigins = new WeightedRandomPicker<>();

    static
    {
        // first number is front (+)/back (-) on ship model
        arcOrigins.add(new Vector2f(-100, 50));
        arcOrigins.add(new Vector2f(-100, -50));
        arcOrigins.add(new Vector2f(-100, 100));
        arcOrigins.add(new Vector2f(-100, -100));
        arcOrigins.add(new Vector2f(-100, 25));
        arcOrigins.add(new Vector2f(-100, -25));
    }

    private int lastEngineHashcode = 0;

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        ship.addListener(new ApexExcessionChargeListener());
    }

    public static class ApexExcessionChargeListener implements DamageDealtModifier
    {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            if (target instanceof ShipAPI && ((ShipAPI) target).isHulk())
                return null;
            if (param instanceof DamagingProjectileAPI)
            {
                float chargeAmount = damage.getDamage();
                DamagingProjectileAPI proj = (DamagingProjectileAPI)param;
                if (proj.getSource().getSystem().isActive())
                    chargeAmount *= CHARGE_MULT;
                addCharge(proj.getSource(), chargeAmount);
                if (Misc.shouldShowDamageFloaty(proj.getSource(), proj.getSource()) && proj.getSource().getSystem().isActive() && proj.getWeapon() != null)
                {
                    Global.getCombatEngine().addFloatingDamageText(proj.getWeapon().getLocation(), chargeAmount * DAMAGE_CHARGE_MULT, Color.MAGENTA, proj.getSource(), proj.getSource());
                }
            }
            return null;
        }

        private void addCharge(ShipAPI source, float damageAmount)
        {
            chargeMap.put(source, Math.min(chargeMap.get(source) + damageAmount * DAMAGE_CHARGE_MULT, MAX_STORED_CHARGE));
        }
    }

    @Override
    public int getDisplayCategoryIndex()
    {
        return -1;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec)
    {
        if (ship != null)
        {
            float pad = 10f;
            tooltip.addSectionHeading(text("Details"), Alignment.MID, pad);
            tooltip.addPara("\n• " + text("excb1"), 0,
                    CHARGE_COLOR,
                    text("excb2"));
            tooltip.addPara("• " + text("excb3"), 0, CHARGE_COLOR, text("excb4"));
            tooltip.addPara("• " + text("excb5"), 0, CHARGE_COLOR, text("excb4"));
            Color[] colors = {Misc.getHighlightColor(), CHARGE_COLOR};
            tooltip.addPara("• " + text("excb6"), 0, colors, text("excb7"),text("excb4"));
            tooltip.addPara("• " + text("excb8"),
                    0,
                    Misc.getHighlightColor(),
                    (int) (REPAIR_RATE) + "");
            tooltip.addPara("• " + text("excb9"), 0, Misc.getHighlightColor(), text("excb10"));
            tooltip.addPara("• " + text("excb11"), 0, Misc.getHighlightColor(), text("excb12"));
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount)
    {
        if (lastEngineHashcode != Global.getCombatEngine().hashCode())
        {
            chargeMap.clear();
            repairMap.clear();
            dpTimeMap.clear();
            pluginMap.clear();
            lastEngineHashcode = Global.getCombatEngine().hashCode();
        }
        if (!ship.isAlive() || ship.isHulk())
            return;
        // update maps first
        if (!chargeMap.containsKey(ship))
            chargeMap.put(ship, MAX_STORED_CHARGE);
        if (!repairMap.containsKey(ship))
            repairMap.put(ship, 0f);
        if (!dpTimeMap.containsKey(ship))
            dpTimeMap.put(ship, 0f);
        if (!pluginMap.containsKey(ship))
        {
            ApexExcessionRenderPlugin plugin = new ApexExcessionRenderPlugin(ship);
            pluginMap.put(ship, plugin);
            Global.getCombatEngine().addLayeredRenderingPlugin(plugin);
        }
        if (!ship.getFluxTracker().isOverloadedOrVenting())
        {
            doArcs(ship, amount);
            if (ship.isPhased())
                doRepair(ship, amount);
            fixDeploymentTime(ship, amount);
        } else
        {
            float repair = repairMap.get(ship);
            repair = Math.max(repair - amount * MAX_STORED_ARMOR, 0);
            repairMap.put(ship, repair);
            float charge = chargeMap.get(ship);
            charge = Math.max(charge - amount * MAX_STORED_CHARGE, 0);
            chargeMap.put(ship, charge);
        }
        // show reactor/repair status
        if (ship == Global.getCombatEngine().getPlayerShip())
        {
            float storedDamage = chargeMap.get(ship);
            float storedRepair = repairMap.get(ship);
            Global.getCombatEngine().maintainStatusForPlayerShip(
                    "apex_excession_reactor",
                    "graphics/icons/buffs/apex_breachcore.png",
                    Global.getSettings().getHullModSpec("apex_excession_reactor").getDisplayName(),
                    text("excb4") + ": " + (int) (storedDamage / MAX_STORED_CHARGE * 100f) + "%" + " / " + text("excb13") +": " + (int) storedRepair,
                    false
            );

        }

        // trigger killswitch, if necessary
        // no, they're not giving you a supership without some precautions
        // triggers if enemy fleet is apex faction, and will not give a rep penalty on death
        BattleCreationContext context = Global.getCombatEngine().getContext();
        if (!Global.getCombatEngine().isSimulation() && context != null && context.getOtherFleet() != null && context.getOtherFleet().getFaction() != null)
        {
            // checks to see if no rep impact flag is set
            MemoryAPI mem = context.getOtherFleet().getMemoryWithoutUpdate();
            if (mem.contains(MemFlags.MEMORY_KEY_NO_REP_IMPACT) && mem.get(MemFlags.MEMORY_KEY_NO_REP_IMPACT) instanceof Boolean && (boolean)(mem.get(MemFlags.MEMORY_KEY_NO_REP_IMPACT)))
                return;
            if (context.getOtherFleet().getFaction().getId().equals("apex_design"))
            {
                Global.getCombatEngine().addFloatingText(
                        ship.getLocation(),
                        ApexModPlugin.Companion.xd("S2lsbHN3aXRjaCBBY3RpdmF0ZWQh"), // hiding text for funsies
                        40,
                        Color.RED,
                        ship,
                        0.5f,
                        6f);
                // you fool, you utter buffoon
                // did you really think they'd give you a ship that you could use against them
                ship.getMutableStats().getMaxSpeed().modifyMult("get owned", 0.33f);
                ship.getMutableStats().getFluxDissipation().modifyMult("you idiot", 0.25f);
                Global.getCombatEngine().applyDamage(
                        ship,
                        ship.getLocation(),
                        3000f,
                        DamageType.ENERGY,
                        10000f,
                        true,
                        false,
                        ship);
            }
        }
    }

    private void fixDeploymentTime(ShipAPI ship, float amount)
    {
        float dpTime = dpTimeMap.get(ship);
        if (dpTime < ship.getTimeDeployedForCRReduction())
        {
            dpTime += amount / ship.getMutableStats().getTimeMult().getModifiedValue();
            ship.setTimeDeployed(dpTime);
            dpTimeMap.put(ship, dpTime);
        }
    }

    private void doRepair(ShipAPI ship, float amount)
    {
        //System.out.println("did effect tick");
        CombatEngineAPI engine = Global.getCombatEngine();
        float timeMult = ship.getMutableStats().getTimeMult().getModifiedValue();
        amount *= timeMult; // first one is to bring it back to "normal" timeflow, second is to multiply it by timeflow.
        float repairThisFrame = Math.min(REPAIR_RATE * amount, repairMap.get(ship));
        if (repairThisFrame <= 0)
            return;

        ArmorGridAPI grid = ship.getArmorGrid();
        if (grid == null) return;
        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;
        float maxArmorInCell = grid.getMaxArmorInCell() * ApexArmorRepairHullmod.MAX_REGEN_FRACTION;

        // first, get number of cells missing armor
        int numCellsToRepair = 0;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                    numCellsToRepair++;
            }
        }
        if (numCellsToRepair == 0)
            return;

        // then, repair the cells

        float repairPerCell = repairThisFrame / (float) numCellsToRepair;
        float repairDoneThisFrame = 0f;
        for (int x = 0; x < gridWidth; x++)
        {
            for (int y = 0; y < gridHeight; y++)
            {
                if (grid.getArmorValue(x, y) < maxArmorInCell)
                {
                    repairDoneThisFrame += Math.min(repairPerCell, maxArmorInCell - grid.getArmorValue(x, y));
                    grid.setArmorValue(x, y, Math.min(grid.getArmorValue(x, y) + repairPerCell, maxArmorInCell));
                    /*if (!ApexModPlugin.POTATO_MODE)
                    {
                        Global.getCombatEngine().addSmokeParticle(
                                Vector2f.add(grid.getLocation(x, y), MathUtils.getRandomPointInCircle(Misc.ZERO, 12f), new Vector2f()),
                                Vector2f.add(target.getVelocity(),MathUtils.getRandomPointInCircle(Misc.ZERO, 15f), new Vector2f()),
                                Misc.random.nextFloat() * 5f,
                                0.75f,
                                0.66f,
                                Color.GREEN
                        );
                    }*/
                }
            }
        }

        if (repairDoneThisFrame > 0)
        {
            // only show rapid regen while phased, normal regen is pretty damn slow
            if (Misc.shouldShowDamageFloaty(ship, ship) && ship.isPhased())
            {
                engine.addFloatingDamageText(ship.getLocation(), repairDoneThisFrame, Color.GREEN, ship, ship);
            }
            repairMap.put(ship, Math.max(repairMap.get(ship) - repairDoneThisFrame, 0f));
            ship.syncWithArmorGridState();
        }
    }

    private void doArcs(ShipAPI ship, float amount)
    {

        // updates stored dps
        float storedDamage = chargeMap.get(ship);
        if (storedDamage < MAX_STORED_CHARGE)
        {
            float toStore = BASE_CHARGE_RATE * amount * (ship.isPhased() ? PHASE_CHARGE_MULT : 0f);
            storedDamage = storedDamage + toStore;
        }
        storedDamage = Math.min(storedDamage, MAX_STORED_CHARGE);

        if (!ship.isPhased() && ship.isAlive())
        {
            // bias arcs towards more dangerous threats - fighters and high-damage projectiles
            // this is not particularly efficient, but it should be pretty damn hard to get more than one of these things
            WeightedRandomPicker<CombatEntityAPI> targets = new WeightedRandomPicker<>();
            for (CombatEntityAPI entity : CombatUtils.getEntitiesWithinRange(ship.getLocation(), ARC_RANGE))
            {
                if (entity.getOwner() != ship.getOwner())
                {
                    if (entity instanceof DamagingProjectileAPI)
                    {
                        DamagingProjectileAPI proj = ((DamagingProjectileAPI) entity);
                        // grab emp-heavy projectiles too, even if they deal little damage
                        float damage = proj.getDamageAmount() + proj.getEmpAmount();
                        if (proj.getDamageType().equals(DamageType.FRAGMENTATION))
                            damage *= 0.125f;
                        else if (proj.getDamageType().equals(DamageType.KINETIC))
                            damage *= 0.25f;
                        else if (proj.getDamageType().equals(DamageType.ENERGY))
                            damage *= 0.5f;

                        if (damage >= DAMAGE_CUTOFF || storedDamage > MAX_STORED_CHARGE * 0.9f)
                        {
                            if (proj.getDamageType().equals(DamageType.FRAGMENTATION))
                                // not quite 1/4, because fuck you, that's why
                                targets.add(entity, proj.getDamageAmount() * 0.33f);
                            else
                                targets.add(entity, proj.getDamageAmount());
                        }
                    } else if (entity instanceof ShipAPI
                            && ((ShipAPI) entity).getHullSize().equals(ShipAPI.HullSize.FIGHTER)
                            && ((ShipAPI) entity).isAlive())
                    {
                        targets.add(entity, FIGHTER_WEIGHT);
                    }
                }
            }

            while (!targets.isEmpty() && storedDamage > 0f)
            {
                CombatEntityAPI target = targets.pickAndRemove();
                float value;
                if (target instanceof ShipAPI)
                    value = ARC_FIGHTER_DAMAGE;
                else
                    value = ((DamagingProjectileAPI) target).getDamageAmount();
                if (value < storedDamage)
                {
                    strike(target, ship);
                    storedDamage -= value;
                }
            }
        }
        // update after whatever we've done this frame
        chargeMap.put(ship, Math.max(storedDamage, 0f));
    }

    private void strike(CombatEntityAPI target, ShipAPI ship)
    {
        Vector2f origin = new Vector2f(arcOrigins.pick());
        VectorUtils.rotate(origin, ship.getFacing(), origin);
        Vector2f.add(ship.getLocation(), origin, origin);
        Vector2f to = new Vector2f(target.getLocation());
        /*Global.getCombatEngine().spawnEmpArc(
                ship,
                origin,
                ship,
                target,
                DamageType.ENERGY,
                ARC_FIGHTER_DAMAGE,
                ARC_FIGHTER_DAMAGE,
                999999f,
                "tachyon_lance_emp_arc_impact",
                5f,
                Color.WHITE,
                Color.MAGENTA
        );*/
        // draw removal effects
        ApexUtils.plasmaEffects(target, REMOVE_COLOR, Math.min(target.getCollisionRadius() * 2f, 10f));
        if (!POTATO_MODE)
        {
            ApexUtils.addWaveDistortion(target.getLocation(), 5f, 20f, 0.1f);
            Global.getCombatEngine().addHitParticle(
                    target.getLocation(),
                    target.getVelocity(),
                    100f,
                    1.0f,
                    0.1f,
                    Color.WHITE);
        }
        // spawn siphon particles
        float siphonAmount = ARC_SIPHON_AMOUNT;
        int numParticles = 10;
        if (target instanceof DamagingProjectileAPI)
        {
            DamagingProjectileAPI proj = (DamagingProjectileAPI) target;
            siphonAmount = proj.getDamageAmount() * 0.25f; //Math.min(proj.getDamageAmount() * 0.25f, siphonAmount);
            numParticles = Math.min((int)proj.getDamageAmount() / 20, numParticles);
        } else
        {
            //numParticles = 3;
            Global.getCombatEngine().applyDamage(
                    target,
                    target.getLocation(),
                    ARC_FIGHTER_DAMAGE,
                    DamageType.ENERGY,
                    ARC_FIGHTER_DAMAGE,
                    false,
                    false,
                    ship);
        }
        for (int i = 0; i < numParticles; i++)
        {
            Vector2f startVel = MathUtils.getRandomPointInCircle(Misc.ZERO, 100f);
            // haha, just write the whole fucking render plugin you dipshit
            // fuck you past me, I did
            pluginMap.get(ship).addTargetedParticle(to, startVel, ship, arcOrigins.pick(), REMOVE_COLOR);
        }

        repairMap.put(ship, Math.min(repairMap.get(ship) + siphonAmount, MAX_STORED_ARMOR));
        if (target instanceof DamagingProjectileAPI)
            Global.getCombatEngine().removeEntity(target);

    }
}