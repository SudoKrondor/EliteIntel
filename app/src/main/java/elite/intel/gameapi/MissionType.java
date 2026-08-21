package elite.intel.gameapi;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MissionType {
    // Fighting
    MISSION_PIRATE_MASSACRE("Mission_Massacre", "pirates, massacre, pirate, bounty, hunt, kill, combat, criminals, faction, wing"),
    MISSION_PIRATE_MASSACRE_WING("Mission_MassacreWing", "pirates, massacre, pirate, wing, group, squad, multiplayer, bounty, hunt, combat"),
    MISSION_ASSASSINATE_PLANETARY_EXPANSION("Mission_Assassinate_Planetary_Expansion", "assassinate, kill, expansion, contract, clandestine, planetary, target, stealth, hit, covert"),
    MISSION_PIRACY_ANARCHY("Mission_Piracy_Anarchy", "pirate, smuggle, piracy, anarchy, smuggling, black market"),
    MISSION_PIRATE_LORD("Mission_Assassinate_Legal_Corporate", "assassination, corporate, contract, hit, pirate lord, boss, corpo, high value target"),

    // Ranking missions
    MISSION_ASSASSINATE_RANKEMP("Mission_Assassinate_RankEmp", "assassination, contract, hit, empire, rank, promotion, rank up, imperial, wanted"),
    MISSION_ASSASSINATE_RANKFED("Mission_Assassinate_RankFed", "assassination, contract, hit, federation, rank, promotion, federal, wanted"),

    // Courier
    MISSION_COURIER("Mission_Courier", "courier, delivery, transport, data, package, commodity, goods"),
    MISSION_COURIER_ELECTIONS("Mission_Courier_Elections", "courier, delivery, elections, political, voting, campaign, data"),
    MISSION_COURIER_EXPANSION("Mission_Courier_Expansion", "courier, delivery, expansion, faction, grow, influence"),
    MISSION_COURIER_OUTBREAK("Mission_Courier_Outbreak", "courier, delivery, outbreak, medical, disease, epidemic, supplies"),
    MISSION_COURIER_SERVICE("Mission_Courier_Service", "courier, delivery, service, community, aid"),
    MISSION_DELIVERY("Mission_Delivery", "courier, delivery, transport, cargo, goods, package"),
    MISSION_COURIER_CIVILWAR("Mission_Courier_CivilWar", "courier, delivery, war, civil war, supplies, wartime"),

    // Passenger
    MISSION_PASSENGERBULK("Mission_PassengerBulk", "passenger, passengers, bulk, economy, transport, group"),
    MISSION_PASSENGERVIP("Mission_PassengerVIP", "passenger, passengers, vip, luxury, important, executive, elite"),
    MISSION_SIGHTSEEING("Mission_Sightseeing", "sightseeing, tourist, tourism, sighseeting, visit, beacon, poi, points of interest, tour"),
    MISSION_RESCUE_ELECTIONS("Mission_Rescue_Elections", "rescue, delivery, resource, elections, evacuate, voters, aid"),
    MISSION_LONGDISTANCEEXPEDITION_EXPLORER_BOOM("Mission_LongDistanceExpedition_Explorer_Boom", "expedition, explorer, passenger, tourism, long distance, boom, sightseeing"),
    MISSION_PASSENGERBULK_AIDWORKER_ARRIVING("Mission_PassengerBulk_AIDWORKER_ARRIVING", "aid, passenger, passengers, aidworker, humanitarian, arriving, relief, worker"),
    MISSION_PASSENGERVIP_TERRORIST_CIVILWAR("Mission_PassengerVIP_Terrorist_CIVILWAR", "passenger, passengers, vip, terrorist, civil war, extraction"),
    MISSION_SIGHTSEEING_CRIMINAL_CIVILWAR("Mission_Sightseeing_Criminal_CIVILWAR", "passenger, passengers, sightseeing, criminal, civil war"),
    MISSION_SIGHTSEEING_SCIENTIST_CIVILWAR("Mission_Sightseeing_Scientist_CIVILWAR", "passenger, passengers, sightseeing, scientist, civil war"),
    MISSION_SIGHTSEEING_TERRORIST_CIVILWAR("Mission_Sightseeing_Terrorist_CIVILWAR", "passenger, passengers, sightseeing, terrorist, civil war"),
    MISSION_SIGHTSEEING_TOURIST_BOOM("Mission_Sightseeing_Tourist_BOOM", "passenger, passengers, sightseeing, tourist, boom"),
    MISSION_SIGHTSEEING_WHISTLEBLOWER_ELECTION("Mission_Sightseeing_Whistleblower_ELECTION", "passenger, passengers, sightseeing, whistleblower, election"),
    MISSION_PASSENGERBULK_PRISONEROFWAR_LEAVING("Mission_PassengerBulk_PRISONEROFWAR_LEAVING", "passenger, passengers, prisonerofwar, pow, leaving, evacuate, prisoner"),

    // Altruism
    MISSION_ALTRUISM("Mission_Altruism", "donation, donations, altruism, credits, money, charity, contribute"),
    MISSION_ALTRUISMCREDITS("Mission_AltruismCredits", "donation, donations, altruism, credits, money, charity"),
    MISSION_ALTRUISMCREDITS_OUTBREAK("Mission_AltruismCredits_Outbreak", "donation, donations, altruism, credits, money, charity, outbreak, medical"),

    // Salvage
    MISSION_SALVAGE_ILLEGAL("Mission_Salvage_Illegal", "salvage, illegal, wreck, black market, cargo, debris, recovery"),
    MISSION_RESCUE_PLANET_CIVILWAR("Mission_Rescue_Planet_CivilWar", "rescue, evacuate, planet, civil war, passengers"),

    // Collecting
    MISSION_COLLECT_INDUSTRIAL("Mission_Collect_Industrial", "collection, industrial, collect, materials, retrieval, goods"),

    // Mining
    MISSION_MINING("Mission_Mining", "mining, resource, mine, extract, laser mining, core mining"),

    // OnFoot / Odyssey
    MISSION_ONFOOT_ONSLAUGHT_OFFLINE_002("Mission_OnFoot_Onslaught_Offline_002", "on foot, onslaught, massacre, raid, ground combat, offline, settlement"),
    MISSION_ONFOOT_SALVAGE_MB("Mission_OnFoot_Salvage_MB", "on foot, salvage, ground, settlement, recover, loot"),
    MISSION_ONFOOT_SALVAGEILLEGAL_MB("Mission_OnFoot_SalvageIllegal_MB", "on foot, illegal, salvage, black market, settlement, loot"),
    MISSION_HACK_BLOPS_BOOM("Mission_Hack_BLOPS_Boom", "hack, tech, hacking, data, breach, terminal, blops, covert"),
    MISSION_ONFOOT_ONSLAUGHTILLEGAL("Mission_OnFoot_OnslaughtIllegal", "onslaught, illegal, ground, massacre, raid, settlement"),
    MISSION_ONFOOT_HEIST_POI("Mission_OnFoot_Heist_POI", "heist, robbery, bank, bank job, steal, infiltration, poi, point of interest, theft"),
    MISSION_ONFOOT_ASSASSINATION_HARD("Mission_OnFoot_Assassination_Hard", "hit, contract, assassination, ground, on foot"),
    MISSION_ONFOOT_ASSASSINATION_COVERT("Mission_OnFoot_Assassination_Covert", "covert, hit, contract, assassination, stealth, ground, on foot"),
    MISSION_ONFOOT_MASSACREILLEGAL("Mission_OnFoot_MassacreIllegal", "massacre, illegal, murder, ground, on foot, raid"),
    MISSION_ONFOOT_ASSASSINATION("Mission_OnFoot_Assassination", "assassination, hit, contract, ground, on foot"),
    MISSION_ONFOOT_DELIVERY_CONTACT("Mission_OnFoot_Delivery_Contact", "delivery, courier, ground, contact, on foot"),
    MISSION_ONFOOT_ONSLAUGHT("Mission_OnFoot_Onslaught", "massacre, hit, onslaught, ground, raid, settlement"),
    // todo: verify ranking mission
    MISSION_COLLECT_RANKEMP("Mission_Collect_RankEmp", "collection, repossession, collect, rank, empire, retrieve"),
    MISSION_RESCUE_PLANET("Mission_Rescue_Planet", "rescue, evacuate, planet, passengers"),
    MISSION_SCAN("Mission_Scan", "scan, scanning, recon, reconnaissance, data, survey"),
    MISSION_ASSASSINATE_ILLEGAL_BLOPS_CIVILLIBERTY("Mission_assassinate_Illegal_BLOPS_CivilLiberty", "contract, hit, assassination, assassin, illegal, murder, blops, civil liberty"),
    MISSION_ASSASSINATE_PLANETARY("Mission_Assassinate_Planetary", "contract, hit, assassination, assassin, illegal, murder, planetary"),
    MISSION_ONFOOT_ASSASSINATION_MB("Mission_OnFoot_Assassination_MB", "contract, hit, assassination, assassin, illegal, murder, ground"),
    MISSION_ONFOOT_COLLECT_MB("Mission_OnFoot_Collect_MB", "collect, retrieval, on foot, ground, materials, repossession"),

    UNKNOWN("UnknownType", "*****");

    private final String missionType;
    private final String keywords;

    MissionType(String missionType, String keyWords) {
        this.missionType = missionType;
        this.keywords = keyWords;
    }

    public static MissionType getUnknown() {
        return UNKNOWN;
    }

    public String getMissionType() {
        return this.missionType;
    }

    /**
     * How this mission family is named to a commander. The constant name is an identifier, so
     * {@code @JsonValue} keeps it out of the YAML that LLM prompts are built from - a prompt
     * carrying MISSION_COLLECT_RANKEMP is a prompt that can be read back out loud.
     */
    @JsonValue
    public String label() {
        return MissionTitle.fromKey(missionType);
    }

    public String getKeywords() {
        return keywords;
    }
}