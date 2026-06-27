package elite.intel.companion.prompt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measurement (report) for {@link CompanionWordMatch} across every language the app supports. Each language
 * lists 50+ word pairs from the command vocabulary: {@code same=true} pairs are the same word in two inflected
 * forms (must match), {@code same=false} pairs are different words (must not match). It prints per-language
 * accuracy and every wrong call, so we can see whether one budget covers all languages or a language needs its
 * own. English is shown for reference only - in production English uses exact matching, not this helper.
 */
class CompanionWordMatchLanguagesTest {

    private record Probe(String a, String b, boolean same) {}

    private static void p(List<Probe> out, boolean same, String... words) {
        // words are pairs flattened: a1,b1, a2,b2, ...
        for (int i = 0; i + 1 < words.length; i += 2) {
            out.add(new Probe(words[i], words[i + 1], same));
        }
    }

    /** Scores one language: counts correct calls, prints misses, asserts it ran. Returns accuracy 0..1. */
    private double report(String language, List<Probe> probes) {
        int correct = 0;
        StringBuilder misses = new StringBuilder();
        for (Probe probe : probes) {
            boolean got = CompanionWordMatch.similar(probe.a(), probe.b());
            if (got == probe.same()) {
                correct++;
            } else {
                misses.append(String.format("%n  WRONG %-22s ~ %-22s want=%s got=%s",
                        probe.a(), probe.b(), probe.same(), got));
            }
        }
        double accuracy = probes.isEmpty() ? 0 : (double) correct / probes.size();
        System.out.printf("%n==== %s : %d/%d (%.0f%%), %d probes ====%s%n",
                language, correct, probes.size(), accuracy * 100, probes.size(), misses);
        assertTrue(probes.size() >= 50, language + " must have at least 50 probes");
        return accuracy;
    }

