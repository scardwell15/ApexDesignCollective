package data.missions.apex_2afrigs;

import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;

import static data.ApexUtils.text;

public class MissionDefinition implements MissionDefinitionPlugin
{

    @Override
    public void defineMission(MissionDefinitionAPI api)
    {


        // Set up the fleets so we can add ships and fighter wings to them.
        // In this scenario, the fleets are attacking each other, but
        // in other scenarios, a fleet may be defending or trying to escape
        api.initFleet(FleetSide.PLAYER, text("mis1-1"), FleetGoal.ATTACK, false);
        api.initFleet(FleetSide.ENEMY, text("mis2-1"), FleetGoal.ATTACK, true);

        // Set a small blurb for each fleet that shows up on the mission detail and
        // mission results screens to identify each side.
        api.setFleetTagline(FleetSide.PLAYER, text("mis2-2"));
        api.setFleetTagline(FleetSide.ENEMY, text("mis2-3"));

        // These show up as items in the bulleted list under
        // "Tactical Objectives" on the mission detail screen
        api.addBriefingItem(text("mis2-4"));

        // Set up the player's fleet.  Variant names come from the
        // files in data/variants and data/variants/fighters

        for (int i = 0; i < 5; i++)
        {
            api.addToFleet(FleetSide.PLAYER, "apex_lacerta_attack", FleetMemberType.SHIP, text("mis2-5") + " " + (i + 181), i == 0);
        }
        // Mark both ships as essential - losing either one results
        // in mission failure. Could also be set on an enemy ship,
        // in which case destroying it would result in a win.
        api.defeatOnShipLoss(text("mis2-5") + " 181");

        // Set up the enemy fleet.
        api.addToFleet(FleetSide.ENEMY, "enforcer_Elite", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "vanguard_pirates_Attack", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "vanguard_pirates_Strike", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "wolf_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "centurion_Assault", FleetMemberType.SHIP, false);
        api.addToFleet(FleetSide.ENEMY, "brawler_Assault", FleetMemberType.SHIP, false);

        // Set up the map.
        float width = 11000f;
        float height = 11000f;
        api.initMap((float) -width / 2f, (float) width / 2f, (float) -height / 2f, (float) height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        // All the addXXX methods take a pair of coordinates followed by data for
        // whatever object is being added.

        // And a few random ones to spice up the playing field.
        // A similar approach can be used to randomize everything
        // else, including fleet composition.
        for (int i = 0; i < 7; i++)
        {
            float x = (float) Math.random() * width - width / 2;
            float y = (float) Math.random() * height - height / 2;
            float radius = 100f + (float) Math.random() * 800f;
            api.addNebula(x, y, radius);
        }
    }
}
