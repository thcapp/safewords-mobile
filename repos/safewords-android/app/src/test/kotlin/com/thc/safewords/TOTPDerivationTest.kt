package com.thc.safewords

import com.thc.safewords.crypto.TOTPDerivation
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests for TOTP derivation using frozen test vectors.
 *
 * These tests validate the core algorithm independently of Android,
 * using inline word lists (the same 197 adjectives and 300 nouns
 * that are bundled as JSON assets in the app).
 */
class TOTPDerivationTest {

    // Inline word lists for testing without Android context
    private val adjectives = listOf(
        "golden", "silver", "purple", "crimson", "coral",
        "amber", "scarlet", "cobalt", "ivory", "copper",
        "bronze", "teal", "maroon", "violet", "indigo",
        "emerald", "ruby", "sapphire", "rusty", "rosy",
        "sandy", "pearly", "dusty", "ashen", "olive",
        "cherry", "lemon", "walnut", "tiny", "giant",
        "round", "tall", "narrow", "broad", "vast",
        "compact", "massive", "petite", "sturdy", "lanky",
        "plump", "slender", "chunky", "bulky", "stubby",
        "lengthy", "steep", "hollow", "deep", "fuzzy",
        "smooth", "frozen", "warm", "bumpy", "silky",
        "crispy", "toasty", "frosty", "velvety", "grainy",
        "spongy", "gritty", "squishy", "fluffy", "prickly",
        "glossy", "rubbery", "chalky", "foamy", "crusty",
        "molten", "chilly", "balmy", "tepid", "brave",
        "gentle", "clever", "jolly", "merry", "humble",
        "eager", "witty", "bold", "cheerful", "daring",
        "faithful", "graceful", "honest", "kindly", "lively",
        "mighty", "noble", "playful", "polite", "proud",
        "quirky", "rowdy", "sneaky", "tender", "thrifty",
        "zesty", "peppy", "plucky", "snappy", "spunky",
        "steady", "trusty", "chipper", "bashful", "dapper",
        "feisty", "giddy", "grumpy", "hasty", "nimble",
        "perky", "patient", "restful", "silly", "timely",
        "tricky", "sunny", "stormy", "misty", "mossy",
        "windy", "cloudy", "breezy", "dewy", "foggy",
        "muddy", "rainy", "snowy", "tropical", "arctic",
        "coastal", "leafy", "thorny", "blooming", "grassy",
        "shady", "wooded", "alpine", "earthy", "lunar",
        "solar", "polar", "rustic", "gravel", "ancient",
        "modern", "rapid", "quiet", "loud", "lucky",
        "fancy", "simple", "classic", "cosmic", "electric",
        "magnetic", "vintage", "bubbly", "crunchy", "sparkly",
        "winding", "rugged", "hidden", "distant", "double",
        "triple", "junior", "super", "ultra", "mega",
        "turbo", "jumbo", "atomic", "digital", "wobbly",
        "tangy", "salty", "nutty", "roomy", "curly",
        "jagged", "savory", "toothy", "bendy", "frilly",
        "spotty", "striped", "twisted", "dotted", "braided",
        "knotty", "starry"
    )

