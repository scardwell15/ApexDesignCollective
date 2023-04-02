package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.HashMap;

import static data.ApexUtils.text;

public class ApexSpectrumCargo extends BaseHullMod
{

    public static final float CREW_MULT = 0.5f;

    public static final HashMap<ShipAPI.HullSize, Float> CARGO_MAP = new HashMap<>();
    static
    {
        CARGO_MAP.put(ShipAPI.HullSize.FRIGATE, 200f);
        CARGO_MAP.put(ShipAPI.HullSize.DESTROYER, 500f);
        CARGO_MAP.put(ShipAPI.HullSize.CRUISER, 1100f);
        CARGO_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 2300f);
    }

    public static final HashMap<ShipAPI.HullSize, Float> DEPLOY_MAP = new HashMap<>();
    static
    {
        DEPLOY_MAP.put(ShipAPI.HullSize.FRIGATE, 2f/4f);
        DEPLOY_MAP.put(ShipAPI.HullSize.DESTROYER, 4.5f/12f);
        DEPLOY_MAP.put(ShipAPI.HullSize.CRUISER, 9f/28f);
        DEPLOY_MAP.put(ShipAPI.HullSize.CAPITAL_SHIP, 18f/45f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id)
    {
        float cargomod = CARGO_MAP.get(hullSize);
        float deploymod = DEPLOY_MAP.get(hullSize);
        // this is a flat bonus, and expanded holds/tanks looks at base capacity when deciding whether to give % or flat bonus
        // this means that it will always give the flat bonus for these ships- which sucks
        // so instead we do some math to "undo" that flat bonus and actually give us the correct percent bonus
        if (!stats.getVariant().hasHullMod("expanded_cargo_holds"))
            stats.getCargoMod().modifyFlat(id, cargomod);
        else
            stats.getCargoMod().modifyFlat(id, (cargomod + 100f) * 1.3f - 200f);
        stats.getSuppliesPerMonth().modifyMult(id, deploymod);
        stats.getSuppliesToRecover().modifyMult(id, deploymod);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, deploymod);
        stats.getNumFighterBays().modifyMult(id, 0f);
        stats.getMinCrewMod().modifyMult(id, CREW_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id)
    {
        // just in case
        if (!ship.getHullSpec().getBaseHullId().equals("apex_spectrum") && !ship.getHullSpec().getBaseHullId().equals("apex_backscatter"))
            return;
        SpriteAPI sprite;
        String spriteId = ship.getHullSpec().getBaseHullId() + "_cargo";
        sprite = Global.getSettings().getSprite(ship.getHullSpec().getBaseHullId(), spriteId, false);

        if (sprite != null)
        {
            float x = ship.getSpriteAPI().getCenterX();
            float y = ship.getSpriteAPI().getCenterY();
            float alpha = ship.getSpriteAPI().getAlphaMult();
            float angle = ship.getSpriteAPI().getAngle();
            Color color = ship.getSpriteAPI().getColor();
            ship.setSprite(ship.getHullSpec().getBaseHullId(), spriteId);
            ship.getSpriteAPI().setCenter(x, y);
            ship.getSpriteAPI().setAlphaMult(alpha);
            ship.getSpriteAPI().setAngle(angle);
            ship.getSpriteAPI().setColor(color);
        }
    }

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize)
    {
        if (index == 0)
            return (CARGO_MAP.get(hullSize)).intValue() + "";
        if (index == 1)
            return (int) (100f - 100f * DEPLOY_MAP.get(hullSize)) + "%";
        if (index == 2)
            return (int)(100f - CREW_MULT * 100f) + "%";
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship)
    {
        if (ship == null)
            return false;
        if (ship.getVariant().hasHullMod("apex_spectrum_fuel"))
            return false;
        return (ship.getHullSpec().getBaseHullId().equals("apex_spectrum") || ship.getHullSpec().getBaseHullId().equals("apex_backscatter"));
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship)
    {
        if (ship == null)
            return "Ship does not exist, what the fuck";
        if (ship.getVariant().hasHullMod("apex_spectrum_fuel"))
            return text("conv1");
        return text("conv2");
    }
}
