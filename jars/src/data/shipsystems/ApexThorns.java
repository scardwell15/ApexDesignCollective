package data.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.EnumSet;
import java.util.HashMap;

public class ApexThorns extends BaseShipSystemScript
{
    public static Object KEY_SHIP = new Object();
    public static final int MAX_PROJS = 20;
    public static final float DAMAGE_PER_PROJ = 400f;

    public static HashMap<ShipAPI.HullSize, Float> DR_MAP = new HashMap<>();
    static
    {
        DR_MAP.put(ShipAPI.HullSize.FRIGATE, 0.33f);
        DR_MAP.put(ShipAPI.HullSize.DESTROYER, 0.33f);
        DR_MAP.put(ShipAPI.HullSize.CRUISER, 0.5f);
        DR_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 0.5f);
    }

    private ApexThornsListener thornsListener;
    private WeaponAPI dummyWep;
    private float damageStored = 0;

    public class ApexThornsListener implements DamageTakenModifier
    {

        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit)
        {
            if (!(target instanceof ShipAPI) || shieldHit)
                return null;
            if (!((ShipAPI) target).getSystem().isActive())
                return null;
            if (damage.getDamage() <= 0)
                return null;
            if (param instanceof BeamAPI)
            {
                // beams seem to only deal damage every 5-6 frames, but the "damage" instance is the beam's full dps value
                // "good enough, lol"
                if (damage.getType().equals(DamageType.FRAGMENTATION))
                    damageStored += 0.25f * damage.getDamage() * 0.12f;
                else
                    damageStored += damage.getDamage() * 0.12f;
            } else
            {
                if (damage.getType().equals(DamageType.FRAGMENTATION))
                    damageStored += 0.25 * damage.getDamage();
                else
                    damageStored += damage.getDamage();
            }
            CombatEntityAPI source = null;
            if (param instanceof DamagingProjectileAPI)
                source = ((DamagingProjectileAPI) param).getSource();
            else if (param instanceof BeamAPI)
                source = ((BeamAPI) param).getSource();

            float angle = 0;
            if (source != null)
                angle = VectorUtils.getAngle(point, source.getLocation());
            else
                angle = VectorUtils.getAngle(target.getLocation(), point);
            return null;
        }
    }

    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel)
    {
        effectLevel = 1f;




        ShipAPI ship;
        if (stats.getEntity() instanceof ShipAPI)
        {
            ship = (ShipAPI) stats.getEntity();
            if (dummyWep == null)
                dummyWep = Global.getCombatEngine().createFakeWeapon(ship, "apex_thorn_wpn");
            ship.fadeToColor(KEY_SHIP, new Color(135, 75, 135, 255), 0.25f, 0.25f, effectLevel);
            ship.getEngineController().fadeToOtherColor(KEY_SHIP, new Color(0, 0, 0, 0), new Color(0, 0, 0, 0), effectLevel, 0.75f * effectLevel);
            ship.setJitterUnder(KEY_SHIP, new Color(255, 165, 255, 255), effectLevel, 15, 0f, 15f);

            if (!ship.hasListenerOfClass(ApexThornsListener.class))
            {
                thornsListener = new ApexThornsListener();
                ship.addListener(thornsListener);
            }
            float damMult = DR_MAP.get(ship.getHullSize());

            stats.getHullDamageTakenMult().modifyMult(id, 1f - (1f - damMult) * effectLevel);
            stats.getArmorDamageTakenMult().modifyMult(id, 1f - (1f - damMult) * effectLevel);
            stats.getEmpDamageTakenMult().modifyMult(id, 1f - (1f - damMult) * effectLevel);
        }


    }

    public void unapply(MutableShipStatsAPI stats, String id)
    {
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);
        stats.getEmpDamageTakenMult().unmodify(id);
        int projFired = 0;
        while (damageStored > DAMAGE_PER_PROJ && projFired < MAX_PROJS)
        {
            Vector2f spawnLoc = MathUtils.getRandomPointInCircle(stats.getEntity().getLocation(), stats.getEntity().getCollisionRadius());
            DamagingProjectileAPI newProj = (DamagingProjectileAPI) Global.getCombatEngine().spawnProjectile(
                    (ShipAPI)(stats.getEntity()),
                    dummyWep,
                    "apex_thorn_wpn",
                    spawnLoc,
                    Misc.random.nextFloat() * 360,
                    Misc.ZERO);
            MagicLensFlare.createSharpFlare(Global.getCombatEngine(),
                    (ShipAPI)(stats.getEntity()),
                    spawnLoc,
                    5f,
                    50f,
                    0f,
                    new Color(165, 88, 255),
                    new Color(165, 88, 255));
            //newProj.getVelocity().scale((Misc.random.nextFloat() - 0.5f) * VELOCITY_SPREAD);
            damageStored -= DAMAGE_PER_PROJ;
            projFired++;
        }

        damageStored = 0;
        // listener just turns off if the system isn't on
    }
}