    private val nouns = listOf(
        "penguin", "tiger", "dolphin", "falcon", "otter",
        "panda", "parrot", "bison", "cobra", "condor",
        "coyote", "cricket", "donkey", "eagle", "ferret",
        "flamingo", "gopher", "gorilla", "hamster", "heron",
        "husky", "iguana", "jaguar", "kitten", "lemur",
        "lizard", "lobster", "macaw", "mantis", "minnow",
        "moose", "narwhal", "osprey", "pelican", "puffin",
        "python", "rabbit", "raven", "salmon", "scorpion",
        "seahorse", "sparrow", "squid", "stingray", "toucan",
        "turtle", "walrus", "wombat", "zebra", "badger",
        "chipmunk", "gazelle", "gibbon", "panther", "starling",
        "urchin", "vulture", "taco", "mango", "waffle",
        "pretzel", "biscuit", "cashew", "cobbler", "coconut",
        "crumpet", "donut", "dumpling", "fig", "ginger",
        "gumdrop", "hazelnut", "kiwi", "lentil", "macaron",
        "muffin", "mustard", "noodle", "pancake", "papaya",
        "pecan", "pepper", "pumpkin", "raisin", "sorbet",
        "tamale", "truffle", "turnip", "yogurt", "almond",
        "apricot", "brioche", "cheddar", "churro", "granola",
        "lychee", "radish", "parsnip", "focaccia", "brisket",
        "burrito", "empanada", "mountain", "river", "canyon",
        "meadow", "glacier", "volcano", "forest", "desert",
        "island", "lagoon", "summit", "valley", "bamboo",
        "boulder", "cavern", "delta", "driftwood", "fossil",
        "geyser", "harbor", "iceberg", "jungle", "kelp",
        "marsh", "oasis", "pebble", "plateau", "prairie",
        "ravine", "reef", "ridge", "savanna", "tundra",
        "timber", "willow", "acorn", "birch", "cypress",
        "daisy", "fern", "hemlock", "jasmine", "juniper",
        "lotus", "maple", "orchid", "sequoia", "spruce",
        "thistle", "tulip", "lantern", "compass", "anchor",
        "puzzle", "basket", "beacon", "blanket", "bucket",
        "candle", "castle", "chisel", "cinder", "cushion",
        "fiddle", "funnel", "gadget", "garland", "goblet",
        "hammock", "helmet", "journal", "kayak", "ladder",
        "locket", "magnet", "marble", "mitten", "napkin",
        "paddle", "pendant", "pillow", "platter", "quiver",
        "ribbon", "saddle", "sandal", "satchel", "shovel",
        "slipper", "spindle", "stencil", "thimble", "trinket",
        "trophy", "tugboat", "umbrella", "vessel", "wagon",
        "whistle", "wrench", "zipper", "banner", "bonnet",
        "bugle", "cradle", "drawbridge", "emblem", "flagon",
        "gazebo", "mortar", "rampart", "scepter", "spyglass",
        "sundial", "tambourine", "turret", "windmill", "anvil",
        "comet", "nebula", "orbit", "crater", "cosmos",
        "eclipse", "galaxy", "meteor", "neutron", "photon",
        "plasma", "prism", "pulsar", "quasar", "rocket",
        "saturn", "signal", "spectrum", "starship", "sunspot",
        "vortex", "zenith", "quantum", "isotope", "proton",
        "neptune", "jupiter", "pluto", "cabin", "chapel",
        "cottage", "fountain", "lighthouse", "pavilion", "steeple",
        "terrace", "tunnel", "tower", "bridge", "cellar",
        "bunker", "attic", "depot", "hangar", "villa",
        "chalet", "tavern", "fortress", "banjo", "cello",
        "cymbal", "mandolin", "piano", "trumpet", "violin",
        "canvas", "fresco", "mosaic", "palette", "sonnet",
        "ballad", "anthem", "rhythm", "tempo", "mural",
        "sketch", "ditty", "jingle", "button", "kernel",
        "ratchet", "socket", "toggle", "piston", "bobbin",
        "cactus", "pigment", "pinecone", "cobweb", "pendulum"
    )

    @Before
    fun setUp() {
        assertEquals("Adjective list must have 197 entries", 197, adjectives.size)
        assertEquals("Noun list must have 300 entries", 300, nouns.size)
    }

    /**
     * Core derivation function that mirrors TOTPDerivation.deriveSafeword
     * but uses inline word lists (no Android context needed).
     */
    private fun deriveSafewordForTest(seedHex: String, interval: Int, timestamp: Long): String {
        val seed = hexToBytes(seedHex)
        val counter = timestamp / interval
        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(seed, "HmacSHA256"))
        val hash = mac.doFinal(counterBytes)

