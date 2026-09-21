-- Ship modules, in Spansh's own vocabulary from the stations search module filter.
--
-- English only, on purpose: Spansh matches a module name exactly and knows no other language, and the
-- game gives us no per-language module list to translate from. So the commander's spoken words are
-- fuzzy-matched against these English names and the winner is sent to Spansh as-is, exactly as the
-- commodity search does with its English column.
--
-- The Mk II passenger cabins appear under both "Mk II" and "MkII" because Spansh holds both spellings,
-- and a search sent with only one of them misses every station filed under the other.
CREATE TABLE ship_modules
(
    name TEXT NOT NULL PRIMARY KEY
);

INSERT INTO ship_modules (name)
VALUES ('AX Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('AX Multi-Cannon');
INSERT INTO ship_modules (name)
VALUES ('Abrasion Blaster');
INSERT INTO ship_modules (name)
VALUES ('Advanced Docking Computer');
INSERT INTO ship_modules (name)
VALUES ('Advanced Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Advanced Multi-Cannon');
INSERT INTO ship_modules (name)
VALUES ('Advanced Planetary Approach Suite');
INSERT INTO ship_modules (name)
VALUES ('Advanced Plasma Accelerator');
INSERT INTO ship_modules (name)
VALUES ('Auto Field-Maintenance Unit');
INSERT INTO ship_modules (name)
VALUES ('Balanced Power Distributor');
INSERT INTO ship_modules (name)
VALUES ('Beam Laser');
INSERT INTO ship_modules (name)
VALUES ('Bi-Weave Shield Generator');
INSERT INTO ship_modules (name)
VALUES ('Burst Laser');
INSERT INTO ship_modules (name)
VALUES ('Business Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Cannon');
INSERT INTO ship_modules (name)
VALUES ('Cargo Rack');
INSERT INTO ship_modules (name)
VALUES ('Cargo Scanner');
INSERT INTO ship_modules (name)
VALUES ('Caustic Sink Launcher');
INSERT INTO ship_modules (name)
VALUES ('Chaff Launcher');
INSERT INTO ship_modules (name)
VALUES ('Collector Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Concord Cannon');
INSERT INTO ship_modules (name)
VALUES ('Corrosion Resistant Cargo Rack');
INSERT INTO ship_modules (name)
VALUES ('Cytoscrambler Burst Laser');
INSERT INTO ship_modules (name)
VALUES ('Decontamination Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Detailed Surface Scanner');
INSERT INTO ship_modules (name)
VALUES ('Double Screaming Fragment Cannon');
INSERT INTO ship_modules (name)
VALUES ('Economy Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Electronic Countermeasure');
INSERT INTO ship_modules (name)
VALUES ('Enduring Feedback Rail Gun');
INSERT INTO ship_modules (name)
VALUES ('Enforcer Cannon');
INSERT INTO ship_modules (name)
VALUES ('Enhanced AX Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Enhanced AX Multi-Cannon');
INSERT INTO ship_modules (name)
VALUES ('Enhanced Performance Thrusters');
INSERT INTO ship_modules (name)
VALUES ('Enhanced Xeno Scanner');
INSERT INTO ship_modules (name)
VALUES ('Enzyme Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Experimental Weapon Stabiliser');
INSERT INTO ship_modules (name)
VALUES ('Exposing Missiles');
INSERT INTO ship_modules (name)
VALUES ('Extended Cargo Rack');
INSERT INTO ship_modules (name)
VALUES ('Far-Reaching Abrasion Blaster');
INSERT INTO ship_modules (name)
VALUES ('Fighter Hangar');
INSERT INTO ship_modules (name)
VALUES ('First Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Force Impact Cannon');
INSERT INTO ship_modules (name)
VALUES ('Fragment Cannon');
INSERT INTO ship_modules (name)
VALUES ('Frame Shift Drive');
INSERT INTO ship_modules (name)
VALUES ('Frame Shift Drive (SCO)');
INSERT INTO ship_modules (name)
VALUES ('Frame Shift Drive Interdictor');
INSERT INTO ship_modules (name)
VALUES ('Frame Shift Wake Scanner');
INSERT INTO ship_modules (name)
VALUES ('Fuel Scoop');
INSERT INTO ship_modules (name)
VALUES ('Fuel Tank');
INSERT INTO ship_modules (name)
VALUES ('Fuel Transfer Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Guardian FSD Booster');
INSERT INTO ship_modules (name)
VALUES ('Guardian Gauss Cannon');
INSERT INTO ship_modules (name)
VALUES ('Guardian Hull Reinforcement');
INSERT INTO ship_modules (name)
VALUES ('Guardian Hybrid Power Distributor');
INSERT INTO ship_modules (name)
VALUES ('Guardian Hybrid Power Plant');
INSERT INTO ship_modules (name)
VALUES ('Guardian Module Reinforcement');
INSERT INTO ship_modules (name)
VALUES ('Guardian Nanite Torpedo Pylon');
INSERT INTO ship_modules (name)
VALUES ('Guardian Plasma Charger');
INSERT INTO ship_modules (name)
VALUES ('Guardian Shard Cannon');
INSERT INTO ship_modules (name)
VALUES ('Guardian Shield Reinforcement');
INSERT INTO ship_modules (name)
VALUES ('Hatch Breaker Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Heat Sink Launcher');
INSERT INTO ship_modules (name)
VALUES ('Heavy Duty Module Reinforcement Package');
INSERT INTO ship_modules (name)
VALUES ('High-Yield Enzyme Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Hull Reinforcement Package');
INSERT INTO ship_modules (name)
VALUES ('Imperial Hammer Rail Gun');
INSERT INTO ship_modules (name)
VALUES ('Kill Warrant Scanner');
INSERT INTO ship_modules (name)
VALUES ('Large Planetary Vehicle Hangar');
INSERT INTO ship_modules (name)
VALUES ('Life Support');
INSERT INTO ship_modules (name)
VALUES ('Lightweight Alloy');
INSERT INTO ship_modules (name)
VALUES ('Lockdown Seeker Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Long Range Detailed Surface Scanner');
INSERT INTO ship_modules (name)
VALUES ('Long Range Mining Laser');
INSERT INTO ship_modules (name)
VALUES ('Luxury Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Meta Alloy Hull Reinforcement');
INSERT INTO ship_modules (name)
VALUES ('Military Grade Composite');
INSERT INTO ship_modules (name)
VALUES ('Mine Launcher');
INSERT INTO ship_modules (name)
VALUES ('Mining Lance');
INSERT INTO ship_modules (name)
VALUES ('Mining Laser');
INSERT INTO ship_modules (name)
VALUES ('Mining Multi Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Mining Volley Repeater');
INSERT INTO ship_modules (name)
VALUES ('Mirrored Surface Composite');
INSERT INTO ship_modules (name)
VALUES ('Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Mk II Ablative Lightweight Alloys');
INSERT INTO ship_modules (name)
VALUES ('Mk II Ablative Military Grade Composite');
INSERT INTO ship_modules (name)
VALUES ('Mk II Ablative Mirrored Surface Composite');
INSERT INTO ship_modules (name)
VALUES ('Mk II Ablative Reactive Surface Composite');
INSERT INTO ship_modules (name)
VALUES ('Mk II Ablative Reinforced Alloys');
INSERT INTO ship_modules (name)
VALUES ('Mk II Agile Boost Thrusters');
INSERT INTO ship_modules (name)
VALUES ('Mk II Business Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Mk II Cargo Rack');
INSERT INTO ship_modules (name)
VALUES ('Mk II Economy Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Mk II Gravity Optimised Thrusters');
INSERT INTO ship_modules (name)
VALUES ('Mk II Large Planetary Vehicle Hangar');
INSERT INTO ship_modules (name)
VALUES ('Mk II Mining Multi-Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Mk II Plasma Shock Accelerator');
INSERT INTO ship_modules (name)
VALUES ('Mk II Supercharge Optimised Frame Shift Drive (SCO)');
INSERT INTO ship_modules (name)
VALUES ('Mk II Vessel Hangar');
INSERT INTO ship_modules (name)
VALUES ('MkII Business Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('MkII Economy Class Passenger Cabin');
INSERT INTO ship_modules (name)
VALUES ('Module Reinforcement Package');
INSERT INTO ship_modules (name)
VALUES ('Multi-Cannon');
INSERT INTO ship_modules (name)
VALUES ('Operations Multi Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Overloaded Beam Laser');
INSERT INTO ship_modules (name)
VALUES ('Pacifier Frag-Cannon');
INSERT INTO ship_modules (name)
VALUES ('Pack-Hound Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Planetary Approach Suite');
INSERT INTO ship_modules (name)
VALUES ('Planetary Vehicle Hangar');
INSERT INTO ship_modules (name)
VALUES ('Plasma Accelerator');
INSERT INTO ship_modules (name)
VALUES ('Point Defence');
INSERT INTO ship_modules (name)
VALUES ('Power Distributor');
INSERT INTO ship_modules (name)
VALUES ('Power Plant');
INSERT INTO ship_modules (name)
VALUES ('Prismatic Shield Generator');
INSERT INTO ship_modules (name)
VALUES ('Prospector Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Pulse Disruptor Laser');
INSERT INTO ship_modules (name)
VALUES ('Pulse Laser');
INSERT INTO ship_modules (name)
VALUES ('Pulse Wave Analyser');
INSERT INTO ship_modules (name)
VALUES ('Pulse Wave Xeno Scanner');
INSERT INTO ship_modules (name)
VALUES ('Rail Gun');
INSERT INTO ship_modules (name)
VALUES ('Rapid Phase Multi-Cannon');
INSERT INTO ship_modules (name)
VALUES ('Reactive Surface Composite');
INSERT INTO ship_modules (name)
VALUES ('Recon Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Refinery');
INSERT INTO ship_modules (name)
VALUES ('Regenerative Burst Laser');
INSERT INTO ship_modules (name)
VALUES ('Reinforced Alloy');
INSERT INTO ship_modules (name)
VALUES ('Remote Release Flak Launcher');
INSERT INTO ship_modules (name)
VALUES ('Remote Release Flechette Launcher');
INSERT INTO ship_modules (name)
VALUES ('Repair Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Rescue Multi Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Research Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Retributor Beam Laser');
INSERT INTO ship_modules (name)
VALUES ('Rocket Propelled FSD Disruptor');
INSERT INTO ship_modules (name)
VALUES ('Seeker Missile Rack');
INSERT INTO ship_modules (name)
VALUES ('Seismic Charge Launcher');
INSERT INTO ship_modules (name)
VALUES ('Sensors');
INSERT INTO ship_modules (name)
VALUES ('Shield Booster');
INSERT INTO ship_modules (name)
VALUES ('Shield Cell Bank');
INSERT INTO ship_modules (name)
VALUES ('Shield Generator');
INSERT INTO ship_modules (name)
VALUES ('Shock Cannon');
INSERT INTO ship_modules (name)
VALUES ('Shock Mine Launcher');
INSERT INTO ship_modules (name)
VALUES ('Shutdown Field Neutraliser');
INSERT INTO ship_modules (name)
VALUES ('Standard Docking Computer');
INSERT INTO ship_modules (name)
VALUES ('Sub-Surface Displacement Missile');
INSERT INTO ship_modules (name)
VALUES ('Sub-Surface Extraction Missile');
INSERT INTO ship_modules (name)
VALUES ('Supercruise Assist');
INSERT INTO ship_modules (name)
VALUES ('Support Focused Power Distributor');
INSERT INTO ship_modules (name)
VALUES ('Thargoid Pulse Neutraliser');
INSERT INTO ship_modules (name)
VALUES ('Thrusters');
INSERT INTO ship_modules (name)
VALUES ('Torpedo Pylon');
INSERT INTO ship_modules (name)
VALUES ('Universal Multi Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Vessel Hangar');
INSERT INTO ship_modules (name)
VALUES ('Xeno Multi Limpet Controller');
INSERT INTO ship_modules (name)
VALUES ('Xeno Scanner');