    @Test
    void english() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "ship", "ships", "engine", "engines", "contact", "contacts", "shield", "shields",
                "target", "targets", "module", "modules", "weapon", "weapons", "panel", "panels",
                "route", "routes", "mission", "missions", "station", "stations", "scanner", "scanners",
                "light", "lights", "carrier", "carriers", "system", "systems", "planet", "planets",
                "fighter", "fighters", "material", "materials", "thruster", "thrusters", "hardpoint", "hardpoints",
                "commander", "commanders", "limpet", "limpets", "beacon", "beacons", "signal", "signals",
                "deploy", "deploys", "retract", "retracts", "navigate", "navigates", "scanning", "scanned");
        p(x, false,
                "ship", "shop", "target", "carpet", "light", "night", "panel", "pedal",
                "route", "robot", "star", "scar", "fuel", "full", "dock", "duck",
                "mine", "wine", "scan", "swan", "engine", "engineer", "navigate", "navigation",
                "contact", "contour", "module", "noodle");
        p(x, true,
                "scan", "scans", "jump", "jumps", "dock", "docks", "mine", "mines",
                "upgrade", "upgrades", "reading", "readings", "sensor", "sensors", "report", "reports");
        p(x, false,
                "jump", "pump", "dock", "deck", "mine", "mint", "scan", "scam");
        report("EN", x);
    }

    @Test
    void russian() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "навигация", "навигации", "навигация", "навигацию", "двигатель", "двигатели",
                "двигатель", "двигателя", "ведомый", "ведомого", "ведомый", "ведомому",
                "контакт", "контакты", "контакт", "контактов", "объявление", "объявления",
                "объявления", "объявлений", "инвентарь", "инвентаря", "авианосец", "авианосца",
                "авианосец", "авианосцем", "управление", "управления", "распределитель", "распределителя",
                "жизнеобеспечение", "жизнеобеспечения", "грузозаборник", "грузозаборника", "корабль", "корабля",
                "оружие", "оружия", "щит", "щиты", "станция", "станции", "маршрут", "маршруты",
                "миссия", "миссии", "планета", "планеты", "сканер", "сканеры", "топливо", "топлива",
                "товар", "товары", "истребитель", "истребители", "цель", "цели", "панель", "панели");
        p(x, false,
                "цель", "щель", "стоп", "стол", "торт", "порт", "ракета", "работа",
                "навигация", "навигатор", "контакты", "контейнер", "двигатели", "движение",
                "корабль", "корабел", "оружие", "орудие", "планета", "планка");
        p(x, true,
                "ракета", "ракеты", "ангар", "ангары", "реактор", "реакторы", "датчик", "датчики",
                "орбита", "орбиты", "экипаж", "экипажи", "броня", "брони", "шлюз", "шлюзы");
        p(x, false,
                "ангар", "анкер", "орбита", "обида", "реактор", "редактор", "датчик", "дельта");
        report("RU", x);
    }

    @Test
    void ukrainian() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "навігація", "навігації", "навігація", "навігацію", "двигун", "двигуни",
                "двигун", "двигуна", "контакт", "контакти", "контакт", "контактів",
                "оголошення", "оголошень", "авіаносець", "авіаносця", "авіаносець", "авіаносцем",
                "керування", "керуванням", "корабель", "корабля", "зброя", "зброї",
                "щит", "щити", "станція", "станції", "маршрут", "маршрути", "місія", "місії",
                "планета", "планети", "сканер", "сканери", "паливо", "палива", "товар", "товари",
                "винищувач", "винищувачі", "ціль", "цілі", "панель", "панелі", "ведений", "веденого",
                "система", "системи", "модуль", "модулі", "зірка", "зірки", "вантаж", "вантажу",
                "ринок", "ринку", "місток", "містка");
        p(x, false,
                "ціль", "щілина", "стоп", "стіл", "ракета", "робота", "корабель", "коралі",
                "планета", "планка", "контакти", "контейнер", "зброя", "збори",
                "двигуни", "двигтіння", "система", "сирена", "маршрут", "марка");
        p(x, true,
                "ракета", "ракети", "ангар", "ангари", "реактор", "реактори", "датчик", "датчики",
                "орбіта", "орбіти", "екіпаж", "екіпажі", "шлюз", "шлюзи", "сектор", "сектори");
        p(x, false,
                "ангар", "анкер", "орбіта", "образа", "реактор", "редактор", "сектор", "секрет");
        report("UK", x);
    }

    @Test
    void german() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "Schiff", "Schiffe", "Schiff", "Schiffes", "Triebwerk", "Triebwerke",
                "Karte", "Karten", "Waffe", "Waffen", "Schild", "Schilde",
                "Kontakt", "Kontakte", "Modul", "Module", "Station", "Stationen",
                "Planet", "Planeten", "Mission", "Missionen", "Route", "Routen",
                "Scanner", "Scannern", "Frachtraum", "Frachtraums", "Treibstoff", "Treibstoffs",
                "Antrieb", "Antriebe", "Panel", "Panels", "System", "Systeme",
                "Signal", "Signale", "Bake", "Baken", "Jäger", "Jägern",
                "Ziel", "Ziele", "ziele", "zielen", "ausfahren", "ausfährt",
                "Markt", "Märkte", "Fracht", "Frachten", "Material", "Materialien",
                "Sensor", "Sensoren", "Kommandant", "Kommandanten");
        p(x, false,
                "Schiff", "Schaff", "Ziel", "Zeil", "Karte", "Kerze", "Modul", "Nudel",
                "Route", "Robe", "Waffe", "Affe", "Station", "Statik", "Planet", "Platte",
                "Markt", "Mark", "Signal", "Single");
        p(x, true,
                "Rakete", "Raketen", "Sensor", "Sensoren", "Reaktor", "Reaktoren", "Tank", "Tanks",
                "Luke", "Luken", "Tor", "Tore", "Geschütz", "Geschütze", "Panzerung", "Panzerungen");
        p(x, false,
                "Tank", "Dank", "Tor", "Tier", "Luke", "Lupe", "Sensor", "Säbel");
        report("DE", x);
    }

    @Test
    void french() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "moteur", "moteurs", "contact", "contacts", "bouclier", "boucliers",
                "cible", "cibles", "module", "modules", "station", "stations",
                "mission", "missions", "route", "routes", "scanner", "scanners",
                "panneau", "panneaux", "vaisseau", "vaisseaux", "système", "systèmes",
                "planète", "planètes", "signal", "signaux", "arme", "armes",
                "marché", "marchés", "carburant", "carburants", "chasseur", "chasseurs",
                "navigation", "navigations", "matériau", "matériaux", "capteur", "capteurs",
                "porte", "portes", "cargaison", "cargaisons", "réacteur", "réacteurs",
                "cibler", "ciblez", "déployer", "déploie", "ouvrir", "ouvre",
                "commandant", "commandants", "balise", "balises", "trajet", "trajets");
        p(x, false,
                "moteur", "monteur", "cible", "sable", "route", "robot", "arme", "âme",
                "porte", "perte", "module", "moule", "signal", "cygne", "marché", "marche",
                "planète", "planche", "station", "potion");
        p(x, true,
                "fusée", "fusées", "réservoir", "réservoirs", "tourelle", "tourelles", "radar", "radars",
                "orbite", "orbites", "équipage", "équipages", "secteur", "secteurs", "drone", "drones");
        p(x, false,
                "fusée", "musée", "radar", "ravage", "orbite", "orbe", "secteur", "sécateur");
        report("FR", x);
    }

    @Test
    void spanish() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "motor", "motores", "contacto", "contactos", "escudo", "escudos",
                "objetivo", "objetivos", "módulo", "módulos", "estación", "estaciones",
                "misión", "misiones", "ruta", "rutas", "escáner", "escáneres",
                "panel", "paneles", "nave", "naves", "sistema", "sistemas",
                "planeta", "planetas", "señal", "señales", "arma", "armas",
                "mercado", "mercados", "combustible", "combustibles", "caza", "cazas",
                "navegación", "navegaciones", "material", "materiales", "sensor", "sensores",
                "puerta", "puertas", "carga", "cargas", "propulsor", "propulsores",
                "apuntar", "apunta", "desplegar", "despliega", "abrir", "abre",
                "comandante", "comandantes", "baliza", "balizas", "trayecto", "trayectos");
        p(x, false,
                "motor", "motel", "ruta", "rata", "arma", "alma", "nave", "nieve",
                "puerta", "puerto", "carga", "cargo", "panel", "papel", "señal", "sello",
                "planeta", "plancha", "escudo", "estudio");
        p(x, true,
                "cohete", "cohetes", "tanque", "tanques", "torreta", "torretas", "radar", "radares",
                "órbita", "órbitas", "tripulación", "tripulaciones", "sector", "sectores", "dron", "drones");
        p(x, false,
                "torreta", "tarjeta", "sector", "sermón", "órbita", "orla", "radar", "radio");
        report("ES", x);
    }

    @Test
    void portuguese() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "motor", "motores", "contato", "contatos", "escudo", "escudos",
                "alvo", "alvos", "módulo", "módulos", "estação", "estações",
                "missão", "missões", "rota", "rotas", "scanner", "scanners",
                "painel", "painéis", "nave", "naves", "sistema", "sistemas",
                "planeta", "planetas", "sinal", "sinais", "arma", "armas",
                "mercado", "mercados", "combustível", "combustíveis", "caça", "caças",
                "navegação", "navegações", "material", "materiais", "sensor", "sensores",
                "porta", "portas", "carga", "cargas", "propulsor", "propulsores",
                "mirar", "mira", "implantar", "implanta", "abrir", "abre",
                "comandante", "comandantes", "baliza", "balizas", "trajeto", "trajetos");
        p(x, false,
                "motor", "motel", "rota", "rato", "arma", "alma", "nave", "neve",
                "porta", "porto", "carga", "cargo", "painel", "papel", "sinal", "selo",
                "planeta", "prancha", "escudo", "estudo");
        p(x, true,
                "foguete", "foguetes", "tanque", "tanques", "torre", "torres", "radar", "radares",
                "órbita", "órbitas", "tripulação", "tripulações", "setor", "setores", "drone", "drones");
        p(x, false,
                "torre", "terra", "setor", "sermão", "órbita", "orla", "radar", "rádio");
        report("PT", x);
    }

    @Test
    void italian() {
        List<Probe> x = new ArrayList<>();
        p(x, true,
                "motore", "motori", "contatto", "contatti", "scudo", "scudi",
                "bersaglio", "bersagli", "modulo", "moduli", "stazione", "stazioni",
                "missione", "missioni", "rotta", "rotte", "scanner", "scanners",
                "pannello", "pannelli", "nave", "navi", "sistema", "sistemi",
                "pianeta", "pianeti", "segnale", "segnali", "arma", "armi",
                "mercato", "mercati", "carburante", "carburanti", "caccia", "caccie",
                "navigazione", "navigazioni", "materiale", "materiali", "sensore", "sensori",
                "porta", "porte", "carico", "carichi", "propulsore", "propulsori",
                "mirare", "mira", "schierare", "schiera", "aprire", "apre",
                "comandante", "comandanti", "boa", "boe", "tragitto", "tragitti");
        p(x, false,
                "motore", "monitore", "rotta", "ratto", "arma", "alma", "nave", "neve",
                "porta", "porto", "carico", "calore", "pannello", "panino", "segnale", "sigillo",
                "pianeta", "pinza", "scudo", "studio");
        p(x, true,
                "razzo", "razzi", "serbatoio", "serbatoi", "torretta", "torrette", "reattore", "reattori",
                "orbita", "orbite", "equipaggio", "equipaggi", "settore", "settori", "drone", "droni");
        p(x, false,
                "torretta", "torta", "settore", "sciatore", "orbita", "orbace", "razzo", "pazzo");
        report("IT", x);
    }
}