        val offset = hash[31].toInt() and 0x0F
        val adjIdx = ((hash[offset].toInt() and 0x7F) shl 8 or
                (hash[offset + 1].toInt() and 0xFF)) % 197
        val nounIdx = ((hash[offset + 2].toInt() and 0x7F) shl 8 or
                (hash[offset + 3].toInt() and 0xFF)) % 300
        val number = ((hash[offset + 4].toInt() and 0x7F) shl 8 or
                (hash[offset + 5].toInt() and 0xFF)) % 100

        return "${adjectives[adjIdx]} ${nouns[nounIdx]} $number"
    }

    private fun hexToBytes(hex: String): ByteArray {
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    // --- Test vectors ---

    @Test
    fun testVector1_daily_1741651200() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741651200
        )
        assertEquals("breezy rocket 75", result)
    }

    @Test
    fun testVector2_daily_1741737600() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741737600
        )
        assertEquals("proud lantern 98", result)
    }

    @Test
    fun testVector3_hourly_1741651200() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 3600,
            timestamp = 1741651200
        )
        assertEquals("misty tambourine 40", result)
    }

    @Test
    fun testVector4_hourly_1741654800() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 3600,
            timestamp = 1741654800
        )
        assertEquals("distant volcano 60", result)
    }

    @Test
    fun testVector5_weekly_1741651200() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 604800,
            timestamp = 1741651200
        )
        assertEquals("chalky ribbon 4", result)
    }

    @Test
    fun testVector6_monthly_1741651200() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 2592000,
            timestamp = 1741651200
        )
        assertEquals("salty bunker 34", result)
    }

    @Test
    fun testVector7_daily_epoch_zero() {
        val result = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 0
        )
        assertEquals("merry pulsar 18", result)
    }

    @Test
    fun testVector8_allFF_seed() {
        val result = deriveSafewordForTest(
            seedHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            interval = 86400,
            timestamp = 1741651200
        )
        assertEquals("toasty coyote 49", result)
    }

    // --- Utility tests ---

    @Test
    fun testHexToBytes() {
        val bytes = hexToBytes("0102030405")
        assertEquals(5, bytes.size)
        assertEquals(1, bytes[0].toInt())
        assertEquals(2, bytes[1].toInt())
        assertEquals(3, bytes[2].toInt())
        assertEquals(4, bytes[3].toInt())
        assertEquals(5, bytes[4].toInt())
    }

    @Test
    fun testHexToBytes_ff() {
        val bytes = hexToBytes("ff")
        assertEquals(1, bytes.size)
        assertEquals(-1, bytes[0].toInt()) // 0xFF as signed byte
    }

    @Test
    fun testCounterCalculation() {
        // Daily interval: 86400 seconds
        // Timestamp 1741651200 / 86400 = 20158
        val counter = 1741651200L / 86400
        assertEquals(20158L, counter)
    }

    @Test
    fun testSameTimestampInSameWindow() {
        // Two timestamps within the same daily window should produce the same phrase
        val phrase1 = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741651200
        )
        val phrase2 = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741651200 + 43200 // 12 hours later, same day
        )
        assertEquals(phrase1, phrase2)
    }

    @Test
    fun testDifferentWindowsDifferentPhrase() {
        // Two timestamps in different daily windows should (almost certainly) produce different phrases
        val phrase1 = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741651200
        )
        val phrase2 = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741737600
        )
        // These are known test vectors with different expected values
        assertEquals("breezy rocket 75", phrase1)
        assertEquals("proud lantern 98", phrase2)
        assert(phrase1 != phrase2)
    }

    @Test
    fun testDifferentSeedsDifferentPhrase() {
        val phrase1 = deriveSafewordForTest(
            seedHex = "0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
            interval = 86400,
            timestamp = 1741651200
        )
        val phrase2 = deriveSafewordForTest(
            seedHex = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
            interval = 86400,
            timestamp = 1741651200
        )
        assert(phrase1 != phrase2)
    }
}
